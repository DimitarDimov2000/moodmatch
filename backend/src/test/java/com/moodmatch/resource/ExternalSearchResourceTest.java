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
import com.moodmatch.external.anilist.TestAniListGateway;
import com.moodmatch.external.librivox.TestLibriVoxGateway;
import com.moodmatch.external.openlibrary.TestOpenLibraryGateway;
import com.moodmatch.external.rawg.TestRawgGateway;
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
        TestLibriVoxGateway.reset();
        TestRawgGateway.reset();
        TestAniListGateway.reset();
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
    void shouldSearchCompatibleAutomaticFilmProvidersAndReturnWarningsForSkippedProviders() {
        given()
                .when()
                .get("/api/external/search?query=a&mediaType=FILM")
                .then()
                .statusCode(200)
                .body("query", is("a"))
                .body("mediaType", is("FILM"))
                .body("source", is("AUTOMATIC"))
                .body("warnings[0]", is("TMDB provider is not configured. Set MOODMATCH_TMDB_API_KEY. Provider skipped in automatic search."))
                .body("results.size()", is(1))
                .body("results[0].title", is("Sen to Chihiro no Kamikakushi"))
                .body("results[0].source", is("ANILIST"))
                .body("results[0].mediaType", is("FILM"))
                .body("results[0].attribution", is("Metadata from AniList"));
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
                .get("/api/external/search?query=dark&mediaType=SERIES&source=DEMO")
                .then()
                .statusCode(200)
                .body("results.size()", is(1))
                .body("results[0].title", is("Dark"));

        given()
                .when()
                .get("/api/external/search?query=dark&mediaType=FILM&source=DEMO")
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
                .body("source", is("AUTOMATIC"))
                .body("warnings.size()", is(0))
                .body("results.size()", is(2))
                .body("results[0].title", is("Dune"))
                .body("results[0].externalId", is("OL12345W"))
                .body("results[0].source", is("OPEN_LIBRARY"))
                .body("results[0].creatorNames[0]", is("Frank Herbert"))
                .body("results[0].mediaType", is("BOOK"))
                .body("results[0].attribution", is("Metadata from Open Library"))
                .body("results[1].title", is("Berserk"))
                .body("results[1].source", is("ANILIST"));
    }

    @Test
    void shouldSearchAudiobooksThroughLibriVoxByDefault() {
        given()
                .when()
                .get("/api/external/search?query=pride&mediaType=AUDIOBOOK")
                .then()
                .statusCode(200)
                .body("source", is("AUTOMATIC"))
                .body("warnings.size()", is(0))
                .body("results.size()", is(1))
                .body("results[0].title", is("Pride and Prejudice"))
                .body("results[0].externalId", is("253"))
                .body("results[0].creatorNames[0]", is("Author: Jane Austen"))
                .body("results[0].creatorNames[1]", is("Reader: Annie Coleman Rothenberg"))
                .body("results[0].mediaType", is("AUDIOBOOK"))
                .body("results[0].attribution", is("LibriVox public domain audiobook catalog"));
    }

    @Test
    void shouldSearchAutomaticSeriesThroughAniListWhenTmdbIsUnconfigured() {
        given()
                .when()
                .get("/api/external/search?query=attack%20on%20titan&mediaType=SERIES")
                .then()
                .statusCode(200)
                .body("source", is("AUTOMATIC"))
                .body("warnings[0]", is("TMDB provider is not configured. Set MOODMATCH_TMDB_API_KEY. Provider skipped in automatic search."))
                .body("results.size()", is(1))
                .body("results[0].source", is("ANILIST"))
                .body("results[0].title", is("Shingeki no Kyojin"))
                .body("results[0].mediaType", is("SERIES"));
    }

    @Test
    void shouldSearchAniListWhenExplicitlySelectedForAnimeAndManga() {
        given()
                .when()
                .get("/api/external/search?query=attack%20on%20titan&mediaType=SERIES&source=ANILIST")
                .then()
                .statusCode(200)
                .body("source", is("ANILIST"))
                .body("warnings.size()", is(0))
                .body("results.size()", is(1))
                .body("results[0].title", is("Shingeki no Kyojin"))
                .body("results[0].originalTitle", is("進撃の巨人"))
                .body("results[0].creatorNames[0]", is("Wit Studio"))
                .body("results[0].mediaType", is("SERIES"))
                .body("results[0].externalSubjects[0]", is("Format: TV"))
                .body("results[0].attribution", is("Metadata from AniList"));

        given()
                .when()
                .get("/api/external/search?query=spirited%20away&mediaType=FILM&source=ANILIST")
                .then()
                .statusCode(200)
                .body("source", is("ANILIST"))
                .body("results.size()", is(1))
                .body("results[0].title", is("Sen to Chihiro no Kamikakushi"))
                .body("results[0].mediaType", is("FILM"))
                .body("results[0].externalSubjects[0]", is("Format: MOVIE"));

        given()
                .when()
                .get("/api/external/search?query=berserk&mediaType=BOOK&source=ANILIST")
                .then()
                .statusCode(200)
                .body("source", is("ANILIST"))
                .body("results.size()", is(1))
                .body("results[0].title", is("Berserk"))
                .body("results[0].creatorNames[0]", is("Kentaro Miura"))
                .body("results[0].mediaType", is("BOOK"));
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

    @Test
    void shouldImportLibriVoxResultsAsAudiobooksAndPreserveExternalMetadata() {
        QuarkusTransaction.requiringNew().run(() -> {
            Tag tag = new Tag();
            tag.setId(UUID.fromString("10000000-0000-0000-0000-000000000030"));
            tag.setName("Romantik");
            tag.setCategory(TagCategory.GENRE);
            tagRepository.persist(tag);

            ExternalTagMapping mapping = new ExternalTagMapping();
            mapping.setSourceName(ExternalSourceName.LIBRIVOX);
            mapping.setExternalField("genre");
            mapping.setExternalValue("Romance");
            mapping.setTag(tag);
            mapping.setConfidence(TagMappingConfidence.HIGH);
            externalTagMappingRepository.persist(mapping);
        });

        TestCurrentUserProvider.useUserA();
        given()
                .contentType(io.restassured.http.ContentType.JSON)
                .body(buildLibriVoxImportPayload())
                .when()
                .post("/api/external/import")
                .then()
                .statusCode(201)
                .body("created", is(true))
                .body("media.title", is("Pride and Prejudice"))
                .body("media.mediaType", is("AUDIOBOOK"))
                .body("media.commitmentLevel", is("LONG"))
                .body("media.externalSourceName", is("LIBRIVOX"))
                .body("media.externalSourceId", is("253"))
                .body("media.externalReferences[0].sourceName", is("LIBRIVOX"))
                .body("media.externalReferences[0].externalId", is("253"))
                .body("media.tags[0].name", is("Romantik"));
    }

    @Test
    void shouldImportRawgResultsAsGamesAndPreserveExternalMetadata() {
        QuarkusTransaction.requiringNew().run(() -> {
            Tag tag = new Tag();
            tag.setId(UUID.fromString("10000000-0000-0000-0000-000000000040"));
            tag.setName("Open World");
            tag.setCategory(TagCategory.THEME);
            tagRepository.persist(tag);

            ExternalTagMapping mapping = new ExternalTagMapping();
            mapping.setSourceName(ExternalSourceName.RAWG);
            mapping.setExternalField("subject");
            mapping.setExternalValue("Open World");
            mapping.setTag(tag);
            mapping.setConfidence(TagMappingConfidence.HIGH);
            externalTagMappingRepository.persist(mapping);
        });

        TestCurrentUserProvider.useUserA();
        given()
                .contentType(io.restassured.http.ContentType.JSON)
                .body(buildRawgImportPayload())
                .when()
                .post("/api/external/import")
                .then()
                .statusCode(201)
                .body("created", is(true))
                .body("media.title", is("Elden Ring"))
                .body("media.mediaType", is("GAME"))
                .body("media.commitmentLevel", is("LONG"))
                .body("media.externalSourceName", is("RAWG"))
                .body("media.externalSourceId", is("3498"))
                .body("media.externalReferences[0].sourceName", is("RAWG"))
                .body("media.externalReferences[0].externalId", is("3498"))
                .body("media.tags[0].name", is("Open World"));
    }

    @Test
    void shouldImportAniListResultsAndPreserveExternalMetadata() {
        QuarkusTransaction.requiringNew().run(() -> {
            Tag tag = new Tag();
            tag.setId(UUID.fromString("10000000-0000-0000-0000-000000000050"));
            tag.setName("Survival");
            tag.setCategory(TagCategory.THEME);
            tagRepository.persist(tag);

            ExternalTagMapping mapping = new ExternalTagMapping();
            mapping.setSourceName(ExternalSourceName.ANILIST);
            mapping.setExternalField("subject");
            mapping.setExternalValue("Survival");
            mapping.setTag(tag);
            mapping.setConfidence(TagMappingConfidence.HIGH);
            externalTagMappingRepository.persist(mapping);
        });

        TestCurrentUserProvider.useUserA();
        given()
                .contentType(io.restassured.http.ContentType.JSON)
                .body(buildAniListImportPayload())
                .when()
                .post("/api/external/import")
                .then()
                .statusCode(201)
                .body("created", is(true))
                .body("media.title", is("Shingeki no Kyojin"))
                .body("media.mediaType", is("SERIES"))
                .body("media.externalSourceName", is("ANILIST"))
                .body("media.externalSourceId", is("16498"))
                .body("media.externalReferences[0].sourceName", is("ANILIST"))
                .body("media.externalReferences[0].externalId", is("16498"))
                .body("media.externalReferences[0].attributionText", is("Metadata from AniList"))
                .body("media.tags[0].name", is("Survival"));
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

    private Map<String, Object> buildLibriVoxImportPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", "LIBRIVOX");
        payload.put("externalId", "253");
        payload.put("mediaType", "AUDIOBOOK");
        payload.put("title", "Pride and Prejudice");
        payload.put("originalTitle", null);
        payload.put("creatorNames", java.util.List.of("Author: Jane Austen", "Reader: Annie Coleman Rothenberg"));
        payload.put("description", "Jane Austen's classic novel about wit, family, and first impressions.");
        payload.put("releaseYear", 1813);
        payload.put("coverUrl", "https://archive.org/covers/pride.jpg");
        payload.put("sourceUrl", "https://librivox.org/pride-and-prejudice-by-jane-austen/");
        payload.put("externalGenres", java.util.List.of("Romance"));
        payload.put("externalSubjects", java.util.List.of("English"));
        payload.put("attribution", "LibriVox public domain audiobook catalog");
        return payload;
    }

    private Map<String, Object> buildRawgImportPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", "RAWG");
        payload.put("externalId", "3498");
        payload.put("mediaType", "GAME");
        payload.put("title", "Elden Ring");
        payload.put("originalTitle", null);
        payload.put("creatorNames", java.util.List.of(
                "Developer: FromSoftware",
                "Publisher: Bandai Namco Entertainment"));
        payload.put("description", "Rise, Tarnished, and be guided by grace.");
        payload.put("releaseYear", 2022);
        payload.put("coverUrl", "https://media.rawg.io/media/games/elden-ring.jpg");
        payload.put("sourceUrl", "https://rawg.io/games/elden-ring");
        payload.put("externalGenres", java.util.List.of("Action", "RPG"));
        payload.put("externalSubjects", java.util.List.of("PC", "PlayStation 5", "Open World"));
        payload.put("attribution", "Metadata from RAWG. View source on RAWG for full provider details.");
        return payload;
    }

    private Map<String, Object> buildAniListImportPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", "ANILIST");
        payload.put("externalId", "16498");
        payload.put("mediaType", "SERIES");
        payload.put("title", "Shingeki no Kyojin");
        payload.put("originalTitle", "進撃の巨人");
        payload.put("creatorNames", java.util.List.of("Wit Studio"));
        payload.put("description", "Humanity fights titans beyond the walls.");
        payload.put("releaseYear", 2013);
        payload.put("coverUrl", "https://img.anilist.co/aot-large.jpg");
        payload.put("sourceUrl", "https://anilist.co/anime/16498");
        payload.put("externalGenres", java.util.List.of("Action", "Drama"));
        payload.put("externalSubjects", java.util.List.of("Format: TV", "Status: FINISHED", "Survival"));
        payload.put("attribution", "Metadata from AniList");
        return payload;
    }
}
