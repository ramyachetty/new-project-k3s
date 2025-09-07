Feature: Search API Querying

  Validate the handling of query parameters.

  Background:
    Given the API key is provided
    And the site key is provided

  Scenario: API Key and Site Key validation
    When a GET request is sent to "/random/random/search" with query "*"
    Then the response status code should be 401
    And the response should contain status "Unauthorized"
    And the response should not contain "searchMetaData"

  Scenario: Authentication failure with incorrect Site Key
    When a search request is sent using the correct API key and an incorrect site key for query "*"
    Then the response status code should be 401
    And the response should contain status "Unauthorized"
    And the response message should be "Incorrect auth credentials"
    And the response error message should contain "no auth found"

  Scenario: Authentication failure with incorrect API Key
    When a search request is sent using an incorrect API key and the correct site key for query "*"
    Then the response status code should be 401
    And the response should contain status "Unauthorized"
    And the response message should be "Incorrect auth credentials"
    And the response error message should contain "not authorized"

  Scenario: Search with empty query parameter
    When a search is performed for query ""
    Then the response status code should be 400
    And the response should contain status "Bad Request"
    And the response message should be "Constraint violation, refer documentation for details"
    And the response error message should contain "missing required parameter"

  Scenario: Search with semicolon in query parameter
    When a search is performed for query "abc;"
    Then the response status code should be 200
    And the searchMetaData status should be 0

  Scenario Outline: Multiple queries test
    When a search is performed for query "<query>"
    Then the response should be received within 5000 milliseconds
    And the response status code should be 200
    And the searchMetaData status should be 0
    And the response should contain 0 or more products
    And each product should have required fields
    And the original query should match "<query>"

    Examples:
      | query   |
      | *       |
      | shoes   |
      | t-shirt |
      | jeans   |
      | shoes   |


  Scenario: Search endpoint called without any query parameters
    When the search endpoint is requested without a query parameter
    Then the response status code should be 400
    And the response should contain status "Bad Request"

    And the response message should be "Constraint violation, refer documentation for details"
    And the response error message should contain "missing required parameter"

#  Scenario: Search with excessively long query parameter
#    # Verifies response when the 'q' parameter is extremely long
#    When a search is performed using an excessively long query string
#    # Observed behavior via Cloudflare was 500 error page
#    Then the response status code should be 500
#    And the response body should contain "Cloudflare"
#    And the response body should contain "Error code 500"