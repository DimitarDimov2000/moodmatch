package com.moodmatch.external.openlibrary;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

@Mock
@ApplicationScoped
public class TestOpenLibraryGateway implements OpenLibraryGateway {

    private static final List<OpenLibrarySearchItem> DEFAULT_RESULTS = List.of(new OpenLibrarySearchItem(
            "/works/OL12345W",
            List.of("OL98765M"),
            "Dune",
            List.of("Frank Herbert"),
            1965,
            12345,
            List.of("Politics", "Desert planets"),
            null));

    private static final AtomicReference<List<OpenLibrarySearchItem>> RESULTS = new AtomicReference<>(DEFAULT_RESULTS);

    public static void reset() {
        RESULTS.set(DEFAULT_RESULTS);
    }

    public static void useResults(List<OpenLibrarySearchItem> results) {
        RESULTS.set(List.copyOf(results));
    }

    @Override
    public List<OpenLibrarySearchItem> searchBooks(String query, int limit) {
        return RESULTS.get().stream().limit(limit).toList();
    }
}
