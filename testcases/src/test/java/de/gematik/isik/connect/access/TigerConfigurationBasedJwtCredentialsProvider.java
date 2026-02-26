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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import org.hspconsortium.client.auth.credentials.JWTCredentials;

import java.text.ParseException;
import java.util.UUID;

public class TigerConfigurationBasedJwtCredentialsProvider {

    public JWTCredentials generateFor(String aud) throws ParseException, JOSEException {
        String jwt = TigerGlobalConfiguration.readString("user.authz-credentials.asym-client-jwt-key");
        String clientId = TigerGlobalConfiguration.readString("user.authz-credentials.asym-client-id");

        JWKSet jwks = JWKSet.parse(jwt);
        RSAKey rsaKey = (RSAKey) jwks.getKeys().get(0);
        JWTCredentials jwtCredentials = new JWTCredentials(rsaKey.toRSAPrivateKey());

        jwtCredentials.setIssuer(clientId);
        jwtCredentials.setSubject(clientId);
        jwtCredentials.setAudience(aud);
        jwtCredentials.setTokenReference(UUID.randomUUID().toString());
        jwtCredentials.setDuration(60L*60*24);

        return jwtCredentials;
    }
}
