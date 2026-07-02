package com.moodmatch.external.podcastindex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class DefaultPodcastIndexGatewayTest {

    @Test
    void shouldMapFixturePayloadIntoPodcastFeedRecords() {
        DefaultPodcastIndexGateway gateway = new DefaultPodcastIndexGateway();
        gateway.objectMapper = new ObjectMapper();

        List<PodcastIndexGateway.PodcastFeed> results = gateway.parseSearchResponse(readFixture("podcastindex/search-response.json"));

        assertEquals(2, results.size());
        PodcastIndexGateway.PodcastFeed first = results.getFirst();
        assertEquals(75075L, first.id());
        assertEquals("Lex Fridman Podcast", first.title());
        assertEquals("https://lexfridman.com/feed/podcast/", first.url());
        assertEquals("https://lexfridman.com/podcast/", first.link());
        assertEquals("Lex Fridman", first.author());
        assertEquals("Lex Fridman", first.ownerName());
        assertEquals("https://image.simplecastcdn.com/images/lex-fridman.jpg", first.artwork());
        assertEquals(1_719_838_400L, first.newestItemPubdate());
        assertEquals("en", first.language());
        assertEquals(0, first.explicit());
        assertEquals("podcast", first.medium());
        assertEquals(List.of("Technology", "Science"), first.categories());
    }

    @Test
    void shouldBuildSignedSearchRequestsWithPodcastIndexHeaders() {
        DefaultPodcastIndexGateway gateway = new DefaultPodcastIndexGateway();
        gateway.baseUrl = "https://api.podcastindex.org/api/1.0";

        java.net.http.HttpRequest request =
                gateway.buildSearchRequest("test-key", "test-secret", "radiolab", 5, 1_700_000_000L);

        assertEquals(
                "https://api.podcastindex.org/api/1.0/search/byterm?q=radiolab&max=5",
                request.uri().toString());
        assertEquals("test-key", request.headers().firstValue("X-Auth-Key").orElseThrow());
        assertEquals("1700000000", request.headers().firstValue("X-Auth-Date").orElseThrow());
        assertEquals(
                "2782ad65bd878a76107dd3f1cdbfabe647607c5d",
                request.headers().firstValue("Authorization").orElseThrow());
        assertEquals("MoodMatch/1.0", request.headers().firstValue("User-Agent").orElseThrow());
    }

    @Test
    void shouldReturnEmptyListWhenPayloadContainsNoFeedsArray() {
        DefaultPodcastIndexGateway gateway = new DefaultPodcastIndexGateway();
        gateway.objectMapper = new ObjectMapper();

        assertEquals(List.of(), gateway.parseSearchResponse("{\"status\":\"true\"}"));
    }

    private String readFixture(String resourcePath) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Fixture not found: " + resourcePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read fixture: " + resourcePath, exception);
        }
    }
}
