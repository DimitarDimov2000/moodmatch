package com.moodmatch.external.tmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.moodmatch.entity.MediaType;
import com.moodmatch.external.adapter.ExternalSearchRequest;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;

class TmdbExternalSearchProviderTest {

    @Test
    void shouldMapMovieSearchResultsIntoNormalizedExternalResults() {
        TmdbExternalSearchProvider provider = new TmdbExternalSearchProvider();
        provider.tmdbGateway = new FakeTmdbGateway();
        provider.apiKey = Optional.of("test-key");
        provider.imageBaseUrl = "https://image.tmdb.org/t/p/w342";
        provider.websiteBaseUrl = "https://www.themoviedb.org";

        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("arrival", MediaType.FILM, ExternalSearchSourceName.TMDB, 5));

        assertEquals(1, results.size());
        ExternalSearchResult result = results.getFirst();
        assertEquals(ExternalSearchSourceName.TMDB, result.source());
        assertEquals("11", result.externalId());
        assertEquals("Arrival", result.title());
        assertEquals("Arrival Original", result.originalTitle());
        assertEquals(2016, result.releaseYear());
        assertEquals(List.of("Science Fiction", "Drama"), result.externalGenres());
        assertTrue(result.externalSubjects().isEmpty());
        assertEquals("https://image.tmdb.org/t/p/w342/poster.jpg", result.coverUrl());
        assertEquals("https://www.themoviedb.org/movie/11", result.sourceUrl());
    }

    @Test
    void shouldExposeMissingConfigurationStateClearly() {
        TmdbExternalSearchProvider provider = new TmdbExternalSearchProvider();
        provider.apiKey = Optional.empty();

        assertTrue(!provider.isConfigured());
        assertEquals(
                "TMDB provider is not configured. Set MOODMATCH_TMDB_API_KEY.",
                provider.configurationErrorMessage());
    }

    private static final class FakeTmdbGateway implements TmdbGateway {

        @Override
        public List<TmdbSearchItem> searchMovies(String apiKey, String query) {
            return List.of(new TmdbSearchItem(
                    11,
                    "Arrival",
                    "Arrival Original",
                    "First contact changes everything.",
                    "2016-11-11",
                    "/poster.jpg",
                    List.of(878, 18)));
        }

        @Override
        public List<TmdbSearchItem> searchSeries(String apiKey, String query) {
            return List.of();
        }

        @Override
        public Map<Integer, String> fetchMovieGenres(String apiKey) {
            return Map.of(878, "Science Fiction", 18, "Drama");
        }

        @Override
        public Map<Integer, String> fetchSeriesGenres(String apiKey) {
            return Map.of();
        }
    }
}
