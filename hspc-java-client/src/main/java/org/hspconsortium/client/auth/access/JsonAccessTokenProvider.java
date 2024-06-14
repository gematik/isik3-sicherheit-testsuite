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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.hspconsortium.client.auth.credentials.ClientSecretCredentials;
import org.hspconsortium.client.auth.credentials.Credentials;
import org.hspconsortium.client.auth.credentials.JWTCredentials;
import org.hspconsortium.client.auth.validation.IdTokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@AllArgsConstructor
@Slf4j
public class JsonAccessTokenProvider implements AccessTokenProvider<JsonAccessToken> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonAccessTokenProvider.class);

    private String proxyHost;
    private Integer proxyPort;
    private SSLContext sslContext;

    private final IdTokenValidator idTokenValidator = new IdTokenValidator.Impl();

    @Override
    public JsonAccessToken getAccessToken(String tokenEndpointUrl, AccessTokenRequest request) {
        String clientId = request.getClientId();
        Credentials<?> clientSecretCredentials = request.getCredentials();

        Map<String, String> paramPairs = request.getParameters();

        JsonObject rootResponse = post(tokenEndpointUrl, clientId, clientSecretCredentials, paramPairs);
        JsonAccessToken jsonAccessToken = buildAccessToken(rootResponse, null);

        String idToken = jsonAccessToken.getIdTokenStr();

        if (idToken != null) {
            //validate the id token
            boolean idTokenValidationSuccess = idTokenValidator.validate(jsonAccessToken.getIdToken(), tokenEndpointUrl, clientId);
            if (!idTokenValidationSuccess) {
                throw new RuntimeException("IdToken is not valid");
            }
        }

        return jsonAccessToken;
    }

    @Override
    public JsonAccessToken refreshAccessToken(String tokenEndpointUrl, AccessTokenRequest request, AccessToken accessToken) {
//        String clientId = request.getClientId();
//        Credentials<?> clientSecretCredentials = request.getCredentials();
//
//        JsonObject rootResponse = post(tokenEndpointUrl, clientId, clientSecretCredentials, accessToken.asNameValuePairList());
//        return buildAccessToken(rootResponse, new String[]{});
        throw new RuntimeException("Not implemented");
    }

    @Override
    @SneakyThrows
    public UserInfo getUserInfo(String userInfoEndpointUrl, JsonAccessToken jsonAccessToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(userInfoEndpointUrl))
                .header("Authorization", String.format("Bearer %s", jsonAccessToken.getValue()))
                .GET()
                .build();

        HttpClient.Builder builder = HttpClient.newBuilder();
        HttpResponse<String> response = builder
                .proxy(proxyPort == null ? ProxySelector.getDefault() : ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)))
                .connectTimeout(Duration.ofSeconds(60))
                .sslContext(sslContext)
                .version(HttpClient.Version.HTTP_1_1)
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();

        return buildUserInfo(jsonObject);
    }

    protected JsonAccessToken buildAccessToken(JsonObject rootResponse, String[] params) {
        return new JsonAccessToken(
                rootResponse,
                getResponseElement(AccessToken.ACCESS_TOKEN, rootResponse),
                getResponseElement(AccessToken.TOKEN_TYPE, rootResponse),
                getResponseElement(AccessToken.EXPIRES_IN, rootResponse),
                getResponseElement(AccessToken.SCOPE, rootResponse),
                getResponseElement(AccessToken.INTENT, rootResponse),
                getResponseElement(AccessToken.SMART_STYLE_URL, rootResponse),
                getResponseElement(AccessToken.PATIENT, rootResponse),
                getResponseElement(AccessToken.ENCOUNTER, rootResponse),
                getResponseElement(AccessToken.LOCATION, rootResponse),
                Boolean.parseBoolean(getResponseElement(AccessToken.NEED_PATIENT_BANNER, rootResponse)),
                getResponseElement(AccessToken.RESOURCE, rootResponse),
                getResponseElement(AccessToken.REFRESH_TOKEN, rootResponse),
                getResponseElement(AccessToken.ID_TOKEN, rootResponse)
        );
    }

    protected JsonUserInfo buildUserInfo(JsonObject rootResponse) {
        return new JsonUserInfo(
                rootResponse,
                getResponseElement(UserInfo.SUB, rootResponse),
                getResponseElement(UserInfo.NAME, rootResponse),
                getResponseElement(UserInfo.PREFERRED_USERNAME, rootResponse)
        );
    }

    @SneakyThrows
    protected JsonObject post(String serviceUrl, String clientId, Credentials clientCredentials, Map<String, String> transferParams) {
        var httpRequestBuilder = java.net.http.HttpRequest.newBuilder()
                .uri(new URI(serviceUrl))
                .header("Content-Type", "application/x-www-form-urlencoded");

        if (clientCredentials instanceof ClientSecretCredentials) {
            Object credentialsObj = clientCredentials.getCredentials();
            if (credentialsObj instanceof String credentialsStr) {
                if (StringUtils.isNotBlank(clientId) && StringUtils.isNotBlank(credentialsStr)) {
                    setAuthorizationHeader(httpRequestBuilder, clientId, credentialsStr);
                } else {
                    throw new RuntimeException("Confidential client authorization requires clientId and client secret.");
                }
            } else {
                throw new IllegalArgumentException("Credentials not supported");
            }
        } else if (clientCredentials instanceof JWTCredentials jwtCredentials) {
            jwtCredentials.setAudience(serviceUrl);
        } else {
            throw new IllegalArgumentException("Credentials type not supported");
        }

        httpRequestBuilder = httpRequestBuilder.POST(HttpRequest.BodyPublishers.ofString(transferParams.entrySet().stream()
                .map(p -> p.getKey() + "=" + p.getValue())
                .reduce((p1, p2) -> p1 + "&" + p2)
                .orElse("")));

        HttpResponse<String> response = HttpClient.newBuilder()
                .proxy(proxyPort == null ? ProxySelector.getDefault() : ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)))
                .connectTimeout(Duration.ofSeconds(60))
                .sslContext(sslContext)
                .version(HttpClient.Version.HTTP_1_1)
                .build()
                .send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        log.info("Authz server returned: " + response.body());

        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    protected static void setAuthorizationHeader(HttpRequest.Builder request, String clientId, String clientSecret) {
        String authHeader = String.format("%s:%s", clientId, clientSecret);
        String encoded = new String(org.apache.commons.codec.binary.Base64.encodeBase64(authHeader.getBytes()));
        request.header("Authorization", String.format("Basic %s", encoded));
    }

    protected String getResponseElement(String elementKey, JsonObject rootResponse) {
        JsonElement jsonElement = rootResponse.get(elementKey);
        if (jsonElement != null) {
            return jsonElement.getAsString();
        }
        return null;
    }

}
