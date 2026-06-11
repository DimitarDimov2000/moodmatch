package com.moodmatch.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
class TagResourceTest {

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
    void shouldCreateReuseAndListTagsOverHttp() {
        String tagName = "Mystery-" + UUID.randomUUID();

        Response createdResponse = given()
                .contentType(ContentType.JSON)
                .body(java.util.Map.of("name", tagName, "category", "GENRE"))
                .when()
                .post("/api/tags");

        createdResponse.then()
                .statusCode(200)
                .body("name", is(tagName))
                .body("category", is("GENRE"));

        String firstId = createdResponse.jsonPath().getString("id");

        given()
                .contentType(ContentType.JSON)
                .body(java.util.Map.of("name", tagName.toLowerCase(), "category", "GENRE"))
                .when()
                .post("/api/tags")
                .then()
                .statusCode(200)
                .body("id", is(firstId));

        given()
                .when().get("/api/tags")
                .then()
                .statusCode(200)
                .body("name", hasItem(tagName))
                .body("category", hasItem("GENRE"));
    }

    @Test
    void shouldMapTagValidationErrors() {
        given()
                .contentType(ContentType.JSON)
                .body(java.util.Map.of("name", " ", "category", "GENRE"))
                .when()
                .post("/api/tags")
                .then()
                .statusCode(400)
                .body("code", is("VALIDATION_ERROR"))
                .body("details.field", hasItem("name"));
    }
}
