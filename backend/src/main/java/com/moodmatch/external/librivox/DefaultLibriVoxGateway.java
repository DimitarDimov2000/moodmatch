package com.moodmatch.external.librivox;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodmatch.exception.BusinessRuleViolationException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultLibriVoxGateway implements LibriVoxGateway {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "moodmatch.external.librivox.base-url", defaultValue = "https://librivox.org")
    String baseUrl;

    @ConfigProperty(
            name = "moodmatch.external.librivox.user-agent",
            defaultValue = "MoodMatch/0.1 (contact: local-dev)")
    String userAgent;

    @Override
    public List<LibriVoxAudiobook> searchAudiobooks(String query, int limit) {
        JsonNode root = readJson(
                "/api/feed/audiobooks",
                Map.of(
                        "title", List.of(query),
                        "limit", List.of(String.valueOf(limit)),
                        "format", List.of("json"),
                        "extended", List.of("1"),
                        "coverart", List.of("1")));
        JsonNode books = root.path("books");
        if (!books.isArray()) {
            return List.of();
        }

        return java.util.stream.StreamSupport.stream(books.spliterator(), false)
                .map(this::toAudiobook)
                .filter(Objects::nonNull)
                .toList();
    }

    private JsonNode readJson(String path, Map<String, List<String>> query) {
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
                    "LibriVox request failed. Verify network access and try again.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessRuleViolationException("LibriVox request was interrupted.");
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessRuleViolationException(
                    "LibriVox request failed with status %s.".formatted(response.statusCode()));
        }

        try {
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new BusinessRuleViolationException("LibriVox response could not be parsed.");
        }
    }

    private URI buildUri(String path, Map<String, List<String>> query) {
        StringBuilder builder = new StringBuilder(baseUrl).append(path);
        if (!query.isEmpty()) {
            builder.append("?");
        }

        boolean first = true;
        for (Map.Entry<String, List<String>> entry : new LinkedHashMap<>(query).entrySet()) {
            for (String value : entry.getValue()) {
                if (!first) {
                    builder.append("&");
                }
                first = false;
                builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                builder.append("=");
                builder.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        return URI.create(builder.toString());
    }

    private LibriVoxAudiobook toAudiobook(JsonNode node) {
        String id = textValue(node, "id");
        String title = textValue(node, "title");
        if (id == null || id.isBlank() || title == null || title.isBlank()) {
            return null;
        }

        return new LibriVoxAudiobook(
                id,
                title,
                textValue(node, "description"),
                textValue(node, "language"),
                textValue(node, "copyright_year"),
                textValue(node, "url_librivox"),
                textValue(node, "coverart_jpg"),
                textValue(node, "coverart_thumbnail"),
                authors(node.path("authors")),
                genres(node.path("genres")),
                sections(node.path("sections")));
    }

    private List<LibriVoxAuthor> authors(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                .map(author -> new LibriVoxAuthor(
                        textValue(author, "id"),
                        textValue(author, "first_name"),
                        textValue(author, "last_name")))
                .toList();
    }

    private List<LibriVoxGenre> genres(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                .map(genre -> new LibriVoxGenre(textValue(genre, "id"), textValue(genre, "name")))
                .toList();
    }

    private List<LibriVoxSection> sections(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                .map(section -> new LibriVoxSection(textValue(section, "id"), readers(section.path("readers"))))
                .toList();
    }

    private List<LibriVoxReader> readers(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        List<LibriVoxReader> readers = new ArrayList<>();
        for (JsonNode reader : node) {
            readers.add(new LibriVoxReader(textValue(reader, "reader_id"), textValue(reader, "display_name")));
        }
        return List.copyOf(readers);
    }

    private String textValue(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asText() : null;
    }
}
