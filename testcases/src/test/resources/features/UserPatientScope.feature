@UserPatientScope
Feature: Access to resource server with access-token and user-level scopes

  Background: Access token with Patient resource scope is requested
    Given access token to all Patient resources available for a user has been requested and issued by authorization server

  Scenario: Access to Patient resources is allowed for search operation
    When TGR send empty GET request to "${isik.env.fhir-server-full-url}/Patient?family=Mustermann" with headers:
      | Accept    | application/fhir+json |
      | Authorization | Bearer ${access-code-user-patient} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "200"

  Scenario: Access to Encounter resources is not allowed
    When TGR send empty GET request to "${isik.env.fhir-server-full-url}/Encounter" with headers:
      | Accept    | application/fhir+json |
      | Authorization | Bearer ${access-code-user-patient} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "40\d"

  Scenario: Number of accessible Patient resources is less than total number of patients in the system
    Given access token for backend service with scope "system/Patient.rs" has been requested and issued by authorization server
    When TGR send empty GET request to "${isik.env.fhir-server-full-url}/Patient" with headers:
      | Accept    | application/fhir+json |
      | Authorization | Bearer ${access-code-backend-service} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "200"
    Then TGR store current response node text value at "$.body.total" in variable "overall-number-of-patients"
    Then TGR send empty GET request to "${isik.env.fhir-server-full-url}/Patient" with headers:
      | Accept    | application/fhir+json |
      | Authorization | Bearer ${access-code-user-patient} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "200"
    Then FHIR current response body evaluates the FHIRPath "Bundle.entry.where(resource is Patient).count() < ${overall-number-of-patients}"