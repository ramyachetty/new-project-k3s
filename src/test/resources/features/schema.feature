Feature: Schema Validation

Scenario: Validate the schema properties for the storeId field
    When a request is sent to get the index fields
    Then the response status code should be 200
    And the schema for field "storeId" should have "multiValued" as "NO"
    And the schema for field "storeId" should have "fieldType" as "title"

 Scenario: Validate the schema properties for the store_availability field
    When a request is sent to get the index fields
    Then the response status code should be 200
    And the schema for field "store_availability" should have "fieldType" as "decimal"
    And the schema for field "store_availability" should have "multiValued" as "NO"
