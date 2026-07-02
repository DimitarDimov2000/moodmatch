package com.moodmatch.external.tmdb;

import java.util.List;
import java.util.Map;

public interface TmdbGateway {

    List<TmdbSearchItem> searchMovies(String apiKey, String query);

    List<TmdbSearchItem> searchSeries(String apiKey, String query);

    Map<Integer, String> fetchMovieGenres(String apiKey);

    Map<Integer, String> fetchSeriesGenres(String apiKey);

    record TmdbSearchItem(
            int id,
            String title,
            String originalTitle,
            String overview,
            String releaseDate,
            String posterPath,
            List<Integer> genreIds) {}
}
