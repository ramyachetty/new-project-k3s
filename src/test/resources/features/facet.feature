Feature: Search API Faceting

  Validate the behavior of the facet parameter in the Search API.

  Background:
    Given the API key is provided
    And the site key is provided

  Scenario: Facets are disabled via query parameter
    When a search is performed for query "*" with parameters:
      | facet | false |
    Then the response status code should be 200
    And the searchMetaData status should be 0
    And the response JSON should not have the key "facets"

  @requiresVariantsDisabled @requiresStoreDisabled
  Scenario: Search with an invalid boolean value for the facet parameter
    When a search is performed for query "*" with parameters:
      | facet | @123 |
    Then the response status code should be 400
    And the response error msg should contain "invalid boolean value: @123"

  Scenario: Validate facet sorting by position
    When an initial search is performed for query "*"
    And the site is checked for facet support, skipping if none are found
    And a subsequent search is performed with facet sorting by "position"
    Then the text facets in the response should be sorted by position in ascending order