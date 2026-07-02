package com.moodmatch.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import com.moodmatch.testsupport.YouTubeMissingConfigTestProfile;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(YouTubeMissingConfigTestProfile.class)
class ExternalResolveUrlMissingConfigResourceTest {

    @org.junit.jupiter.api.Test
    void shouldReturnClearErrorsWhenYoutubeProviderIsNotConfigured() {
        given()
                .contentType(io.restassured.http.ContentType.JSON)
                .body(Map.of(
                        "source", "YOUTUBE",
                        "url", "https://www.youtube.com/watch?v=abc123XYZ_0"))
                .when()
                .post("/api/external/resolve-url")
                .then()
                .statusCode(400)
                .body("code", is("BUSINESS_RULE_VIOLATION"))
                .body(
                        "message",
                        is("YouTube provider is not configured. Set MOODMATCH_YOUTUBE_API_KEY in the backend environment."));
    }

    @org.junit.jupiter.api.Test
    void shouldReturnClearErrorsForYoutubeSearchWhenProviderIsNotConfigured() {
        given()
                .when()
                .get("/api/external/search?query=ai%20tutorial&mediaType=VIDEO&source=YOUTUBE")
                .then()
                .statusCode(400)
                .body("code", is("BUSINESS_RULE_VIOLATION"))
                .body(
                        "message",
                        is("YouTube provider is not configured. Set MOODMATCH_YOUTUBE_API_KEY in the backend environment."));
    }
}
