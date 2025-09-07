Feature: Search API Variant Handling

  Validate the handling of the 'variants' parameter based on site configuration.

  Background:
    Given the API key is provided
    And the site key is provided

  @requiresVariantsDisabled
  Scenario: Search with variants enabled on a site where variants are disabled
    When a search is performed for query "*" with parameters:
      | key      | value |
      | variants | true  |
    Then the response status code should be 200
    And the searchMetaData status should be 0
    And the response should contain 0 products

  @requiresVariantsEnabled @requiresStoreDisabled
  Scenario: Verify product count increases when variants=false is added
    When an initial search is performed for query "*"
    And A subsequent search for query "*" with variants disabled is performed
    Then the response status code should be 200 for both searches
    And the number of products in the subsequent search should be greater than the initial search

   @requiresVariantsEnabled @requiresStoreEnabled
  Scenario: Verify product count increases when both variants and store are disabled on a store-enabled site
    When an initial search is performed for query "*"
    And A subsequent search for query "*" with variants and store disabled is performed
    Then the response status code should be 200 for both searches
    And the number of products in the subsequent search should be greater than the initial search