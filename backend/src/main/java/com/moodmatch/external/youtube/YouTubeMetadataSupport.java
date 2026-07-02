package com.moodmatch.external.youtube;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.moodmatch.entity.MediaType;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;
import com.moodmatch.external.adapter.ExternalSourceMappings;

final class YouTubeMetadataSupport {

    static final String ATTRIBUTION = "Metadata from YouTube";
    static final String MISSING_CONFIGURATION_MESSAGE =
            "YouTube provider is not configured. Set MOODMATCH_YOUTUBE_API_KEY in the backend environment.";
    static final String UNSET_CONFIG_SENTINEL = "__missing_youtube_config__";

    private static final int MAX_SUBJECT_VALUES = 12;

    private YouTubeMetadataSupport() {}

    static ExternalSearchResult toExternalSearchResult(
            YouTubeGateway.YouTubeVideo video, String categoryLabel) {
        return new ExternalSearchResult(
                ExternalSearchSourceName.YOUTUBE,
                ExternalSourceMappings.toMappingSource(ExternalSearchSourceName.YOUTUBE, MediaType.VIDEO),
                video.id(),
                MediaType.VIDEO,
                video.title().trim(),
                null,
                creatorNames(video.channelTitle()),
                sanitizeDescription(video.description()),
                parseReleaseYear(video.publishedAt()),
                bestThumbnailUrl(video.thumbnails()),
                canonicalWatchUrl(video.id()),
                externalGenres(categoryLabel),
                externalSubjects(video.tags(), video.channelTitle(), categoryLabel),
                List.of(),
                ATTRIBUTION,
                List.of());
    }

    static List<String> creatorNames(String channelTitle) {
        String normalizedChannelTitle = blankToNull(channelTitle);
        return normalizedChannelTitle == null ? List.of() : List.of(normalizedChannelTitle);
    }

    static List<String> externalGenres(String categoryLabel) {
        String normalizedCategoryLabel = blankToNull(categoryLabel);
        return normalizedCategoryLabel == null ? List.of() : List.of(normalizedCategoryLabel);
    }

    static List<String> externalSubjects(List<String> tags, String channelTitle, String categoryLabel) {
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

    static Integer parseReleaseYear(String publishedAt) {
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

    static String bestThumbnailUrl(YouTubeGateway.ThumbnailSet thumbnails) {
        if (thumbnails == null) {
            return null;
        }

        return Stream.of(
                        thumbnails.maxresUrl(),
                        thumbnails.standardUrl(),
                        thumbnails.highUrl(),
                        thumbnails.mediumUrl(),
                        thumbnails.defaultUrl())
                .map(YouTubeMetadataSupport::blankToNull)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    static String canonicalWatchUrl(String videoId) {
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    static String sanitizeDescription(String description) {
        String normalized = blankToNull(description);
        if (normalized == null) {
            return null;
        }
        return normalized.replaceAll("\\s+", " ").trim();
    }

    static String configuredApiKey(String apiKey) {
        String normalized = blankToNull(apiKey);
        return UNSET_CONFIG_SENTINEL.equals(normalized) ? null : normalized;
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void addIfPresent(LinkedHashSet<String> values, String candidate) {
        if (candidate != null) {
            values.add(candidate);
        }
    }

    private static String labeledValue(String label, String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : label + ": " + normalized;
    }
}
