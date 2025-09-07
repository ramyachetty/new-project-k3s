#Feature: Search API Error Handling for Invalid Category Values
#
#  Background:
#    Given the API key is provided
#    And the site key is provided
#
#  Scenario: Search with improperly formatted 'p' parameter leading to undefined field error
#    When a category request is made for path "categoryPath'"
#    Then the response status code should be 400
#    And the response error message should contain "undefined field"
#    And the response error code should be 400