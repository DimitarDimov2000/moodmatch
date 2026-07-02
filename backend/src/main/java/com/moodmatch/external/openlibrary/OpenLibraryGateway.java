package com.moodmatch.external.openlibrary;

import java.util.List;

public interface OpenLibraryGateway {

    List<OpenLibrarySearchItem> searchBooks(String query, int limit);

    record OpenLibrarySearchItem(
            String workKey,
            List<String> editionKeys,
            String title,
            List<String> authorNames,
            Integer firstPublishYear,
            Integer coverId,
            List<String> subjectNames,
            String firstSentence) {}
}
