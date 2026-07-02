package com.moodmatch.external.openlibrary;

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
public class DefaultOpenLibraryGateway implements OpenLibraryGateway {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "moodmatch.external.open-library.base-url", defaultValue = "https://openlibrary.org")
    String baseUrl;

    @ConfigProperty(
            name = "moodmatch.external.open-library.user-agent",
            defaultValue = "MoodMatch/0.1 (contact: local-dev)")
    String userAgent;

    @Override
    public List<OpenLibrarySearchItem> searchBooks(String query, int limit) {
        JsonNode root = readJson("/search.json", Map.of(
                "q", query,
                "limit", String.valueOf(limit),
                "fields", "key,title,author_name,first_publish_year,cover_i,subject,edition_key,first_sentence"));
        JsonNode docs = root.path("docs");
        if (!docs.isArray()) {
            return List.of();
        }

        return java.util.stream.StreamSupport.stream(docs.spliterator(), false)
                .map(this::toSearchItem)
                .toList();
    }

    private JsonNode readJson(String path, Map<String, String> query) {
        HttpRequest request = HttpRequest.newBuilder(buildUri(path, query))
                .GET()
                .header("Accept", "application/json")
                .header("User-Agent", userAgent)
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new BusinessRuleViolationException(
                    "Open Library request failed. Verify network access and try again.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessRuleViolationException("Open Library request was interrupted.");
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessRuleViolationException(
                    "Open Library request failed with status %s.".formatted(response.statusCode()));
        }

        try {
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new BusinessRuleViolationException("Open Library response could not be parsed.");
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

    private OpenLibrarySearchItem toSearchItem(JsonNode node) {
        return new OpenLibrarySearchItem(
                textValue(node, "key"),
                textList(node.path("edition_key")),
                textValue(node, "title"),
                textList(node.path("author_name")),
                integerValue(node, "first_publish_year"),
                integerValue(node, "cover_i"),
                textList(node.path("subject")),
                firstSentence(node.path("first_sentence")));
    }

    private List<String> textList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                .filter(JsonNode::isValueNode)
                .map(JsonNode::asText)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private String firstSentence(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                    .filter(JsonNode::isValueNode)
                    .map(JsonNode::asText)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private Integer integerValue(JsonNode node, String field) {
        return node.hasNonNull(field) && node.path(field).canConvertToInt() ? node.path(field).asInt() : null;
    }

    private String textValue(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asText() : null;
    }
}
