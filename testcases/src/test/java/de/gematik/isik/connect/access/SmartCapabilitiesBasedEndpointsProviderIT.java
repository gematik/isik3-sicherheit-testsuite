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

import groovy.util.logging.Slf4j;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;

@Slf4j
public class SmartCapabilitiesBasedEndpointsProviderIT {

    @Test
    @SneakyThrows
    public void testTls() {
        var endpoints = new SmartCapabilitiesBasedEndpointsProvider("",null, SSLContext.getDefault()).getEndpoints("https://secure.server.fire.ly");
        System.out.println(endpoints.getTokenEndpoint());
    }
}
