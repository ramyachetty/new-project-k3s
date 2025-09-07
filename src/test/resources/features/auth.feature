#Feature: Admin API - Authorization Management
#
#  As an administrator, manage authorization entries for sites.
#
#  Background:
#    Given the Admin API base URI is configured
#
#  Scenario Outline: Create authorization entry for specific sites
#    When  a request is sent to create an authorization entry for site "<site>"
#    Then the admin response status code should be 204
#
#    Examples:
#      | site          |
#      | test-feed-1   |
#      | test-feed-2   |
