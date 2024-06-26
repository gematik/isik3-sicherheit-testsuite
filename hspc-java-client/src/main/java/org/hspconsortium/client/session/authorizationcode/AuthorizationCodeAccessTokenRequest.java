/*
 * #%L
 * hspc-client
 * %%
 * Copyright (C) 2014 - 2015 Healthcare Services Platform Consortium
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 *
 * Modifications copyright (C) 2024 gematik GmbH
 */

package org.hspconsortium.client.session.authorizationcode;

import org.apache.commons.lang3.Validate;
import org.hspconsortium.client.auth.access.AbstractAccessTokenRequest;
import org.hspconsortium.client.auth.access.AccessTokenGrantType;
import org.hspconsortium.client.auth.credentials.Credentials;
import org.hspconsortium.client.auth.credentials.JWTCredentials;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class AuthorizationCodeAccessTokenRequest<T extends Credentials>
        extends AbstractAccessTokenRequest<T> implements Serializable {

    private final Map<String, String> tokenRequestParams = new HashMap<>();

    public AuthorizationCodeAccessTokenRequest(String clientId, T clientCredentials, String authorizationCode, String redirectUri) {
        super(clientId, clientCredentials, AccessTokenGrantType.AUTHORIZATION_CODE);
        Validate.notNull(authorizationCode, "the authorizationCode must not be null");
        Validate.notNull(redirectUri, "the redirectUri must not be null");

        this.tokenRequestParams.put("code", authorizationCode);
        this.tokenRequestParams.put("redirect_uri", redirectUri);
        if (clientCredentials instanceof JWTCredentials) {
            this.tokenRequestParams.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
            this.tokenRequestParams.put("client_assertion", ((JWTCredentials) clientCredentials).getCredentials().serialize());
        }
    }

    @Override
    public Map<String, String> getAdditionalParameters() {
        return this.tokenRequestParams;
    }
}