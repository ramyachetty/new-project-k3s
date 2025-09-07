Feature: Search API Invalid Handler

  Validate the API response when an incorrect handler is requested

  Background:
    Given the API key is provided
    And the site key is provided

  Scenario: Request search endpoint with incorrect path typo
    When a request is sent to the API sub-path "/searh" with query "skrt"
    Then the response status code should be 404
    And the response should contain status "Not Found"
    And the response message should be "Invalid URL Path. refer documentation for details"
    And the response error message should contain "Invalid URL Path. refer documentation for details"
    And the response error status_code should be 404