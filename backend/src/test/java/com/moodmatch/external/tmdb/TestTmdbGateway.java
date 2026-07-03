package com.moodmatch.external.tmdb;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

@Mock
@ApplicationScoped
public class TestTmdbGateway implements TmdbGateway {

    private static final TmdbSearchItem DEFAULT_MOVIE = new TmdbSearchItem(
            11,
            "Arrival",
            "Arrival Original",
            "First contact changes everything.",
            "2016-11-11",
            "/poster.jpg",
            List.of(878, 18));
    private static final TmdbSearchItem DEFAULT_SERIES = new TmdbSearchItem(
            1396,
            "Breaking Bad",
            "Breaking Bad",
            "A chemistry teacher turns to crime.",
            "2008-01-20",
            "/breaking-bad.jpg",
            List.of(18));

    private static final AtomicReference<List<TmdbSearchItem>> MOVIES = new AtomicReference<>(List.of(DEFAULT_MOVIE));
    private static final AtomicReference<List<TmdbSearchItem>> SERIES = new AtomicReference<>(List.of(DEFAULT_SERIES));
    private static final AtomicReference<Map<Integer, String>> MOVIE_GENRES =
            new AtomicReference<>(Map.of(878, "Science Fiction", 18, "Drama"));
    private static final AtomicReference<Map<Integer, String>> SERIES_GENRES =
            new AtomicReference<>(Map.of(18, "Drama"));

    public static void reset() {
        MOVIES.set(List.of(DEFAULT_MOVIE));
        SERIES.set(List.of(DEFAULT_SERIES));
        MOVIE_GENRES.set(Map.of(878, "Science Fiction", 18, "Drama"));
        SERIES_GENRES.set(Map.of(18, "Drama"));
    }

    public static void useMovieResults(List<TmdbSearchItem> results) {
        MOVIES.set(List.copyOf(results));
    }

    public static void useSeriesResults(List<TmdbSearchItem> results) {
        SERIES.set(List.copyOf(results));
    }

    @Override
    public List<TmdbSearchItem> searchMovies(String apiKey, String query) {
        return MOVIES.get();
    }

    @Override
    public List<TmdbSearchItem> searchSeries(String apiKey, String query) {
        return SERIES.get();
    }

    @Override
    public Map<Integer, String> fetchMovieGenres(String apiKey) {
        return MOVIE_GENRES.get();
    }

    @Override
    public Map<Integer, String> fetchSeriesGenres(String apiKey) {
        return SERIES_GENRES.get();
    }
}
