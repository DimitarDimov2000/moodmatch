package com.moodmatch.external.librivox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.moodmatch.entity.MediaType;
import com.moodmatch.external.adapter.ExternalSearchRequest;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;
import com.moodmatch.external.librivox.LibriVoxGateway.LibriVoxAudiobook;
import com.moodmatch.external.librivox.LibriVoxGateway.LibriVoxAuthor;
import com.moodmatch.external.librivox.LibriVoxGateway.LibriVoxGenre;
import com.moodmatch.external.librivox.LibriVoxGateway.LibriVoxReader;
import com.moodmatch.external.librivox.LibriVoxGateway.LibriVoxSection;

class LibriVoxExternalSearchProviderTest {

    @Test
    void shouldMapAudiobookSearchResultsIntoNormalizedExternalResults() {
        LibriVoxExternalSearchProvider provider = new LibriVoxExternalSearchProvider();
        provider.libriVoxGateway = new FakeLibriVoxGateway();

        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("pride", MediaType.AUDIOBOOK, ExternalSearchSourceName.LIBRIVOX, 5));

        assertEquals(1, results.size());
        ExternalSearchResult result = results.getFirst();
        assertEquals(ExternalSearchSourceName.LIBRIVOX, result.source());
        assertEquals("253", result.externalId());
        assertEquals(MediaType.AUDIOBOOK, result.mediaType());
        assertEquals("Pride and Prejudice", result.title());
        assertNull(result.originalTitle());
        assertEquals(
                List.of(
                        "Author: Jane Austen",
                        "Readers: Annie Coleman Rothenberg, Chris Goringe, Kara Shallenberg (1969-2023), +1 more"),
                result.creatorNames());
        assertEquals(
                "Pride and Prejudice is the most famous of Jane Austen's novels.\n\n(Summary from Wikipedia)",
                result.description());
        assertEquals(1813, result.releaseYear());
        assertEquals("https://archive.org/covers/pride.jpg", result.coverUrl());
        assertEquals("https://librivox.org/pride-and-prejudice-by-jane-austen/", result.sourceUrl());
        assertEquals(List.of("Romance"), result.externalGenres());
        assertEquals(List.of("English"), result.externalSubjects());
        assertEquals("LibriVox public domain audiobook catalog", result.attribution());
    }

    @Test
    void shouldReturnEmptyResultsWhenTheGatewayHasNoMatches() {
        LibriVoxExternalSearchProvider provider = new LibriVoxExternalSearchProvider();
        provider.libriVoxGateway = (query, limit) -> List.of();

        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("missing", MediaType.AUDIOBOOK, ExternalSearchSourceName.LIBRIVOX, 5));

        assertTrue(results.isEmpty());
    }

    private static final class FakeLibriVoxGateway implements LibriVoxGateway {

        @Override
        public List<LibriVoxAudiobook> searchAudiobooks(String query, int limit) {
            return List.of(new LibriVoxAudiobook(
                    "253",
                    "Pride and Prejudice",
                    "<em>Pride and Prejudice</em> is the most famous of Jane Austen&#39;s novels.<br /><br />(Summary from Wikipedia)",
                    "English",
                    "1813",
                    "https://librivox.org/pride-and-prejudice-by-jane-austen/",
                    "https://archive.org/covers/pride.jpg",
                    "https://archive.org/covers/pride-thumb.jpg",
                    List.of(new LibriVoxAuthor("155", "Jane", "Austen")),
                    List.of(new LibriVoxGenre("27", "Romance")),
                    List.of(
                            new LibriVoxSection("1", List.of(new LibriVoxReader("30", "Annie Coleman Rothenberg"))),
                            new LibriVoxSection("2", List.of(new LibriVoxReader("168", "Chris Goringe"))),
                            new LibriVoxSection("3", List.of(new LibriVoxReader("19", "Kara Shallenberg (1969-2023)"))),
                            new LibriVoxSection("4", List.of(new LibriVoxReader("89", "Kristen McQuillin"))))));
        }
    }
}
