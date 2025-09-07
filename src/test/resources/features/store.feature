Feature: Search api store feature

  Background:
    Given the API key is provided
    And the site key is provided

#store.id tests

    #SID-01
  @requiresStoreEnabled
  Scenario: Successfully filter search results by first product store ID
    When an initial search is performed for query "*"
    And a store ID is extracted from the first product
    And a subsequent search is performed using the found store ID
    Then the response status code should be 200
    And the number of products should be less than the initial search
    And the query parameters should reflect the found store ID

   #SID-02
  @requiresStoreEnabled
  Scenario: Successfully filter search results by multiple product store IDs
    When an initial search is performed for query "*"
    And two distinct store IDs are extracted from the products
    And a subsequent search is performed using the two found store IDs
    Then the response status code should be 200
    And the number of products should be less than the initial search
    And all products in the response should have one of the found store IDs

  #SID-03
  @requiresStoreEnabled
  Scenario: Successfully filter search results by multiple store.id parameters
    When an initial search is performed for query "*"
    And four distinct store IDs are extracted from the products
    And a subsequent search is performed using the four found store IDs as multiple parameters
    Then the response status code should be 200
    And the number of products should be less than the initial search
    And all products in the response should have one of the found store IDs

   #SID-04
  @requiresStoreEnabled
  Scenario: Search with a non-existent store ID returns a successful response with zero products
    When a search is performed for query "*" with parameters:
      | parameter | value   |
      | store.id  | invalid |
    Then the response status code should be 200
    And the response should contain 0 products

    #SID-05
  @requiresStoreEnabled
  Scenario: Search with a non-existent store ID returns a successful response with zero products
    When a search is performed for query "*" with parameters:
      | parameter | value |
      | store.id  | 123   |
    Then the response status code should be 200
    And the response should contain 0 products

  @requiresStoreEnabled
  Scenario: Search with a URL-breaking character in store ID returns a syntax error
    When a search is performed for query "*" with parameters:
      | parameter | value |
      | store.id  | !@#   |
    Then the response status code should be 400
    And the response error msg should contain "Cannot parse '+(filter(storeId:!@#))'"

  @requiresStoreEnabled
  Scenario: Search with a store ID parameter that has no equals sign or value returns a syntax error
    When a search is performed with the raw query string "q=*&store.id"
    Then the response status code should be 400
    And the response error msg should contain "org.apache.solr.search.SyntaxError"
    
   
    
   #SID-06
  #@requiresStoreEnabled @skipKeyValidation
  #Scenario: Search with empty store.id parameter returns a syntax error
    #When a search is performed for query "*" with parameters:
      #| parameter | value |
      #| store.id  |       |
    #Then the response status code should be 400
    #And the response error msg should contain "Cannot parse"
    #
  #SID-07
  #@requiresStoreEnabled @skipKeyValidation
  #Scenario: Search with incorrect parameter name returns an unknown parameter error
    #When a search is performed for query "*" with parameters:
      #| parameter   | value |
      #| store.ids   | 123   |
    #Then the response status code should be 400
    #And the response error msg should contain "Unexpected parameter 'store.ids'"
    #
  #SID-08
  #@requiresStoreEnabled @skipKeyValidation
  #Scenario: Search with semicolon-separated store IDs returns a syntax error
    #When a search is performed for query "*" with parameters:
      #| parameter | value   |
      #| store.id  | 1;2;3   |
    #Then the response status code should be 400
    #And the response error msg should contain "org.apache.solr.search.SyntaxError"
    #
  #SID-09
  #@requiresStoreEnabled @skipKeyValidation
  #Scenario: Search with whitespace in store.id parameter returns a syntax error
    #When a search is performed for query "*" with parameters:
      #| parameter | value    |
      #| store.id  | " 123 "  |
    #Then the response status code should be 400
    #And the response error msg should contain "Cannot parse"
    
  #SID-10
  @requiresStoreEnabled @skipKeyValidation
  Scenario: Search without API key returns unauthorized error
    Given the API key is not provided
    When a search is performed for query "*"
    Then the response status code should be 401
    
  #SID-11
  @requiresStoreEnabled @skipKeyValidation
  Scenario: Search without site key returns forbidden error
    Given the site key is not provided
    When a search is performed for query "*"
    Then the response status code should be 404
    
   #SID-12
  #@requiresStoreEnabled
  #Scenario: Search with an overly long store.id value returns a validation error
    #When a search is performed for query "*" with parameters:
      #| parameter | value                                                                                   |
      #| store.id  | 1234567890123456789012345678901234567890123456789012345678901234                     |
    #Then the response status code should be 400
    #And the response error msg should contain "storeId exceeds maximum length"
    #
  #SID-13
  #@requiresStoreEnabled
  #Scenario: Search mixing one valid and one invalid store ID returns only the valid store results
    #Given a valid store ID is extracted from the first product
    #And the invalid store ID is "INVALID_STORE"
    #When a search is performed with the raw query string "q=*&store.id={validStoreId},{invalidStoreId}"
    #Then the response status code should be 200
    #And all products should only have valid store ID "{validStoreId}"
    #
  #SID-14
  #@requiresStoreEnabled
  #Scenario: Search with non-existent store.fields name returns a field-not-found error
    #When a search is performed for query "*" with parameters:
      #| parameter    | value            |
      #| store.fields | nonExistentField |
    #Then the response status code should be 400
    #And the response error msg should contain "Field 'nonExistentField' not found"
    #
  #SID-15
  #@requiresStoreEnabled
  #Scenario: Search with unsupported operator in store.filters returns a parsing error
    #When a search is performed for query "*" with parameters:
      #| parameter      | value  |
      #| store.filters  | storeId>100 |
    #Then the response status code should be 400
    #And the response error msg should contain "unsupported operator"
    #
  #SID-16
  #@requiresStoreEnabled
  #Scenario: Search with negative store ID returns an invalid parameter error
    #When a search is performed for query "*" with parameters:
      #| parameter | value |
      #| store.id  | -1    |
    #Then the response status code should be 400
    #And the response error msg should contain "Invalid store ID"
    #


#store.fields tests

  @requiresStoreEnabled
  Scenario: Search with invalid special characters in store.fields parameter returns a parsing error
    When a search is performed for query "*" with parameters:
      | parameter    | value |
      | store.fields | @#    |
    Then the response status code should be 400
    And the response error msg should contain one of "Error parsing fieldname or while invoking store:[subquery] on doc=SolrDocument"

  @requiresStoreEnabled
  Scenario: Search with an incorrect syntax for store filter parameter returns an undefined field error
    When a search is performed for query "*" with parameters:
      | parameter     | value |
      | store.filters | 132   |
    Then the response status code should be 400
    And the response error msg should contain "undefined field"

  #SFD-01
  @requiresStoreEnabled @requiresVariantsDisabled
  Scenario: Validate store.fields parameter restricts fields in the store object
    When a search is performed for query "*" with parameters:
      | parameter    | value   |
      | store.fields | parentId |
    Then the response status code should be 200
    And each store object in the response should only contain the specified field "parentId"

  #SFD-02 
  @requiresStoreEnabled @requiresVariantsDisabled
  Scenario: Validate store.fields parameter with multiple fields
    When a search is performed for query "*" with parameters:
      | parameter    | value                                |
      | store.fields | parentId,timeStamp_unbxd,unbxdFeedId |
    Then the response status code should be 200
    And each store object in the response should only contain the fields:
      | parentId        |
      | timeStamp_unbxd |
      | unbxdFeedId     |

  #SFD-02 (variants true)
  @requiresStoreEnabled @requiresVariantsEnabled
  Scenario: Validate store.fields with variants enabled includes default and requested fields
    When a search is performed for query "*" with parameters:
      | parameter    | value           |
      | store.fields | timeStamp_unbxd |
    Then the response status code should be 200
    And each store object inside a variant should contain the exact fields:
      | parentId        |
      | storeId         |
      | uniqueId        |
      | timeStamp_unbxd |

  #SFD-05
  @requiresStoreEnabled @requiresVariantsDisabled
  Scenario: Validate store.fields with an invalid field returns an empty store object
    When a search is performed for query "*" with parameters:
      | parameter    | value |
      | store.fields | abc   |
    Then the response status code should be 200
    And each store object in the response should be empty

  # SFD-05 (variants true)
  @requiresStoreEnabled @requiresVariantsEnabled
  Scenario: Validate store.fields with an invalid field returns only default fields for variants
    When a search is performed for query "*" with parameters:
      | parameter    | value |
      | store.fields | abc   |
    Then the response status code should be 200
    And each store object inside a variant should contain the exact fields:
      | parentId  |
      | storeId   |
      | uniqueId  |

  #SFD-06
  @requiresStoreEnabled @requiresVariantsDisabled
  Scenario: Validate store.fields with a mix of valid and invalid fields (variants disabled)
  When a search is performed for query "*" with parameters:
    | parameter    | value        |
    | store.fields | abc,parentId |
  Then the response status code should be 200
  And each store object in the response should only contain the fields:
    | parentId  |

  # SFD-06 (variants true)
  @requiresStoreEnabled @requiresVariantsEnabled
  Scenario: Validate store.fields with a mix of valid and invalid fields (variants enabled)
    When a search is performed for query "*" with parameters:
      | parameter    | value        |
      | store.fields | abc,parentId |
    Then the response status code should be 200
    And each store object inside a variant should contain the exact fields:
      | parentId  |
      | storeId   |
      | uniqueId  |

#store.filters tests

  #SFT-01
  @requiresStoreEnabled @requiresVariantsEnabled
  Scenario: Successfully filter store results using store.filters
    When a search is performed for query "*" with parameters:
      | parameter     | value             |
      | store.filters | st_price_Mrp:199 |
    Then the response status code should be 200
    And each store object inside a variant should have the field "st_price_Mrp" with value "199"

  #SFT-02
  @requiresStoreEnabled @requiresVariantsEnabled
  Scenario: Successfully apply multiple store.filters simultaneously
    When a search is performed with the raw query string "q=*&store.filters=st_price_Mrp:[0 TO 200]&store.filters=st_discount:[30 TO *]"
    Then the response status code should be 200
    And each store object inside a variant should satisfy all of the following numeric conditions:
      | fieldName    | operator | value |
      | st_price_Mrp | >=       | 0     |
      | st_price_Mrp | <=       | 200   |
      | st_discount  | >=       | 30    |

 # SFT-03
  @requiresStoreEnabled @requiresVariantsEnabled
  Scenario: Successfully filter store results using a numeric range in store.filters
    When a search is performed for query "*" with parameters:
      | parameter     | value                   |
      | store.filters | st_price_Mrp:[0 TO 200] |
    Then the response status code should be 200
    And each store object inside a variant should have the numeric field "st_price_Mrp" within the range 0 to 200

  #SFT-04
 @requiresStoreEnabled @requiresVariantsEnabled
  Scenario: Successfully apply a boolean filter in store.filters
    When a search is performed for query "*" with parameters:
      | parameter     | value             |
      | store.filters | parent_unbxd:false |
    Then the response status code should be 200
    And each store object inside a variant should satisfy all of the following boolean conditions:
      | fieldName    | value |
      | parent_unbxd | false |

  #SFT-05
  @requiresStoreEnabled
  Scenario: Search with a boolean value for an expected numeric store filter returns a type mismatch error
    When a search is performed for query "*" with parameters:
      | parameter     | value                       |
      | store.filters | st_store_availability:true |
    Then the response status code should be 400
    And the response error msg should contain "Invalid Number: true"
    And the response error code should be 400

  #SFT-06
  @requiresStoreEnabled
  Scenario: Search with a filter on a non-existent field returns an undefined field error
    When a search is performed for query "*" with parameters:
      | parameter     | value      |
      | store.filters | abcd:false |
    Then the response status code should be 400
    And the response error msg should contain "undefined field abcd"
    And the response error code should be 400

  #store.count tests

  #SCT-01
  @requiresStoreEnabled
  Scenario: Validate store.count parameter limit the number of store objects
    When a search is performed for query "*" with parameters:
      | parameter   | value |
      | store.count | 2     |
    Then the response status code should be 200
    And each variant store should contain at most 2 stores

  #SCT-02
  @requiresStoreEnabled
  Scenario: Validate default store.count limit is 5
    When a search is performed for query "*"
    Then the response status code should be 200
    And each variant store should contain at most 5 stores
    