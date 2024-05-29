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
package de.gematik.isik.connect.glue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.rest.api.EncodingEnum;
import de.gematik.isik.connect.access.BackendServiceAccessTokenRequest;
import de.gematik.isik.connect.access.EncounterContextAccessCodeRequest;
import de.gematik.isik.connect.access.PatientContextAccessCodeRequest;
import de.gematik.isik.connect.access.UserLevelAccessCodeRequest;
import de.gematik.rbellogger.data.facet.RbelJsonFacet;
import de.gematik.test.tiger.common.config.SourceType;
import de.gematik.test.tiger.common.config.TigerConfigurationException;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.test.tiger.lib.rbel.RbelMessageValidator;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import java.util.ArrayList;
import java.util.List;

import static de.gematik.test.tiger.common.config.TigerGlobalConfiguration.resolvePlaceholders;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class IsikConnectGlue {

    @And("if present supported scopes include 'patient', 'system' and 'user' scopes")
    public void supportedScopescheckForProfileWithIdFromYaml() {
        String attribute = "$.body.scopes_supported";
        var scopesSupported = RbelMessageValidator.instance.getCurrentResponse().findElement(attribute);
        if(scopesSupported.isEmpty())
            return;

        List<String> list = new ArrayList<>();
        scopesSupported.get().getFacetOrFail(RbelJsonFacet.class).getJsonElement().forEach(node -> list.add(node.asText()));
        assertThat(list).anyMatch(s -> s.matches("patient/.*")).anyMatch(s -> s.matches("system/.*")).anyMatch(s -> s.matches("user/.*"));
    }

    @And("grant types include 'authorization_code' and 'client_credentials'")
    public void grantTypesIncludeAuthorization_codeAndClient_credentials() {
        String attribute = "$.body.grant_types_supported";
        var grantTypesSupported = RbelMessageValidator.instance.findElementInCurrentResponse(attribute);
        List<String> list = new ArrayList<>();
        grantTypesSupported.getFacetOrFail(RbelJsonFacet.class).getJsonElement().forEach(node -> list.add(node.asText()));
        assertThat(list).contains("authorization_code", "client_credentials");
    }

    @And("code challenge methods include 'S256' but not 'plain'")
    public void code_challenge_methodsIncludeSButNotPlain() {
        String attribute = "$.body.code_challenge_methods_supported";
        var codeChallengeMethodsSupported = RbelMessageValidator.instance.findElementInCurrentResponse(attribute);

        List<String> list = new ArrayList<>();
        codeChallengeMethodsSupported.getFacetOrFail(RbelJsonFacet.class).getJsonElement().forEach(node -> list.add(node.asText()));
        assertThat(list).contains("S256").doesNotContain("plain");
    }

    @SneakyThrows
    @Given("access token to Patient resource with Id {tigerResolvedString} has been requested and issued by authorization server")
    public void accessTokenHasBeenIssuedBy(String patientId) {
        String varName = "access-code-patient-allowed";
        try {
            TigerGlobalConfiguration.readString(varName);
            log.info(String.format("Access for Patient resource '%s' is in cache. Skipping request...", patientId));
            return;
        } catch (TigerConfigurationException e) {
            log.info("No access code found in cache ({}). Requesting new one...", e.getMessage());
        }
        var accessCode = new PatientContextAccessCodeRequest().requestAccessCodeFor(patientId);

        log.info("Received access code: " + accessCode);
        TigerGlobalConfiguration.putValue(varName, accessCode, SourceType.TEST_CONTEXT);
        log.info(String.format("Storing '%s' in variable '%s'", accessCode, varName));
    }

    @Given("access token for backend service with scope {string} has been requested and issued by authorization server")
    @SneakyThrows
    public void accessTokenForBackendServiceWithScopeHasBeenRequestedAndIssuedByAuthorizationServer(String scopes) {
        String varName = "access-code-backend-service";
        try {
            TigerGlobalConfiguration.readString(varName);
            log.info("Access code for backend service is in cache. Skipping request...");
            return;
        } catch (TigerConfigurationException e) {
            log.info("No access code found in cache ({}). Requesting new one...", e.getMessage());
        }

        var accessCode = new BackendServiceAccessTokenRequest().requestAccessCode(scopes);

        log.info("Received access code: " + accessCode);
        TigerGlobalConfiguration.putValue(varName, accessCode, SourceType.TEST_CONTEXT);
        log.info(String.format("Storing '%s' in variable '%s'", accessCode, varName));
    }

    @Given("access token to all Patient resources available for a user has been requested and issued by authorization server")
    @SneakyThrows
    public void accessTokenToAllPatientResourcesAvailableForAUserHasBeenRequestedAndIssuedByAuthorizationServer() {
        String varName = "access-code-user-patient";
        try {
            TigerGlobalConfiguration.readString(varName);
            log.info("Access code for all Patient resources of a user is in cache. Skipping request...");
            return;
        } catch (TigerConfigurationException e) {
            log.info("No access code found in cache ({}). Requesting new one...", e.getMessage());
        }

        var accessCode = new UserLevelAccessCodeRequest().requestAccessCode();

        log.info("Received access code: " + accessCode);
        TigerGlobalConfiguration.putValue(varName, accessCode, SourceType.TEST_CONTEXT);
        log.info(String.format("Storing '%s' in variable '%s'", accessCode, varName));
    }

    @And("access token to Condition resources of Patient with Id {tigerResolvedString} and Encounter with Id {tigerResolvedString} has been issued by authorization server")
    @SneakyThrows
    public void accessTokenToConditionResourcesOfPatientWithIdAndEncounterWithIdHasBeenIssuedByAuthorizationServer(String patientId, String encounterId) {
        String varName = "access-code-encounter-allowed";
        try {
            TigerGlobalConfiguration.readString(varName);
            log.info(String.format("Access for Patient/Encounter context '%s/%s' is in cache. Skipping request...", patientId, encounterId));
            return;
        } catch (TigerConfigurationException e) {
            log.info("No access code found in cache ({}). Requesting new one...", e.getMessage());
        }
        var accessCode = new EncounterContextAccessCodeRequest().requestAccessCode(patientId, encounterId);
        log.info("Received access code: " + accessCode);
        TigerGlobalConfiguration.putValue(varName, accessCode, SourceType.TEST_CONTEXT);
        log.info(String.format("Storing '%s' in variable '%s'", accessCode, varName));
    }

    @Then("evaluate FHIRPath {tigerResolvedString} on current response body and store result in variable {string}")
    @SneakyThrows
    public void fhirEvaluateFHIRPathOnCurrentResponseBodyAndStoreResultInVariable(String fhirPath, String variableName) {
        var ctx = FhirContext.forR4();
        var fhirPathEngine = new FHIRPathEngine(new HapiWorkerContext(ctx, new DefaultProfileValidationSupport(ctx)));

        var messageValidator = RbelMessageValidator.instance;
        var resourceAsString = messageValidator.findElementInCurrentResponse("$.body").getRawStringContent();
        final IBaseResource ressource = EncodingEnum.detectEncoding(resourceAsString).newParser(ctx).parseResource(resourceAsString);
        var tigerResolvedFhirPath = resolvePlaceholders(fhirPath);
        final var result = fhirPathEngine.evaluate((Base)ressource, tigerResolvedFhirPath);

        log.debug("FHIRPath evaluation result: " + result);

        if(result.size() > 1)
            throw new IllegalArgumentException(String.format("FHIRPath expression %s returned more than one result. This is not supported.", fhirPath));

        if(result.isEmpty())
            throw new IllegalArgumentException(String.format("FHIRPath expression %s didn't return any results", fhirPath));

        String value = result.get(0).primitiveValue();
        TigerGlobalConfiguration.putValue(variableName, value, SourceType.TEST_CONTEXT);
        log.info(String.format("Storing '%s' in variable '%s'", value, variableName));
    }

}
