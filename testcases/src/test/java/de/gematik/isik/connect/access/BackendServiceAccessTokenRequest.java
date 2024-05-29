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

import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hspconsortium.client.auth.Scopes;
import org.hspconsortium.client.auth.SimpleScope;
import org.hspconsortium.client.auth.access.JsonAccessTokenProvider;
import org.hspconsortium.client.auth.credentials.JWTCredentials;
import org.hspconsortium.client.controller.FhirEndpointsProvider;
import org.hspconsortium.client.session.ApacheHttpClientFactory;
import org.hspconsortium.client.session.clientcredentials.ClientCredentialsAccessTokenRequest;

import java.util.Arrays;
import java.util.UUID;

@Slf4j
public class BackendServiceAccessTokenRequest {

    @SneakyThrows
    public String requestAccessCode(String scopes) {
        String fhirServerUrl = TigerGlobalConfiguration.readString("isik.env.fhir-server-full-url");
        String clientId = TigerGlobalConfiguration.readString("user.authz-credentials.asym-client-id");
        String jwt = TigerGlobalConfiguration.readString("user.authz-credentials.asym-client-jwt-key");

        String proxyHost = System.getProperty("http.proxyHost");
        Integer proxyPort = System.getProperty("http.proxyPort") != null ? Integer.parseInt(System.getProperty("http.proxyPort")) : null;
        if(proxyHost != null && proxyPort != null)
            log.debug("Using proxy: {}:{}",proxyHost, proxyPort);

        FhirEndpointsProvider fhirEndpointsProvider = new SmartCapabilitiesBasedEndpointsProvider();
        var endpoints = fhirEndpointsProvider.getEndpoints(fhirServerUrl);

        JWKSet jwks = JWKSet.parse(jwt);
        RSAKey rsaKey = (RSAKey) jwks.getKeys().get(0);
        JWTCredentials jwtCredentials = new JWTCredentials(rsaKey.toRSAPrivateKey());

        jwtCredentials.setIssuer(clientId);
        jwtCredentials.setSubject(clientId);
        jwtCredentials.setAudience("http://localhost:8080/reference-server/r4");
        jwtCredentials.setTokenReference(UUID.randomUUID().toString());
        jwtCredentials.setDuration(60L*60*24); // 1 day

        Scopes requestedScopes = new Scopes();
        Arrays.stream(scopes.split(" ")).forEach(s -> requestedScopes.add(new SimpleScope(s)));
        var tokenRequest = new ClientCredentialsAccessTokenRequest<>(clientId, jwtCredentials, requestedScopes);

        var accessTokenProvider = new JsonAccessTokenProvider(new ApacheHttpClientFactory(proxyHost, proxyPort, null, null,
                IRestfulClientFactory.DEFAULT_CONNECT_TIMEOUT, IRestfulClientFactory.DEFAULT_CONNECTION_REQUEST_TIMEOUT));
        var accessToken = accessTokenProvider.getAccessToken(endpoints.getTokenEndpoint(), tokenRequest);
        log.info("Access token: {}", accessToken.getValue());

        return accessToken.getValue();
    }
}
