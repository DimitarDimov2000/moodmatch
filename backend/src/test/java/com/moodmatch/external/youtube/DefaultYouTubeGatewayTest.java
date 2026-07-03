package com.moodmatch.external.youtube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodmatch.exception.BusinessRuleViolationException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultYouTubeGatewayTest {

    private final AtomicReference<URI> lastRequestUri = new AtomicReference<>();
    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private final AtomicReference<String> responseBody = new AtomicReference<>("{\"items\":[]}");

    private HttpServer server;
    private DefaultYouTubeGateway gateway;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/search", this::respond);
        server.createContext("/videos", this::respond);
        server.createContext("/videoCategories", this::respond);
        server.start();

        gateway = new DefaultYouTubeGateway();
        gateway.objectMapper = new ObjectMapper();
        gateway.baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldRequestVideosListWithOnlySnippetIdAndKeyParameters() {
        responseBody.set("""
                {
                  "items": [
                    {
                      "id": "abc123XYZ_0",
                      "snippet": {
                        "title": "VueConf 2024 Keynote",
                        "description": "A practical keynote.",
                        "channelTitle": "MoodMatch Dev",
                        "publishedAt": "2024-05-20T10:30:00Z",
                        "categoryId": "27",
                        "tags": ["Vue 3"],
                        "thumbnails": {
                          "high": {
                            "url": "https://img.youtube.test/high.jpg"
                          }
                        }
                      }
                    }
                  ]
                }
                """);

        var video = gateway.fetchVideo("test-key", "abc123XYZ_0");

        assertTrue(video.isPresent());
        assertEquals("abc123XYZ_0", video.get().id());

        Map<String, List<String>> queryParameters = parseQuery(lastRequestUri.get().getRawQuery());
        assertEquals("/videos", lastRequestUri.get().getPath());
        assertEquals(java.util.Set.of("part", "id", "key"), queryParameters.keySet());
        assertEquals(List.of("snippet"), queryParameters.get("part"));
        assertEquals(List.of("abc123XYZ_0"), queryParameters.get("id"));
        assertEquals(List.of("test-key"), queryParameters.get("key"));
        assertFalse(queryParameters.containsKey("q"));
        assertFalse(queryParameters.containsKey("order"));
        assertFalse(queryParameters.containsKey("type"));
        assertFalse(queryParameters.containsKey("maxResults"));
        assertFalse(queryParameters.containsKey("publishedAfter"));
    }

    @Test
    void shouldKeepYoutubeSearchParametersOnSearchRequestsOnly() {
        responseBody.set("""
                {
                  "items": [
                    {
                      "id": {
                        "videoId": "abc123XYZ_0"
                      },
                      "snippet": {
                        "title": "VueConf 2024 Keynote",
                        "description": "A practical keynote.",
                        "channelTitle": "MoodMatch Dev",
                        "publishedAt": "2024-05-20T10:30:00Z",
                        "thumbnails": {
                          "high": {
                            "url": "https://img.youtube.test/high.jpg"
                          }
                        }
                      }
                    }
                  ]
                }
                """);

        var videos = gateway.searchVideos("test-key", "ai tutorial", 5, "viewCount");

        assertEquals(1, videos.size());

        Map<String, List<String>> queryParameters = parseQuery(lastRequestUri.get().getRawQuery());
        assertEquals("/search", lastRequestUri.get().getPath());
        assertEquals(List.of("snippet"), queryParameters.get("part"));
        assertEquals(List.of("ai tutorial"), queryParameters.get("q"));
        assertEquals(List.of("video"), queryParameters.get("type"));
        assertEquals(List.of("5"), queryParameters.get("maxResults"));
        assertEquals(List.of("viewCount"), queryParameters.get("order"));
        assertEquals(List.of("test-key"), queryParameters.get("key"));
        assertFalse(queryParameters.containsKey("id"));
    }

    @Test
    void shouldMapVideoLookup400IntoSafeUrlImportMessage() {
        responseStatus.set(400);
        responseBody.set("{\"error\":{\"message\":\"Bad Request\"}}");

        BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                () -> gateway.fetchVideo("test-key", "abc123XYZ_0"));

        assertEquals("/videos", lastRequestUri.get().getPath());
        assertEquals(
                "YouTube URL import request was rejected. Check that the URL contains a valid public video ID.",
                exception.getMessage());
    }

    private void respond(HttpExchange exchange) throws IOException {
        lastRequestUri.set(exchange.getRequestURI());
        byte[] responseBytes = responseBody.get().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(responseStatus.get(), responseBytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }

    private Map<String, List<String>> parseQuery(String rawQuery) {
        Map<String, List<String>> queryParameters = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return queryParameters;
        }

        Arrays.stream(rawQuery.split("&"))
                .forEach(pair -> {
                    int separatorIndex = pair.indexOf('=');
                    String rawKey = separatorIndex >= 0 ? pair.substring(0, separatorIndex) : pair;
                    String rawValue = separatorIndex >= 0 ? pair.substring(separatorIndex + 1) : "";
                    String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
                    queryParameters.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(value);
                });

        return queryParameters;
    }
}
