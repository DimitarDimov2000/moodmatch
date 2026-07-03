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
import com.moodmatch.external.anilist.TestAniListGateway;
import com.moodmatch.external.librivox.TestLibriVoxGateway;
import com.moodmatch.external.openlibrary.TestOpenLibraryGateway;
import com.moodmatch.external.podcastindex.TestPodcastIndexGateway;
import com.moodmatch.external.rawg.TestRawgGateway;
import com.moodmatch.external.youtube.TestYouTubeGateway;
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
        TestAniListGateway.reset();
        TestPodcastIndexGateway.reset();
        TestYouTubeGateway.reset();
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
    void shouldReturnFallbackSuggestedTagsWhenNoMappingsExist() {
        ExternalSearchResponse response = externalSearchService.search("arrival", "FILM", "DEMO", null);

        assertEquals(1, response.results().size());
        assertEquals(List.of("Science Fiction", "Drama", "Zeit", "Identitaet", "Entdeckung"),
                response.results().getFirst().suggestedTags().stream().map(suggestion -> suggestion.tagName()).toList());
        assertEquals("DEMO", response.source().name());
        assertTrue(response.warnings().isEmpty());
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
        assertTrue(response.results().getFirst().suggestedTags().stream()
                .anyMatch(suggestion -> suggestion.tagName().equals("Entdeckung")
                        && suggestion.sourceValue().equals("Entdeckung")
                        && suggestion.confidence() == TagMappingConfidence.HIGH));
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
            assertEquals(
                    "TMDB provider is not configured. Set MOODMATCH_TMDB_API_KEY in the backend environment to a TMDB v3 API key.",
                    exception.getMessage());
            return;
        }

        throw new AssertionError("Expected TMDB configuration error.");
    }

    @Test
    void shouldSearchOpenLibraryAndAniListForBookSearchesWhenNoSourceIsSpecified() {
        ExternalSearchResponse response = externalSearchService.search("dune", "BOOK", null, 5);

        assertEquals("AUTOMATIC", response.source().name());
        assertEquals(2, response.results().size());
        assertEquals("Dune", response.results().getFirst().title());
        assertEquals(List.of("Frank Herbert"), response.results().getFirst().creatorNames());
        assertEquals("OL12345W", response.results().getFirst().externalId());
        assertEquals("OPEN_LIBRARY", response.results().getFirst().source().name());
        assertEquals("Berserk", response.results().get(1).title());
        assertEquals("ANILIST", response.results().get(1).source().name());
        assertTrue(response.warnings().isEmpty());
    }

    @Test
    void shouldSearchAniListForAutomaticSeriesWhenTmdbConfigIsMissing() {
        ExternalSearchResponse response = externalSearchService.search("attack on titan", "SERIES", null, 5);

        assertEquals("AUTOMATIC", response.source().name());
        assertEquals("Shingeki no Kyojin", response.results().getFirst().title());
        assertEquals("ANILIST", response.results().getFirst().source().name());
        assertEquals("SERIES", response.results().getFirst().mediaType().name());
        assertEquals(
                List.of(
                        "TMDB provider is not configured. Set MOODMATCH_TMDB_API_KEY in the backend environment to a TMDB v3 API key. Provider skipped in automatic search."),
                response.warnings());
    }

    @Test
    void shouldLimitAutomaticSearchFanoutPerProvider() {
        TestOpenLibraryGateway.useResults(java.util.stream.IntStream.rangeClosed(1, 7)
                .mapToObj(index -> new com.moodmatch.external.openlibrary.OpenLibraryGateway.OpenLibrarySearchItem(
                        "/works/OL" + index + "W",
                        List.of("OL" + index + "M"),
                        "Open Library Result " + index,
                        List.of("Author " + index),
                        2000 + index,
                        1000 + index,
                        List.of("Science Fiction"),
                        null))
                .toList());
        TestAniListGateway.useResults(java.util.stream.IntStream.rangeClosed(1, 7)
                .mapToObj(index -> new com.moodmatch.external.anilist.AniListGateway.AniListMedia(
                        30000 + index,
                        "MANGA",
                        "MANGA",
                        "RELEASING",
                        null,
                        null,
                        1990 + index,
                        new com.moodmatch.external.anilist.AniListGateway.AniListTitle(
                                "AniList Result " + index,
                                null,
                                null),
                        "<p>Result " + index + "</p>",
                        null,
                        "https://anilist.co/manga/" + (30000 + index),
                        List.of("Fantasy"),
                        List.of("Adventure"),
                        List.of(),
                        List.of("Creator " + index)))
                .toList());

        ExternalSearchResponse response = externalSearchService.search("result", "BOOK", null, 10);

        assertEquals(10, response.results().size());
        assertEquals(List.of(
                "Open Library Result 1",
                "Open Library Result 2",
                "Open Library Result 3",
                "Open Library Result 4",
                "Open Library Result 5",
                "AniList Result 1",
                "AniList Result 2",
                "AniList Result 3",
                "AniList Result 4",
                "AniList Result 5"), response.results().stream().map(result -> result.title()).toList());
    }

    @Test
    void shouldSearchAniListMovieResultsForAutomaticFilmWhenTmdbConfigIsMissing() {
        ExternalSearchResponse response = externalSearchService.search("spirited away", "FILM", "AUTOMATIC", 5);

        assertEquals("AUTOMATIC", response.source().name());
        assertEquals("Sen to Chihiro no Kamikakushi", response.results().getFirst().title());
        assertEquals("ANILIST", response.results().getFirst().source().name());
        assertEquals("FILM", response.results().getFirst().mediaType().name());
        assertEquals(
                List.of(
                        "TMDB provider is not configured. Set MOODMATCH_TMDB_API_KEY in the backend environment to a TMDB v3 API key. Provider skipped in automatic search."),
                response.warnings());
    }

    @Test
    void shouldSearchYoutubeVideosExplicitlyForVideoResults() {
        ExternalSearchResponse response = externalSearchService.search("ai tutorial", "VIDEO", "YOUTUBE", null);

        assertEquals("YOUTUBE", response.source().name());
        assertEquals(1, response.results().size());
        assertEquals("VueConf 2024 Keynote", response.results().getFirst().title());
        assertEquals("MoodMatch Dev", response.results().getFirst().creatorNames().getFirst());
        assertEquals("VIDEO", response.results().getFirst().mediaType().name());
        assertEquals("YOUTUBE", response.results().getFirst().source().name());
        assertEquals("https://img.youtube.test/maxres.jpg", response.results().getFirst().coverUrl());
        assertTrue(response.warnings().isEmpty());
    }

    @Test
    void shouldUseYoutubeDefaultLimitOfTenWhenExplicitVideoSearchOmitsLimit() {
        TestYouTubeGateway.useSearchResults(java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(index -> new com.moodmatch.external.youtube.YouTubeGateway.YouTubeVideo(
                        "video0000" + String.format("%02d", index),
                        "YouTube Result " + index,
                        "Description " + index,
                        "MoodMatch Dev",
                        "2024-05-20T10:30:00Z",
                        null,
                        java.util.List.of(),
                        new com.moodmatch.external.youtube.YouTubeGateway.ThumbnailSet(
                                null,
                                null,
                                "https://img.youtube.test/" + index + ".jpg",
                                null,
                                null)))
                .toList());

        ExternalSearchResponse response = externalSearchService.search("ai tutorial", "VIDEO", "YOUTUBE", null);

        assertEquals(10, response.results().size());
        assertEquals("YouTube Result 1", response.results().getFirst().title());
        assertEquals("YouTube Result 10", response.results().get(9).title());
    }

    @Test
    void shouldPassYoutubeExplicitSortOrderToTheGateway() {
        externalSearchService.search("ai tutorial", "VIDEO", "YOUTUBE", null, "most_viewed");

        assertEquals("viewCount", TestYouTubeGateway.lastSearchOrder());
    }

    @Test
    void shouldDeduplicateAutomaticResultsBySourceAndExternalIdOnly() {
        TestAniListGateway.useResults(List.of(
                new com.moodmatch.external.anilist.AniListGateway.AniListMedia(
                        16498,
                        "ANIME",
                        "TV",
                        "FINISHED",
                        "SPRING",
                        2013,
                        2013,
                        new com.moodmatch.external.anilist.AniListGateway.AniListTitle(
                                "Shingeki no Kyojin", "Attack on Titan", "進撃の巨人"),
                        "<p>Humanity fights titans beyond the walls.</p>",
                        new com.moodmatch.external.anilist.AniListGateway.AniListCoverImage(
                                "https://img.anilist.co/aot-large.jpg", null),
                        "https://anilist.co/anime/16498",
                        List.of("Action"),
                        List.of("Survival"),
                        List.of("Wit Studio"),
                        List.of()),
                new com.moodmatch.external.anilist.AniListGateway.AniListMedia(
                        16498,
                        "ANIME",
                        "TV",
                        "FINISHED",
                        "SPRING",
                        2013,
                        2013,
                        new com.moodmatch.external.anilist.AniListGateway.AniListTitle(
                                "Attack on Titan Duplicate", null, null),
                        "<p>Duplicate.</p>",
                        null,
                        "https://anilist.co/anime/16498",
                        List.of("Action"),
                        List.of("Survival"),
                        List.of("Wit Studio"),
                        List.of())));

        ExternalSearchResponse response = externalSearchService.search("attack on titan", "SERIES", null, 5);

        assertEquals(1, response.results().size());
        assertEquals("Shingeki no Kyojin", response.results().getFirst().title());
    }

    @Test
    void shouldAcceptExplicitOpenLibrarySourceNames() {
        ExternalSearchResponse response = externalSearchService.search("dune", "BOOK", "OPEN_LIBRARY", 5);

        assertEquals("OPEN_LIBRARY", response.source().name());
        assertEquals("OPEN_LIBRARY", response.results().getFirst().source().name());
    }

    @Test
    void shouldAcceptExplicitAniListSourceNamesForAnimeAndManga() {
        ExternalSearchResponse animeResponse = externalSearchService.search("attack on titan", "SERIES", "ANILIST", 5);
        ExternalSearchResponse animeMovieResponse = externalSearchService.search("spirited away", "FILM", "ANILIST", 5);
        ExternalSearchResponse mangaResponse = externalSearchService.search("berserk", "BOOK", "ANILIST", 5);

        assertEquals("ANILIST", animeResponse.source().name());
        assertEquals("ANILIST", animeResponse.results().getFirst().source().name());
        assertEquals("SERIES", animeResponse.results().getFirst().mediaType().name());
        assertEquals("Shingeki no Kyojin", animeResponse.results().getFirst().title());
        assertEquals("ANILIST", animeMovieResponse.source().name());
        assertEquals("FILM", animeMovieResponse.results().getFirst().mediaType().name());
        assertEquals("Sen to Chihiro no Kamikakushi", animeMovieResponse.results().getFirst().title());
        assertEquals("ANILIST", mangaResponse.source().name());
        assertEquals("BOOK", mangaResponse.results().getFirst().mediaType().name());
        assertEquals("Berserk", mangaResponse.results().getFirst().title());
    }

    @Test
    void shouldPreferLibriVoxForAudiobookSearchesWhenNoSourceIsSpecified() {
        ExternalSearchResponse response = externalSearchService.search("pride", "AUDIOBOOK", null, 5);

        assertEquals("AUDIOBOOK", response.mediaType().name());
        assertEquals("AUTOMATIC", response.source().name());
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
    void shouldSkipUnconfiguredPodcastIndexInAutomaticPodcastSearchWithoutCrashing() {
        ExternalSearchResponse response = externalSearchService.search("lex fridman", "PODCAST", null, 5);

        assertEquals("PODCAST", response.mediaType().name());
        assertEquals("AUTOMATIC", response.source().name());
        assertTrue(response.results().isEmpty());
        assertEquals(
                List.of(
                        "Podcast Index provider is not configured. Set MOODMATCH_PODCASTINDEX_KEY and MOODMATCH_PODCASTINDEX_SECRET. Provider skipped in automatic search."),
                response.warnings());
    }

    @Test
    void shouldRejectExplicitPodcastIndexSearchWhenProviderConfigIsMissing() {
        com.moodmatch.exception.BusinessRuleViolationException exception = assertThrows(
                com.moodmatch.exception.BusinessRuleViolationException.class,
                () -> externalSearchService.search("radiolab", "PODCAST", "PODCAST_INDEX", 5));

        assertEquals(
                "Podcast Index provider is not configured. Set MOODMATCH_PODCASTINDEX_KEY and MOODMATCH_PODCASTINDEX_SECRET.",
                exception.getMessage());
    }

    @Test
    void shouldFallbackToDemoForGameSearchesWhenRawgConfigIsMissing() {
        ExternalSearchResponse response = externalSearchService.search("zelda", "GAME", null, 5);

        assertEquals("GAME", response.mediaType().name());
        assertEquals("AUTOMATIC", response.source().name());
        assertEquals(
                List.of(
                        "RAWG provider is not configured. Set MOODMATCH_RAWG_API_KEY. Provider skipped in automatic search.",
                        "Using DEMO fallback."),
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
