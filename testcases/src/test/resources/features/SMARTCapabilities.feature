@SMARTCapabilities
Feature: SMART Capabilities

  Scenario: Access to SMART-on-FHIR metadata is allowed and content is complete
    When TGR send empty GET request to "${isik.env.fhir-server-full-url}/.well-known/smart-configuration"
    Then TGR find the last request
    And TGR current response with attribute "$.body.authorization_endpoint" matches "http[s]?://.*"
    And TGR current response with attribute "$.body.token_endpoint" matches "http[s]?://.*"
    And grant types include 'authorization_code' and 'client_credentials'
    And code challenge methods include 'S256' but not 'plain'
    And TGR current response with attribute "$.body.capabilities" matches ".*\"permission-v2\".*"
    And if present supported scopes include 'patient', 'system' and 'user' scopes