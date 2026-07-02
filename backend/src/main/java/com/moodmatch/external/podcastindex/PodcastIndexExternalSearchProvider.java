package com.moodmatch.external.podcastindex;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

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
public class PodcastIndexExternalSearchProvider implements ExternalSearchProvider {

    private static final String ATTRIBUTION = "Metadata from Podcast Index";
    private static final Set<MediaType> SUPPORTED_MEDIA_TYPES = Set.of(MediaType.PODCAST);
    private static final int MAX_SUBJECT_VALUES = 4;
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final String UNSET_CONFIG_SENTINEL = "__missing_podcastindex_config__";

    @Inject
    PodcastIndexGateway podcastIndexGateway;

    @ConfigProperty(name = "moodmatch.external.podcast-index.key")
    String apiKey;

    @ConfigProperty(name = "moodmatch.external.podcast-index.secret")
    String apiSecret;

    @Override
    public ExternalSearchSourceName sourceName() {
        return ExternalSearchSourceName.PODCAST_INDEX;
    }

    @Override
    public Set<MediaType> supportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }

    @Override
    public boolean isConfigured() {
        return configuredValue(apiKey) != null && configuredValue(apiSecret) != null;
    }

    @Override
    public String configurationErrorMessage() {
        return "Podcast Index provider is not configured. Set MOODMATCH_PODCASTINDEX_KEY and MOODMATCH_PODCASTINDEX_SECRET.";
    }

    @Override
    public List<ExternalSearchResult> search(ExternalSearchRequest request) {
        String resolvedApiKey = configuredValue(apiKey);
        String resolvedApiSecret = configuredValue(apiSecret);
        if (resolvedApiKey == null || resolvedApiSecret == null) {
            throw new IllegalStateException(configurationErrorMessage());
        }

        return podcastIndexGateway.searchShows(resolvedApiKey, resolvedApiSecret, request.query(), request.limit()).stream()
                .filter(item -> item.title() != null && !item.title().isBlank())
                .map(this::toResult)
                .filter(result -> result.externalId() != null && !result.externalId().isBlank())
                .toList();
    }

    private ExternalSearchResult toResult(PodcastIndexGateway.PodcastFeed item) {
        return new ExternalSearchResult(
                ExternalSearchSourceName.PODCAST_INDEX,
                ExternalSourceMappings.toMappingSource(ExternalSearchSourceName.PODCAST_INDEX, MediaType.PODCAST),
                externalId(item),
                MediaType.PODCAST,
                item.title().trim(),
                null,
                creatorNames(item.author(), item.ownerName()),
                stripHtml(item.description()),
                parseReleaseYear(item.newestItemPubdate()),
                firstNonBlank(item.artwork(), item.image()),
                firstNonBlank(item.link(), item.url(), item.originalUrl()),
                sanitizeTextList(item.categories()),
                externalSubjects(item),
                List.of(),
                ATTRIBUTION,
                List.of());
    }

    private String externalId(PodcastIndexGateway.PodcastFeed item) {
        if (item.id() > 0) {
            return String.valueOf(item.id());
        }
        return firstNonBlank(item.originalUrl(), item.url(), item.link());
    }

    private List<String> creatorNames(String author, String ownerName) {
        ArrayList<String> creators = new ArrayList<>();
        addDistinct(creators, blankToNull(author));
        addDistinct(creators, blankToNull(ownerName));
        return List.copyOf(creators);
    }

    private List<String> externalSubjects(PodcastIndexGateway.PodcastFeed item) {
        ArrayList<String> subjects = new ArrayList<>();
        addDistinct(subjects, labeledValue("Language", normalizeLanguage(item.language())));
        if (item.explicit() != null) {
            addDistinct(subjects, "Explicit: " + (item.explicit() == 1 ? "Yes" : "No"));
        }
        addDistinct(subjects, labeledValue("Feed type", normalizeSubjectValue(item.medium())));
        return subjects.stream().limit(MAX_SUBJECT_VALUES).toList();
    }

    String stripHtml(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        return unescapeHtml(HTML_TAG_PATTERN.matcher(normalized).replaceAll(" "))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Integer parseReleaseYear(Long newestItemPubdate) {
        if (newestItemPubdate == null || newestItemPubdate <= 0) {
            return null;
        }

        try {
            int year = Instant.ofEpochSecond(newestItemPubdate)
                    .atZone(ZoneOffset.UTC)
                    .getYear();
            return year >= 1900 && year <= 3000 ? year : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String normalizeLanguage(String language) {
        String normalized = blankToNull(language);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeSubjectValue(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String labeledValue(String label, String value) {
        return value == null ? null : label + ": " + value;
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

    private void addDistinct(List<String> values, String candidate) {
        if (candidate == null || values.contains(candidate)) {
            return;
        }
        values.add(candidate);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = blankToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String unescapeHtml(String value) {
        return value
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String configuredValue(String value) {
        String normalized = blankToNull(value);
        return UNSET_CONFIG_SENTINEL.equals(normalized) ? null : normalized;
    }
}
