Feature: Search API Boost and DemoteQuery Handling

  Validate the API's behavior with boost and demoteQuery parameters, particularly focusing on error conditions
  related to field resolution within these parameters.

  Background:
    Given the API key is provided
    And the site key is provided

  Scenario: Search with invalid boost value referencing an undefined field
    When a search is performed for query "*" with parameters:
      | key   | value |
      | boost | abc   |
    Then the response status code should be 400
    And the searchMetaData status should be 400
    And the response error msg should contain "undefined field: \"abc\""
    And the response error code should be 400
    And the query parameters should contain "boost=abc"

  Scenario: Search with boost expression and demoteQuery leading to undefined field error
    When a search is performed for query "*" with parameters:
      | key         | value                                 |
      | boost       | if(eq(query($demoteQuery),false),1,0) |
      | demoteQuery | random:1234                           |
    Then the response status code should be 400
    And the searchMetaData status should be 400
    And the response error msg should contain "undefined field random"
    And the response error code should be 400
    And the query parameters should contain "boost=if(eq(query($demoteQuery),false),1,0)"

  Scenario: Search with incorrect boost expression causing SyntaxError
    When a search is performed for query "*" with parameters:
      | key         | value                               |
      | boost       | (eq(query($demoteQuery),false),1,0) |
      | demoteQuery | random:1234                         |
    Then the response status code should be 400
    And the searchMetaData status should be 400
    And the response error msg should contain one of "search.SyntaxError: Expected identifier at pos 0 str='(eq(query($demoteQuery),false),1,0)' or Error parsing fieldname: Expected identifier at pos 0 str"
    And the response error code should be 400
    And the query parameters should contain "boost=(eq(query($demoteQuery),false),1,0)"

  Scenario: Search with boost expression using an empty query() function call
    When a search is performed for query "*" with parameters:
      | key         | value                     |
      | boost       | if(eq(query(),false),1,0) |
      | demoteQuery | random:1234               |
    Then the response status code should be 400
    And the searchMetaData status should be 400
    And the response error msg should contain "Nested function query must use $param or {!v=value} forms."
    And the response error code should be 400
    And the query parameters should contain "boost=if(eq(query(),false),1,0)"

  Scenario: Search with boost having missing value after colon
    When a search is performed for query "*" with parameters:
      | key         | value                                 |
      | boost       | if(eq(query($demoteQuery),false),1,0) |
      | demoteQuery | id:                                   |
    Then the response status code should be 400
    And the searchMetaData status should be 400
    And the response error msg should contain "Cannot parse 'id:'"
    And the response error code should be 400
    And the query parameters should contain "boost=if(eq(query($demoteQuery),false),1,0)"
    And the query parameters should contain "demoteQuery=id:"

  Scenario: Search with unknown function name in boost expression
    When a search is performed for query "*" with parameters:
      | key         | value                                  |
      | boost       | iff(eq(query($demoteQuery),false),1,0) |
      | demoteQuery | abcd:0                                 |
    Then the response status code should be 400
    And the searchMetaData status should be 400
    And the response error msg should contain "Unknown function iff"
    And the response error code should be 400
    And the query parameters should contain "boost=iff(eq(query($demoteQuery),false),1,0)"
    And the query parameters should contain "demoteQuery=abcd:0"
