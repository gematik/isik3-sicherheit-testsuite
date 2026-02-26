/*
Copyright 2026 gematik GmbH

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

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.test.tiger.lib.TigerDirector;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hspconsortium.client.auth.Scopes;
import org.hspconsortium.client.auth.SimpleScope;
import org.hspconsortium.client.auth.access.JsonAccessTokenProvider;
import org.hspconsortium.client.controller.FhirEndpointsProvider;
import org.hspconsortium.client.session.clientcredentials.ClientCredentialsAccessTokenRequest;

import javax.net.ssl.SSLContext;
import java.util.Arrays;

@Slf4j
public class BackendServiceAccessTokenRequest {

    private final TigerConfigurationBasedJwtCredentialsProvider jwtCredentialsProvider = new TigerConfigurationBasedJwtCredentialsProvider();

    @SneakyThrows
    public String requestAccessCode(String scopes) {
        String fhirServerUrl = TigerGlobalConfiguration.readString("isik.env.fhir-server-full-url");
        String clientId = TigerGlobalConfiguration.readString("user.authz-credentials.asym-client-id");
        String jwt = TigerGlobalConfiguration.readString("user.authz-credentials.asym-client-jwt-key");
        var tigerProxy = TigerDirector.getTigerTestEnvMgr()
                .getLocalTigerProxyOptional().get();
        SSLContext sslContext = tigerProxy.getConfiguredTigerProxySslContext();

        var tigerProxyHost = "localhost";
        var tigerProxyPort = TigerGlobalConfiguration.readIntegerOptional("tiger.tigerProxy.proxyPort").get();

        FhirEndpointsProvider fhirEndpointsProvider = new SmartCapabilitiesBasedEndpointsProvider(tigerProxyHost, tigerProxyPort, sslContext);
        var endpoints = fhirEndpointsProvider.getEndpoints(fhirServerUrl);
        String tokenEndpoint = endpoints.getTokenEndpoint();
        var jwtCredentials = jwtCredentialsProvider.generateFor(tokenEndpoint);

        Scopes requestedScopes = new Scopes();
        Arrays.stream(scopes.split(" ")).forEach(s -> requestedScopes.add(new SimpleScope(s)));
        var tokenRequest = new ClientCredentialsAccessTokenRequest<>(clientId, jwtCredentials, requestedScopes);
        var accessTokenProvider = new JsonAccessTokenProvider(tigerProxyHost, tigerProxyPort, sslContext);
        var accessToken = accessTokenProvider.getAccessToken(tokenEndpoint, tokenRequest);
        log.info("Access token: {}", accessToken.getValue());

        return accessToken.getValue();
    }
}
