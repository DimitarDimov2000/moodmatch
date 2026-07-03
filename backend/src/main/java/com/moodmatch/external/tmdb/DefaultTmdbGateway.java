package com.moodmatch.external.tmdb;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodmatch.exception.BusinessRuleViolationException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultTmdbGateway implements TmdbGateway {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "moodmatch.external.tmdb.base-url", defaultValue = "https://api.themoviedb.org/3")
    String baseUrl;

    @Override
    public List<TmdbSearchItem> searchMovies(String apiKey, String query) {
        JsonNode root = readJson("/search/movie", Map.of(
                "api_key", apiKey,
                "query", query,
                "include_adult", "false",
                "language", "en-US",
                "page", "1"));
        return toSearchItems(root.path("results"), "title", "original_title", "release_date");
    }

    @Override
    public List<TmdbSearchItem> searchSeries(String apiKey, String query) {
        JsonNode root = readJson("/search/tv", Map.of(
                "api_key", apiKey,
                "query", query,
                "include_adult", "false",
                "language", "en-US",
                "page", "1"));
        return toSearchItems(root.path("results"), "name", "original_name", "first_air_date");
    }

    @Override
    public Map<Integer, String> fetchMovieGenres(String apiKey) {
        JsonNode root = readJson("/genre/movie/list", Map.of(
                "api_key", apiKey,
                "language", "en-US"));
        return toGenreMap(root.path("genres"));
    }

    @Override
    public Map<Integer, String> fetchSeriesGenres(String apiKey) {
        JsonNode root = readJson("/genre/tv/list", Map.of(
                "api_key", apiKey,
                "language", "en-US"));
        return toGenreMap(root.path("genres"));
    }

    private JsonNode readJson(String path, Map<String, String> query) {
        HttpRequest request = HttpRequest.newBuilder(buildUri(path, query))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new BusinessRuleViolationException("TMDB request failed. Verify network access and API key.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessRuleViolationException("TMDB request was interrupted.");
        }

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new BusinessRuleViolationException(
                    "TMDB request was rejected. Check MOODMATCH_TMDB_API_KEY and confirm it is a TMDB v3 API key.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessRuleViolationException(
                    "TMDB request failed with status %s.".formatted(response.statusCode()));
        }

        try {
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new BusinessRuleViolationException("TMDB response could not be parsed.");
        }
    }

    private URI buildUri(String path, Map<String, String> query) {
        StringBuilder builder = new StringBuilder(baseUrl)
                .append(path)
                .append("?");

        boolean first = true;
        for (Map.Entry<String, String> entry : query.entrySet()) {
            if (!first) {
                builder.append("&");
            }
            first = false;
            builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        return URI.create(builder.toString());
    }

    private List<TmdbSearchItem> toSearchItems(
            JsonNode resultsNode, String titleField, String originalTitleField, String releaseDateField) {
        if (!resultsNode.isArray()) {
            return List.of();
        }

        return java.util.stream.StreamSupport.stream(resultsNode.spliterator(), false)
                .map(node -> new TmdbSearchItem(
                        node.path("id").asInt(),
                        textValue(node, titleField),
                        textValue(node, originalTitleField),
                        textValue(node, "overview"),
                        textValue(node, releaseDateField),
                        textValue(node, "poster_path"),
                        readGenreIds(node.path("genre_ids"))))
                .toList();
    }

    private Map<Integer, String> toGenreMap(JsonNode genresNode) {
        if (!genresNode.isArray()) {
            return Map.of();
        }

        Map<Integer, String> genres = new LinkedHashMap<>();
        for (JsonNode node : genresNode) {
            if (!node.hasNonNull("id") || !node.hasNonNull("name")) {
                continue;
            }
            genres.put(node.path("id").asInt(), node.path("name").asText());
        }
        return genres;
    }

    private List<Integer> readGenreIds(JsonNode genreIdsNode) {
        if (!genreIdsNode.isArray()) {
            return List.of();
        }

        return java.util.stream.StreamSupport.stream(genreIdsNode.spliterator(), false)
                .filter(JsonNode::canConvertToInt)
                .map(JsonNode::asInt)
                .toList();
    }

    private String textValue(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asText() : null;
    }
}
