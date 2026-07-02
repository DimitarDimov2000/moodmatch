package com.moodmatch.external.youtube;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;

import com.moodmatch.entity.MediaType;
import com.moodmatch.exception.BusinessRuleViolationException;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;
import com.moodmatch.external.adapter.ExternalSourceMappings;
import com.moodmatch.external.adapter.ExternalUrlResolver;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class YouTubeExternalUrlResolver implements ExternalUrlResolver {

    private static final String ATTRIBUTION = "Metadata from YouTube";
    private static final int MAX_SUBJECT_VALUES = 12;

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
        return configuredApiKey() != null;
    }

    @Override
    public String configurationErrorMessage() {
        return "YouTube provider is not configured. Set MOODMATCH_YOUTUBE_API_KEY in the backend environment.";
    }

    @Override
    public ExternalSearchResult resolve(String urlOrId) {
        String resolvedApiKey = configuredApiKey();
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

        return new ExternalSearchResult(
                ExternalSearchSourceName.YOUTUBE,
                ExternalSourceMappings.toMappingSource(ExternalSearchSourceName.YOUTUBE, MediaType.VIDEO),
                videoId,
                MediaType.VIDEO,
                title,
                null,
                creatorNames(video.channelTitle()),
                sanitizeDescription(video.description()),
                parseReleaseYear(video.publishedAt()),
                bestThumbnailUrl(video.thumbnails()),
                canonicalWatchUrl(videoId),
                externalGenres(categoryLabel),
                externalSubjects(video.tags(), video.channelTitle(), categoryLabel),
                List.of(),
                ATTRIBUTION,
                List.of());
    }

    private List<String> creatorNames(String channelTitle) {
        String normalizedChannelTitle = blankToNull(channelTitle);
        return normalizedChannelTitle == null ? List.of() : List.of(normalizedChannelTitle);
    }

    private List<String> externalGenres(String categoryLabel) {
        String normalizedCategoryLabel = blankToNull(categoryLabel);
        return normalizedCategoryLabel == null ? List.of() : List.of(normalizedCategoryLabel);
    }

    private List<String> externalSubjects(List<String> tags, String channelTitle, String categoryLabel) {
        LinkedHashSet<String> subjects = new LinkedHashSet<>();
        if (tags != null) {
            for (String tag : tags) {
                String normalizedTag = blankToNull(tag);
                if (normalizedTag != null) {
                    subjects.add(normalizedTag);
                }
                if (subjects.size() >= MAX_SUBJECT_VALUES) {
                    break;
                }
            }
        }

        addIfPresent(subjects, labeledValue("Channel", channelTitle));
        addIfPresent(subjects, labeledValue("Category", categoryLabel));

        return subjects.stream().limit(MAX_SUBJECT_VALUES).toList();
    }

    private void addIfPresent(LinkedHashSet<String> values, String candidate) {
        if (candidate != null) {
            values.add(candidate);
        }
    }

    private String labeledValue(String label, String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : label + ": " + normalized;
    }

    private Integer parseReleaseYear(String publishedAt) {
        String normalized = blankToNull(publishedAt);
        if (normalized == null) {
            return null;
        }

        try {
            return OffsetDateTime.parse(normalized).getYear();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String bestThumbnailUrl(YouTubeGateway.ThumbnailSet thumbnails) {
        if (thumbnails == null) {
            return null;
        }

        return java.util.stream.Stream.of(
                        thumbnails.maxresUrl(),
                        thumbnails.standardUrl(),
                        thumbnails.highUrl(),
                        thumbnails.mediumUrl(),
                        thumbnails.defaultUrl())
                .map(this::blankToNull)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String canonicalWatchUrl(String videoId) {
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    private String sanitizeDescription(String description) {
        String normalized = blankToNull(description);
        if (normalized == null) {
            return null;
        }
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private String configuredApiKey() {
        String normalized = blankToNull(apiKey);
        return "__missing_youtube_config__".equals(normalized) ? null : normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
