@EncounterContext
Feature: Access to resource within patient and encounter context

  Background: Access token has been issued and is active
    Given access token for backend service with scope "system/Condition.rs" has been requested and issued by authorization server
    And access token to Condition resources of Patient with Id "${user.testresources.patient-id-in-context}" and Encounter with Id "${user.testresources.encounter-id-in-context}" has been issued by authorization server

  Scenario: Access to Condition resource within the passed encounter context is allowed
   When TGR send empty GET request to "${isik.env.fhir-server-full-url}/Condition?subject=Patient/${user.testresources.patient-id-in-context}" with headers:
     | Accept    | application/fhir+json |
     | Authorization | Bearer ${access-code-backend-service} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "200"
    Then FHIR current response body evaluates the FHIRPath "Bundle.entry.where(resource is Condition).count() > 0"
    Then evaluate FHIRPath "Bundle.entry.where(resource is Condition).count()" on current response body and store result in variable "overall-number-of-patient-conditions"
    Then TGR send empty GET request to "${isik.env.fhir-server-full-url}/Condition" with headers:
      | Accept    | application/fhir+json |
      | Authorization | Bearer ${access-code-encounter-allowed} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "200"
    Then FHIR current response body evaluates the FHIRPath "Bundle.entry.where(resource is Condition).count() < ${overall-number-of-patient-conditions}"
    Then TGR store current response node text value at "$.body.entry.0.resource.id" in variable "first-condition-reference"
    Then TGR send empty GET request to "${isik.env.fhir-server-full-url}/Condition/${first-condition-reference}" with headers:
      | Accept    | application/fhir+json |
      | Authorization | Bearer ${access-code-encounter-allowed} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "200"

  Scenario: Access to Condition resources outside of the passed encounter context is forbidden
    When TGR send empty GET request to "${isik.env.fhir-server-full-url}/Condition?encounter=Encounter/${user.testresources.encounter-id-not-in-context}" with headers:
      | Accept    | application/fhir+json |
      | Authorization | Bearer ${access-code-backend-service} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "200"
    Then FHIR current response body evaluates the FHIRPath "Bundle.entry.where(resource is Condition).count() > 0"
    Then evaluate FHIRPath "Bundle.entry.where(resource is Condition).first().fullUrl" on current response body and store result in variable "fullUrl-of-condition-of-encounter-not-in-context"
    Then TGR send empty GET request to "${fullUrl-of-condition-of-encounter-not-in-context}" with headers:
      | Accept    | application/fhir+json |
      | Authorization | Bearer ${access-code-encounter-allowed} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "40\d"