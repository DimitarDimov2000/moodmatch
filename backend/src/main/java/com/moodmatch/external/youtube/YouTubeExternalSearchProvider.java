package com.moodmatch.external.youtube;

import java.util.List;
import java.util.Set;

import com.moodmatch.entity.MediaType;
import com.moodmatch.exception.BusinessRuleViolationException;
import com.moodmatch.external.adapter.ExternalSearchProvider;
import com.moodmatch.external.adapter.ExternalSearchRequest;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class YouTubeExternalSearchProvider implements ExternalSearchProvider {

    public static final int DEFAULT_MAX_RESULTS = 10;
    static final String DEFAULT_ORDER = "relevance";
    static final String DATE_ORDER = "date";
    static final String VIEW_COUNT_ORDER = "viewCount";
    static final String RELEVANCE_SORT = "relevance";
    static final String NEWEST_SORT = "newest";
    static final String MOST_VIEWED_SORT = "most_viewed";

    private static final Set<MediaType> SUPPORTED_MEDIA_TYPES = Set.of(MediaType.VIDEO);

    @Inject
    YouTubeGateway youTubeGateway;

    @ConfigProperty(name = "moodmatch.external.youtube.api-key")
    String apiKey;

    @Override
    public ExternalSearchSourceName sourceName() {
        return ExternalSearchSourceName.YOUTUBE;
    }

    @Override
    public Set<MediaType> supportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }

    @Override
    public boolean isConfigured() {
        return configuredApiKey() != null;
    }

    @Override
    public String configurationErrorMessage() {
        return YouTubeMetadataSupport.MISSING_CONFIGURATION_MESSAGE;
    }

    @Override
    public List<ExternalSearchResult> search(ExternalSearchRequest request) {
        String resolvedApiKey = configuredApiKey();
        if (resolvedApiKey == null) {
            throw new IllegalStateException(configurationErrorMessage());
        }

        return youTubeGateway.searchVideos(
                        resolvedApiKey,
                        request.query(),
                        resolveMaxResults(request.limit()),
                        resolveOrder(request.sort()))
                .stream()
                .filter(video -> YouTubeMetadataSupport.blankToNull(video.id()) != null)
                .filter(video -> YouTubeMetadataSupport.blankToNull(video.title()) != null)
                .map(video -> YouTubeMetadataSupport.toExternalSearchResult(video, null))
                .toList();
    }

    private int resolveMaxResults(int requestedLimit) {
        return requestedLimit > 0 ? requestedLimit : DEFAULT_MAX_RESULTS;
    }

    private String configuredApiKey() {
        return YouTubeMetadataSupport.configuredApiKey(apiKey);
    }

    private String resolveOrder(String sort) {
        String normalizedSort = YouTubeMetadataSupport.blankToNull(sort);
        if (normalizedSort == null || RELEVANCE_SORT.equals(normalizedSort)) {
            return DEFAULT_ORDER;
        }
        if (NEWEST_SORT.equals(normalizedSort)) {
            return DATE_ORDER;
        }
        if (MOST_VIEWED_SORT.equals(normalizedSort)) {
            return VIEW_COUNT_ORDER;
        }
        throw new BusinessRuleViolationException(
                "Unsupported YouTube search sort: %s. Use relevance, newest, or most_viewed.".formatted(sort));
    }
}
