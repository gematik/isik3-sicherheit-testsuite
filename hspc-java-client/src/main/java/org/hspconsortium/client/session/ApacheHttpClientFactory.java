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

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;

public class ApacheHttpClientFactory {

    private final String proxyHost;
    private final Integer proxyPort;
    private final String proxyUser;
    private final String proxyPassword;
    private final Integer httpConnectionTimeOut;
    private final Integer httpReadTimeOut;

    public ApacheHttpClientFactory(String proxyHost, Integer proxyPort, String proxyUser, String proxyPassword, Integer httpConnectionTimeOut, Integer httpReadTimeOut) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
        this.httpConnectionTimeOut = httpConnectionTimeOut;
        this.httpReadTimeOut = httpReadTimeOut;
    }

    public CloseableHttpClient getClient() {
        HttpHost proxy = null;
        if (proxyHost != null) {
            proxy = new HttpHost(proxyHost, proxyPort);
        }
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(httpConnectionTimeOut)
                .setConnectionRequestTimeout(httpReadTimeOut)
                .setStaleConnectionCheckEnabled(true)
                .setProxy(proxy)
                .build();

        HttpClientBuilder builder = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .disableCookieManagement();

        if (proxyUser != null) {
            CredentialsProvider  credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(proxyHost, proxyPort), new UsernamePasswordCredentials(proxyUser, proxyPassword));
            builder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
            builder.setDefaultCredentialsProvider(credentialsProvider);
        }

        return builder.build();
//
//
//        BasicCredentialsProvider credsProvider = null;
//        if (proxyUser != null) {
//            credsProvider = new BasicCredentialsProvider();
//            credsProvider.setCredentials(
//                    new AuthScope(proxyHost, proxyPort),
//                    new UsernamePasswordCredentials(proxyUser, proxyPassword)
//            );
//        }
//
//        return HttpClientBuilder.create()
//                .setRoutePlanner(new SystemDefaultRoutePlanner(ProxySelector.getDefault()))
//                .setDefaultCredentialsProvider(credsProvider)
//                .setDefaultRequestConfig(config).build();
    }
}
