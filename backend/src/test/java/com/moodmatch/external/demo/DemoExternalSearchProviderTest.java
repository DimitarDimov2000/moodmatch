package com.moodmatch.external.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.moodmatch.entity.MediaType;
import com.moodmatch.external.adapter.ExternalSearchRequest;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;

class DemoExternalSearchProviderTest {

    private final DemoExternalSearchProvider provider = new DemoExternalSearchProvider();

    @Test
    void shouldMatchCaseInsensitivelyAndSortByScoreThenTitleThenExternalId() {
        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("A", MediaType.FILM, ExternalSearchSourceName.DEMO, 10));

        assertEquals(2, results.size());
        assertEquals("Arrival", results.getFirst().title());
        assertEquals("Severance Preview Reel", results.get(1).title());
    }

    @Test
    void shouldFilterByMediaTypeAndRespectTheRequestedLimit() {
        List<ExternalSearchResult> limitedResults = provider.search(
                new ExternalSearchRequest("a", MediaType.FILM, ExternalSearchSourceName.DEMO, 1));
        List<ExternalSearchResult> seriesResults = provider.search(
                new ExternalSearchRequest("dark", MediaType.SERIES, ExternalSearchSourceName.DEMO, 10));

        assertEquals(1, limitedResults.size());
        assertEquals("Arrival", limitedResults.getFirst().title());
        assertEquals(1, seriesResults.size());
        assertEquals(MediaType.SERIES, seriesResults.getFirst().mediaType());
    }

    @Test
    void shouldReturnEmptyResultsWhenNothingMatches() {
        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("nothing-here", MediaType.GAME, ExternalSearchSourceName.DEMO, 10));

        assertTrue(results.isEmpty());
    }
}
