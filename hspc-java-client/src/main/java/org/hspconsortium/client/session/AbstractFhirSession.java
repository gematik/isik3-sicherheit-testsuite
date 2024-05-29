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

package org.hspconsortium.client.session;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.IInterceptorService;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RequestFormatParamStyleEnum;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.gclient.ICreate;
import ca.uhn.fhir.rest.gclient.IDelete;
import ca.uhn.fhir.rest.gclient.IFetchConformanceUntyped;
import ca.uhn.fhir.rest.gclient.IGetPage;
import ca.uhn.fhir.rest.gclient.IHistory;
import ca.uhn.fhir.rest.gclient.IMeta;
import ca.uhn.fhir.rest.gclient.IOperation;
import ca.uhn.fhir.rest.gclient.IPatch;
import ca.uhn.fhir.rest.gclient.IRead;
import ca.uhn.fhir.rest.gclient.ITransaction;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.rest.gclient.IUpdate;
import ca.uhn.fhir.rest.gclient.IValidate;
import lombok.NonNull;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hspconsortium.client.auth.access.AccessToken;
import org.hspconsortium.client.auth.access.AccessTokenProvider;
import org.hspconsortium.client.auth.access.AccessTokenRequest;
import org.hspconsortium.client.auth.access.JsonAccessToken;
import org.hspconsortium.client.auth.access.JsonAccessTokenProvider;
import org.hspconsortium.client.auth.access.UserInfo;

import java.io.IOException;
import java.util.Map;

public class AbstractFhirSession implements FhirSession {

    protected final FhirContext hapiFhirContext;
    protected final IGenericClient client;
    protected AccessToken accessToken;
    protected final AccessTokenRequest refreshTokenRequest;
    protected final String tokenEndpoint;
    protected final UserInfo userInfo;

    protected AccessTokenProvider accessTokenProvider;

    public AbstractFhirSession(FhirContext hapiFhirContext, String fhirServiceApi, AccessToken accessToken,
                               UserInfo userInfo, AccessTokenRequest refreshTokenRequest, String tokenEndpoint) {
        this.hapiFhirContext = hapiFhirContext;
        Validate.notNull(fhirServiceApi, "the fhirServiceApi must not be null");
        Validate.notNull(accessToken, "the accessToken must not be null");

        this.accessToken = accessToken;
        this.refreshTokenRequest = refreshTokenRequest;
        this.tokenEndpoint = tokenEndpoint;
        this.client = this.hapiFhirContext.newRestfulGenericClient(fhirServiceApi);
        if (refreshTokenRequest == null) {
            this.client.registerInterceptor(new BearerTokenAuthInterceptor(accessToken.getTokenValue()));
        } else {
            this.client.registerInterceptor(new AutoRefreshingBearerTokenAuthorizationHeaderInterceptor(30));
        }
        this.userInfo = userInfo;
    }

    public void setAccessTokenProvider(JsonAccessTokenProvider accessTokenProvider) {
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public AccessToken getAccessToken() {
        return accessToken;
    }

    @Override
    public String getIdTokenProfileClaim() {
        AccessToken accessToken = this.getAccessToken();
        if (accessToken instanceof JsonAccessToken) {
            Map<String, Object> claims = ((JsonAccessToken)accessToken).getIdToken().getClaimsMap();
            return (String)claims.get("profile");
        }
        return null;
    }

    @Override
    public FhirContext getFhirContext() {
        return this.hapiFhirContext;
    }

    @Override
    public IHttpClient getHttpClient() {
        return this.client.getHttpClient();
    }

    @Override
    public void setEncoding(EncodingEnum encodingEnum) {
        this.client.setEncoding(encodingEnum);
    }

    @Override
    public void setPrettyPrint(Boolean aBoolean) {
        this.client.setPrettyPrint(aBoolean);
    }

    @Override
    public String getServerBase() {
        return this.client.getServerBase();
    }

    @Override
    public IFetchConformanceUntyped capabilities() {
        return this.client.capabilities();
    }

    @Override
    public IFetchConformanceUntyped fetchConformance() {
        return this.client.fetchConformance();
    }

    @Override
    public ICreate create() {
        return this.client.create();
    }

    @Override
    public IDelete delete() {
        return this.client.delete();
    }

    @Override
    public void forceConformanceCheck() throws FhirClientConnectionException {
        this.client.forceConformanceCheck();
    }

    @Override
    public IHistory history() {
        return this.client.history();
    }

    @Override
    public IGetPage loadPage() {
        return this.client.loadPage();
    }

    @Override
    public IOperation operation() {
        return this.client.operation();
    }

    @Override
    public IRead read() {
        return this.client.read();
    }

    @Override
    public <T extends IBaseResource> T read(Class<T> tClass, String s) {
        return this.client.read(tClass, s);
    }

    @Override
    public <T extends IBaseResource> T read(Class<T> tClass, UriDt uriDt) {
        return this.client.read(tClass, uriDt);
    }

    @Override
    public IBaseResource read(UriDt uriDt) {
        return this.client.read(uriDt);
    }

    @Override
    public void registerInterceptor(Object theInterceptor) {
        this.client.registerInterceptor(theInterceptor);
    }

    @Override
    public IUntypedQuery search() {
        return this.client.search();
    }

    @Override
    public void setLogRequestAndResponse(boolean b) {
        this.client.setLogRequestAndResponse(b);
    }

    @Override
    public ITransaction transaction() {
        return this.client.transaction();
    }

    @Override
    public void unregisterInterceptor(Object theInterceptor) {
        this.client.unregisterInterceptor(theInterceptor);
    }

    @Override
    public void setFormatParamStyle(RequestFormatParamStyleEnum theRequestFormatParamStyle) {
        this.client.setFormatParamStyle(theRequestFormatParamStyle);
    }

    @Override
    public IValidate validate() {
        return this.client.validate();
    }

    @Override
    public IUpdate update() {
        return this.client.update();
    }

    @Override
    public MethodOutcome update(IdDt theId, IBaseResource theResource) {
        return client.update(theId, theResource);
    }

    @Override
    public MethodOutcome update(String theId, IBaseResource theResource) {
        return client.update(theId, theResource);
    }

    @Override
    public MethodOutcome validate(IBaseResource theResource) {
        return client.validate(theResource);
    }

    @Override
    public <T extends IBaseResource> T vread(Class<T> tClass, IdDt idDt) {
        return this.client.vread(tClass, idDt);
    }

    @Override
    public <T extends IBaseResource> T vread(Class<T> tClass, String s, String s1) {
        return this.client.vread(tClass, s, s1);
    }

    @Override
    public IInterceptorService getInterceptorService() {
        return this.client.getInterceptorService();
    }

    @Override
    public void setInterceptorService(@NonNull IInterceptorService theInterceptorService) {
        this.client.setInterceptorService(theInterceptorService);
    }

    @Override
    public <T extends IBaseResource> T fetchResourceFromUrl(Class<T> aClass, String s) {
        return this.client.fetchResourceFromUrl(aClass, s);
    }

    @Override
    public EncodingEnum getEncoding() {
        return this.client.getEncoding();
    }

    @Override
    public IPatch patch() {
        return this.client.patch();
    }

    @Override
    public void setSummary(SummaryEnum theSummary) {
        this.client.setSummary(theSummary);
    }

    @Override
    public IMeta meta() {
        return this.client.meta();
    }



    private class AutoRefreshingBearerTokenAuthorizationHeaderInterceptor implements IClientInterceptor {

        private final int refreshThreshold;

        public AutoRefreshingBearerTokenAuthorizationHeaderInterceptor(int refreshThreshold) {
            this.refreshThreshold = refreshThreshold;
        }

        @Override
        public void interceptRequest(IHttpRequest theRequest) {
//            if ((accessToken.getExpiresAt() <= refreshThreshold)) {
//                accessToken = accessTokenProvider.refreshAccessToken(tokenEndpoint, refreshTokenRequest, accessToken);
//            }
            theRequest.addHeader("Authorization", String.format("Bearer %s", accessToken.getTokenValue()));
        }

        @Override
        public void interceptResponse(IHttpResponse theResponse) throws IOException {

        }

    }
}
