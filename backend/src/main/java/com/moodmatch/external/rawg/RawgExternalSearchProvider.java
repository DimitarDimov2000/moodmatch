package com.moodmatch.external.rawg;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
public class RawgExternalSearchProvider implements ExternalSearchProvider {

    private static final String ATTRIBUTION = "Metadata from RAWG. View source on RAWG for full provider details.";
    private static final Set<MediaType> SUPPORTED_MEDIA_TYPES = Set.of(MediaType.GAME);
    private static final int MAX_SUBJECT_VALUES = 8;

    @Inject
    RawgGateway rawgGateway;

    @ConfigProperty(name = "moodmatch.rawg.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "moodmatch.external.rawg.website-base-url", defaultValue = "https://rawg.io")
    String websiteBaseUrl;

    @Override
    public ExternalSearchSourceName sourceName() {
        return ExternalSearchSourceName.RAWG;
    }

    @Override
    public Set<MediaType> supportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }

    @Override
    public boolean isConfigured() {
        return apiKey.isPresent() && !apiKey.get().isBlank();
    }

    @Override
    public String configurationErrorMessage() {
        return "RAWG provider is not configured. Set MOODMATCH_RAWG_API_KEY.";
    }

    @Override
    public List<ExternalSearchResult> search(ExternalSearchRequest request) {
        String resolvedApiKey = apiKey.filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalStateException(configurationErrorMessage()));
        return rawgGateway.searchGames(resolvedApiKey.trim(), request.query(), request.limit()).stream()
                .filter(item -> item.name() != null && !item.name().isBlank())
                .map(this::toResult)
                .filter(result -> result.externalId() != null && !result.externalId().isBlank())
                .toList();
    }

    private ExternalSearchResult toResult(RawgGateway.RawgGame item) {
        return new ExternalSearchResult(
                ExternalSearchSourceName.RAWG,
                ExternalSourceMappings.toMappingSource(ExternalSearchSourceName.RAWG, MediaType.GAME),
                externalId(item),
                MediaType.GAME,
                item.name().trim(),
                null,
                buildCreatorNames(item.developers(), item.publishers()),
                blankToNull(item.descriptionRaw()),
                parseReleaseYear(item.released()),
                blankToNull(item.backgroundImage()),
                buildSourceUrl(item.slug()),
                sanitizeTextList(item.genres()),
                buildSubjects(item.platforms(), item.tags()),
                List.of(),
                ATTRIBUTION,
                List.of());
    }

    private String externalId(RawgGateway.RawgGame item) {
        if (item.id() > 0) {
            return String.valueOf(item.id());
        }
        return blankToNull(item.slug());
    }

    private List<String> buildCreatorNames(List<String> developers, List<String> publishers) {
        List<String> developerNames = sanitizeTextList(developers);
        List<String> publisherNames = sanitizeTextList(publishers);

        ArrayList<String> creatorNames = new ArrayList<>();
        if (!developerNames.isEmpty()) {
            creatorNames.add("Developer: " + String.join(", ", developerNames));
        }
        if (!publisherNames.isEmpty()) {
            creatorNames.add("Publisher: " + String.join(", ", publisherNames));
        }
        return List.copyOf(creatorNames);
    }

    private List<String> buildSubjects(List<String> platforms, List<String> tags) {
        ArrayList<String> subjects = new ArrayList<>();
        subjects.addAll(sanitizeTextList(platforms));
        for (String tag : sanitizeTextList(tags)) {
            if (subjects.size() >= MAX_SUBJECT_VALUES) {
                break;
            }
            subjects.add(tag);
        }
        return subjects.stream().distinct().limit(MAX_SUBJECT_VALUES).toList();
    }

    private Integer parseReleaseYear(String released) {
        String normalized = blankToNull(released);
        if (normalized == null || normalized.length() < 4) {
            return null;
        }

        try {
            return Integer.valueOf(normalized.substring(0, 4));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String buildSourceUrl(String slug) {
        String normalizedSlug = blankToNull(slug);
        return normalizedSlug == null ? null : websiteBaseUrl + "/games/" + normalizedSlug;
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
