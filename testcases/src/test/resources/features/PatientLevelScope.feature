@PatientLevelScope
Feature: Access to resource server with access-token

  Background: Access token has been issued and is active
    Given access token to Patient resource with Id "${user.testresources.patient-id-in-context}" has been requested and issued by authorization server

  Scenario: Access to Patient resource within the passed patient context is allowed
   When TGR send empty GET request to "${isik.env.fhir-server-full-url}/Patient/${user.testresources.patient-id-in-context}" with headers:
     | Accept    | application/fhir+json |
     | Authorization | Bearer ${access-code-patient-allowed} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "200"

  Scenario: Access to Patient resources outside of the passed patient context is forbidden
    When TGR send empty GET request to "${isik.env.fhir-server-full-url}/Patient/${user.testresources.patient-id-not-in-context}" with headers:
      | Accept    | application/fhir+json |
      | Authorization | Bearer ${access-code-patient-allowed} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "401"

  Scenario: Access to Condition resources of the patient in context is allowed
    When TGR send empty GET request to "${isik.env.fhir-server-full-url}/Condition?subject=Patient/${user.testresources.patient-id-in-context}" with headers:
      | Accept    | application/fhir+json |
      | Authorization | Bearer ${access-code-patient-allowed} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "200"
    Then FHIR current response body evaluates the FHIRPath "Bundle.entry.where(resource is Condition).count() > 0"
    Then TGR store current response node text value at "$.body.entry.0.resource.id" in variable "first-condition-reference"
    And TGR send empty GET request to "${isik.env.fhir-server-full-url}/Condition/${first-condition-reference}" with headers:
      | Accept    | application/fhir+json |
      | Authorization | Bearer ${access-code-patient-allowed} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "200"

  Scenario: Access to Condition resources of patients not in context is forbidden
    When TGR send empty GET request to "${isik.env.fhir-server-full-url}/Condition?subject=Patient/${user.testresources.patient-id-not-in-context}" with headers:
      | Accept    | application/fhir+json |
      | Authorization | Bearer ${access-code-patient-allowed} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "200"
    Then FHIR current response body evaluates the FHIRPath "Bundle.entry.where(resource is Condition).count() = 0"

  Scenario: Access to Encounter resources is forbidden if they were not specified in the client authorization request
    When TGR send empty GET request to "${isik.env.fhir-server-full-url}/Encounter?patient=Patient/${user.testresources.patient-id-in-context}" with headers:
      | Accept    | application/fhir+json |
      | Authorization | Bearer ${access-code-patient-allowed} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "401"