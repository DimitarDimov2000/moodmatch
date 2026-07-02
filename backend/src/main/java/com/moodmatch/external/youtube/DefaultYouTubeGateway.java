package com.moodmatch.external.youtube;

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
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodmatch.exception.BusinessRuleViolationException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultYouTubeGateway implements YouTubeGateway {

    private static final String DEFAULT_ORDER = "relevance";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "moodmatch.external.youtube.base-url", defaultValue = "https://www.googleapis.com/youtube/v3")
    String baseUrl;

    @Override
    public List<YouTubeVideo> searchVideos(String apiKey, String query, int maxResults, String order) {
        JsonNode root = readJson("/search", Map.of(
                "part", "snippet",
                "q", query,
                "type", "video",
                "maxResults", String.valueOf(maxResults),
                "order", order == null || order.isBlank() ? DEFAULT_ORDER : order,
                "key", apiKey));

        JsonNode items = root.path("items");
        if (!items.isArray() || items.size() == 0) {
            return List.of();
        }

        return java.util.stream.StreamSupport.stream(items.spliterator(), false)
                .map(this::toSearchVideo)
                .toList();
    }

    @Override
    public Optional<YouTubeVideo> fetchVideo(String apiKey, String videoId) {
        JsonNode root = readJson("/videos", Map.of(
                "part", "snippet",
                "id", videoId,
                "key", apiKey));
        JsonNode items = root.path("items");
        if (!items.isArray() || items.size() == 0) {
            return Optional.empty();
        }

        return Optional.of(toVideo(items.get(0)));
    }

    @Override
    public Optional<String> fetchCategoryLabel(String apiKey, String categoryId) {
        JsonNode root = readJson("/videoCategories", Map.of(
                "part", "snippet",
                "id", categoryId,
                "regionCode", "US",
                "key", apiKey));
        JsonNode items = root.path("items");
        if (!items.isArray() || items.size() == 0) {
            return Optional.empty();
        }

        String categoryLabel = textValue(items.get(0).path("snippet"), "title");
        return categoryLabel == null || categoryLabel.isBlank() ? Optional.empty() : Optional.of(categoryLabel.trim());
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
            throw new BusinessRuleViolationException("YouTube request failed. Verify network access and API key.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessRuleViolationException("YouTube request was interrupted.");
        }

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new BusinessRuleViolationException(
                    "YouTube request was rejected. Check MOODMATCH_YOUTUBE_API_KEY in the backend environment.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessRuleViolationException(
                    "YouTube request failed with status %s.".formatted(response.statusCode()));
        }

        try {
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new BusinessRuleViolationException("YouTube response could not be parsed.");
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

    private YouTubeVideo toSearchVideo(JsonNode node) {
        JsonNode snippetNode = node.path("snippet");
        JsonNode idNode = node.path("id");
        return new YouTubeVideo(
                textValue(idNode, "videoId"),
                textValue(snippetNode, "title"),
                textValue(snippetNode, "description"),
                textValue(snippetNode, "channelTitle"),
                textValue(snippetNode, "publishedAt"),
                null,
                List.of(),
                thumbnails(snippetNode.path("thumbnails")));
    }

    private YouTubeVideo toVideo(JsonNode node) {
        JsonNode snippetNode = node.path("snippet");
        return new YouTubeVideo(
                textValue(node, "id"),
                textValue(snippetNode, "title"),
                textValue(snippetNode, "description"),
                textValue(snippetNode, "channelTitle"),
                textValue(snippetNode, "publishedAt"),
                textValue(snippetNode, "categoryId"),
                textList(snippetNode.path("tags")),
                thumbnails(snippetNode.path("thumbnails")));
    }

    private ThumbnailSet thumbnails(JsonNode node) {
        return new ThumbnailSet(
                textValue(node.path("default"), "url"),
                textValue(node.path("medium"), "url"),
                textValue(node.path("high"), "url"),
                textValue(node.path("standard"), "url"),
                textValue(node.path("maxres"), "url"));
    }

    private List<String> textList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                .filter(JsonNode::isValueNode)
                .map(JsonNode::asText)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String textValue(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asText() : null;
    }
}
