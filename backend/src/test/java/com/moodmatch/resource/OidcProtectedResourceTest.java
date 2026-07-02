package com.moodmatch.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import com.moodmatch.testsupport.OidcAuthTestProfile;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(OidcAuthTestProfile.class)
class OidcProtectedResourceTest {

    @Test
    void shouldRejectUnauthenticatedAccessToProtectedEndpointsInOidcMode() {
        given()
                .when()
                .get("/api/media")
                .then()
                .statusCode(401);

        given()
                .when()
                .get("/api/external/search?query=arrival&mediaType=FILM")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldKeepHealthEndpointPublicInOidcMode() {
        given()
                .when()
                .get("/api/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }
}
