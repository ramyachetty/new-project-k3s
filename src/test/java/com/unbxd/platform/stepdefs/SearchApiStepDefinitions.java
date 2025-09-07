package com.unbxd.platform.stepdefs;

import com.unbxd.platform.config.CliOptions;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.response.Response;
import io.restassured.path.json.JsonPath;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import org.hamcrest.Matcher;

import org.junit.jupiter.api.Assumptions;

import io.cucumber.datatable.DataTable;
import static org.hamcrest.MatcherAssert.assertThat;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.*;

public class SearchApiStepDefinitions {

  private static String apiKey;
  private static String siteKey;
  private static String baseUri;
  private static Boolean siteSupportsVariants = null;
  private static Boolean siteSupportsStore = null;
  private String dynamicallyFoundStoreId = null;
  private Response rawResponse;
  private ValidatableResponse response;
  private Scenario currentScenario;

  private Response initialSearchResponse;
  private Response subsequentSearchResponse;
  private Integer initialNumberOfProducts = null;
  private Integer subsequentNumberOfProducts = null;
  private List<String> dynamicallyFoundStoreIds = new ArrayList<>();

  private static final String INCORRECT_API_KEY = "INVALID-API-KEY-12345";
  private static final String INCORRECT_SITE_KEY = "INVALID-SITE-KEY-67890";
  private static final String SCHEMA_BASE_URI = "http://odin.prod.ap-southeast-1.infra";

  @BeforeAll
  public static void setup() {
    apiKey = CliOptions.apiKey;
    siteKey = CliOptions.siteKey;
    baseUri = CliOptions.baseUri;

    if (apiKey == null
        || apiKey.isEmpty()
        || siteKey == null
        || siteKey.isEmpty()
        || baseUri == null
        || baseUri.isEmpty()) {
      throw new IllegalStateException(
          "API Key, Site Key, and Base URI must be provided via command line arguments using the runner.");
    }

    RestAssured.baseURI = baseUri;
    checkSiteVariantsStatus();
    checkSiteStoreStatus();
  }

  @Before
  public void beforeScenario(Scenario scenario) {
    this.currentScenario = scenario;
    this.rawResponse = null;
    this.response = null;
    this.initialSearchResponse = null;
    this.subsequentSearchResponse = null;
    this.initialNumberOfProducts = null;
    this.subsequentNumberOfProducts = null;
    this.dynamicallyFoundStoreIds.clear();
    Collection<String> tags = scenario.getSourceTagNames();

    if (tags.contains("@requiresVariantsDisabled")) {
      System.out.println("Scenario tagged with @requiresVariantsDisabled. Checking site status...");
      if (siteSupportsVariants == null) {
        Assumptions.abort(
            "Skipping scenario: Could not determine variants status in preliminary check.");
      } else if (siteSupportsVariants) {
        Assumptions.abort(
            "Skipping scenario: Variants are ENABLED for site '"
                + siteKey
                + "', but scenario requires them to be disabled.");
      } else {
        System.out.println(
            "Proceeding with scenario: Variants are DISABLED for site '"
                + siteKey
                + "' as required.");
      }
    }
    if (tags.contains("@requiresVariantsEnabled")) {
      System.out.println("Scenario tagged with @requiresVariantsEnabled. Checking site status...");
      if (siteSupportsVariants == null) {
        Assumptions.abort(
            "Skipping scenario: Could not determine variants status in preliminary check.");
      } else if (!siteSupportsVariants) {
        Assumptions.abort(
            "Skipping scenario: Variants are effectively DISABLED for site '"
                + siteKey
                + "' (siteSupportsVariants=false), but scenario requires them to be enabled/supported.");
      } else {
        System.out.println(
            "Proceeding with scenario: Variants seem ENABLED or supported for site '"
                + siteKey
                + "' (siteSupportsVariants=true) as required.");
      }
    }
    if (tags.contains("@requiresSpellcheckFallback")) {
      System.out.println("Scenario tagged with @requiresSpellcheckFallback. Checking fallback status...");
      Response checkResponse = performSearchRequest(Map.of("q", "clithes"));
      String fallbackMethod = checkResponse.jsonPath().getString("searchMetaData.queryParams.'fallback.method'");

      System.out.println("Fallback method found in response: " + fallbackMethod);

      if (fallbackMethod == null || !fallbackMethod.equals("spellcheck")) {
        Assumptions.abort(
            "Skipping scenario: Fallback method is not 'spellcheck' as expected. Found: "
                + fallbackMethod);
      } else {
        System.out.println("Proceeding with scenario: Fallback method is 'spellcheck' as required.");
      }
    }
    if (tags.contains("@requiresStoreDisabled")) {
      System.out.println("Scenario tagged with @requiresStoreDisabled. Checking site status...");
      if (siteSupportsStore == null) {
        Assumptions.abort(
            "Skipping scenario: Could not determine store status in preliminary check.");
      } else if (siteSupportsStore) {
        Assumptions.abort(
            "Skipping scenario: Store is ENABLED for site '"
                + siteKey
                + "', but scenario requires it to be disabled.");
      } else {
        System.out.println(
            "Proceeding with scenario: Store is DISABLED for site '"
                + siteKey
                + "' as required.");
      }
    }
    if (tags.contains("@requiresStoreEnabled")) {
      System.out.println("Scenario tagged with @requiresStoreEnabled. Checking site status...");
      if (siteSupportsStore == null) {
        Assumptions.abort(
            "Skipping scenario: Could not determine store status in preliminary check.");
      } else if (!siteSupportsStore) {
        Assumptions.abort(
            "Skipping scenario: Store is DISABLED for site '"
                + siteKey
                + "', but scenario requires it to be enabled.");
      } else {
        System.out.println(
            "Proceeding with scenario: Store is ENABLED for site '"
                + siteKey
                + "' as required.");
      }
    }
    if (tags.contains("@skipAssertionsIfStoreEnabled")) {
      System.out.println("Scenario tagged with @skipAssertionsIfStoreEnabled. Checking site status...");
      if (siteSupportsStore != null && siteSupportsStore) {
        Assumptions.abort(
            "Skipping scenario: Test is flagged with @skipAssertionsIfStoreEnabled and site '"
                + siteKey
                + "' is store-enabled.");
      } else {
        System.out.println(
            "Proceeding with scenario: The site is not store-enabled, so this test is applicable.");
      }
    }
  }

  @Given("the API key is provided")
  public void the_api_key_is_provided() {
	  if (currentScenario.getSourceTagNames().contains("@skipKeyValidation")) {
		     return;
		   }
    assertNotNull(apiKey, "API Key should be provided");
    assertFalse(apiKey.isEmpty(), "API Key should not be empty");
//    if (apiKey == null || apiKey.isEmpty()) {
//    	System.out.println("→ API key is empty; skipping presence check");
//    	return;
//    	  }
//    	  assertNotNull(apiKey, "API Key should be provided");
//    	  assertFalse(apiKey.isEmpty(), "API Key should not be empty");
  }

  @Given("the site key is provided")
  public void the_site_key_is_provided() {
	  if (currentScenario.getSourceTagNames().contains("@skipKeyValidation")) {
		     return;
		   }
    assertNotNull(siteKey, "Site Key should be provided");
    assertFalse(siteKey.isEmpty(), "Site Key should not be empty");
//    if (siteKey == null || siteKey.isEmpty()) {
//    	System.out.println("→ SITE key is empty; skipping presence check");
//    	return;
//    	  }
//    	  assertNotNull(siteKey, "SITE Key should be provided");
//    	  assertFalse(siteKey.isEmpty(), "SITE Key should not be empty");
  }

  
  @Given("the API key is not provided")
  public void the_api_key_is_not_provided() {
	  apiKey = "";
  }
  @Given("the site key is not provided")
  public void the_site_key_is_not_provided() {
	  siteKey = "";
}
  
  
  private Response performSearchRequest(Map<String, String> queryParams) {
    Response res = given()
        .contentType(ContentType.JSON)
        .queryParams(queryParams)
        .when()
        .get("/{apiKey}/{siteKey}/search", apiKey, siteKey);
    return res;
  }

  private ValidatableResponse searchRequest(Map<String, String> queryParams) {
    this.rawResponse = performSearchRequest(queryParams);
    this.response = this.rawResponse.then();
    return this.response;
  }

  @When("a search request is sent using the correct API key and an incorrect site key for query {string}")
  public void a_search_request_is_sent_using_correct_api_key_incorrect_site_key(String query) {
    response = given()
        .contentType(ContentType.JSON)
        .queryParam("q", query)
        .when()
        .get("/{apiKey}/{siteKey}/search", apiKey, INCORRECT_SITE_KEY)
        .then();
  }

  @When("a search request is sent using an incorrect API key and the correct site key for query {string}")
  public void a_search_request_is_sent_using_incorrect_api_key_correct_site_key(String query) {
    response = given()
        .contentType(ContentType.JSON)
        .queryParam("q", query)
        .when()
        .get("/{apiKey}/{siteKey}/search", INCORRECT_API_KEY, siteKey)
        .then();
  }

  @When("a GET request is sent to {string} with query {string}")
  public void a_get_request_is_sent_to_with_query(String path, String query) {
    response = given().contentType(ContentType.JSON).when().get(path + "?q=" + query).then();
  }

  @When("a search is performed for query {string}")
  public void a_search_is_performed_for_query(String query) {
    response = searchRequest(Map.of("q", query));
  }

  @When("a search is performed for query {string} with filter {string}")
  public void a_search_is_performed_for_query_with_filter(String query, String filter) {
    this.rawResponse = given()
        .contentType(ContentType.JSON)
        .queryParams(Map.of("q", query, "filter", filter))
        .when()
        .get("/{apiKey}/{siteKey}/search", apiKey, siteKey);
    this.response = this.rawResponse.then();
  }

  @When("a search is performed for query {string} sorted by {string} in {string} order")
  public void a_search_is_performed_for_query_sorted_by_in_order(
      String query, String sortField, String sortOrder) {
    response = searchRequest(Map.of("q", query, "sort", sortField + " " + sortOrder));
  }

  @When("a search is performed for query {string} with {string} items per page")
  public void a_search_is_performed_for_query_with_items_per_page(String query, String rows) {
    response = searchRequest(Map.of("q", query, "rows", String.valueOf(rows)));
  }

  @When("the search endpoint is requested without a query parameter")
  public void the_search_endpoint_is_requested_without_query_parameter() {
    response = given()
        .contentType(ContentType.JSON)
        .when()
        .get("/{apiKey}/{siteKey}/search", apiKey, siteKey)
        .then();
  }

  @When("a search is attempted for query {string} with sort parameter {string}")
  public void a_search_is_attempted_for_query_with_sort_parameter(String query, String sortValue) {
    response = searchRequest(
        Map.of(
            "q", query,
            "sort", sortValue));
  }

  @When("a search is performed for query {string} with rows {string} and start {string}")
  public void a_search_is_performed_for_query_with_rows_and_start(
      String query, String rows, String start) {
    System.out.println(
        "Performing search for: " + query + " with rows: " + rows + " and start: " + start);
    response = searchRequest(
        Map.of(
            "q", query,
            "rows", rows,
            "start", start));
  }

  @When("a request is sent to the API sub-path {string} with query {string}")
  public void a_request_is_sent_to_api_sub_path_with_query(String subPath, String query) {
    String fullPath = String.format("/%s/%s%s", apiKey, siteKey, subPath);
    response = given()
        .contentType(ContentType.JSON)
        .queryParam("q", query)
        .urlEncodingEnabled(true)
        .when()
        .get(fullPath)
        .then();
  }

  @When("a search is performed for query {string} with parameters:")
  public void a_search_is_performed_for_query_with_parameters(String query, DataTable paramsTable) {
    Map<String, String> queryParams = new HashMap<>(paramsTable.asMap(String.class, String.class));
    queryParams.put("q", query);

    System.out.println(
        "Performing search for query: \"" + query + "\" with parameters: " + queryParams);

    response = searchRequest(queryParams);
  }
  // @When("a category request is made for path {string}")
  // public void a_category_request_is_made_for_path(String categoryPath) {
  // Map<String, String> queryParams = Map.of("p", categoryPath);
  // this.rawResponse =
  // given()
  // .contentType(ContentType.JSON)
  // .queryParams(queryParams)
  // .when()
  // .get("/{apiKey}/{siteKey}/category", apiKey, siteKey);
  // this.response = rawResponse.then();
  // }

  @When("A subsequent search for query {string} with variants and store disabled is performed")
  public void perform_a_subsequent_search_for_query_with_variants_and_store_disabled(String query) {
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("q", query);
    queryParams.put("variants", "false");
    queryParams.put("store", "false");

    System.out.println("Performing subsequent search with variants=false and store=false for query: '" + query + "'");
    this.subsequentSearchResponse = performSearchRequest(queryParams);

    try {
      this.subsequentNumberOfProducts = this.subsequentSearchResponse.jsonPath().getInt("response.numberOfProducts");
      System.out.println(
          "Subsequent search (variants=false, store=false) numberOfProducts: " + this.subsequentNumberOfProducts);
    } catch (Exception e) {
      System.err.println(
          "Failed to extract numberOfProducts from subsequent search: " + e.getMessage());
      fail("Could not extract 'response.numberOfProducts' from the subsequent search response.");
    }
    this.rawResponse = this.subsequentSearchResponse;
    this.response = this.rawResponse.then();
  }

  @When("a search is performed with the raw query string {string}")
  public void a_search_is_performed_with_the_raw_query_string(String rawQueryString) {
    String fullPath = String.format(
        "/%s/%s/search?%s",
        apiKey,
        siteKey,
        rawQueryString);

    System.out.println(
        "Performing search with a raw query string: " + fullPath);
    this.rawResponse = given()
        .contentType(ContentType.JSON)
        .when()
        .get(fullPath);

    this.response = this.rawResponse.then();
  }

  @When("a request is sent to get the index fields")
  public void a_request_is_sent_to_get_the_index_fields() {
    this.rawResponse = given()
        .baseUri(SCHEMA_BASE_URI)
        .contentType(ContentType.JSON)
        .when()
        .get("/api/{siteKey}/getIndexFields", siteKey);
    this.response = this.rawResponse.then();
  }

  @Then("the response should be received within {int} milliseconds")
  public void the_response_should_be_received_within_milliseconds(int milliseconds) {
    assertNotNull(response, "Response object should not be null");
    response.time(lessThan((long) milliseconds), TimeUnit.MILLISECONDS);
  }

  @Then("the response status code should be {int}")
  public void the_response_status_code_should_be(int statusCode) {
    if (currentScenario.getSourceTagNames().contains("@skipAssertionsIfVariantsFalse")
        && siteSupportsVariants != null
        && !siteSupportsVariants) {
      Assumptions.abort(
          "Skipping assertion: Scenario tagged @skipAssertionsIfVariantsFalse and variants are disabled.");
    }
    assertNotNull(response, "Response object should not be null for status code check");
    response.statusCode(statusCode);
  }

  @Then("the response should contain status {string}")
  public void the_response_should_contain_status(String status) {
    assertNotNull(response, "Response object should not be null");
    response.body("status", equalTo(status));
  }

  @Then("the response should not contain {string}")
  public void the_response_should_not_contain(String content) {
    response.body(not(containsString(content)));
  }

  @Then("the searchMetaData status should be {int}")
  public void the_search_meta_data_status_should_be(int status) {
    if (currentScenario.getSourceTagNames().contains("@skipAssertionsIfVariantsFalse")
        && siteSupportsVariants != null
        && !siteSupportsVariants) {
      Assumptions.abort(
          "Skipping assertion: Scenario tagged @skipAssertionsIfVariantsFalse and variants are disabled.");
    }
    assertNotNull(response, "Response object should not be null for metaData check");
    response.body("searchMetaData.status", equalTo(status));
  }

  @Then("the response should contain more than {int} products")
  public void the_response_should_contain_more_than_products(int count) {
    assertNotNull(response, "Response object should not be null");
    response.body("response.numberOfProducts", greaterThan(count));
  }

  @Then("each product should have required fields")
  public void each_product_should_have_required_fields() {
    List<Map<String, Object>> products = response.extract().path("response.products");
    for (Map<String, Object> product : products) {
      assertNotNull(product.get("uniqueId"), "Product missing uniqueId");
      assertTrue(
          product.containsKey("categoryPath") || product.containsKey("collections") || product.containsKey("store"),
          "Product missing either 'categoryPath', 'collections' or 'store' field");
    }
  }

  @Then("the original query should match {string}")
  public void the_original_query_should_match(String query) {
    assertNotNull(response, "Response object should not be null");
    response.body("searchMetaData", notNullValue());
    response.body("searchMetaData.queryParams", notNullValue());

    Map<String, String> queryParams = response.extract().path("searchMetaData.queryParams");
    assertTrue(
        queryParams.containsKey("q"),
        "Response metadata queryParams does not contain the key 'q'. Found keys: "
            + queryParams.keySet());

    assertEquals(
        query, queryParams.get("q"), "Value for query parameter 'q' did not match expected value.");
  }

  @Then("the query parameters should contain {string}")
  public void the_query_parameters_should_contain(String filterField) {
    Map<String, String> queryParams = response.extract().path("searchMetaData.queryParams");
    assertNotNull(queryParams);
    assertTrue(
        queryParams.toString().contains(filterField),
        "Param should be reflected in query parameters");
  }

  @Then("the response should contain {int} products")
  public void the_response_should_contain_products(int count) {
    int productCount = response.extract().path("response.numberOfProducts");
    assertEquals(count, productCount, "Expected " + count + " products");
  }

  @Then("if products exist, they should be returned")
  public void if_products_exist_they_should_be_returned() {
    assertNotNull(response, "Response object should not be null");
    response.body("response.numberOfProducts", notNullValue());
    int productCount = response.extract().path("response.numberOfProducts");
    if (productCount > 0) {
      response.body("response.products", notNullValue());
      response.body("response.products", instanceOf(List.class));
      response.body("response.products", not(empty()));
    }
  }

  @Then("the response message should be {string}")
  public void the_response_message_should_be(String expectedMessage) {
    assertNotNull(response, "Response object should not be null");
    response.body("message", equalTo(expectedMessage));
  }

  @Then("the response error msg should contain {string}")
  public void the_response_error_msg_should_contain(String expectedSubstring) {
    if (currentScenario.getSourceTagNames().contains("@skipAssertionsIfVariantsFalse")
        && siteSupportsVariants != null
        && !siteSupportsVariants) {
      Assumptions.abort(
          "Skipping assertion: Scenario tagged @skipAssertionsIfVariantsFalse and variants are disabled.");
    }
    assertNotNull(response, "Response object should not be null for error message check");
    response.body("error", notNullValue());
    response.body("error.msg", containsString(expectedSubstring));
  }

  @Then("the response error message should contain {string}")
  public void the_response_error_message_should_contain(String expectedSubstring) {
    assertNotNull(response, "Response object should not be null");
    response.body("error.errmessage", containsString(expectedSubstring));
  }

  @Then("the response error code should be {int}")
  public void the_response_error_code_should_be(int expectedCode) {
    if (currentScenario.getSourceTagNames().contains("@skipAssertionsIfVariantsFalse")
        && siteSupportsVariants != null
        && !siteSupportsVariants) {
      Assumptions.abort(
          "Skipping assertion: Scenario tagged @skipAssertionsIfVariantsFalse and variants are disabled.");
    }
    assertNotNull(response, "Response object should not be null for error code check");
    response.body("error", notNullValue());
    response.body("error.code", equalTo(expectedCode));
  }

  @Then("the response error status_code should be {int}")
  public void the_response_error_status_code_should_be(int expectedStatusCode) {
    assertNotNull(response, "Response object should not be null");
    response.body("error.status_code", equalTo(expectedStatusCode));
  }

  private static void checkSiteVariantsStatus() {
    System.out.println("Performing preliminary check for variants status for site: " + siteKey);
    try {
      Response qStarResponse = given()
          .contentType(ContentType.JSON)
          .queryParams(Map.of("q", "*"))
          .when()
          .get("/{apiKey}/{siteKey}/search", apiKey, siteKey)
          .thenReturn();

      Response qStarVariantsFalseResponse = given()
          .contentType(ContentType.JSON)
          .queryParams(Map.of("q", "*", "variants", "false"))
          .when()
          .get("/{apiKey}/{siteKey}/search", apiKey, siteKey)
          .thenReturn();

      if (qStarResponse.getStatusCode() != 200) {
        System.err.println("Preliminary check: q=* request failed with status: " + qStarResponse.getStatusCode());
        siteSupportsVariants = null;
        return;
      }
      if (qStarVariantsFalseResponse.getStatusCode() != 200) {
        System.err.println("Preliminary check: q=*&variants=false request failed with status: "
            + qStarVariantsFalseResponse.getStatusCode());
        siteSupportsVariants = null;
        return;
      }

      JsonPath qStarJsonPath = qStarResponse.jsonPath();
      JsonPath qStarVariantsFalseJsonPath = qStarVariantsFalseResponse.jsonPath();
      String qStarVariantsParam = qStarJsonPath.getString("searchMetaData.queryParams.variants");
      if ("false".equals(qStarVariantsParam)) {
        siteSupportsVariants = false;
        System.out.println(
            "Preliminary check: Variants implicitly disabled (q=* response contained variants=false in metadata).");
      } else if ("true".equals(qStarVariantsParam)) {
        siteSupportsVariants = true;
        System.out.println(
            "Preliminary check: Variants explicitly enabled (q=* response contained variants=true in metadata).");
      }

      else {
        Integer initialNumberOfProducts = qStarJsonPath.getInt("response.numberOfProducts");
        Integer subsequentNumberOfProducts = qStarVariantsFalseJsonPath.getInt("response.numberOfProducts");

        if (initialNumberOfProducts != null && subsequentNumberOfProducts != null) {
          if (initialNumberOfProducts.equals(subsequentNumberOfProducts)) {
            siteSupportsVariants = false;
            System.out.println(
                "Preliminary check: Variants effectively disabled (q=* product count matches q=*&variants=false).");
          } else {
            siteSupportsVariants = true;
            System.out.println(
                "Preliminary check: Variants appear enabled (q=* product count differs from q=*&variants=false).");
          }
        } else {
          System.err.println("Could not get product counts for preliminary variant checks. Defaulting to true.");
          siteSupportsVariants = true;
        }
      }

      System.out.println(
          "Final preliminary variants status for site " + siteKey + ": siteSupportsVariants = " + siteSupportsVariants);

    } catch (Exception e) {
      System.err.println("Error during preliminary variants check: " + e.getMessage());
      e.printStackTrace();
      siteSupportsVariants = null;
    }
  }

  @When("an initial search is performed for query {string}")
  public void an_initial_search_is_performed_for_query(String query) {
    System.out.println(
        "Performing initial search for query: '" + query + "'");
    Map<String, String> queryParams = Map.of("q", query);
    this.initialSearchResponse = performSearchRequest(queryParams);

    try {
      this.initialNumberOfProducts = this.initialSearchResponse.jsonPath().getInt("response.numberOfProducts");
    } catch (Exception e) {
      System.err.println(
          "Failed to extract numberOfProducts from initial search: " + e.getMessage());
      fail("Could not extract 'response.numberOfProducts' from the initial search response.");
    }
    this.rawResponse = this.initialSearchResponse;
    this.response = this.rawResponse.then();
  }

  @When("A subsequent search for query {string} with variants disabled is performed")
  public void perform_a_subsequent_search_for_query_with_variants_disabled(String query) {
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("q", query);
    queryParams.put("variants", "false");

    this.subsequentSearchResponse = performSearchRequest(queryParams);

    try {
      this.subsequentNumberOfProducts = this.subsequentSearchResponse.jsonPath().getInt("response.numberOfProducts");
      System.out.println("Subsequent search numberOfProducts: " + this.subsequentNumberOfProducts);
    } catch (Exception e) {
      System.err.println(
          "Failed to extract numberOfProducts from subsequent search: " + e.getMessage());
      fail("Could not extract 'response.numberOfProducts' from the subsequent search response.");
    }
    this.rawResponse = this.subsequentSearchResponse;
    this.response = this.rawResponse.then();
  }

  @When("two distinct store IDs are extracted from the products")
  public void two_distinct_store_ids_are_extracted_from_the_products() {
    assertNotNull(initialSearchResponse, "This step requires an initial search to have been performed first.");
    JsonPath jsonPath = this.initialSearchResponse.jsonPath();
    List<Map<String, Object>> products = jsonPath.getList("response.products");
    assertNotNull(products, "Products list is null in the initial search response.");
    assertFalse(products.isEmpty(), "No products found in the initial search response.");
    for (Map<String, Object> product : products) {
      if (this.dynamicallyFoundStoreIds.size() >= 2) {
        break;
      }

      List<Map<String, Object>> storesToCheck = new ArrayList<>();
      if (Boolean.TRUE.equals(siteSupportsVariants) && product.containsKey("variants")) {
        List<Map<String, Object>> variants = (List<Map<String, Object>>) product.get("variants");
        if (variants != null) {
          for (Map<String, Object> variant : variants) {
            if (variant.containsKey("store")) {
              storesToCheck.addAll((List<Map<String, Object>>) variant.get("store"));
            }
          }
        }
      }
      if (product.containsKey("store")) {
        storesToCheck.addAll((List<Map<String, Object>>) product.get("store"));
      }

      for (Map<String, Object> store : storesToCheck) {
        String storeId = (String) store.get("storeId");
        if (storeId != null && !storeId.isEmpty() && !this.dynamicallyFoundStoreIds.contains(storeId)) {
          this.dynamicallyFoundStoreIds.add(storeId);
          if (this.dynamicallyFoundStoreIds.size() >= 2)
            break;
        }
      }
    }

    if (this.dynamicallyFoundStoreIds.size() < 2) {
      fail("Could not find two distinct store IDs in the initial search result. Found: "
          + this.dynamicallyFoundStoreIds.size());
    }

    System.out.println("Successfully extracted two distinct store IDs: " + this.dynamicallyFoundStoreIds);
  }

  @When("a subsequent search is performed using the two found store IDs")
  public void a_subsequent_search_is_performed_using_the_two_found_store_ids() {
    assertTrue(this.dynamicallyFoundStoreIds.size() >= 2,
        "Cannot perform subsequent search because two store IDs were not found in the initial search.");

    String storeIdParamValue = String.join(",", this.dynamicallyFoundStoreIds);
    System.out.println("Performing subsequent search with multiple store.id: " + storeIdParamValue);
    Map<String, String> queryParams = Map.of(
        "q", "*",
        "store.id", storeIdParamValue);
    searchRequest(queryParams);
  }

  @When("four distinct store IDs are extracted from the products")
  public void four_distinct_store_ids_are_extracted_from_the_products() {
    assertNotNull(initialSearchResponse, "This step requires an initial search to have been performed first.");
    JsonPath jsonPath = this.initialSearchResponse.jsonPath();
    List<Map<String, Object>> products = jsonPath.getList("response.products");
    assertNotNull(products, "Products list is null in the initial search response.");
    assertFalse(products.isEmpty(), "No products found in the initial search response.");

    for (Map<String, Object> product : products) {
      if (this.dynamicallyFoundStoreIds.size() >= 4) {
        break;
      }

      List<Map<String, Object>> storesToCheck = new ArrayList<>();

      if (Boolean.TRUE.equals(siteSupportsVariants) && product.containsKey("variants")) {
        List<Map<String, Object>> variants = (List<Map<String, Object>>) product.get("variants");
        if (variants != null) {
          for (Map<String, Object> variant : variants) {
            if (variant.containsKey("store")) {
              storesToCheck.addAll((List<Map<String, Object>>) variant.get("store"));
            }
          }
        }
      }
      if (product.containsKey("store")) {
        storesToCheck.addAll((List<Map<String, Object>>) product.get("store"));
      }

      for (Map<String, Object> store : storesToCheck) {
        String storeId = (String) store.get("storeId");
        if (storeId != null && !storeId.isEmpty() && !this.dynamicallyFoundStoreIds.contains(storeId)) {
          this.dynamicallyFoundStoreIds.add(storeId);
          if (this.dynamicallyFoundStoreIds.size() >= 4)
            break;
        }
      }
    }

    if (this.dynamicallyFoundStoreIds.size() < 4) {
      fail("Could not find 4 distinct store IDs in the initial search result. Found: "
          + this.dynamicallyFoundStoreIds.size());
    }

    System.out.println("Successfully extracted four distinct store IDs: " + this.dynamicallyFoundStoreIds);
  }

  @When("a subsequent search is performed using the four found store IDs as multiple parameters")
  public void a_subsequent_search_is_performed_using_the_four_found_store_ids_as_multiple_parameters() {
    assertTrue(this.dynamicallyFoundStoreIds.size() >= 4,
        "Cannot perform subsequent search because four store IDs were not found in the initial search.");

    String storeIdParamValue1 = String.join(",", this.dynamicallyFoundStoreIds.subList(0, 2));
    String storeIdParamValue2 = String.join(",", this.dynamicallyFoundStoreIds.subList(2, 4));
    String rawQueryString = String.format("q=*&store.id=%s&store.id=%s", storeIdParamValue1, storeIdParamValue2);

    String fullPath = String.format("/%s/%s/search?%s", apiKey, siteKey, rawQueryString);

    System.out.println("Performing search with a raw query string: " + fullPath);

    this.rawResponse = given()
        .contentType(ContentType.JSON)
        .when()
        .get(fullPath);

    this.response = this.rawResponse.then();
  }

  @When("a subsequent search is performed with facet sorting by {string}")
  public void a_subsequent_search_is_performed_with_facet_sorting_by(String sortField) {
    System.out.println("Performing subsequent search with facet.sort=" + sortField);
    Map<String, String> queryParams = Map.of("q", "*", "facet.sort", sortField);
    searchRequest(queryParams);
  }

  @Then("the response status code should be 200 for both searches")
  public void the_response_status_code_should_be_200_for_both_searches() {
    assertNotNull(initialSearchResponse, "Initial search response should not be null");
    assertEquals(
        200, initialSearchResponse.getStatusCode(), "Initial search status code should be 200");

    assertNotNull(subsequentSearchResponse, "Subsequent search response should not be null");
    assertEquals(
        200,
        subsequentSearchResponse.getStatusCode(),
        "Subsequent search status code should be 200");
  }

  @Then("the number of products in the subsequent search should be greater than the initial search")
  public void the_number_of_products_in_the_subsequent_search_should_be_greater_than_the_initial_search() {
    assertNotNull(initialNumberOfProducts, "Initial number of products was not captured.");
    assertNotNull(subsequentNumberOfProducts, "Subsequent number of products was not captured.");
    assertTrue(
        subsequentNumberOfProducts > initialNumberOfProducts,
        "Expected numberOfProducts with variants=false ("
            + subsequentNumberOfProducts
            + ") to be greater than without explicit variants ("
            + initialNumberOfProducts
            + ")");
  }

  @Then("the response should contain fallback with corrected query {string}")
  public void the_response_should_contain_corrected_fallback(String correctedQuery) {
    assertNotNull(response, "Response object should not be null");
    response.body("searchMetaData.fallback.q", equalTo(correctedQuery));
  }

  @Then("the response should contain {int} or more products")
  public void the_response_should_contain_more_than_equal_products(int count) {
    assertNotNull(response, "Response object should not be null");
    response.body("response.numberOfProducts", greaterThanOrEqualTo(count));
  }

  @Then("the first product in the response should contain a doctype field")
  public void the_first_product_should_contain_doctype_field() {
    JsonPath jsonPath = rawResponse.jsonPath();

    List<Map<String, Object>> products = jsonPath.getList("response.products");
    assertNotNull(products, "Products list should not be null");
    assertFalse(products.isEmpty(), "Products list should not be empty");

    Map<String, Object> firstProduct = products.get(0);
    assertTrue(firstProduct.containsKey("doctype"), "First product should contain 'doctype' field");

    Object doctype = firstProduct.get("doctype");
    assertNotNull(doctype, "'doctype' field should not be null");
    assertTrue(doctype instanceof String, "'doctype' field should be a string");
    System.out.println("First product doctype: " + doctype);
  }

  @Then("the response error msg should contain one of {string}")
  public void the_response_error_msg_should_contain_one_of(String expectedSubstringsCombined) {
    String[] possibleErrors = expectedSubstringsCombined.split(" or ");
    List<Matcher<String>> errorMatchersList = new ArrayList<>();
    for (String error : possibleErrors) {
      errorMatchersList.add(containsString(error.trim()));
    }
    Matcher<String>[] matchersArray = errorMatchersList.toArray(new Matcher[0]);
    response.body("error.msg", anyOf(matchersArray));
  }

  @Then("a store ID is extracted from the first product")
  public void a_store_id_is_extracted_from_the_first_product() {
    assertNotNull(initialSearchResponse, "This step requires an initial search to have been performed first.");

    JsonPath jsonPath = this.initialSearchResponse.jsonPath();
    String storeIdPath;
    if (Boolean.TRUE.equals(siteSupportsVariants)) {
      storeIdPath = "response.products[0].variants[0].store[0].storeId";
      System.out.println(
          "INFO: Variants are enabled for this site. Trying to extract storeId from variants path: " + storeIdPath);
    } else {
      storeIdPath = "response.products[0].store[0].storeId";
      System.out.println(
          "INFO: Variants are disabled for this site. Trying to extract storeId from product path: " + storeIdPath);
    }

    try {
      this.dynamicallyFoundStoreId = jsonPath.getString(storeIdPath);
      assertNotNull(this.dynamicallyFoundStoreId, "Extracted storeId from response is null. Path: " + storeIdPath);
      assertFalse(this.dynamicallyFoundStoreId.isEmpty(),
          "Extracted storeId from response is empty. Path: " + storeIdPath);

      System.out.println("Successfully extracted dynamic storeId for subsequent test: " + this.dynamicallyFoundStoreId);

    } catch (Exception e) {
      fail("Could not extract a valid 'storeId' from the first product using the expected path: '" + storeIdPath + "'. "
          +
          "Please check the response structure and ensure the product has a store ID. Error: " + e.getMessage());
    }
  }

  @Then("a subsequent search is performed using the found store ID")
  public void a_subsequent_search_is_performed_using_the_found_store_id() {
    assertNotNull(dynamicallyFoundStoreId,
        "Cannot perform subsequent search because store ID was not found in the initial search.");

    System.out.println("Performing subsequent search with dynamically found store.id: " + dynamicallyFoundStoreId);
    Map<String, String> queryParams = Map.of(
        "q", "*",
        "store.id", this.dynamicallyFoundStoreId);
    searchRequest(queryParams);
  }

  @Then("the number of products should be less than the initial search")
  public void the_number_of_products_should_be_less_than_the_initial_search() {
    Integer currentNumberOfProducts = rawResponse.jsonPath().getInt("response.numberOfProducts");
    assertNotNull(
        initialNumberOfProducts,
        "Initial number of products was not captured. Ensure an 'initial search' step was run first.");
    assertNotNull(
        currentNumberOfProducts,
        "Could not extract number of products from the current search response.");

    System.out.println(
        "Comparing product counts: Initial("
            + initialNumberOfProducts
            + "), Current with store filter("
            + currentNumberOfProducts
            + ")");
    assertTrue(
        currentNumberOfProducts > 0,
        "Expected the store-specific search to return at least one product, but found 0. The store ID may have no inventory for the query '*'.");

    assertTrue(
        currentNumberOfProducts < initialNumberOfProducts,
        "Expected number of products with store filter ("
            + currentNumberOfProducts
            + ") to be less than the initial total ("
            + initialNumberOfProducts
            + ")");
  }

  @Then("the query parameters should reflect the found store ID")
  public void the_query_parameters_should_reflect_the_found_store_id() {
    assertNotNull(dynamicallyFoundStoreId,
        "Cannot verify query parameters because store ID was not found in the initial search.");
    response.body("searchMetaData.queryParams.'store.id'", equalTo(this.dynamicallyFoundStoreId));
  }

  @Then("all products in the response should have one of the found store IDs")
  public void all_products_in_the_response_should_have_one_of_the_found_store_ids() {
    assertFalse(this.dynamicallyFoundStoreIds.isEmpty(),
        "Cannot validate response because no store IDs were found in the initial search.");

    JsonPath jsonPath = this.rawResponse.jsonPath();
    List<Map<String, Object>> products = jsonPath.getList("response.products");
    assertNotNull(products, "Products list is null in the subsequent search response.");

    assertFalse(products.isEmpty(),
        "The subsequent search using the found store IDs returned 0 products. This is an error, as a filtered search on existing store IDs should yield at least one result.");

    for (Map<String, Object> product : products) {
      boolean foundMatchingStoreId = false;
      List<Map<String, Object>> storesToCheck = new ArrayList<>();

      if (Boolean.TRUE.equals(siteSupportsVariants) && product.containsKey("variants")) {
        List<Map<String, Object>> variants = (List<Map<String, Object>>) product.get("variants");
        if (variants != null) {
          for (Map<String, Object> variant : variants) {
            if (variant.containsKey("store")) {
              storesToCheck.addAll((List<Map<String, Object>>) variant.get("store"));
            }
          }
        }
      }
      if (product.containsKey("store")) {
        storesToCheck.addAll((List<Map<String, Object>>) product.get("store"));
      }

      for (Map<String, Object> store : storesToCheck) {
        String storeId = (String) store.get("storeId");
        if (this.dynamicallyFoundStoreIds.contains(storeId)) {
          foundMatchingStoreId = true;
          break;
        }
      }
      assertTrue(foundMatchingStoreId,
          "Product with uniqueId '" + product.get("uniqueId") + "' did not have one of the expected store IDs: "
              + this.dynamicallyFoundStoreIds);
    }
    System.out.println("Validation successful: All products in the response have one of the expected store IDs.");
  }

  @Then("the response JSON should not have the key {string}")
  public void the_response_json_should_not_have_the_key(String keyName) {
    assertNotNull(rawResponse, "Raw response is null.");
    JsonPath jsonPath = rawResponse.jsonPath();
    rawResponse.then()
        .body(not(hasKey(keyName)));
  }

  @Then("the response JSON should have the key {string}")
  public void the_response_json_should_have_the_key(String keyName) {
    assertNotNull(rawResponse, "Raw response is null.");
    rawResponse.then()
        .body(hasEntry(keyName, notNullValue()));
  }

  @Then("the site is checked for facet support, skipping if none are found")
  public void the_site_is_checked_for_facet_support_skipping_if_none_are_found() {
    assertNotNull(
        initialSearchResponse,
        "An initial search must be performed before checking for facet support.");

    JsonPath jsonPath = initialSearchResponse.jsonPath();
    List<Object> facetList = jsonPath.getList("facets.text.list");

    if (facetList == null || facetList.isEmpty()) {
      Assumptions.abort(
          "Skipping scenario: Response does not contain a non-empty 'facets.text.list'.");
    }

    System.out.println(
        "Proceeding with scenario: Site supports facets and has "
            + facetList.size()
            + " text facets.");
  }

  @Then("the text facets in the response should be sorted by position in ascending order")
  public void the_text_facets_in_the_response_should_be_sorted_by_position_in_ascending_order() {
    assertNotNull(
        rawResponse, "A subsequent search with sorting must be performed before this step.");
    JsonPath jsonPath = rawResponse.jsonPath();

    List<Map<String, Object>> facetList = jsonPath.getList("facets.text.list");

    if (facetList == null || facetList.size() < 2) {
      System.out.println(
          "Verification skipped: Not enough facets ("
              + (facetList == null ? 0 : facetList.size())
              + ") to check sorting order.");
      return;
    }

    int previousPosition = -1;
    for (int i = 0; i < facetList.size(); i++) {
      Map<String, Object> facet = facetList.get(i);
      assertNotNull(facet.get("position"), "Facet '" + facet.get("displayName") + "' is missing a 'position' field.");
      int currentPosition = ((Number) facet.get("position")).intValue();

      if (i > 0) {
        assertTrue(
            currentPosition >= previousPosition,
            "Facets are not sorted correctly by position. Facet '"
                + facet.get("displayName")
                + "' with position "
                + currentPosition
                + " appeared after facet with position "
                + previousPosition);
      }
      previousPosition = currentPosition;
    }

    System.out
        .println("Validation successful: All " + facetList.size() + " text facets are sorted correctly by position.");
  }

  @Then("each store object in the response should only contain the specified field {string}")
  public void each_store_object_in_the_response_should_only_contain_the_specified_field(
      String expectedField) {
    assertNotNull(
        rawResponse, "A search must be performed before this step can be executed.");
    JsonPath jsonPath = rawResponse.jsonPath();

    List<Map<String, Object>> products = jsonPath.getList("response.products");

    if (products == null || products.isEmpty()) {
      System.out.println(
          "Verification skipped: Response contains no products to validate.");
      return;
    }

    for (Map<String, Object> product : products) {
      List<Map<String, Object>> stores = (List<Map<String, Object>>) product.get("store");

      if (stores == null || stores.isEmpty()) {
        continue;
      }

      for (Map<String, Object> storeObject : stores) {
        assertEquals(
            1,
            storeObject.keySet().size(),
            "Product '"
                + product.get("uniqueId")
                + "' has a store object that should only have 1 key ('"
                + expectedField
                + "') but found "
                + storeObject.keySet().size()
                + ". Keys found: "
                + storeObject.keySet());
        assertTrue(
            storeObject.containsKey(expectedField),
            "Product '"
                + product.get("uniqueId")
                + "' has a store object that is missing the expected key '"
                + expectedField
                + "'. Keys found: "
                + storeObject.keySet());
      }
    }

    System.out.println(
        "Validation successful: All store objects in the response contain only the '"
            + expectedField
            + "' field as specified.");
  }

  @Then("each store object in the response should only contain the fields:")
  public void each_store_object_in_the_response_should_only_contain_the_fields(
      DataTable dataTable) {
    assertNotNull(
        rawResponse, "A search must be performed before this step can be executed.");
    List<String> expectedFieldsList = dataTable.asList(String.class);
    Set<String> expectedFieldsSet = new HashSet<>(expectedFieldsList);

    JsonPath jsonPath = rawResponse.jsonPath();
    List<Map<String, Object>> products = jsonPath.getList("response.products");

    if (products == null || products.isEmpty()) {
      System.out.println(
          "Verification skipped: Response contains no products to validate.");
      return;
    }

    for (Map<String, Object> product : products) {
      List<Map<String, Object>> stores = (List<Map<String, Object>>) product.get("store");

      if (stores == null || stores.isEmpty()) {
        continue;
      }

      for (Map<String, Object> storeObject : stores) {
        Set<String> actualFields = storeObject.keySet();
        assertEquals(
            expectedFieldsSet,
            actualFields,
            "Product '"
                + product.get("uniqueId")
                + "' has a store object with incorrect fields. Expected: "
                + expectedFieldsSet
                + ", but found: "
                + actualFields);
      }
    }

    System.out.println(
        "Validation successful: All store objects contain the exact set of expected fields: "
            + expectedFieldsSet);
  }

  @Then("each store object inside a variant should contain the exact fields:")
  public void each_store_object_inside_a_variant_should_contain_the_exact_fields(
      DataTable dataTable) {
    assertNotNull(
        rawResponse, "A search must be performed before this step can be executed.");
    Set<String> expectedFieldsSet = new HashSet<>(dataTable.asList(String.class));

    JsonPath jsonPath = rawResponse.jsonPath();
    List<Map<String, Object>> products = jsonPath.getList("response.products");

    if (products == null || products.isEmpty()) {
      System.out.println(
          "Verification skipped: Response contains no products to validate.");
      return;
    }

    for (Map<String, Object> product : products) {
      List<Map<String, Object>> variants = (List<Map<String, Object>>) product.get("variants");
      if (variants == null || variants.isEmpty()) {
        continue;
      }

      for (Map<String, Object> variant : variants) {
        List<Map<String, Object>> stores = (List<Map<String, Object>>) variant.get("store");
        if (stores == null || stores.isEmpty()) {
          continue;
        }

        for (Map<String, Object> storeObject : stores) {
          Set<String> actualFields = storeObject.keySet();

          assertEquals(
              expectedFieldsSet,
              actualFields,
              String.format(
                  "Product '%s', Variant '%s' has a store object with incorrect fields. Expected: %s, but found: %s",
                  product.get("uniqueId"),
                  variant.get("uniqueId"),
                  expectedFieldsSet,
                  actualFields));
        }
      }
    }

    System.out.println(
        "Validation successful: All store objects within variants contain the exact set of expected fields: "
            + expectedFieldsSet);
  }

  @Then("each store object in the response should be empty")
  public void each_store_object_in_the_response_should_be_empty() {
    assertNotNull(
        rawResponse, "A search must be performed before this step can be executed.");
    JsonPath jsonPath = rawResponse.jsonPath();

    List<Map<String, Object>> products = jsonPath.getList("response.products");

    if (products == null || products.isEmpty()) {
      System.out.println(
          "Verification skipped: Response contains no products to validate.");
      return;
    }

    for (Map<String, Object> product : products) {
      List<Map<String, Object>> stores = (List<Map<String, Object>>) product.get("store");

      if (stores == null || stores.isEmpty()) {
        continue;
      }

      for (Map<String, Object> storeObject : stores) {
        assertTrue(
            storeObject.isEmpty(),
            String.format(
                "Product '%s' has a store object that was expected to be empty, but it contained: %s",
                product.get("uniqueId"),
                storeObject));
      }
    }

    System.out.println(
        "Validation successful: All store objects in the response are empty as expected.");
  }
  @Then("each store object inside a variant should have the field {string} with value {string}")
  public void each_store_object_inside_a_variant_should_have_the_field_with_value(
      String fieldName, String expectedValue) {
    assertNotNull(
        rawResponse, "A search must be performed before this step can be executed.");

    JsonPath jsonPath = rawResponse.jsonPath();
    List<Map<String, Object>> products = jsonPath.getList("response.products");

    if (products == null || products.isEmpty()) {
      System.out.println(
          "Verification skipped: Response contains no products with store data to validate.");
      return;
    }

    boolean foundAtLeastOneStore = false;
    for (Map<String, Object> product : products) {
      List<Map<String, Object>> variants = (List<Map<String, Object>>) product.get("variants");
      if (variants == null || variants.isEmpty()) {
        continue;
      }

      for (Map<String, Object> variant : variants) {
        List<Map<String, Object>> stores = (List<Map<String, Object>>) variant.get("store");
        if (stores == null || stores.isEmpty()) {
          continue;
        }

        foundAtLeastOneStore = true;

        for (Map<String, Object> storeObject : stores) {
          assertTrue(
              storeObject.containsKey(fieldName),
              String.format(
                  "Product '%s', Variant '%s': Store object is missing the expected field '%s'. Found keys: %s",
                  product.get("uniqueId"),
                  variant.get("uniqueId"),
                  fieldName,
                  storeObject.keySet()));
          Object actualValue = storeObject.get(fieldName);
          assertEquals(
              expectedValue,
              String.valueOf(actualValue),
              String.format(
                  "Product '%s', Variant '%s': Field '%s' has an incorrect value.",
                  product.get("uniqueId"),
                  variant.get("uniqueId"),
                  fieldName));
        }
      }
    }
    assertTrue(foundAtLeastOneStore, "The search returned products, but none of them had any store information after applying the filter.");

    System.out.println(
        "Validation successful: All found store objects have the field '"
            + fieldName
            + "' with the value '"
            + expectedValue
            + "'.");
  }
  @Then("each store object inside a variant should have the numeric field {string} within the range {int} to {int}")
  public void each_store_object_inside_a_variant_should_have_the_numeric_field_within_the_range(
      String fieldName, int lowerBound, int upperBound) {
    assertNotNull(
        rawResponse, "A search must be performed before this step can be executed.");

    JsonPath jsonPath = rawResponse.jsonPath();
    List<Map<String, Object>> products = jsonPath.getList("response.products");

    if (products == null || products.isEmpty()) {
      System.out.println(
          "Verification skipped: Response contains no products with store data to validate.");
      return;
    }

    boolean foundAtLeastOneStore = false;
    for (Map<String, Object> product : products) {
      List<Map<String, Object>> variants = (List<Map<String, Object>>) product.get("variants");
      if (variants == null || variants.isEmpty()) {
        continue;
      }

      for (Map<String, Object> variant : variants) {
        List<Map<String, Object>> stores = (List<Map<String, Object>>) variant.get("store");
        if (stores == null || stores.isEmpty()) {
          continue;
        }

        foundAtLeastOneStore = true;

        for (Map<String, Object> storeObject : stores) {
          assertTrue(
              storeObject.containsKey(fieldName),
              String.format(
                  "Product '%s', Variant '%s': Store object is missing the expected field '%s'.",
                  product.get("uniqueId"), variant.get("uniqueId"), fieldName));

          Object actualValue = storeObject.get(fieldName);
          assertNotNull(actualValue, "The value for field '" + fieldName + "' was null.");
          assertTrue(actualValue instanceof Number,
              String.format("The value for field '%s' was not a number. Found value: '%s'",
                  fieldName, actualValue));
          double numericValue = ((Number) actualValue).doubleValue();

          assertTrue(
              numericValue >= lowerBound && numericValue <= upperBound,
              String.format(
                  "Product '%s', Variant '%s': Field '%s' has a value of %s which is outside the expected range [%d, %d].",
                  product.get("uniqueId"),
                  variant.get("uniqueId"),
                  fieldName,
                  numericValue,
                  lowerBound,
                  upperBound));
        }
      }
    }
    assertTrue(foundAtLeastOneStore, "The search returned products, but none of them had any store information after applying the range filter.");

    System.out.println(
        "Validation successful: All found store objects have the field '"
            + fieldName
            + "' with a value within the range ["
            + lowerBound
            + ", "
            + upperBound
            + "].");
  }

  @Then("each store object inside a variant should satisfy all of the following numeric conditions:")
  public void each_store_object_inside_a_variant_should_satisfy_the_following_numeric_conditions(
      DataTable conditionsTable) {
    assertNotNull(
        rawResponse, "A search must be performed before this step can be executed.");

    List<Map<String, String>> conditions = conditionsTable.asMaps(String.class, String.class);

    JsonPath jsonPath = rawResponse.jsonPath();
    List<Map<String, Object>> products = jsonPath.getList("response.products");

    if (products == null || products.isEmpty()) {
      System.out.println("Verification skipped: Response contains no products to validate.");
      return;
    }

    boolean foundAtLeastOneStore = false;
    for (Map<String, Object> product : products) {
      List<Map<String, Object>> variants = (List<Map<String, Object>>) product.get("variants");
      if (variants == null || variants.isEmpty()) continue;

      for (Map<String, Object> variant : variants) {
        List<Map<String, Object>> stores = (List<Map<String, Object>>) variant.get("store");
        if (stores == null || stores.isEmpty()) continue;

        foundAtLeastOneStore = true;

        for (Map<String, Object> storeObject : stores) {
          for (Map<String, String> condition : conditions) {
            String fieldName = condition.get("fieldName");
            String operator = condition.get("operator");
            double expectedValue = Double.parseDouble(condition.get("value"));

            assertTrue(storeObject.containsKey(fieldName), String.format("Store object is missing field '%s'", fieldName));
            Object actualValueRaw = storeObject.get(fieldName);
            assertTrue(actualValueRaw instanceof Number, String.format("Field '%s' is not a number", fieldName));
            double actualValue = ((Number) actualValueRaw).doubleValue();
            boolean conditionMet = false;
            switch (operator) {
              case ">=":
                conditionMet = actualValue >= expectedValue;
                break;
              case "<=":
                conditionMet = actualValue <= expectedValue;
                break;
              case "==":
                conditionMet = actualValue == expectedValue;
                break;
              case ">":
                conditionMet = actualValue > expectedValue;
                break;
              case "<":
                conditionMet = actualValue < expectedValue;
                break;
              default:
                fail("Unsupported operator in BDD step: " + operator);
            }

            assertTrue(conditionMet,
                String.format("Validation failed for product '%s'. Field '%s' with value %f did not satisfy the condition '%s %f'.",
                    product.get("uniqueId"), fieldName, actualValue, operator, expectedValue));
          }
        }
      }
    }

    assertTrue(foundAtLeastOneStore, "The search returned products, but none had any store information after applying filters.");
    System.out.println("Validation successful: All store objects satisfied all specified numeric conditions.");
  }
    @Then("each store object inside a variant should satisfy all of the following boolean conditions:")
  public void each_store_object_inside_a_variant_should_satisfy_the_following_boolean_conditions(
      DataTable conditionsTable) {
    assertNotNull(
        rawResponse, "A search must be performed before this step can be executed.");
    List<Map<String, String>> conditions = conditionsTable.asMaps(String.class, String.class);

    JsonPath jsonPath = rawResponse.jsonPath();
    List<Map<String, Object>> products = jsonPath.getList("response.products");

    if (products == null || products.isEmpty()) {
      System.out.println("Verification skipped: Response contains no products to validate.");
      return;
    }

    boolean foundAtLeastOneStore = false;
    for (Map<String, Object> product : products) {
      List<Map<String, Object>> variants = (List<Map<String, Object>>) product.get("variants");
      if (variants == null || variants.isEmpty()) continue;

      for (Map<String, Object> variant : variants) {
        List<Map<String, Object>> stores = (List<Map<String, Object>>) variant.get("store");
        if (stores == null || stores.isEmpty()) continue;

        foundAtLeastOneStore = true;

        for (Map<String, Object> storeObject : stores) {
          for (Map<String, String> condition : conditions) {
            String fieldName = condition.get("fieldName");
            boolean expectedValue = Boolean.parseBoolean(condition.get("value"));
            assertTrue(storeObject.containsKey(fieldName), String.format("Store object is missing field '%s'", fieldName));
            Object actualValueRaw = storeObject.get(fieldName);
            assertTrue(actualValueRaw instanceof Boolean, String.format("Field '%s' is not a boolean. Found: %s", fieldName, actualValueRaw));
            assertEquals(expectedValue, (Boolean) actualValueRaw,
                String.format("Validation failed for product '%s'. Field '%s' was expected to be %b but was %b.",
                    product.get("uniqueId"), fieldName, expectedValue, actualValueRaw));
          }
        }
      }
    }

    assertTrue(foundAtLeastOneStore, "The search returned products, but none had any store information after applying filters.");
    System.out.println("Validation successful: All store objects satisfied all specified boolean conditions.");
  }
  
  @Then("each variant store should contain at most {int} stores")
  public void each_variants_store_array_should_contain_at_most_stores(int maxStores) {
    assertNotNull(
        rawResponse, "A search must be performed before this step can be executed.");

    JsonPath jsonPath = rawResponse.jsonPath();
    List<Map<String, Object>> products = jsonPath.getList("response.products");

    if (products == null || products.isEmpty()) {
      System.out.println(
          "Verification skipped: Response contains no products to validate.");
      return;
    }

    boolean foundAtLeastOneProductWithStores = false;
    for (Map<String, Object> product : products) {
      if (product.containsKey("variants")) {
        List<Map<String, Object>> variants = (List<Map<String, Object>>) product.get("variants");
        if (variants == null || variants.isEmpty()) {
          continue;
        }

        for (Map<String, Object> variant : variants) {
          if (variant.containsKey("store")) {
            List<Map<String, Object>> stores = (List<Map<String, Object>>) variant.get("store");
            if (stores != null) {
              foundAtLeastOneProductWithStores = true;
              assertTrue(
                  stores.size() <= maxStores,
                  String.format(
                      "Product '%s', Variant '%s': store array size was %d, which is greater than the maximum allowed of %d.",
                      product.get("uniqueId"),
                      variant.get("uniqueId"),
                      stores.size(),
                      maxStores));
            }
          }
        }
      } 
      else if (product.containsKey("store")) {
        List<Map<String, Object>> stores = (List<Map<String, Object>>) product.get("store");
        if (stores != null) {
          foundAtLeastOneProductWithStores = true;
          assertTrue(
              stores.size() <= maxStores,
              String.format(
                  "Product '%s': store array size was %d, which is greater than the maximum allowed of %d.",
                  product.get("uniqueId"),
                  stores.size(),
                  maxStores));
        }
      }
    }

    assertTrue(foundAtLeastOneProductWithStores, "The search returned products, but none of them had any 'store' information to validate the count.");

    System.out.println(
        "Validation successful: All found store arrays contain at most "
            + maxStores
            + " stores as expected.");
  }
  @Then("the schema for field {string} should have {string} as {string}")
  public void the_schema_for_field_should_have_as(String fieldName, String key, String value) {
    JsonPath jsonPath = rawResponse.jsonPath();
        Map<String, Object> fieldSchema = jsonPath.param("fieldName", fieldName)
                                              .get("find { it.fieldName == fieldName }");

    assertNotNull(fieldSchema, "Field '" + fieldName + "' was not found in the index fields schema response.");

    assertThat("For field '" + fieldName + "', the key '" + key + "'",
               String.valueOf(fieldSchema.get(key)),
               equalTo(value));
  }
  
  
  @Then("all products should only have valid store ID {string}")
  public void all_products_should_only_have_valid_store_id(String validStoreId) {
  // extract list of products
  List<Map<String,Object>> products = response.extract().path("response.products");
  assertNotNull(products, "Products list must not be null");
  for (Map<String,Object> product : products) {
    // build unified storeId list from product or its variants
    List<Map<String,Object>> stores = new ArrayList<>();
    if (Boolean.TRUE.equals(siteSupportsVariants) && product.containsKey("variants")) {
      ((List<Map<String,Object>>)product.get("variants"))
        .forEach(v -> stores.addAll((List<Map<String,Object>>)v.getOrDefault("store", List.of())));
    }
    if (product.containsKey("store")) {
      stores.addAll((List<Map<String,Object>>)product.get("store"));
    }
    // assert every storeId matches the expected validStoreId
    for (Map<String,Object> store : stores) {
      String storeId = (String) store.get("storeId");
      assertEquals(validStoreId, storeId,
        "Found unexpected storeId '" + storeId + "', expected only '" + validStoreId + "'");
    }
  }
}

  
  private static void checkSiteStoreStatus() {
    System.out.println("Performing preliminary check for store status for site: " + siteKey);
    try {
      Response response = given()
          .contentType(ContentType.JSON)
          .queryParams(Map.of("q", "*"))
          .when()
          .get("/{apiKey}/{siteKey}/search", apiKey, siteKey)
          .thenReturn();

      if (response.getStatusCode() != 200) {
        System.err.println("Preliminary check for store status failed with status: " + response.getStatusCode());
        siteSupportsStore = null;
        return;
      }

      JsonPath jsonPath = response.jsonPath();
      String storeParam = jsonPath.getString("searchMetaData.queryParams.store");
      if ("true".equals(storeParam)) {
        siteSupportsStore = true;
        System.out
            .println("Preliminary check: Store is ENABLED for site " + siteKey + " (store=true found in metadata).");
      } else {
        siteSupportsStore = false;
        System.out.println("Preliminary check: Store is DISABLED for site " + siteKey
            + " (store parameter is missing or not 'true').");
      }
    } catch (Exception e) {
      System.err.println("Error during preliminary store check: " + e.getMessage());
      siteSupportsStore = null;
    }
  }
}
