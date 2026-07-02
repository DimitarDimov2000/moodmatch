package com.moodmatch.external.openlibrary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.moodmatch.entity.MediaType;
import com.moodmatch.external.adapter.ExternalSearchRequest;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;

class OpenLibraryExternalSearchProviderTest {

    @Test
    void shouldMapBookSearchResultsIntoNormalizedExternalResults() {
        OpenLibraryExternalSearchProvider provider = new OpenLibraryExternalSearchProvider();
        provider.openLibraryGateway = new FakeOpenLibraryGateway();
        provider.coverBaseUrl = "https://covers.openlibrary.org";
        provider.websiteBaseUrl = "https://openlibrary.org";

        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("dune", MediaType.BOOK, ExternalSearchSourceName.OPEN_LIBRARY, 5));

        assertEquals(1, results.size());
        ExternalSearchResult result = results.getFirst();
        assertEquals(ExternalSearchSourceName.OPEN_LIBRARY, result.source());
        assertEquals("OL82563W", result.externalId());
        assertEquals(MediaType.BOOK, result.mediaType());
        assertEquals("Dune", result.title());
        assertNull(result.originalTitle());
        assertEquals(List.of("Frank Herbert"), result.creatorNames());
        assertEquals("Book by Frank Herbert. First published in 1965.", result.description());
        assertEquals(1965, result.releaseYear());
        assertEquals("https://covers.openlibrary.org/b/id/987654-M.jpg", result.coverUrl());
        assertEquals("https://openlibrary.org/works/OL82563W", result.sourceUrl());
        assertEquals(List.of(), result.externalGenres());
        assertEquals(List.of("Politics", "Desert planets"), result.externalSubjects());
        assertEquals("Metadata from Open Library", result.attribution());
    }

    private static final class FakeOpenLibraryGateway implements OpenLibraryGateway {

        @Override
        public List<OpenLibrarySearchItem> searchBooks(String query, int limit) {
            return List.of(new OpenLibrarySearchItem(
                    "/works/OL82563W",
                    List.of("OL123456M"),
                    "Dune",
                    List.of("Frank Herbert"),
                    1965,
                    987654,
                    List.of("Politics", "Desert planets"),
                    null));
        }
    }
}
