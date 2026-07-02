package com.moodmatch.external.podcastindex;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
public class DefaultPodcastIndexGateway implements PodcastIndexGateway {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "moodmatch.external.podcast-index.base-url", defaultValue = "https://api.podcastindex.org/api/1.0")
    String baseUrl;

    @Override
    public List<PodcastFeed> searchShows(String apiKey, String apiSecret, String query, int limit) {
        HttpRequest request = buildSearchRequest(apiKey, apiSecret, query, limit);

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new BusinessRuleViolationException(
                    "Podcast Index request failed. Verify network access and provider credentials.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessRuleViolationException("Podcast Index request was interrupted.");
        }

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new BusinessRuleViolationException(
                    "Podcast Index request was rejected. Check MOODMATCH_PODCASTINDEX_KEY and MOODMATCH_PODCASTINDEX_SECRET.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessRuleViolationException(
                    "Podcast Index request failed with status %s.".formatted(response.statusCode()));
        }

        return parseSearchResponse(response.body());
    }

    HttpRequest buildSearchRequest(String apiKey, String apiSecret, String query, int limit) {
        return buildSearchRequest(apiKey, apiSecret, query, limit, Instant.now().getEpochSecond());
    }

    HttpRequest buildSearchRequest(String apiKey, String apiSecret, String query, int limit, long epochSeconds) {
        PodcastIndexAuthHeaders headers = PodcastIndexAuthHeaders.create(apiKey, apiSecret, epochSeconds);
        LinkedHashMap<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("q", query);
        queryParameters.put("max", String.valueOf(limit));
        return HttpRequest.newBuilder(buildUri("/search/byterm", queryParameters))
                .GET()
                .header("Accept", "application/json")
                .header("User-Agent", headers.userAgent())
                .header("X-Auth-Key", headers.apiKey())
                .header("X-Auth-Date", headers.authDate())
                .header("Authorization", headers.authorization())
                .build();
    }

    List<PodcastFeed> parseSearchResponse(String payload) {
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (IOException exception) {
            throw new BusinessRuleViolationException("Podcast Index response could not be parsed.");
        }

        JsonNode feeds = root.path("feeds");
        if (!feeds.isArray()) {
            return List.of();
        }

        return java.util.stream.StreamSupport.stream(feeds.spliterator(), false)
                .map(this::toPodcastFeed)
                .toList();
    }

    private PodcastFeed toPodcastFeed(JsonNode node) {
        return new PodcastFeed(
                longValue(node, "id"),
                textValue(node, "title"),
                textValue(node, "url"),
                textValue(node, "originalUrl"),
                textValue(node, "link"),
                textValue(node, "description"),
                textValue(node, "author"),
                textValue(node, "ownerName"),
                textValue(node, "image"),
                textValue(node, "artwork"),
                longObjectValue(node, "newestItemPubdate"),
                textValue(node, "language"),
                integerObjectValue(node, "explicit"),
                textValue(node, "medium"),
                categoryValues(node.path("categories")));
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

    private List<String> categoryValues(JsonNode categoriesNode) {
        if (categoriesNode.isObject()) {
            return java.util.stream.StreamSupport.stream(categoriesNode.spliterator(), false)
                    .map(JsonNode::asText)
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .distinct()
                    .toList();
        }
        if (categoriesNode.isArray()) {
            return java.util.stream.StreamSupport.stream(categoriesNode.spliterator(), false)
                    .map(node -> node.isTextual() ? node.asText() : textValue(node, "name"))
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .distinct()
                    .toList();
        }
        return List.of();
    }

    private long longValue(JsonNode node, String field) {
        return node.hasNonNull(field) && node.path(field).canConvertToLong() ? node.path(field).asLong() : 0L;
    }

    private Long longObjectValue(JsonNode node, String field) {
        return node.hasNonNull(field) && node.path(field).canConvertToLong() ? node.path(field).asLong() : null;
    }

    private Integer integerObjectValue(JsonNode node, String field) {
        return node.hasNonNull(field) && node.path(field).canConvertToInt() ? node.path(field).asInt() : null;
    }

    private String textValue(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asText() : null;
    }
}
