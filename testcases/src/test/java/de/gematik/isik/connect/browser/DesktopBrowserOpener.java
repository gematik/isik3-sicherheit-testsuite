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
package de.gematik.isik.connect.browser;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
public class DesktopBrowserOpener implements BrowserOpener {
    @Override
    @SneakyThrows
    public void open(String url) {
        if(!DesktopApi.browse(new URI(url))) {
            log.info("--------  ACTION REQUIRED ------------");
            log.info("Please open the following URL in your browser: " + url);
        }
    }


}
