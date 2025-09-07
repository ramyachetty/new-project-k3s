Feature: Search API Filtering

  Validate the handling of filter parameters.

  Background:
    Given the API key is provided
    And the site key is provided

  Scenario Outline: Search with valid filters
    When a search is performed for query "<query>" with filter "<filter>"
    Then the response should be received within 5000 milliseconds
    And the response status code should be 200
    And the query parameters should contain "<filterField>"
    And if products exist, they should be returned

    Examples:
      | query | filter         | filterField  |
      | *     | uniqueId:*     | uniqueId     |
      | shoes | categoryPath:* | categoryPath |

  Scenario Outline: Search with invalid filters
    When a search is performed for query "<query>" with filter "<filter>"
    Then the response should be received within 5000 milliseconds
    And the response status code should be 200
    And the searchMetaData status should be 0
    And the response should contain 0 products

    Examples:
      | query | filter       |
      | *     | category:abc |


  @skipAssertionsIfVariantsFalse
  Scenario: Search with filter parameter containing only field name
    When a search is performed for query "*" with filter "productimage"
    Then the response status code should be 400
    And the searchMetaData status should be 400
    And the response error msg should contain "undefined field"
    And the response error code should be 400

  @skipAssertionsIfStoreEnabled
  Scenario: Search with filter parameter missing field name
    When a search is performed for query "*" with filter ":shoes"
    Then the response status code should be 400
    And the searchMetaData status should be 400
    And the response error msg should contain one of "Error while parsing the filter param :shoes or Cannot parse ':shoes'"
    And the response error code should be 400

  Scenario Outline: Search with space between filters
    When a search is performed for query "<query>" with filter "<filter>"
    Then the response should be received within 5000 milliseconds
    And the response status code should be 200
    And the searchMetaData status should be 0

    Examples:
      | query | filter            |
      | *     | categoryPath1 :*  |
      | *     | categoryPath1: *  |
      | *     | categoryPath1 : * |

  @skipAssertionsIfVariantsFalse @skipAssertionsIfStoreEnabled
  Scenario: Search with filter value containing special characters
    When a search is performed for query "*" with filter "categoryPath2_uFilter:#$%^&*()"
    Then the response status code should be 400
    And the searchMetaData status should be 400
    And the response error msg should contain "Error while parsing the filter param categoryPath2_uFilter:"
    And the response error code should be 400

  @skipAssertionsIfVariantsFalse @skipAssertionsIfStoreEnabled
  Scenario: Search with filter value containing XML/HTML characters
    When a search is performed for query "*" with filter "categoryPath2_uFilter:</>"
    Then the response status code should be 400
    And the searchMetaData status should be 400
    And the response error msg should contain "Error while parsing the filter param categoryPath2_uFilter:</>"
    And the response error code should be 400