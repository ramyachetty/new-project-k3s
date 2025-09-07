Feature: Search API Pagination

  Validate the handling of paging parameters.

  Background:
    Given the API key is provided
    And the site key is provided

  Scenario Outline: Pagination test
    When a search is performed for query "*" with "<rows>" items per page
    Then the response should be received within 5000 milliseconds
    And the response status code should be 200
    And the searchMetaData status should be 0
    Examples:
      | rows     |
      | 10       |
      | -20      |
      | 99999999 |
      | abcd     |
      | 5#%      |
      | 5/0      |

  @skipAssertionsIfStoreEnabled
  Scenario: Search with negative start parameter
    When a search is performed for query "*" with rows "5/0" and start "-5"
    Then the response status code should be 400
    And the searchMetaData status should be 400
    And the response error msg should contain "'start' parameter cannot be negative"
    And the response error code should be 400