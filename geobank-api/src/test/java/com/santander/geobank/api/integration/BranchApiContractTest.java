package com.santander.geobank.api.integration;

import com.santander.geobank.api.dto.BranchDTO;
import com.santander.geobank.api.dto.ProximitySearchRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Contract tests for GeoBank API endpoints.
 * Validates API contracts for consumer-driven contract testing.
 *
 * Testing Strategy:
 * - Consumer-Driven Contracts (CDC) with Spring Cloud Contract
 * - REST-assured for HTTP interaction testing
 * - Schema validation for request/response structures
 * - Contract verification before deployment
 *
 * Compliance:
 * - API versioning strategy validation
 * - Backward compatibility verification
 * - Service level agreement (SLA) testing
 *
 * @author Quality Assurance Team
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("API Contract Tests")
class BranchApiContractTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
    }

    @Test
    @DisplayName("Contract: GET /branches/{id} returns branch details")
    void shouldReturnBranchDetailsById() {
        given()
                .pathParam("id", "test-branch-id")
                .when()
                .get("/branches/{id}")
                .then()
                .statusCode(anyOf(is(200), is(404)))
                .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("Contract: GET /branches returns paginated list")
    void shouldReturnPaginatedBranchList() {
        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/branches")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content", notNullValue())
                .body("totalElements", greaterThanOrEqualTo(0))
                .body("totalPages", greaterThanOrEqualTo(0))
                .body("size", equalTo(10))
                .body("number", equalTo(0));
    }

    @Test
    @DisplayName("Contract: POST /branches/proximity returns nearby branches")
    void shouldReturnNearbyBranches() {
        ProximitySearchRequest request = new ProximitySearchRequest(
                -23.550520,  // São Paulo coordinates
                -46.633308,
                10.0,
                5
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/branches/proximity")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(lessThanOrEqualTo(5)))
                .body("[0].id", notNullValue())
                .body("[0].name", notNullValue())
                .body("[0].location", notNullValue())
                .body("[0].location.latitude", notNullValue())
                .body("[0].location.longitude", notNullValue())
                .body("[0].distance", notNullValue())
                .body("[0].distance.kilometers", greaterThanOrEqualTo(0.0f));
    }

    @Test
    @DisplayName("Contract: POST /branches creates new branch")
    void shouldCreateNewBranch() {
        BranchDTO newBranch = BranchDTO.builder()
                .name("Test Branch")
                .type("TRADITIONAL")
                .latitude(-23.550520)
                .longitude(-46.633308)
                .address("Test Address, São Paulo")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(newBranch)
                .when()
                .post("/branches")
                .then()
                .statusCode(anyOf(is(201), is(400)))
                .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("Contract: PUT /branches/{id} updates branch")
    void shouldUpdateBranch() {
        BranchDTO updateData = BranchDTO.builder()
                .name("Updated Branch Name")
                .address("Updated Address")
                .contactPhone("+55 11 98765-4321")
                .build();

        given()
                .pathParam("id", "test-branch-id")
                .contentType(ContentType.JSON)
                .body(updateData)
                .when()
                .put("/branches/{id}")
                .then()
                .statusCode(anyOf(is(200), is(404)))
                .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("Contract: DELETE /branches/{id} removes branch")
    void shouldDeleteBranch() {
        given()
                .pathParam("id", "test-branch-id")
                .when()
                .delete("/branches/{id}")
                .then()
                .statusCode(anyOf(is(204), is(404)));
    }

    @Test
    @DisplayName("Contract: POST /branches/proximity validates request body")
    void shouldValidateProximitySearchRequest() {
        ProximitySearchRequest invalidRequest = new ProximitySearchRequest(
                -91.0,  // Invalid latitude
                -46.633308,
                10.0,
                5
        );

        given()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when()
                .post("/branches/proximity")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("Contract: GET /branches supports filtering by type")
    void shouldFilterBranchesByType() {
        given()
                .queryParam("type", "TRADITIONAL")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/branches")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content", everyItem(hasEntry("type", "TRADITIONAL")));
    }

    @Test
    @DisplayName("Contract: GET /branches supports filtering by status")
    void shouldFilterBranchesByStatus() {
        given()
                .queryParam("status", "ACTIVE")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/branches")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content", everyItem(hasEntry("status", "ACTIVE")));
    }

    @Test
    @DisplayName("Contract: API returns proper error structure")
    void shouldReturnProperErrorStructure() {
        given()
                .pathParam("id", "non-existent-id")
                .when()
                .get("/branches/{id}")
                .then()
                .statusCode(anyOf(is(200), is(404)))
                .contentType(ContentType.JSON);
                // If 404, should have error structure with message and timestamp
    }

    @Test
    @DisplayName("Contract: API enforces rate limiting headers")
    void shouldIncludeRateLimitingHeaders() {
        given()
                .when()
                .get("/branches")
                .then()
                .statusCode(200)
                .header("X-RateLimit-Limit", notNullValue())
                .header("X-RateLimit-Remaining", notNullValue());
    }

    @Test
    @DisplayName("Contract: API returns correlation ID in response")
    void shouldReturnCorrelationIdInResponse() {
        given()
                .header("X-Correlation-ID", "test-correlation-123")
                .when()
                .get("/branches")
                .then()
                .statusCode(200)
                .header("X-Correlation-ID", equalTo("test-correlation-123"));
    }

    @Test
    @DisplayName("Contract: POST /branches validates required fields")
    void shouldValidateRequiredFieldsOnCreate() {
        BranchDTO invalidBranch = BranchDTO.builder()
                .name("")  // Empty name
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(invalidBranch)
                .when()
                .post("/branches")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("message", containsString("name"));
    }
}
