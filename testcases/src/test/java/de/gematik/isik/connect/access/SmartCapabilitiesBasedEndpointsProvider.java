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

import com.google.gson.JsonParser;
import groovy.util.logging.Slf4j;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.hspconsortium.client.controller.FhirEndpoints;
import org.hspconsortium.client.controller.FhirEndpointsProvider;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@AllArgsConstructor
@Slf4j
public class SmartCapabilitiesBasedEndpointsProvider implements FhirEndpointsProvider {

    private String proxyHost;
    private Integer proxyPort;
    private SSLContext sslContext;


    @Override
    @SneakyThrows
    public FhirEndpoints getEndpoints(String fhirServiceUrl) {
        String smartConfigurationUrl = fhirServiceUrl;
        if(!smartConfigurationUrl.endsWith("/"))
            smartConfigurationUrl += "/";
        smartConfigurationUrl += ".well-known/smart-configuration";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(smartConfigurationUrl))
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

        try {
            var smartConfiguration = JsonParser.parseString(response.body()).getAsJsonObject();
            var authEndpoint = smartConfiguration.get("authorization_endpoint").getAsString();
            var tokenEndpoint = smartConfiguration.get("token_endpoint").getAsString();
            return new FhirEndpoints(fhirServiceUrl, authEndpoint, tokenEndpoint, null);
        } catch (Exception e) {
            System.out.println(response.body());
            throw new RuntimeException("Failed to fetch smart configuration from " + fhirServiceUrl, e);
        }
    }

}
