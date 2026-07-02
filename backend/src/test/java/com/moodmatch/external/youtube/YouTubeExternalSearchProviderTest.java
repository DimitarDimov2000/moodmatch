package com.moodmatch.external.youtube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.moodmatch.entity.MediaType;
import com.moodmatch.external.adapter.ExternalSearchRequest;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;

class YouTubeExternalSearchProviderTest {

    @Test
    void shouldMapYoutubeSearchResultsIntoNormalizedVideoMetadata() {
        YouTubeExternalSearchProvider provider = new YouTubeExternalSearchProvider();
        provider.youTubeGateway = new FakeYouTubeGateway();
        provider.apiKey = "test-key";

        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("ai tutorial", MediaType.VIDEO, ExternalSearchSourceName.YOUTUBE, 10));

        assertEquals(1, results.size());
        ExternalSearchResult result = results.getFirst();
        assertEquals(ExternalSearchSourceName.YOUTUBE, result.source());
        assertEquals("abc123XYZ_0", result.externalId());
        assertEquals(MediaType.VIDEO, result.mediaType());
        assertEquals("AI Tutorial for Builders", result.title());
        assertEquals(List.of("MoodMatch Dev"), result.creatorNames());
        assertEquals("Build better search imports with the official YouTube API.", result.description());
        assertEquals(2024, result.releaseYear());
        assertEquals("https://img.youtube.test/high.jpg", result.coverUrl());
        assertEquals("https://www.youtube.com/watch?v=abc123XYZ_0", result.sourceUrl());
        assertTrue(result.externalGenres().isEmpty());
        assertEquals(List.of("Channel: MoodMatch Dev"), result.externalSubjects());
        assertEquals("Metadata from YouTube", result.attribution());
    }

    @Test
    void shouldExposeMissingConfigurationClearly() {
        YouTubeExternalSearchProvider provider = new YouTubeExternalSearchProvider();
        provider.apiKey = "__missing_youtube_config__";

        assertFalse(provider.isConfigured());
        assertEquals(
                "YouTube provider is not configured. Set MOODMATCH_YOUTUBE_API_KEY in the backend environment.",
                provider.configurationErrorMessage());
    }

    @Test
    void shouldSkipInvalidGatewayRowsInsteadOfReturningBrokenResults() {
        YouTubeExternalSearchProvider provider = new YouTubeExternalSearchProvider();
        provider.youTubeGateway = new FakeYouTubeGatewayWithInvalidRows();
        provider.apiKey = "test-key";

        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("ai tutorial", MediaType.VIDEO, ExternalSearchSourceName.YOUTUBE, 10));

        assertEquals(1, results.size());
        assertEquals("valid12345A", results.getFirst().externalId());
    }

    private static final class FakeYouTubeGateway implements YouTubeGateway {

        @Override
        public List<YouTubeVideo> searchVideos(String apiKey, String query, int maxResults, String order) {
            assertEquals("test-key", apiKey);
            assertEquals("ai tutorial", query);
            assertEquals(10, maxResults);
            assertEquals("relevance", order);
            return List.of(new YouTubeVideo(
                    "abc123XYZ_0",
                    "AI Tutorial for Builders",
                    "Build better\nsearch imports with the official YouTube API.",
                    "MoodMatch Dev",
                    "2024-05-20T10:30:00Z",
                    null,
                    List.of(),
                    new ThumbnailSet(
                            null,
                            null,
                            "https://img.youtube.test/high.jpg",
                            null,
                            null)));
        }

        @Override
        public Optional<YouTubeVideo> fetchVideo(String apiKey, String videoId) {
            return Optional.empty();
        }

        @Override
        public Optional<String> fetchCategoryLabel(String apiKey, String categoryId) {
            return Optional.empty();
        }
    }

    private static final class FakeYouTubeGatewayWithInvalidRows implements YouTubeGateway {

        @Override
        public List<YouTubeVideo> searchVideos(String apiKey, String query, int maxResults, String order) {
            return List.of(
                    new YouTubeVideo(
                            "",
                            "Missing id",
                            null,
                            "MoodMatch Dev",
                            "2024-05-20T10:30:00Z",
                            null,
                            List.of(),
                            null),
                    new YouTubeVideo(
                            "missing-title",
                            " ",
                            null,
                            "MoodMatch Dev",
                            "2024-05-20T10:30:00Z",
                            null,
                            List.of(),
                            null),
                    new YouTubeVideo(
                            "valid12345A",
                            "Valid row",
                            null,
                            "MoodMatch Dev",
                            "2024-05-20T10:30:00Z",
                            null,
                            List.of(),
                            null));
        }

        @Override
        public Optional<YouTubeVideo> fetchVideo(String apiKey, String videoId) {
            return Optional.empty();
        }

        @Override
        public Optional<String> fetchCategoryLabel(String apiKey, String categoryId) {
            return Optional.empty();
        }
    }
}
