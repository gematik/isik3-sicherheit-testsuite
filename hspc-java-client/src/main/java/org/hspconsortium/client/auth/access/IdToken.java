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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.Validate;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IdToken implements Serializable {

    String token;

    String joseHeader;

    String claims;

    Map<String, Object> claimsMap;

    String idSignature;

    public IdToken(String token) {
        Validate.notNull(token);

        this.token = token;
        String[] idTokenSegments = token.split("\\.");

        // todo support encryption

        org.apache.commons.codec.binary.Base64 decoder = new org.apache.commons.codec.binary.Base64(true);
        byte[] bytes0 = decoder.decode(idTokenSegments[0]);
        joseHeader = new String(bytes0);

        byte[] bytes1 = decoder.decode(idTokenSegments[1]);
        claims = new String(bytes1);
        try {
            claimsMap = new ObjectMapper().readValue(claims, HashMap.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] bytes2 = decoder.decode(idTokenSegments[2]);
        idSignature = new String(bytes2);
    }

    public String getToken() {
        return token;
    }

    public String getJoseHeader() {
        return joseHeader;
    }

    public String getClaims() {
        return claims;
    }

    public Map<String, Object> getClaimsMap() {
        return Collections.unmodifiableMap(claimsMap);
    }

    public String getIdSignature() {
        return idSignature;
    }
}
