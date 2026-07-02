package com.moodmatch.external.rawg;

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
public class DefaultRawgGateway implements RawgGateway {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "moodmatch.external.rawg.base-url", defaultValue = "https://api.rawg.io/api")
    String baseUrl;

    @Override
    public List<RawgGame> searchGames(String apiKey, String query, int limit) {
        JsonNode root = readJson("/games", Map.of(
                "key", apiKey,
                "search", query,
                "page_size", String.valueOf(limit),
                "page", "1"));
        JsonNode results = root.path("results");
        if (!results.isArray()) {
            return List.of();
        }

        return java.util.stream.StreamSupport.stream(results.spliterator(), false)
                .map(node -> toSearchGame(apiKey, node))
                .toList();
    }

    private RawgGame toSearchGame(String apiKey, JsonNode node) {
        RawgGame searchGame = toGame(node);
        String detailIdentifier = searchGame.id() > 0 ? String.valueOf(searchGame.id()) : searchGame.slug();
        if (detailIdentifier == null || detailIdentifier.isBlank()) {
            return searchGame;
        }

        JsonNode detailNode = readJson("/games/" + detailIdentifier, Map.of("key", apiKey));
        RawgGame detailGame = toGame(detailNode);
        return new RawgGame(
                searchGame.id(),
                firstNonBlank(detailGame.slug(), searchGame.slug()),
                firstNonBlank(detailGame.name(), searchGame.name()),
                firstNonBlank(detailGame.descriptionRaw(), searchGame.descriptionRaw()),
                firstNonBlank(detailGame.released(), searchGame.released()),
                firstNonBlank(detailGame.backgroundImage(), searchGame.backgroundImage()),
                firstNonEmpty(detailGame.genres(), searchGame.genres()),
                firstNonEmpty(detailGame.platforms(), searchGame.platforms()),
                firstNonEmpty(detailGame.tags(), searchGame.tags()),
                firstNonEmpty(detailGame.developers(), searchGame.developers()),
                firstNonEmpty(detailGame.publishers(), searchGame.publishers()));
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
            throw new BusinessRuleViolationException("RAWG request failed. Verify network access and API key.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessRuleViolationException("RAWG request was interrupted.");
        }

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new BusinessRuleViolationException("RAWG request was rejected. Check MOODMATCH_RAWG_API_KEY.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessRuleViolationException(
                    "RAWG request failed with status %s.".formatted(response.statusCode()));
        }

        try {
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new BusinessRuleViolationException("RAWG response could not be parsed.");
        }
    }

    private URI buildUri(String path, Map<String, String> query) {
        StringBuilder builder = new StringBuilder(baseUrl).append(path).append("?");
        boolean first = true;
        for (Map.Entry<String, String> entry : new LinkedHashMap<>(query).entrySet()) {
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

    private RawgGame toGame(JsonNode node) {
        return new RawgGame(
                integerValue(node, "id"),
                textValue(node, "slug"),
                textValue(node, "name"),
                textValue(node, "description_raw"),
                textValue(node, "released"),
                textValue(node, "background_image"),
                namesFromArray(node.path("genres")),
                platformNames(node.path("platforms")),
                namesFromArray(node.path("tags")),
                namesFromArray(node.path("developers")),
                namesFromArray(node.path("publishers")));
    }

    private List<String> platformNames(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                .map(entry -> textValue(entry.path("platform"), "name"))
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private List<String> namesFromArray(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                .map(entry -> textValue(entry, "name"))
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private int integerValue(JsonNode node, String field) {
        return node.hasNonNull(field) && node.path(field).canConvertToInt() ? node.path(field).asInt() : 0;
    }

    private String textValue(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asText() : null;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private List<String> firstNonEmpty(List<String> first, List<String> second) {
        return first == null || first.isEmpty() ? second : first;
    }
}
