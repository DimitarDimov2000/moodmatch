package com.moodmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moodmatch.entity.ExternalSourceName;
import com.moodmatch.entity.ExternalTagMapping;
import com.moodmatch.entity.MediaType;
import com.moodmatch.entity.Tag;
import com.moodmatch.entity.TagCategory;
import com.moodmatch.entity.TagMappingConfidence;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;
import com.moodmatch.external.adapter.ExternalSourceMappings;
import com.moodmatch.external.adapter.ExternalSuggestedTag;
import com.moodmatch.repository.ExternalTagMappingRepository;
import com.moodmatch.repository.TagRepository;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
class ExternalTagSuggestionServiceTest {

    @Inject
    ExternalTagSuggestionService externalTagSuggestionService;

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
    void shouldPreferExplicitMappingsOverFallbackSuggestions() {
        QuarkusTransaction.requiringNew().run(() -> {
            Tag tag = new Tag();
            tag.setId(UUID.fromString("10000000-0000-0000-0000-000000000101"));
            tag.setName("Sci-Fi");
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

        List<ExternalSuggestedTag> suggestions = externalTagSuggestionService.buildSuggestions(result(
                ExternalSearchSourceName.TMDB,
                MediaType.FILM,
                List.of("Science Fiction"),
                List.of()));

        assertEquals(1, suggestions.size());
        assertEquals("Sci-Fi", suggestions.getFirst().tagName());
        assertEquals("Mapped from external genre value.", suggestions.getFirst().reason());
        assertEquals(TagMappingConfidence.HIGH, suggestions.getFirst().confidence());
    }

    @Test
    void shouldBuildFallbackSuggestionsFromNormalizedExternalGenres() {
        List<ExternalSuggestedTag> suggestions = externalTagSuggestionService.buildSuggestions(result(
                ExternalSearchSourceName.RAWG,
                MediaType.GAME,
                List.of("Action", "RPG", "science-fiction"),
                List.of()));

        assertEquals(List.of("Action", "RPG", "Science Fiction"),
                suggestions.stream().map(ExternalSuggestedTag::tagName).toList());
        assertEquals(List.of(TagCategory.GENRE, TagCategory.GENRE, TagCategory.GENRE),
                suggestions.stream().map(ExternalSuggestedTag::tagCategory).toList());
        assertEquals(List.of(TagMappingConfidence.LOW, TagMappingConfidence.LOW, TagMappingConfidence.LOW),
                suggestions.stream().map(ExternalSuggestedTag::confidence).toList());
    }

    @Test
    void shouldBuildFallbackSuggestionsFromSafeExternalSubjects() {
        List<ExternalSuggestedTag> suggestions = externalTagSuggestionService.buildSuggestions(result(
                ExternalSearchSourceName.OPEN_LIBRARY,
                MediaType.BOOK,
                List.of(),
                List.of("Politics", "Desert planets", "https://example.test/noisy")));

        assertEquals(List.of("Politics", "Desert Planets"),
                suggestions.stream().map(ExternalSuggestedTag::tagName).toList());
        assertEquals(List.of(TagCategory.THEME, TagCategory.THEME),
                suggestions.stream().map(ExternalSuggestedTag::tagCategory).toList());
    }

    @Test
    void shouldFilterNoisyProviderSubjects() {
        List<ExternalSuggestedTag> suggestions = externalTagSuggestionService.buildSuggestions(result(
                ExternalSearchSourceName.ANILIST,
                MediaType.SERIES,
                List.of(),
                List.of(
                        "Status: FINISHED",
                        "Format: TV",
                        "Feed type: podcast",
                        "Explicit: No",
                        "Channel: MoodMatch Dev",
                        "Survival")));

        assertEquals(List.of("Survival"), suggestions.stream().map(ExternalSuggestedTag::tagName).toList());
        assertFalse(suggestions.stream().map(ExternalSuggestedTag::sourceValue).toList().contains("Format: TV"));
    }

    @Test
    void shouldCollapseDuplicateFallbackVariants() {
        List<ExternalSuggestedTag> suggestions = externalTagSuggestionService.buildSuggestions(result(
                ExternalSearchSourceName.TMDB,
                MediaType.FILM,
                List.of("Sci-Fi", "science-fiction", "Science Fiction", "Drama"),
                List.of("kids", "children")));

        assertEquals(List.of("Science Fiction", "Drama", "Children"),
                suggestions.stream().map(ExternalSuggestedTag::tagName).toList());
    }

    private ExternalSearchResult result(
            ExternalSearchSourceName source,
            MediaType mediaType,
            List<String> externalGenres,
            List<String> externalSubjects) {
        return new ExternalSearchResult(
                source,
                ExternalSourceMappings.toMappingSource(source, mediaType),
                "external-id",
                mediaType,
                "External title",
                null,
                List.of(),
                null,
                null,
                null,
                null,
                externalGenres,
                externalSubjects,
                List.of(),
                "Metadata from provider",
                List.of());
    }
}
