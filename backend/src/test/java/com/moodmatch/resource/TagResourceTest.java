package com.moodmatch.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moodmatch.dto.tag.CreateTagRequest;
import com.moodmatch.entity.TagCategory;
import com.moodmatch.service.TagService;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
class TagResourceTest {

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
        QuarkusTransaction.requiringNew().run(() -> {
            entityManager.createNativeQuery("DELETE FROM media_tags").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM media_external_refs").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM media_items").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM external_tag_mappings").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM tags").executeUpdate();
        });
    }

    @Test
    void shouldListTagsOverHttp() {
        String tagName = "Mystery-" + UUID.randomUUID();
        tagService.createTagIfNeeded(new CreateTagRequest(tagName, TagCategory.GENRE));

        given()
                .when().get("/api/tags")
                .then()
                .statusCode(200)
                .body("name", hasItem(tagName))
                .body("category", hasItem("GENRE"));
    }
}
