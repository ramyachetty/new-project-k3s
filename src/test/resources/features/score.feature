Feature: Search api score feature

  Background:
    Given the API key is provided
    And the site key is provided

  Scenario: Search with doctype in field list fl
    When a search is performed for query "*" with parameters:
      | fl | doctype |
    Then the response status code should be 200
    And the searchMetaData status should be 0
    Then the first product in the response should contain a doctype field

  Scenario: Search with an invalid field parameter in field list fl
    When a search is performed for query "*" with parameters:
      | fl | doc"& |
    Then the response status code should be 400
    And the response error msg should contain "undefined field: \"doc\""
    And the response error code should be 400

  Scenario: Search with an invalid field parameter in field query fq
    When a search is performed for query "*" with parameters:
      | fq | idd:"13365" |
    Then the response status code should be 400
    And the response error msg should contain "undefined field idd"
    And the response error code should be 400