package com.moodmatch.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moodmatch.dto.tag.CreateTagRequest;
import com.moodmatch.dto.tag.TagResponse;
import com.moodmatch.entity.TagCategory;
import com.moodmatch.service.TagService;
import com.moodmatch.service.TestCurrentUserProvider;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
class MediaResourceTest {

    @Inject
    EntityManager entityManager;

    @Inject
    TagService tagService;

    @BeforeEach
    void cleanDatabaseBefore() {
        cleanDatabase();
    }

    @AfterEach
    void cleanDatabaseAfter() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        TestCurrentUserProvider.useLocalDemoUser();
        QuarkusTransaction.requiringNew().run(() -> {
            entityManager.createNativeQuery("DELETE FROM media_tags").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM media_external_refs").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM media_items").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM external_tag_mappings").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM tags").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM app_users").executeUpdate();
        });
    }

    @Test
    void shouldSupportFullMediaLifecycleOverHttp() {
        String suffix = UUID.randomUUID().toString();
        String title = "Interstellar-" + suffix;
        TagResponse mystery = tagService.createTagIfNeeded(new CreateTagRequest("Mystery-" + suffix, TagCategory.GENRE));
        TagResponse spannend = tagService.createTagIfNeeded(new CreateTagRequest("Spannend-" + suffix, TagCategory.TONE));
        TagResponse drama = tagService.createTagIfNeeded(new CreateTagRequest("Drama-" + suffix, TagCategory.GENRE));

        Response createdResponse = given()
                .contentType(ContentType.JSON)
                .body(buildMediaPayload(title, 5, false))
                .when()
                .post("/api/media");

        createdResponse.then()
                .statusCode(201)
                .body("title", is(title))
                .body("consumptionStatus", is("CONSUMED"))
                .body("rating", is(5))
                .body("isFavourite", is(false));

        String mediaId = createdResponse.jsonPath().getString("id");
        createdResponse.then().header("Location", endsWith("/api/media/" + mediaId));

        given()
                .when().get("/api/media")
                .then()
                .statusCode(200)
                .body("title", hasItem(title));

        given()
                .when().get("/api/media/{id}", mediaId)
                .then()
                .statusCode(200)
                .body("id", is(mediaId))
                .body("title", is(title));

        String updatedTitle = "Arrival-" + suffix;
        given()
                .contentType(ContentType.JSON)
                .body(buildMediaPayload(updatedTitle, 4, false))
                .when()
                .put("/api/media/{id}", mediaId)
                .then()
                .statusCode(200)
                .body("title", is(updatedTitle))
                .body("rating", is(4))
                .body("isFavourite", is(false));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("isFavourite", true))
                .when()
                .patch("/api/media/{id}/favorite", mediaId)
                .then()
                .statusCode(200)
                .body("isFavourite", is(true));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "tagIds",
                        List.of(mystery.id(), spannend.id())))
                .when()
                .put("/api/media/{id}/tags", mediaId)
                .then()
                .statusCode(200)
                .body("tags.size()", is(2))
                .body("tags.name", hasItem(mystery.name()))
                .body("tags.name", hasItem(spannend.name()));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("tagIds", List.of(drama.id())))
                .when()
                .put("/api/media/{id}/tags", mediaId)
                .then()
                .statusCode(200)
                .body("tags.size()", is(1))
                .body("tags[0].name", is(drama.name()));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "consumptionStatus", "WANT_TO_CONSUME",
                        "isFavourite", false,
                        "confirmDestructiveChange", true))
                .when()
                .patch("/api/media/{id}/status", mediaId)
                .then()
                .statusCode(200)
                .body("consumptionStatus", is("WANT_TO_CONSUME"))
                .body("rating", nullValue())
                .body("isFavourite", is(false));

        given()
                .when()
                .delete("/api/media/{id}", mediaId)
                .then()
                .statusCode(204);

        given()
                .when().get("/api/media/{id}", mediaId)
                .then()
                .statusCode(404)
                .body("code", is("RESOURCE_NOT_FOUND"))
                .body("message", endsWith(mediaId));
    }

    @Test
    void shouldMapValidationAndBusinessRuleErrors() {
        given()
                .contentType(ContentType.JSON)
                .body(buildMediaPayload(" ", 5, false))
                .when()
                .post("/api/media")
                .then()
                .statusCode(400)
                .body("code", is("VALIDATION_ERROR"))
                .body("details.field", hasItem("title"));

        given()
                .contentType(ContentType.JSON)
                .body(buildMediaPayload("Favourite Too Low-" + UUID.randomUUID(), 3, true))
                .when()
                .post("/api/media")
                .then()
                .statusCode(400)
                .body("code", is("BUSINESS_RULE_VIOLATION"))
                .body("message", is("Favourite=true is only allowed when status is CONSUMED and rating is at least 4."));

        String mediaId = given()
                .contentType(ContentType.JSON)
                .body(buildMediaPayload("Low Rated Favourite Patch-" + UUID.randomUUID(), 3, false))
                .when()
                .post("/api/media")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("id");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("isFavourite", true))
                .when()
                .patch("/api/media/{id}/favorite", mediaId)
                .then()
                .statusCode(400)
                .body("code", is("BUSINESS_RULE_VIOLATION"))
                .body("message", is("Favourite=true is only allowed when status is CONSUMED and rating is at least 4."));
    }

    @Test
    void shouldReturnStructuredErrorsForMissingMediaAndUnknownTagsWithoutStackTrace() {
        String missingId = UUID.randomUUID().toString();

        given()
                .when()
                .delete("/api/media/{id}", missingId)
                .then()
                .statusCode(404)
                .body("code", is("RESOURCE_NOT_FOUND"))
                .body("message", endsWith(missingId));

        String mediaId = given()
                .contentType(ContentType.JSON)
                .body(buildMediaPayload("Tag Check-" + UUID.randomUUID(), 5, false))
                .when()
                .post("/api/media")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("id");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("tagIds", List.of(UUID.randomUUID())))
                .when()
                .put("/api/media/{id}/tags", mediaId);

        response.then()
                .statusCode(404)
                .body("code", is("RESOURCE_NOT_FOUND"))
                .body("message", is(not(org.hamcrest.Matchers.containsString("Exception"))));

        Assertions.assertFalse(response.asString().contains("stackTrace"));
    }

    private Map<String, Object> buildMediaPayload(String title, int rating, boolean isFavourite) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("originalTitle", title);
        payload.put("description", "Description for " + title);
        payload.put("mediaType", "FILM");
        payload.put("consumptionStatus", "CONSUMED");
        payload.put("isFavourite", isFavourite);
        payload.put("rating", rating);
        payload.put("sourceType", "MANUAL");
        payload.put("sourceNote", "Own library");
        payload.put("commitmentLevel", "LONG");
        payload.put("releaseYear", 2014);
        payload.put("coverUrl", "https://example.com/" + UUID.randomUUID() + ".jpg");
        payload.put("metadataOrigin", "MANUAL");
        return payload;
    }
}
