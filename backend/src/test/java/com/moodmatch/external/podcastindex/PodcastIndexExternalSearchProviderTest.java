package com.moodmatch.external.podcastindex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.moodmatch.entity.MediaType;
import com.moodmatch.external.adapter.ExternalSearchRequest;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;

class PodcastIndexExternalSearchProviderTest {

    @Test
    void shouldMapPodcastSearchResultsIntoNormalizedExternalResults() {
        PodcastIndexExternalSearchProvider provider = new PodcastIndexExternalSearchProvider();
        provider.podcastIndexGateway = new FakePodcastIndexGateway();
        provider.apiKey = "test-key";
        provider.apiSecret = "test-secret";

        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("lex fridman", MediaType.PODCAST, ExternalSearchSourceName.PODCAST_INDEX, 5));

        assertEquals(1, results.size());
        ExternalSearchResult result = results.getFirst();
        assertEquals(ExternalSearchSourceName.PODCAST_INDEX, result.source());
        assertEquals("75075", result.externalId());
        assertEquals(MediaType.PODCAST, result.mediaType());
        assertEquals("Lex Fridman Podcast", result.title());
        assertEquals(List.of("Lex Fridman"), result.creatorNames());
        assertEquals(
                "Conversations about science, technology, history, philosophy, and the nature of intelligence.",
                result.description());
        assertEquals(2024, result.releaseYear());
        assertEquals("https://image.simplecastcdn.com/images/lex-fridman.jpg", result.coverUrl());
        assertEquals("https://lexfridman.com/podcast/", result.sourceUrl());
        assertEquals(List.of("Technology", "Science"), result.externalGenres());
        assertEquals(List.of("Language: en", "Explicit: No", "Feed type: podcast"), result.externalSubjects());
        assertEquals("Metadata from Podcast Index", result.attribution());
    }

    @Test
    void shouldExposeMissingConfigurationStateClearly() {
        PodcastIndexExternalSearchProvider provider = new PodcastIndexExternalSearchProvider();
        provider.apiKey = "__missing_podcastindex_config__";
        provider.apiSecret = "__missing_podcastindex_config__";

        assertTrue(!provider.isConfigured());
        assertEquals(
                "Podcast Index provider is not configured. Set MOODMATCH_PODCASTINDEX_KEY and MOODMATCH_PODCASTINDEX_SECRET.",
                provider.configurationErrorMessage());
    }

    @Test
    void shouldStripHtmlDescriptionsIntoPlainText() {
        PodcastIndexExternalSearchProvider provider = new PodcastIndexExternalSearchProvider();

        assertEquals(
                "Hello & welcome to podcasts.",
                provider.stripHtml("<p>Hello &amp; welcome<br>to podcasts.</p>"));
    }

    @Test
    void shouldReturnEmptyResultsWhenGatewayReturnsNoFeeds() {
        PodcastIndexExternalSearchProvider provider = new PodcastIndexExternalSearchProvider();
        provider.podcastIndexGateway = (apiKey, apiSecret, query, limit) -> List.of();
        provider.apiKey = "test-key";
        provider.apiSecret = "test-secret";

        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("missing", MediaType.PODCAST, ExternalSearchSourceName.PODCAST_INDEX, 5));

        assertTrue(results.isEmpty());
    }

    private static final class FakePodcastIndexGateway implements PodcastIndexGateway {

        @Override
        public List<PodcastFeed> searchShows(String apiKey, String apiSecret, String query, int limit) {
            return List.of(new PodcastFeed(
                    75075L,
                    "Lex Fridman Podcast",
                    "https://lexfridman.com/feed/podcast/",
                    "https://lexfridman.com/feed/podcast/",
                    "https://lexfridman.com/podcast/",
                    "<p>Conversations about science, technology, history, philosophy, and the nature of intelligence.</p>",
                    "Lex Fridman",
                    "Lex Fridman",
                    "https://image.simplecastcdn.com/images/lex-fridman-image.jpg",
                    "https://image.simplecastcdn.com/images/lex-fridman.jpg",
                    1_719_838_400L,
                    "en",
                    0,
                    "podcast",
                    List.of("Technology", "Science")));
        }
    }
}
