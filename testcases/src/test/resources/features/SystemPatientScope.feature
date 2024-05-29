@SystemPatientScope
Feature: Access to resource server with access-token and system-level Patient scope

  Background: Access token with Patient resource scope is requested
    Given access token for backend service with scope "system/Patient.r" has been requested and issued by authorization server

  Scenario: Access to Patient resources is allowed if Patient resource is in scope
   When TGR send empty GET request to "${isik.env.fhir-server-full-url}/Patient" with headers:
     | Accept    | application/fhir+json |
     | Authorization | Bearer ${access-code-backend-service} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "200"

  Scenario: Access to Patient resources is not allowed for search operation
    When TGR send empty GET request to "${isik.env.fhir-server-full-url}/Patient?family=Mustermann" with headers:
      | Accept    | application/fhir+json |
      | Authorization | Bearer ${access-code-backend-service} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "40\d"

  Scenario: Access to Encounter resources is not allowed if Encounter resource is not scope
    When TGR send empty GET request to "${isik.env.fhir-server-full-url}/Encounter" with headers:
      | Accept    | application/fhir+json |
      | Authorization | Bearer ${access-code-backend-service} |
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "40\d"