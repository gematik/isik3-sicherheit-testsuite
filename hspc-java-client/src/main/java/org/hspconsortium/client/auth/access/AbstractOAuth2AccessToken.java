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
package org.hspconsortium.client.auth.access;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

abstract public class AbstractOAuth2AccessToken implements Serializable, AccessToken {
    protected OAuth2AccessTokenResponse oAuth2AccessToken;

    public AbstractOAuth2AccessToken(String accessToken, String tokenType,
                                     String expires, String scope, String refreshToken, final String idToken) {
        Validate.notNull(accessToken, "AccessToken must not be null");
        Validate.notNull(tokenType, "TokenType must not be null");
        Validate.notNull(scope, "Scope must not be null");

        this.oAuth2AccessToken = OAuth2AccessTokenResponse
                .withToken(accessToken)
                .tokenType(OAuth2AccessToken.TokenType.BEARER)
                .expiresIn(expires != null ? Integer.parseInt(expires) : 0)
                .scopes(createScopeSet(scope))
                .refreshToken(refreshToken)
                .additionalParameters(new HashMap<>() {{
                    put(AccessToken.ID_TOKEN, idToken);
                }}).build();
    }

    private Set<String> createScopeSet(String scope) {
        String[] scopeArray = StringUtils.split(scope, " ");
        return new HashSet<>(Arrays.asList(scopeArray));
    }

    @Override
    public String getTokenValue() {
        return oAuth2AccessToken.getAccessToken().getTokenValue();
    }

    public String getValue() {
        return getTokenValue();
    }

    public String getIdTokenStr() {
        return (String)oAuth2AccessToken.getAdditionalParameters().getOrDefault(AccessToken.ID_TOKEN,null);
    }

    public IdToken getIdToken() {
        return new IdToken(getIdTokenStr());
    }

}
