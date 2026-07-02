package com.moodmatch.external.anilist;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodmatch.entity.MediaType;
import com.moodmatch.exception.BusinessRuleViolationException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultAniListGateway implements AniListGateway {

    private static final String SEARCH_QUERY = """
            query MoodMatchAniListSearch($search: String!, $type: MediaType!, $perPage: Int!) {
              Page(page: 1, perPage: $perPage) {
                media(search: $search, type: $type) {
                  id
                  type
                  format
                  status
                  season
                  seasonYear
                  startDate {
                    year
                  }
                  title {
                    romaji
                    english
                    native
                  }
                  description
                  coverImage {
                    large
                    medium
                  }
                  siteUrl
                  genres
                  tags {
                    name
                    rank
                    isMediaSpoiler
                    isGeneralSpoiler
                  }
                  studios(isMain: true) {
                    nodes {
                      name
                    }
                  }
                  staff(perPage: 5, sort: RELEVANCE) {
                    nodes {
                      name {
                        full
                      }
                    }
                  }
                }
              }
            }
            """;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "moodmatch.external.anilist.base-url", defaultValue = "https://graphql.anilist.co")
    String baseUrl;

    @Override
    public List<AniListMedia> searchMedia(MediaType mediaType, String query, int limit) {
        return toMediaList(readJson(mediaType, query, limit));
    }

    List<AniListMedia> parseSearchResponse(String responseBody) {
        try {
            return toMediaList(objectMapper.readTree(responseBody));
        } catch (IOException exception) {
            throw new BusinessRuleViolationException("AniList response could not be parsed.");
        }
    }

    private List<AniListMedia> toMediaList(JsonNode root) {
        JsonNode results = root.path("data").path("Page").path("media");
        if (!results.isArray()) {
            return List.of();
        }

        return java.util.stream.StreamSupport.stream(results.spliterator(), false)
                .map(this::toMedia)
                .toList();
    }

    private JsonNode readJson(MediaType mediaType, String query, int limit) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(mediaType, query, limit)))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new BusinessRuleViolationException("AniList request failed. Verify network access and try again.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessRuleViolationException("AniList request was interrupted.");
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessRuleViolationException(
                    "AniList request failed with status %s.".formatted(response.statusCode()));
        }

        try {
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new BusinessRuleViolationException("AniList response could not be parsed.");
        }
    }

    private String requestBody(MediaType mediaType, String search, int limit) {
        Map<String, Object> payload = Map.of(
                "query", SEARCH_QUERY,
                "variables", Map.of(
                        "search", search,
                        "type", mediaType == MediaType.BOOK ? "MANGA" : "ANIME",
                        "perPage", limit));
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AniList GraphQL request could not be serialized.", exception);
        }
    }

    private AniListMedia toMedia(JsonNode node) {
        return new AniListMedia(
                intValue(node, "id"),
                textValue(node, "type"),
                textValue(node, "format"),
                textValue(node, "status"),
                textValue(node, "season"),
                integerValue(node, "seasonYear"),
                integerValue(node.path("startDate"), "year"),
                new AniListTitle(
                        textValue(node.path("title"), "romaji"),
                        textValue(node.path("title"), "english"),
                        textValue(node.path("title"), "native")),
                textValue(node, "description"),
                new AniListCoverImage(
                        textValue(node.path("coverImage"), "large"),
                        textValue(node.path("coverImage"), "medium")),
                textValue(node, "siteUrl"),
                textList(node.path("genres")),
                tagNames(node.path("tags")),
                namesFromArray(node.path("studios").path("nodes")),
                staffNames(node.path("staff").path("nodes")));
    }

    private List<String> tagNames(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                .filter(tag -> !tag.path("isMediaSpoiler").asBoolean(false))
                .filter(tag -> !tag.path("isGeneralSpoiler").asBoolean(false))
                .sorted((first, second) -> Integer.compare(second.path("rank").asInt(0), first.path("rank").asInt(0)))
                .map(tag -> textValue(tag, "name"))
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

    private List<String> staffNames(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                .map(entry -> textValue(entry.path("name"), "full"))
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
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

    private Integer integerValue(JsonNode node, String field) {
        return node.hasNonNull(field) && node.path(field).canConvertToInt() ? node.path(field).asInt() : null;
    }

    private int intValue(JsonNode node, String field) {
        return node.hasNonNull(field) && node.path(field).canConvertToInt() ? node.path(field).asInt() : 0;
    }

    private String textValue(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asText() : null;
    }
}
