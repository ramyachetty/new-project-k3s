Feature: Search API Sorting

  Validate the handling of sorting parameters.

  Background:
    Given the API key is provided
    And the site key is provided

  Scenario Outline: Search with sorting
    When a search is performed for query "<query>" sorted by "<sortField>" in "<sortOrder>" order
    Then the response should be received within 5000 milliseconds
    And the searchMetaData status should be 0
    And the query parameters should contain "sort=<sortField>"
    And if products exist, they should be returned

    Examples:
      | query | sortField | sortOrder |
      | *     | uniqueId  | desc      |
      | shoes | price     | asc       |

  @skipAssertionsIfStoreEnabled
  Scenario: Search with incomplete sort order keyword
    When a search is attempted for query "*" with sort parameter "uniqueId des"
    Then the response status code should be 400
    And the response error msg should contain "Can't determine a Sort Order"
    And the response error code should be 400

  @skipAssertionsIfStoreEnabled
  Scenario: Search with missing sort order keyword
    When a search is attempted for query "*" with sort parameter "uniqueId"
    Then the response status code should be 400
    And the response error msg should contain "sort param field can't be found: uniqueId"
    And the response error code should be 400

  @skipAssertionsIfStoreEnabled
  Scenario: Search with incorrect separator in sort parameter
    When a search is attempted for query "*" with sort parameter "uniqueId:asc"
    Then the response status code should be 400
    And the response error msg should contain "sort param field can't be found: uniqueId:"
    And the response error code should be 400

  Scenario: Search attempting to sort by non-sortable field name
    When a search is attempted for query "*" with sort parameter "style_long_description ASC"
    Then the response status code should be 400
    And the response error msg should contain "sort param field can't be found: style_long_"
    And the response error code should be 400

  @skipAssertionsIfStoreEnabled
  Scenario: Search with multiple sort orders for single field
    When a search is attempted for query "*" with sort parameter "uniqueId asc desc"
    Then the response status code should be 400
    And the response error msg should contain "sort param could not be parsed as a query"
    And the response error code should be 400