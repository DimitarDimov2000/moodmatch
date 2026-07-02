package com.moodmatch.external.youtube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.moodmatch.entity.MediaType;
import com.moodmatch.exception.BusinessRuleViolationException;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;

class YouTubeExternalUrlResolverTest {

    @Test
    void shouldResolveYoutubeVideosIntoNormalizedVideoMetadata() {
        YouTubeExternalUrlResolver resolver = new YouTubeExternalUrlResolver();
        resolver.youTubeGateway = new FakeYouTubeGateway();
        resolver.apiKey = "test-key";

        ExternalSearchResult result = resolver.resolve("https://youtu.be/abc123XYZ_0");

        assertEquals(ExternalSearchSourceName.YOUTUBE, result.source());
        assertEquals("abc123XYZ_0", result.externalId());
        assertEquals(MediaType.VIDEO, result.mediaType());
        assertEquals("VueConf 2024 Keynote", result.title());
        assertNull(result.originalTitle());
        assertEquals(java.util.List.of("MoodMatch Dev"), result.creatorNames());
        assertEquals("Detailed walkthrough of the new import flow.", result.description());
        assertEquals(2024, result.releaseYear());
        assertEquals("https://img.youtube.test/maxres.jpg", result.coverUrl());
        assertEquals("https://www.youtube.com/watch?v=abc123XYZ_0", result.sourceUrl());
        assertEquals(java.util.List.of("Education"), result.externalGenres());
        assertEquals(
                java.util.List.of("Vue 3", "Tutorial", "Channel: MoodMatch Dev", "Category: Education"),
                result.externalSubjects());
        assertEquals("Metadata from YouTube", result.attribution());
    }

    @Test
    void shouldExposeMissingConfigurationClearly() {
        YouTubeExternalUrlResolver resolver = new YouTubeExternalUrlResolver();
        resolver.apiKey = "__missing_youtube_config__";

        assertFalse(resolver.isConfigured());
        assertEquals(
                "YouTube provider is not configured. Set MOODMATCH_YOUTUBE_API_KEY in the backend environment.",
                resolver.configurationErrorMessage());
    }

    @Test
    void shouldRejectMissingVideosClearly() {
        YouTubeExternalUrlResolver resolver = new YouTubeExternalUrlResolver();
        resolver.youTubeGateway = new MissingVideoGateway();
        resolver.apiKey = "test-key";

        BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                () -> resolver.resolve("abc123XYZ_0"));

        assertEquals("No YouTube video was found for the provided URL or video ID.", exception.getMessage());
    }

    private static final class FakeYouTubeGateway implements YouTubeGateway {

        @Override
        public java.util.List<YouTubeVideo> searchVideos(String apiKey, String query, int maxResults, String order) {
            return java.util.List.of();
        }

        @Override
        public java.util.Optional<YouTubeVideo> fetchVideo(String apiKey, String videoId) {
            assertEquals("test-key", apiKey);
            assertEquals("abc123XYZ_0", videoId);
            return java.util.Optional.of(new YouTubeVideo(
                    videoId,
                    "VueConf 2024 Keynote",
                    "Detailed\nwalkthrough of the new import flow.",
                    "MoodMatch Dev",
                    "2024-05-20T10:30:00Z",
                    "27",
                    java.util.List.of("Vue 3", "Tutorial"),
                    new ThumbnailSet(
                            null,
                            "https://img.youtube.test/medium.jpg",
                            "https://img.youtube.test/high.jpg",
                            null,
                            "https://img.youtube.test/maxres.jpg")));
        }

        @Override
        public java.util.Optional<String> fetchCategoryLabel(String apiKey, String categoryId) {
            assertEquals("27", categoryId);
            return java.util.Optional.of("Education");
        }
    }

    private static final class MissingVideoGateway implements YouTubeGateway {

        @Override
        public java.util.List<YouTubeVideo> searchVideos(String apiKey, String query, int maxResults, String order) {
            return java.util.List.of();
        }

        @Override
        public java.util.Optional<YouTubeVideo> fetchVideo(String apiKey, String videoId) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<String> fetchCategoryLabel(String apiKey, String categoryId) {
            assertTrue(false, "Category lookup should not run when the video is missing.");
            return java.util.Optional.empty();
        }
    }
}
