package com.moodmatch.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ExternalSearchResourceTest {

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void cleanDatabaseBefore() {
        cleanDatabase();
    }

    @AfterEach
    void cleanDatabaseAfter() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        QuarkusTransaction.requiringNew().run(() -> {
            entityManager.createNativeQuery("DELETE FROM media_tags").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM media_external_refs").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM media_items").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM external_tag_mappings").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM tags").executeUpdate();
        });
    }

    @Test
    void shouldValidateRequiredQueryParameters() {
        given()
                .when()
                .get("/api/external/search?mediaType=FILM")
                .then()
                .statusCode(400)
                .body("code", is("VALIDATION_ERROR"))
                .body("details.field", hasItem("query"));

        given()
                .when()
                .get("/api/external/search?query=arrival")
                .then()
                .statusCode(400)
                .body("code", is("VALIDATION_ERROR"))
                .body("details.field", hasItem("mediaType"));
    }

    @Test
    void shouldDefaultToDemoSourceAndReturnDeterministicNormalizedResults() {
        given()
                .when()
                .get("/api/external/search?query=a&mediaType=FILM")
                .then()
                .statusCode(200)
                .body("query", is("a"))
                .body("mediaType", is("FILM"))
                .body("source", is("DEMO"))
                .body("results.size()", is(2))
                .body("results[0].title", is("Arrival"))
                .body("results[1].title", is("Severance Preview Reel"))
                .body("results[0].attribution", is("MoodMatch Demo Provider (offline)"));
    }

    @Test
    void shouldRejectUnsupportedSourcesWithStructuredErrors() {
        given()
                .when()
                .get("/api/external/search?query=arrival&mediaType=FILM&source=TMDB")
                .then()
                .statusCode(400)
                .body("code", is("BUSINESS_RULE_VIOLATION"))
                .body("message", is("Unsupported source: TMDB"));
    }

    @Test
    void shouldApplyMediaTypeFilteringAndReturnEmptyResultsWhenNothingMatches() {
        given()
                .when()
                .get("/api/external/search?query=dark&mediaType=SERIES")
                .then()
                .statusCode(200)
                .body("results.size()", is(1))
                .body("results[0].title", is("Dark"));

        given()
                .when()
                .get("/api/external/search?query=dark&mediaType=FILM")
                .then()
                .statusCode(200)
                .body("results.size()", is(0));
    }
}
