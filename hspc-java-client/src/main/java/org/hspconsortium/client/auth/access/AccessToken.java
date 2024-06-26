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

import com.google.gson.JsonObject;
import org.apache.http.NameValuePair;
import org.springframework.security.oauth2.core.OAuth2Token;

import java.io.Serializable;
import java.util.List;

public interface AccessToken extends OAuth2Token, Serializable {
    String ACCESS_TOKEN = "access_token";
    String TOKEN_TYPE = "token_type";
    String EXPIRES_IN = "expires_in";
    String REFRESH_TOKEN = "refresh_token";
    String SCOPE = "scope";
    String PATIENT = "patient";
    String ENCOUNTER = "encounter";
    String LOCATION = "location";
    String NEED_PATIENT_BANNER = "need_patient_banner";
    String RESOURCE = "resource";
    String INTENT = "intent";
    String SMART_STYLE_URL = "smart_style_url";
    String ID_TOKEN = "id_token";

    JsonObject getRootResponse();

    String getIntent();

    String getSmartStyleUrl();

    String getEncounterId();

    String getPatientId();

    String getLocationId();

    boolean needPatientBanner();

    String getResource();

    String getIdTokenStr();

    List<NameValuePair> asNameValuePairList();
}
