package com.moodmatch.external.tmdb;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.moodmatch.entity.MediaType;
import com.moodmatch.external.adapter.ExternalSearchProvider;
import com.moodmatch.external.adapter.ExternalSearchRequest;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;
import com.moodmatch.external.adapter.ExternalSourceMappings;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TmdbExternalSearchProvider implements ExternalSearchProvider {

    private static final String ATTRIBUTION = "Metadata from TMDB";
    private static final Set<MediaType> SUPPORTED_MEDIA_TYPES = Set.of(MediaType.FILM, MediaType.SERIES);
    static final String MISSING_CONFIGURATION_MESSAGE =
            "TMDB provider is not configured. Set MOODMATCH_TMDB_API_KEY in the backend environment to a TMDB v3 API key.";
    private static final String UNSET_CONFIG_SENTINEL = "__missing_tmdb_config__";

    @Inject
    TmdbGateway tmdbGateway;

    @ConfigProperty(name = "moodmatch.external.tmdb.api-key")
    String apiKey;

    @ConfigProperty(name = "moodmatch.external.tmdb.image-base-url", defaultValue = "https://image.tmdb.org/t/p/w342")
    String imageBaseUrl;

    @ConfigProperty(name = "moodmatch.external.tmdb.website-base-url", defaultValue = "https://www.themoviedb.org")
    String websiteBaseUrl;

    @Override
    public ExternalSearchSourceName sourceName() {
        return ExternalSearchSourceName.TMDB;
    }

    @Override
    public Set<MediaType> supportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }

    @Override
    public boolean isConfigured() {
        return configuredApiKey() != null;
    }

    @Override
    public String configurationErrorMessage() {
        return MISSING_CONFIGURATION_MESSAGE;
    }

    @Override
    public List<ExternalSearchResult> search(ExternalSearchRequest request) {
        String resolvedApiKey = configuredApiKey();
        if (resolvedApiKey == null) {
            throw new IllegalStateException(configurationErrorMessage());
        }
        Map<Integer, String> genres = request.mediaType() == MediaType.FILM
                ? tmdbGateway.fetchMovieGenres(resolvedApiKey)
                : tmdbGateway.fetchSeriesGenres(resolvedApiKey);
        List<TmdbGateway.TmdbSearchItem> items = request.mediaType() == MediaType.FILM
                ? tmdbGateway.searchMovies(resolvedApiKey, request.query())
                : tmdbGateway.searchSeries(resolvedApiKey, request.query());

        return items.stream()
                .filter(item -> item.id() > 0 && item.title() != null && !item.title().isBlank())
                .limit(request.limit())
                .map(item -> toResult(request.mediaType(), item, genres))
                .toList();
    }

    private ExternalSearchResult toResult(
            MediaType mediaType, TmdbGateway.TmdbSearchItem item, Map<Integer, String> genresById) {
        List<String> externalGenres = item.genreIds().stream()
                .map(genresById::get)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return new ExternalSearchResult(
                ExternalSearchSourceName.TMDB,
                ExternalSourceMappings.toMappingSource(ExternalSearchSourceName.TMDB, mediaType),
                String.valueOf(item.id()),
                mediaType,
                item.title(),
                item.originalTitle(),
                List.of(),
                blankToNull(item.overview()),
                parseReleaseYear(item.releaseDate()),
                buildCoverUrl(item.posterPath()),
                buildSourceUrl(mediaType, item.id()),
                externalGenres,
                List.of(),
                List.of(),
                ATTRIBUTION,
                List.of());
    }

    private Integer parseReleaseYear(String releaseDate) {
        if (releaseDate == null || releaseDate.isBlank() || releaseDate.length() < 4) {
            return null;
        }

        try {
            return Integer.valueOf(releaseDate.substring(0, 4));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String buildCoverUrl(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) {
            return null;
        }
        return imageBaseUrl + posterPath;
    }

    private String buildSourceUrl(MediaType mediaType, int id) {
        String path = mediaType == MediaType.FILM ? "movie" : "tv";
        return websiteBaseUrl + "/" + path + "/" + id;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String configuredApiKey() {
        String normalizedApiKey = blankToNull(apiKey);
        return UNSET_CONFIG_SENTINEL.equals(normalizedApiKey) ? null : normalizedApiKey;
    }
}
