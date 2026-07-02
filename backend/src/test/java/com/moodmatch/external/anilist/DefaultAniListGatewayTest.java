package com.moodmatch.external.anilist;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class DefaultAniListGatewayTest {

    @Test
    void shouldMapGraphQlFixturePayloadIntoGatewayRecords() {
        DefaultAniListGateway gateway = new DefaultAniListGateway();
        gateway.objectMapper = new ObjectMapper();

        List<AniListGateway.AniListMedia> results = gateway.parseSearchResponse("""
                {
                  "data": {
                    "Page": {
                      "media": [
                        {
                          "id": 16498,
                          "type": "ANIME",
                          "format": "TV",
                          "status": "FINISHED",
                          "season": "SPRING",
                          "seasonYear": 2013,
                          "startDate": { "year": 2013 },
                          "title": {
                            "romaji": "Shingeki no Kyojin",
                            "english": "Attack on Titan",
                            "native": "進撃の巨人"
                          },
                          "description": "<p>Humanity fights giants.</p>",
                          "coverImage": {
                            "large": "https://img.anilist.co/aot-large.jpg",
                            "medium": "https://img.anilist.co/aot-medium.jpg"
                          },
                          "siteUrl": "https://anilist.co/anime/16498",
                          "genres": ["Action", "Drama"],
                          "tags": [
                            { "name": "Survival", "rank": 95, "isMediaSpoiler": false, "isGeneralSpoiler": false },
                            { "name": "Spoiler Tag", "rank": 100, "isMediaSpoiler": true, "isGeneralSpoiler": false }
                          ],
                          "studios": {
                            "nodes": [{ "name": "Wit Studio" }]
                          },
                          "staff": {
                            "nodes": [{ "name": { "full": "Hajime Isayama" } }]
                          }
                        }
                      ]
                    }
                  }
                }
                """);

        assertEquals(1, results.size());
        AniListGateway.AniListMedia result = results.getFirst();
        assertEquals(16498, result.id());
        assertEquals("ANIME", result.type());
        assertEquals("TV", result.format());
        assertEquals("Shingeki no Kyojin", result.title().romaji());
        assertEquals("進撃の巨人", result.title().nativeTitle());
        assertEquals(2013, result.startYear());
        assertEquals("https://img.anilist.co/aot-large.jpg", result.coverImage().large());
        assertEquals(List.of("Action", "Drama"), result.genres());
        assertEquals(List.of("Survival"), result.tags());
        assertEquals(List.of("Wit Studio"), result.studios());
        assertEquals(List.of("Hajime Isayama"), result.staffNames());
    }

    @Test
    void shouldReturnEmptyListWhenGraphQlPayloadHasNoMediaArray() {
        DefaultAniListGateway gateway = new DefaultAniListGateway();
        gateway.objectMapper = new ObjectMapper();

        assertEquals(List.of(), gateway.parseSearchResponse("{\"data\":{\"Page\":{}}}"));
    }
}
