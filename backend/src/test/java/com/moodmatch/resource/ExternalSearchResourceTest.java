package com.moodmatch.resource;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.moodmatch.entity.ExternalSourceName;
import com.moodmatch.entity.ExternalTagMapping;
import com.moodmatch.entity.Tag;
import com.moodmatch.entity.TagCategory;
import com.moodmatch.entity.TagMappingConfidence;
import com.moodmatch.external.openlibrary.TestOpenLibraryGateway;
import com.moodmatch.repository.ExternalTagMappingRepository;
import com.moodmatch.repository.TagRepository;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moodmatch.service.TestCurrentUserProvider;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ExternalSearchResourceTest {

    @Inject
    EntityManager entityManager;

    @Inject
    TagRepository tagRepository;

    @Inject
    ExternalTagMappingRepository externalTagMappingRepository;

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
        TestOpenLibraryGateway.reset();
        QuarkusTransaction.requiringNew().run(() -> {
            entityManager.createNativeQuery("DELETE FROM media_tags").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM media_external_refs").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM media_items").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM external_tag_mappings").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM tags").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM auth_sessions").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM app_users").executeUpdate();
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
                .body("warnings[0]", is("TMDB provider is not configured. Set MOODMATCH_TMDB_API_KEY. Using DEMO fallback."))
                .body("results.size()", is(2))
                .body("results[0].title", is("Arrival"))
                .body("results[1].title", is("Severance Preview Reel"))
                .body("results[0].attribution", is("MoodMatch Demo Provider (offline)"));
    }

    @Test
    void shouldRejectTmdbSearchWithClearConfigurationErrors() {
        given()
                .when()
                .get("/api/external/search?query=arrival&mediaType=FILM&source=TMDB")
                .then()
                .statusCode(400)
                .body("code", is("BUSINESS_RULE_VIOLATION"))
                .body("message", is("TMDB provider is not configured. Set MOODMATCH_TMDB_API_KEY."));
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

    @Test
    void shouldSearchBooksThroughOpenLibraryByDefault() {
        given()
                .when()
                .get("/api/external/search?query=dune&mediaType=BOOK")
                .then()
                .statusCode(200)
                .body("source", is("OPEN_LIBRARY"))
                .body("warnings.size()", is(0))
                .body("results.size()", is(1))
                .body("results[0].title", is("Dune"))
                .body("results[0].externalId", is("OL12345W"))
                .body("results[0].creatorNames[0]", is("Frank Herbert"))
                .body("results[0].mediaType", is("BOOK"))
                .body("results[0].attribution", is("Metadata from Open Library"));
    }

    @Test
    void shouldImportExternalResultsAsUserOwnedMediaAndAllowSameExternalIdForDifferentUsers() {
        QuarkusTransaction.requiringNew().run(() -> {
            Tag tag = new Tag();
            tag.setId(UUID.fromString("10000000-0000-0000-0000-000000000010"));
            tag.setName("Science-Fiction");
            tag.setCategory(TagCategory.GENRE);
            tagRepository.persist(tag);

            ExternalTagMapping mapping = new ExternalTagMapping();
            mapping.setSourceName(ExternalSourceName.TMDB);
            mapping.setExternalField("genre");
            mapping.setExternalValue("Science-Fiction");
            mapping.setTag(tag);
            mapping.setConfidence(TagMappingConfidence.HIGH);
            externalTagMappingRepository.persist(mapping);
        });

        TestCurrentUserProvider.useUserA();
        given()
                .contentType(io.restassured.http.ContentType.JSON)
                .body(buildImportPayload())
                .when()
                .post("/api/external/import")
                .then()
                .statusCode(201)
                .body("created", is(true))
                .body("media.title", is("Arrival"))
                .body("media.externalSourceName", is("DEMO"))
                .body("media.externalSourceId", is("demo-film-arrival"))
                .body("media.externalReferences[0].sourceName", is("DEMO"))
                .body("media.tags[0].name", is("Science-Fiction"))
                .body("media.id", notNullValue());

        TestCurrentUserProvider.useUserB();
        given()
                .contentType(io.restassured.http.ContentType.JSON)
                .body(buildImportPayload())
                .when()
                .post("/api/external/import")
                .then()
                .statusCode(201)
                .body("created", is(true))
                .body("media.externalSourceId", is("demo-film-arrival"));

        QuarkusTransaction.requiringNew().run(() -> {
            Number mediaCount = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM media_items").getSingleResult();
            Number externalRefCount =
                    (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM media_external_refs").getSingleResult();
            assertEquals(2L, mediaCount.longValue());
            assertEquals(2L, externalRefCount.longValue());
        });
    }

    @Test
    void shouldImportOpenLibraryResultsAsBooksAndPreserveExternalMetadata() {
        QuarkusTransaction.requiringNew().run(() -> {
            Tag tag = new Tag();
            tag.setId(UUID.fromString("10000000-0000-0000-0000-000000000020"));
            tag.setName("Politik");
            tag.setCategory(TagCategory.THEME);
            tagRepository.persist(tag);

            ExternalTagMapping mapping = new ExternalTagMapping();
            mapping.setSourceName(ExternalSourceName.OPEN_LIBRARY);
            mapping.setExternalField("subject");
            mapping.setExternalValue("Politics");
            mapping.setTag(tag);
            mapping.setConfidence(TagMappingConfidence.HIGH);
            externalTagMappingRepository.persist(mapping);
        });

        TestCurrentUserProvider.useUserA();
        given()
                .contentType(io.restassured.http.ContentType.JSON)
                .body(buildOpenLibraryImportPayload())
                .when()
                .post("/api/external/import")
                .then()
                .statusCode(201)
                .body("created", is(true))
                .body("media.title", is("Dune"))
                .body("media.mediaType", is("BOOK"))
                .body("media.commitmentLevel", is("LONG"))
                .body("media.description", is("Book by Frank Herbert. First published in 1965."))
                .body("media.externalSourceName", is("OPEN_LIBRARY"))
                .body("media.externalSourceId", is("OL12345W"))
                .body("media.externalReferences[0].sourceName", is("OPEN_LIBRARY"))
                .body("media.externalReferences[0].externalId", is("OL12345W"))
                .body("media.tags[0].name", is("Politik"));
    }

    private Map<String, Object> buildImportPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", "DEMO");
        payload.put("externalId", "demo-film-arrival");
        payload.put("mediaType", "FILM");
        payload.put("title", "Arrival");
        payload.put("originalTitle", null);
        payload.put("creatorNames", java.util.List.of());
        payload.put("description", "A linguist races to understand visitors.");
        payload.put("releaseYear", 2016);
        payload.put("coverUrl", "https://demo.moodmatch.local/covers/arrival.jpg");
        payload.put("sourceUrl", "https://demo.moodmatch.local/items/demo-film-arrival");
        payload.put("externalGenres", java.util.List.of("Science-Fiction", "Drama"));
        payload.put("externalSubjects", java.util.List.of("Zeit", "Entdeckung"));
        payload.put("attribution", "MoodMatch Demo Provider (offline)");
        return payload;
    }

    private Map<String, Object> buildOpenLibraryImportPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", "OPEN_LIBRARY");
        payload.put("externalId", "OL12345W");
        payload.put("mediaType", "BOOK");
        payload.put("title", "Dune");
        payload.put("originalTitle", null);
        payload.put("creatorNames", java.util.List.of("Frank Herbert"));
        payload.put("description", null);
        payload.put("releaseYear", 1965);
        payload.put("coverUrl", "https://covers.openlibrary.org/b/id/12345-M.jpg");
        payload.put("sourceUrl", "https://openlibrary.org/works/OL12345W");
        payload.put("externalGenres", java.util.List.of());
        payload.put("externalSubjects", java.util.List.of("Politics", "Desert planets"));
        payload.put("attribution", "Metadata from Open Library");
        return payload;
    }
}
