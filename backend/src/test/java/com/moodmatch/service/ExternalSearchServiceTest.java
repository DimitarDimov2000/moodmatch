package com.moodmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moodmatch.dto.external.ExternalSearchResponse;
import com.moodmatch.entity.ExternalSourceName;
import com.moodmatch.entity.ExternalTagMapping;
import com.moodmatch.entity.Tag;
import com.moodmatch.entity.TagCategory;
import com.moodmatch.entity.TagMappingConfidence;
import com.moodmatch.external.librivox.TestLibriVoxGateway;
import com.moodmatch.external.openlibrary.TestOpenLibraryGateway;
import com.moodmatch.external.rawg.TestRawgGateway;
import com.moodmatch.repository.ExternalTagMappingRepository;
import com.moodmatch.repository.TagRepository;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ExternalSearchServiceTest {

    @Inject
    ExternalSearchService externalSearchService;

    @Inject
    ExternalTagMappingRepository externalTagMappingRepository;

    @Inject
    TagRepository tagRepository;

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
        TestCurrentUserProvider.useLocalDemoUser();
        TestOpenLibraryGateway.reset();
        TestLibriVoxGateway.reset();
        TestRawgGateway.reset();
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
    void shouldReturnEmptySuggestedTagsWhenNoMappingsExist() {
        ExternalSearchResponse response = externalSearchService.search("arrival", "FILM", null, null);

        assertEquals(1, response.results().size());
        assertTrue(response.results().getFirst().suggestedTags().isEmpty());
        assertEquals("DEMO", response.source().name());
        assertEquals(
                List.of("TMDB provider is not configured. Set MOODMATCH_TMDB_API_KEY. Using DEMO fallback."),
                response.warnings());
    }

    @Test
    @TestTransaction
    void shouldEnrichNormalizedResultsWithExistingMappedTags() {
        Tag tag = new Tag();
        tag.setId(UUID.fromString("10000000-0000-0000-0000-000000000001"));
        tag.setName("Entdeckung");
        tag.setCategory(TagCategory.THEME);
        tagRepository.persist(tag);

        ExternalTagMapping mapping = new ExternalTagMapping();
        mapping.setSourceName(ExternalSourceName.RAWG);
        mapping.setExternalField("subject");
        mapping.setExternalValue("Entdeckung");
        mapping.setTag(tag);
        mapping.setConfidence(TagMappingConfidence.HIGH);
        externalTagMappingRepository.persist(mapping);

        ExternalSearchResponse response = externalSearchService.search("outer", "GAME", "DEMO", 10);

        assertEquals(1, response.results().size());
        assertEquals(1, response.results().getFirst().suggestedTags().size());
        assertEquals("Entdeckung", response.results().getFirst().suggestedTags().getFirst().tagName());
        assertEquals("Entdeckung", response.results().getFirst().suggestedTags().getFirst().sourceValue());
    }

    @Test
    void shouldClampTheRequestedLimitToSafeBounds() {
        ExternalSearchResponse response = externalSearchService.search("a", "FILM", "DEMO", 999);

        assertEquals(List.of("Arrival", "Severance Preview Reel"),
                response.results().stream().map(result -> result.title()).toList());
    }

    @Test
    void shouldRejectExplicitTmdbSearchWhenProviderConfigIsMissing() {
        try {
            externalSearchService.search("arrival", "FILM", "TMDB", 5);
        } catch (com.moodmatch.exception.BusinessRuleViolationException exception) {
            assertEquals("TMDB provider is not configured. Set MOODMATCH_TMDB_API_KEY.", exception.getMessage());
            return;
        }

        throw new AssertionError("Expected TMDB configuration error.");
    }

    @Test
    void shouldPreferOpenLibraryForBookSearchesWhenNoSourceIsSpecified() {
        ExternalSearchResponse response = externalSearchService.search("dune", "BOOK", null, 5);

        assertEquals("OPEN_LIBRARY", response.source().name());
        assertEquals(1, response.results().size());
        assertEquals("Dune", response.results().getFirst().title());
        assertEquals(List.of("Frank Herbert"), response.results().getFirst().creatorNames());
        assertEquals("OL12345W", response.results().getFirst().externalId());
        assertTrue(response.warnings().isEmpty());
    }

    @Test
    void shouldAcceptExplicitOpenLibrarySourceNames() {
        ExternalSearchResponse response = externalSearchService.search("dune", "BOOK", "OPEN_LIBRARY", 5);

        assertEquals("OPEN_LIBRARY", response.source().name());
        assertEquals("OPEN_LIBRARY", response.results().getFirst().source().name());
    }

    @Test
    void shouldPreferLibriVoxForAudiobookSearchesWhenNoSourceIsSpecified() {
        ExternalSearchResponse response = externalSearchService.search("pride", "AUDIOBOOK", null, 5);

        assertEquals("AUDIOBOOK", response.mediaType().name());
        assertEquals("LIBRIVOX", response.source().name());
        assertEquals(1, response.results().size());
        assertEquals("Pride and Prejudice", response.results().getFirst().title());
        assertEquals("LIBRIVOX", response.results().getFirst().source().name());
        assertTrue(response.warnings().isEmpty());
    }

    @Test
    void shouldAcceptExplicitLibriVoxSourceNames() {
        ExternalSearchResponse response = externalSearchService.search("pride", "AUDIOBOOK", "LIBRIVOX", 5);

        assertEquals("LIBRIVOX", response.source().name());
        assertEquals("LIBRIVOX", response.results().getFirst().source().name());
    }

    @Test
    void shouldKeepDemoFallbackForFutureMediaTypesWithoutARealProvider() {
        ExternalSearchResponse response = externalSearchService.search("episode", "PODCAST", null, 5);

        assertEquals("PODCAST", response.mediaType().name());
        assertEquals("DEMO", response.source().name());
        assertTrue(response.results().isEmpty());
        assertTrue(response.warnings().isEmpty());
    }

    @Test
    void shouldFallbackToDemoForGameSearchesWhenRawgConfigIsMissing() {
        ExternalSearchResponse response = externalSearchService.search("zelda", "GAME", null, 5);

        assertEquals("GAME", response.mediaType().name());
        assertEquals("DEMO", response.source().name());
        assertEquals(
                List.of("RAWG provider is not configured. Set MOODMATCH_RAWG_API_KEY. Using DEMO fallback."),
                response.warnings());
    }

    @Test
    void shouldRejectExplicitRawgSearchWhenProviderConfigIsMissing() {
        com.moodmatch.exception.BusinessRuleViolationException exception = assertThrows(
                com.moodmatch.exception.BusinessRuleViolationException.class,
                () -> externalSearchService.search("zelda", "GAME", "RAWG", 5));

        assertEquals("RAWG provider is not configured. Set MOODMATCH_RAWG_API_KEY.", exception.getMessage());
    }

    @Test
    void shouldRejectUnknownProviderNames() {
        com.moodmatch.exception.BusinessRuleViolationException exception = assertThrows(
                com.moodmatch.exception.BusinessRuleViolationException.class,
                () -> externalSearchService.search("song", "AUDIOBOOK", "SPOTIFY", 5));

        assertEquals("Unsupported source: SPOTIFY", exception.getMessage());
    }
}
