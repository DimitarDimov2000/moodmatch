package com.moodmatch.external.openlibrary;

import java.util.List;
import java.util.Set;

import com.moodmatch.entity.MediaType;
import com.moodmatch.external.adapter.ExternalSearchProvider;
import com.moodmatch.external.adapter.ExternalSearchRequest;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;
import com.moodmatch.external.adapter.ExternalSourceMappings;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OpenLibraryExternalSearchProvider implements ExternalSearchProvider {

    private static final String ATTRIBUTION = "Metadata from Open Library";
    private static final Set<MediaType> SUPPORTED_MEDIA_TYPES = Set.of(MediaType.BOOK);

    @Inject
    OpenLibraryGateway openLibraryGateway;

    @ConfigProperty(name = "moodmatch.external.open-library.cover-base-url", defaultValue = "https://covers.openlibrary.org")
    String coverBaseUrl;

    @ConfigProperty(name = "moodmatch.external.open-library.website-base-url", defaultValue = "https://openlibrary.org")
    String websiteBaseUrl;

    @Override
    public ExternalSearchSourceName sourceName() {
        return ExternalSearchSourceName.OPEN_LIBRARY;
    }

    @Override
    public Set<MediaType> supportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }

    @Override
    public List<ExternalSearchResult> search(ExternalSearchRequest request) {
        return openLibraryGateway.searchBooks(request.query(), request.limit()).stream()
                .filter(item -> item.title() != null && !item.title().isBlank())
                .map(this::toResult)
                .filter(result -> result.externalId() != null && !result.externalId().isBlank())
                .toList();
    }

    private ExternalSearchResult toResult(OpenLibraryGateway.OpenLibrarySearchItem item) {
        String workKey = normalizeKey(item.workKey());
        String editionKey = normalizeKeys(item.editionKeys()).stream().findFirst().orElse(null);
        String externalId = workKey != null ? workKey : editionKey;

        return new ExternalSearchResult(
                ExternalSearchSourceName.OPEN_LIBRARY,
                ExternalSourceMappings.toMappingSource(ExternalSearchSourceName.OPEN_LIBRARY, MediaType.BOOK),
                externalId,
                MediaType.BOOK,
                item.title().trim(),
                null,
                sanitizeTextList(item.authorNames()),
                buildDescription(item),
                item.firstPublishYear(),
                buildCoverUrl(item.coverId(), editionKey),
                buildSourceUrl(workKey, editionKey),
                List.of(),
                sanitizeTextList(item.subjectNames()),
                List.of(),
                ATTRIBUTION,
                List.of());
    }

    private List<String> sanitizeTextList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private List<String> normalizeKeys(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(this::normalizeKey)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private String buildDescription(OpenLibraryGateway.OpenLibrarySearchItem item) {
        String firstSentence = blankToNull(item.firstSentence());
        if (firstSentence != null) {
            return firstSentence;
        }

        List<String> authorNames = sanitizeTextList(item.authorNames());
        if (!authorNames.isEmpty() && item.firstPublishYear() != null) {
            return "Book by %s. First published in %s."
                    .formatted(String.join(", ", authorNames), item.firstPublishYear());
        }
        if (!authorNames.isEmpty()) {
            return "Book by %s.".formatted(String.join(", ", authorNames));
        }
        if (item.firstPublishYear() != null) {
            return "Book first published in %s.".formatted(item.firstPublishYear());
        }
        return "Book result from Open Library.";
    }

    private String buildCoverUrl(Integer coverId, String editionKey) {
        if (coverId != null && coverId > 0) {
            return coverBaseUrl + "/b/id/" + coverId + "-M.jpg";
        }
        if (editionKey != null) {
            return coverBaseUrl + "/b/olid/" + editionKey + "-M.jpg";
        }
        return null;
    }

    private String buildSourceUrl(String workKey, String editionKey) {
        if (workKey != null) {
            return websiteBaseUrl + "/works/" + workKey;
        }
        if (editionKey != null) {
            return websiteBaseUrl + "/books/" + editionKey;
        }
        return null;
    }

    private String normalizeKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("/works/")) {
            return trimmed.substring("/works/".length());
        }
        if (trimmed.startsWith("/books/")) {
            return trimmed.substring("/books/".length());
        }
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
