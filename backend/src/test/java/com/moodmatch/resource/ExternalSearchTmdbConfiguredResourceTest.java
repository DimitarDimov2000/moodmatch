package com.moodmatch.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.moodmatch.external.tmdb.TestTmdbGateway;
import com.moodmatch.testsupport.TmdbConfiguredTestProfile;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(TmdbConfiguredTestProfile.class)
class ExternalSearchTmdbConfiguredResourceTest {

    @org.junit.jupiter.api.BeforeEach
    void resetGateway() {
        TestTmdbGateway.reset();
    }

    @org.junit.jupiter.api.Test
    void shouldSearchTmdbThroughTheConfiguredBackendGatewayWithoutLeakingTheConfiguredKey() {
        String responseBody = given()
                .queryParam("query", "arrival")
                .queryParam("mediaType", "FILM")
                .queryParam("source", "TMDB")
                .when()
                .get("/api/external/search")
                .then()
                .statusCode(200)
                .body("query", is("arrival"))
                .body("source", is("TMDB"))
                .body("warnings.size()", is(0))
                .body("results.size()", is(1))
                .body("results[0].externalId", is("11"))
                .body("results[0].title", is("Arrival"))
                .body("results[0].coverUrl", is("https://image.tmdb.org/t/p/w342/poster.jpg"))
                .extract()
                .asString();

        assertFalse(responseBody.contains("test-tmdb-key"));
    }
}
