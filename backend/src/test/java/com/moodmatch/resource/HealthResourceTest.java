package com.moodmatch.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HealthResourceTest {

    @Test
    void healthEndpointReturnsUp() {
        given()
                .when().get("/api/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }
}
