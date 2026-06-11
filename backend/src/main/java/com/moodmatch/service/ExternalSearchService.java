package com.moodmatch.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.moodmatch.dto.external.ExternalSearchResponse;
import com.moodmatch.dto.external.ExternalSearchResultResponse;
import com.moodmatch.dto.external.ExternalSuggestedTagResponse;
import com.moodmatch.entity.MediaType;
import com.moodmatch.exception.BusinessRuleViolationException;
import com.moodmatch.external.adapter.ExternalSearchProvider;
import com.moodmatch.external.adapter.ExternalSearchRequest;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;
import com.moodmatch.external.adapter.ExternalSuggestedTag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class ExternalSearchService {

    static final int DEFAULT_LIMIT = 5;
    static final int MAX_LIMIT = 10;

    @Inject
    Instance<ExternalSearchProvider> externalSearchProviders;

    @Inject
    ExternalTagSuggestionService externalTagSuggestionService;

    @Transactional(TxType.SUPPORTS)
    public ExternalSearchResponse search(String query, String mediaTypeRaw, String sourceRaw, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        MediaType mediaType = parseMediaType(mediaTypeRaw);
        ExternalSearchSourceName source = parseSource(sourceRaw);
        int safeLimit = toSafeLimit(limit);

        ExternalSearchProvider provider = resolveProvider(source);
        if (!provider.supportedMediaTypes().contains(mediaType)) {
            throw new BusinessRuleViolationException(
                    "Source %s does not support media type %s.".formatted(source, mediaType));
        }

        ExternalSearchRequest request = new ExternalSearchRequest(normalizedQuery, mediaType, source, safeLimit);
        List<ExternalSearchResultResponse> results = provider.search(request).stream()
                .map(this::enrichSuggestions)
                .map(this::toResponse)
                .toList();

        return new ExternalSearchResponse(normalizedQuery, mediaType, source, results, List.of());
    }

    private String normalizeQuery(String query) {
        return Objects.requireNonNull(query, "Query must not be null.").trim();
    }

    private MediaType parseMediaType(String mediaTypeRaw) {
        try {
            return MediaType.valueOf(Objects.requireNonNull(mediaTypeRaw, "Media type must not be null.")
                    .trim()
                    .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessRuleViolationException("Unsupported mediaType: " + mediaTypeRaw);
        }
    }

    private ExternalSearchSourceName parseSource(String sourceRaw) {
        if (sourceRaw == null || sourceRaw.isBlank()) {
            return ExternalSearchSourceName.DEMO;
        }

        try {
            return ExternalSearchSourceName.valueOf(sourceRaw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleViolationException("Unsupported source: " + sourceRaw);
        }
    }

    private int toSafeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }

    private ExternalSearchProvider resolveProvider(ExternalSearchSourceName source) {
        return externalSearchProviders.stream()
                .filter(provider -> provider.sourceName() == source)
                .min(Comparator.comparing(provider -> provider.getClass().getName()))
                .orElseThrow(() -> new BusinessRuleViolationException("Source is not available: " + source));
    }

    private ExternalSearchResult enrichSuggestions(ExternalSearchResult result) {
        List<ExternalSuggestedTag> suggestions = externalTagSuggestionService.buildSuggestions(result);
        return new ExternalSearchResult(
                result.source(),
                result.mappingSource(),
                result.externalId(),
                result.mediaType(),
                result.title(),
                result.originalTitle(),
                result.description(),
                result.releaseYear(),
                result.coverUrl(),
                result.sourceUrl(),
                result.externalGenres(),
                result.externalSubjects(),
                suggestions,
                result.attribution(),
                result.warnings());
    }

    private ExternalSearchResultResponse toResponse(ExternalSearchResult result) {
        return new ExternalSearchResultResponse(
                result.source(),
                result.externalId(),
                result.mediaType(),
                result.title(),
                result.originalTitle(),
                result.description(),
                result.releaseYear(),
                result.coverUrl(),
                result.sourceUrl(),
                List.copyOf(result.externalGenres()),
                List.copyOf(result.externalSubjects()),
                result.suggestedTags().stream().map(this::toResponse).toList(),
                result.attribution(),
                List.copyOf(result.warnings()));
    }

    private ExternalSuggestedTagResponse toResponse(ExternalSuggestedTag suggestedTag) {
        return new ExternalSuggestedTagResponse(
                suggestedTag.tagId(),
                suggestedTag.tagName(),
                suggestedTag.tagCategory(),
                suggestedTag.sourceValue(),
                suggestedTag.reason(),
                suggestedTag.confidence());
    }
}
