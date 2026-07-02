package com.moodmatch.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.moodmatch.dto.external.ExternalSearchResponse;
import com.moodmatch.dto.external.ExternalSearchResultResponse;
import com.moodmatch.entity.MediaType;
import com.moodmatch.exception.BusinessRuleViolationException;
import com.moodmatch.external.adapter.ExternalSearchProvider;
import com.moodmatch.external.adapter.ExternalSearchRequest;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class ExternalSearchService {

    static final int DEFAULT_LIMIT = 5;
    static final int MAX_LIMIT = 10;
    static final int AUTOMATIC_PROVIDER_LIMIT = DEFAULT_LIMIT;

    @Inject
    Instance<ExternalSearchProvider> externalSearchProviders;

    @Inject
    ExternalResultMapper externalResultMapper;

    @Transactional(TxType.SUPPORTS)
    public ExternalSearchResponse search(String query, String mediaTypeRaw, String sourceRaw, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        MediaType mediaType = parseMediaType(mediaTypeRaw);
        int safeLimit = toSafeLimit(limit);

        if (isAutomaticSearch(sourceRaw)) {
            return searchAutomatically(normalizedQuery, mediaType, safeLimit);
        }

        ExternalSearchProvider provider = resolveProvider(parseSource(sourceRaw));
        if (!provider.supportedMediaTypes().contains(mediaType)) {
            throw new BusinessRuleViolationException(
                    "Source %s does not support media type %s.".formatted(provider.sourceName(), mediaType));
        }
        ensureConfigured(provider);

        ExternalSearchRequest request =
                new ExternalSearchRequest(normalizedQuery, mediaType, provider.sourceName(), safeLimit);
        List<ExternalSearchResultResponse> results = provider.search(request).stream()
                .map(externalResultMapper::enrichSuggestions)
                .map(externalResultMapper::toResponse)
                .toList();

        return new ExternalSearchResponse(
                normalizedQuery,
                mediaType,
                provider.sourceName(),
                results,
                List.of());
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

    private boolean isAutomaticSearch(String sourceRaw) {
        if (sourceRaw == null || sourceRaw.isBlank()) {
            return true;
        }
        return parseSource(sourceRaw) == ExternalSearchSourceName.AUTOMATIC;
    }

    private ExternalSearchResponse searchAutomatically(String normalizedQuery, MediaType mediaType, int safeLimit) {
        List<String> warnings = new ArrayList<>();
        List<ExternalSearchProvider> realProviders = compatibleRealProvidersFor(mediaType);
        List<ExternalSearchResult> results = new ArrayList<>();
        int successfulRealProviderCount = 0;

        for (ExternalSearchProvider provider : realProviders) {
            if (!provider.isConfigured()) {
                warnings.add(provider.configurationErrorMessage() + " Provider skipped in automatic search.");
                continue;
            }

            try {
                int automaticProviderLimit = Math.min(safeLimit, AUTOMATIC_PROVIDER_LIMIT);
                ExternalSearchRequest request =
                        new ExternalSearchRequest(normalizedQuery, mediaType, provider.sourceName(), automaticProviderLimit);
                results.addAll(provider.search(request));
                successfulRealProviderCount++;
            } catch (RuntimeException exception) {
                warnings.add("%s search failed in automatic mode: %s"
                        .formatted(provider.sourceName(), exception.getMessage()));
            }
        }

        if (successfulRealProviderCount == 0 && shouldUseDemoFallback(mediaType)) {
            Optional<ExternalSearchProvider> demoProvider = findProvider(ExternalSearchSourceName.DEMO);
            if (demoProvider.isPresent()) {
                ExternalSearchProvider provider = demoProvider.get();
                int automaticProviderLimit = Math.min(safeLimit, AUTOMATIC_PROVIDER_LIMIT);
                ExternalSearchRequest request =
                        new ExternalSearchRequest(normalizedQuery, mediaType, provider.sourceName(), automaticProviderLimit);
                results.addAll(provider.search(request));
                if (!warnings.isEmpty()) {
                    warnings.add("Using DEMO fallback.");
                }
            }
        }

        List<ExternalSearchResultResponse> responseResults = deduplicate(results).stream()
                .map(externalResultMapper::enrichSuggestions)
                .map(externalResultMapper::toResponse)
                .toList();

        return new ExternalSearchResponse(
                normalizedQuery,
                mediaType,
                ExternalSearchSourceName.AUTOMATIC,
                responseResults,
                List.copyOf(warnings));
    }

    private List<ExternalSearchProvider> compatibleRealProvidersFor(MediaType mediaType) {
        return switch (mediaType) {
            case FILM, SERIES -> providersInOrder(
                    ExternalSearchSourceName.TMDB,
                    ExternalSearchSourceName.ANILIST);
            case BOOK -> providersInOrder(
                    ExternalSearchSourceName.OPEN_LIBRARY,
                    ExternalSearchSourceName.ANILIST);
            case AUDIOBOOK -> providersInOrder(ExternalSearchSourceName.LIBRIVOX);
            case GAME -> providersInOrder(ExternalSearchSourceName.RAWG);
            case PODCAST -> providersInOrder(ExternalSearchSourceName.PODCAST_INDEX);
            case VIDEO -> List.of();
        };
    }

    private List<ExternalSearchProvider> providersInOrder(ExternalSearchSourceName... sources) {
        ArrayList<ExternalSearchProvider> providers = new ArrayList<>();
        for (ExternalSearchSourceName source : sources) {
            findProvider(source).ifPresent(providers::add);
        }
        return List.copyOf(providers);
    }

    private boolean shouldUseDemoFallback(MediaType mediaType) {
        return switch (mediaType) {
            case FILM, SERIES, BOOK, AUDIOBOOK, GAME -> true;
            case PODCAST, VIDEO -> false;
        };
    }

    private List<ExternalSearchResult> deduplicate(List<ExternalSearchResult> results) {
        Map<String, ExternalSearchResult> bySourceAndId = new LinkedHashMap<>();
        for (ExternalSearchResult result : results) {
            bySourceAndId.putIfAbsent(result.source() + ":" + result.externalId(), result);
        }
        return List.copyOf(bySourceAndId.values());
    }

    private ExternalSearchProvider resolveProvider(ExternalSearchSourceName source) {
        return findProvider(source)
                .orElseThrow(() -> new BusinessRuleViolationException("Source is not available: " + source));
    }

    private Optional<ExternalSearchProvider> findProvider(ExternalSearchSourceName source) {
        return externalSearchProviders.stream()
                .filter(provider -> provider.sourceName() == source)
                .min(Comparator.comparing(provider -> provider.getClass().getName()));
    }

    private void ensureConfigured(ExternalSearchProvider provider) {
        if (!provider.isConfigured()) {
            throw new BusinessRuleViolationException(provider.configurationErrorMessage());
        }
    }
}
