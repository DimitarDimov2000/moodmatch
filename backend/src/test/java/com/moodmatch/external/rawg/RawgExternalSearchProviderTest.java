package com.moodmatch.external.rawg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.moodmatch.entity.MediaType;
import com.moodmatch.external.adapter.ExternalSearchRequest;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;

class RawgExternalSearchProviderTest {

    @Test
    void shouldMapGameSearchResultsIntoNormalizedExternalResults() {
        RawgExternalSearchProvider provider = new RawgExternalSearchProvider();
        provider.rawgGateway = new FakeRawgGateway();
        provider.apiKey = Optional.of("test-key");
        provider.websiteBaseUrl = "https://rawg.io";

        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("elden ring", MediaType.GAME, ExternalSearchSourceName.RAWG, 5));

        assertEquals(1, results.size());
        ExternalSearchResult result = results.getFirst();
        assertEquals(ExternalSearchSourceName.RAWG, result.source());
        assertEquals("3498", result.externalId());
        assertEquals(MediaType.GAME, result.mediaType());
        assertEquals("Elden Ring", result.title());
        assertEquals("Developer: FromSoftware", result.creatorNames().getFirst());
        assertEquals("Publisher: Bandai Namco Entertainment", result.creatorNames().get(1));
        assertEquals("Rise, Tarnished, and be guided by grace.", result.description());
        assertEquals(2022, result.releaseYear());
        assertEquals("https://media.rawg.io/media/games/elden-ring.jpg", result.coverUrl());
        assertEquals("https://rawg.io/games/elden-ring", result.sourceUrl());
        assertEquals(List.of("Action", "RPG"), result.externalGenres());
        assertEquals(List.of("PC", "PlayStation 5", "Singleplayer", "Open World"), result.externalSubjects());
        assertEquals("Metadata from RAWG. View source on RAWG for full provider details.", result.attribution());
    }

    @Test
    void shouldExposeMissingConfigurationStateClearly() {
        RawgExternalSearchProvider provider = new RawgExternalSearchProvider();
        provider.apiKey = Optional.empty();

        assertTrue(!provider.isConfigured());
        assertEquals("RAWG provider is not configured. Set MOODMATCH_RAWG_API_KEY.",
                provider.configurationErrorMessage());
    }

    @Test
    void shouldReturnEmptyResultsWhenGatewayReturnsNoGames() {
        RawgExternalSearchProvider provider = new RawgExternalSearchProvider();
        provider.rawgGateway = (apiKey, query, limit) -> List.of();
        provider.apiKey = Optional.of("test-key");
        provider.websiteBaseUrl = "https://rawg.io";

        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("missing", MediaType.GAME, ExternalSearchSourceName.RAWG, 5));

        assertTrue(results.isEmpty());
    }

    private static final class FakeRawgGateway implements RawgGateway {

        @Override
        public List<RawgGame> searchGames(String apiKey, String query, int limit) {
            return List.of(new RawgGame(
                    3498,
                    "elden-ring",
                    "Elden Ring",
                    "Rise, Tarnished, and be guided by grace.",
                    "2022-02-25",
                    "https://media.rawg.io/media/games/elden-ring.jpg",
                    List.of("Action", "RPG"),
                    List.of("PC", "PlayStation 5"),
                    List.of("Singleplayer", "Open World"),
                    List.of("FromSoftware"),
                    List.of("Bandai Namco Entertainment")));
        }
    }
}
