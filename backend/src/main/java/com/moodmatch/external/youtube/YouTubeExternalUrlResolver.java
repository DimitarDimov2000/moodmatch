package com.moodmatch.external.youtube;

import com.moodmatch.exception.BusinessRuleViolationException;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;
import com.moodmatch.external.adapter.ExternalUrlResolver;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class YouTubeExternalUrlResolver implements ExternalUrlResolver {

    @Inject
    YouTubeGateway youTubeGateway;

    @ConfigProperty(name = "moodmatch.external.youtube.api-key")
    String apiKey;

    private final YouTubeVideoIdResolver videoIdResolver = new YouTubeVideoIdResolver();

    @Override
    public ExternalSearchSourceName sourceName() {
        return ExternalSearchSourceName.YOUTUBE;
    }

    @Override
    public boolean isConfigured() {
        return configuredApiKey(apiKey) != null;
    }

    @Override
    public String configurationErrorMessage() {
        return YouTubeMetadataSupport.MISSING_CONFIGURATION_MESSAGE;
    }

    @Override
    public ExternalSearchResult resolve(String urlOrId) {
        String resolvedApiKey = configuredApiKey(apiKey);
        if (resolvedApiKey == null) {
            throw new IllegalStateException(configurationErrorMessage());
        }

        String videoId = videoIdResolver.resolveVideoId(urlOrId);
        YouTubeGateway.YouTubeVideo video = youTubeGateway.fetchVideo(resolvedApiKey, videoId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "No YouTube video was found for the provided URL or video ID."));

        String categoryLabel = blankToNull(video.categoryId()) == null
                ? null
                : youTubeGateway.fetchCategoryLabel(resolvedApiKey, video.categoryId()).orElse(null);

        String title = blankToNull(video.title());
        if (title == null) {
            throw new BusinessRuleViolationException("The resolved YouTube video does not contain a usable title.");
        }

        return YouTubeMetadataSupport.toExternalSearchResult(video, categoryLabel);
    }

    private String configuredApiKey(String value) {
        return YouTubeMetadataSupport.configuredApiKey(value);
    }

    private String blankToNull(String value) {
        return YouTubeMetadataSupport.blankToNull(value);
    }
}
