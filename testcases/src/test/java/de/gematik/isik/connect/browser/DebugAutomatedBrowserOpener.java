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
package de.gematik.isik.connect.browser;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

@Slf4j
public class DebugAutomatedBrowserOpener implements BrowserOpener {

    private final String patientIdToBeSelected;
    private final String debugEncounterId;

    public DebugAutomatedBrowserOpener(String debugPatientId, String debugEncounterId) {
        this.patientIdToBeSelected = debugPatientId;
        this.debugEncounterId = debugEncounterId;
    }

    @Override
    public void open(String url) {
        var chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless");
        WebDriver driver = new ChromeDriver(chromeOptions);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        try {
            driver.get(url);
            if(debugEncounterId != null)
                driver.findElement(By.id("in-encounter-context")).sendKeys(debugEncounterId);
            if(patientIdToBeSelected != null)
                driver.findElement(By.id("button-" + patientIdToBeSelected)).click();
            else {
                var patientSelectionButtons = driver.findElements(By.className("btn-info"));
                if(patientSelectionButtons.isEmpty())
                    throw new IllegalStateException("Could not locate patient selection buttons: " + driver.getPageSource());

                patientSelectionButtons.get(0).click();
            }
            driver.findElement(By.id("submit")).click();
        } finally {
            driver.quit();
        }
    }
}
