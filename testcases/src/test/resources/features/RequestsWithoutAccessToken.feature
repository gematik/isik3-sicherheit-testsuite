@RequestsWithoutAccessToken
Feature: Access to resource server without authorization

  Scenario: Access to Patient resource without authorization is forbidden
    When TGR send empty GET request to "${isik.env.fhir-server-full-url}/Patient/${user.testresources.patient-id-in-context}"
    Then TGR find the last request
    Then TGR current response with attribute "$.responseCode" matches "4\d\d"