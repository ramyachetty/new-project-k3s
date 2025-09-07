Feature: Conditional SearchAPIFallbackHandling

Validate the handling of the fallback.method parameter in the searchMetaData.

  Background:
    Given the API key is provided
    And the site key is provided

  @requiresSpellcheckFallback
  Scenario: Search with fallback=true and fallback.response=true
    When a search is performed for query "jaket" with parameters:
      | key                | value   |
      | fallback.response  | true    |
    Then the response status code should be 200
    And the searchMetaData status should be 0
    And the response should contain fallback with corrected query "jacket"