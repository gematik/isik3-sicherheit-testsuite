/*
Copyright 2024 gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package de.gematik.isik.connect.access;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import de.gematik.isik.connect.browser.BrowserOpener;
import de.gematik.isik.connect.browser.DebugAutomatedBrowserOpener;
import de.gematik.isik.connect.browser.DesktopBrowserOpener;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.test.tiger.lib.rbel.RbelMessageValidator;
import de.gematik.test.tiger.lib.rbel.RequestParameter;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hspconsortium.client.auth.StateProvider;
import org.hspconsortium.client.auth.access.AccessToken;
import org.hspconsortium.client.auth.access.JsonAccessTokenProvider;
import org.hspconsortium.client.auth.authorizationcode.AuthorizationCodeRequest;
import org.hspconsortium.client.auth.authorizationcode.AuthorizationCodeRequestBuilder;
import org.hspconsortium.client.auth.credentials.ClientSecretCredentials;
import org.hspconsortium.client.session.ApacheHttpClientFactory;
import org.hspconsortium.client.session.SessionKeyRegistry;
import org.hspconsortium.client.session.authorizationcode.AuthorizationCodeAccessTokenRequest;
import org.hspconsortium.client.session.authorizationcode.AuthorizationCodeSessionFactory;
import org.hspconsortium.client.session.impl.SimpleFhirSessionContextHolder;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
public class SMARTAccessCodeRequest {
    public static final String SAMPLE_CONFIDENTIAL_CLIENT_ID = "SAMPLE_CONFIDENTIAL_CLIENT_ID";
    public static final ClientSecretCredentials SAMPLE_CONFIDENTIAL_CLIENT_SECRET = new ClientSecretCredentials("SAMPLE_CONFIDENTIAL_CLIENT_SECRET");
    private final AuthorizationCodeSessionFactory<ClientSecretCredentials> authorizationCodeSessionFactory;
    private AuthorizationCodeRequest authorizationCodeRequest;
    @Getter
    private String accessCode;
    private final JsonAccessTokenProvider accessTokenProvider;
    private String redirectUrl;
    private final Integer tigerProxyPort;
    private final String tigerProxyHost;

    public SMARTAccessCodeRequest() {
        tigerProxyHost = "localhost";
        tigerProxyPort = TigerGlobalConfiguration.readIntegerOptional("tiger.tigerProxy.proxyPort").get();
        authorizationCodeSessionFactory = createAuthorizationCodeSessionFactory(FhirContext.forR4());
        accessTokenProvider = new JsonAccessTokenProvider(new ApacheHttpClientFactory(tigerProxyHost, tigerProxyPort, null, null,
                IRestfulClientFactory.DEFAULT_CONNECT_TIMEOUT, IRestfulClientFactory.DEFAULT_CONNECTION_REQUEST_TIMEOUT));
    }

    @SneakyThrows
    public String requestAccessCodeFor(String scopes, String patientId, String encounterId) {
        var fhirServerUrl = TigerGlobalConfiguration.readString("isik.env.fhir-server-full-url");

        var redirectUrl = String.format("http://%s:%s/auth-server-response", "localhost", tigerProxyPort);
        var session = createSession(fhirServerUrl, redirectUrl, scopes);

        var requestUrl = buildAuthorizationCodeRequestUrl();

        performGetRequestOnUrlToLogItInTigerProxy(requestUrl);

        var debugAutomatedOptional = TigerGlobalConfiguration.readBooleanOptional("isik.env.debug_auth_server_automation");
        var debugIsAuthServerAutomated = !debugAutomatedOptional.isEmpty() && debugAutomatedOptional.get();
        BrowserOpener opener = debugIsAuthServerAutomated ? new DebugAutomatedBrowserOpener(patientId, encounterId) : new DesktopBrowserOpener();
        opener.open(requestUrl);

        var validator = RbelMessageValidator.instance;
        RequestParameter requestParameter = RequestParameter.builder()
                .path("/auth-server-response")
                .startFromLastRequest(true)
                .build();
        var element = validator.waitForMessageToBePresent(requestParameter);

        log.info("Response from authorization server received...");

        var authzCode = element.findElement("$.path.code.value");
        if (authzCode.isEmpty()) {
            throw new IllegalArgumentException("No authorization code received");
        }
        var sessionId = element.findElement("$.path.state.value");
        if (sessionId.isEmpty()) {
            throw new IllegalArgumentException("No session passed in callback URL params");
        }
        if(!session.equals(sessionId.get().getRawStringContent()))
            throw new IllegalArgumentException("Wrong session id returned");

        log.info("Authorization code: " + authzCode);
        return exchangeForAccessCode(authzCode.get().getRawStringContent());
    }

    @SneakyThrows
    private void performGetRequestOnUrlToLogItInTigerProxy(String requestUrl) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(requestUrl))
                .GET()
                .build();

        HttpClient.newBuilder()
                .proxy(ProxySelector.of(new InetSocketAddress(tigerProxyHost, tigerProxyPort)))
                .connectTimeout(Duration.ofSeconds(60))
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());
    }


    private String createSession(String fhirServerUrl, String redirectUrl, String scopes) {
        this.redirectUrl = redirectUrl;
        authorizationCodeRequest = createAuthorizationCodeRequest(fhirServerUrl, scopes);
        // remember the fhirSessionContext based on the state (for request-callback association)
        authorizationCodeSessionFactory.registerInContext(authorizationCodeRequest.getOauthState(), authorizationCodeRequest);
        return authorizationCodeRequest.getOauthState();
    }

    private String buildAuthorizationCodeRequestUrl() {
        return authorizationCodeRequest.getFhirEndpoints().getAuthorizationEndpoint() +
                "?client_id=" + authorizationCodeRequest.getClientId() +
                "&response_type=" + authorizationCodeRequest.getResponseType() +
                "&scope=" + URLEncoder.encode(authorizationCodeRequest.getScopes().asParamValue(), StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(authorizationCodeRequest.getRedirectUri(), StandardCharsets.UTF_8) +
                "&aud=" + URLEncoder.encode(authorizationCodeRequest.getFhirEndpoints().getFhirServiceApi(), StandardCharsets.UTF_8) +
                "&state=" + authorizationCodeRequest.getOauthState();
    }

    private AuthorizationCodeRequest createAuthorizationCodeRequest(String fhirServerUrl, String scopes) {
        AuthorizationCodeRequestBuilder authorizationCodeRequestBuilder = new AuthorizationCodeRequestBuilder(new SmartCapabilitiesBasedEndpointsProvider(tigerProxyHost, tigerProxyPort),
                new StateProvider.DefaultStateProvider()
        );
        return authorizationCodeRequestBuilder
                .buildStandAloneAuthorizationCodeRequest(
                        fhirServerUrl,
                        SAMPLE_CONFIDENTIAL_CLIENT_ID,
                        scopes,
                        redirectUrl);
    }

    private AuthorizationCodeSessionFactory<ClientSecretCredentials> createAuthorizationCodeSessionFactory(FhirContext fhirContext) {
        return new AuthorizationCodeSessionFactory<>(
                fhirContext,
                new SessionKeyRegistry(),
                "MySessionKey",
                new SimpleFhirSessionContextHolder(),
                accessTokenProvider,
                SAMPLE_CONFIDENTIAL_CLIENT_ID,
                SAMPLE_CONFIDENTIAL_CLIENT_SECRET,
                "http://example.org/redirectUri"
        );
    }

    private String exchangeForAccessCode(String authzCode) {
        log.info("Exchanging authorization code against an access code...");
        String tokenEndpoint = authorizationCodeRequest.getFhirEndpoints().getTokenEndpoint();
        AuthorizationCodeAccessTokenRequest<ClientSecretCredentials> authorizationCodeAccessTokenRequest =
                new AuthorizationCodeAccessTokenRequest<>(SAMPLE_CONFIDENTIAL_CLIENT_ID, SAMPLE_CONFIDENTIAL_CLIENT_SECRET, authzCode, redirectUrl);

        AccessToken accessToken = accessTokenProvider.getAccessToken(tokenEndpoint, authorizationCodeAccessTokenRequest);

        return accessToken.getTokenValue();
    }
}
