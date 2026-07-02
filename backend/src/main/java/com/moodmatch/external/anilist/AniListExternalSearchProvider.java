package com.moodmatch.external.anilist;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import com.moodmatch.entity.MediaType;
import com.moodmatch.external.adapter.ExternalSearchProvider;
import com.moodmatch.external.adapter.ExternalSearchRequest;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;
import com.moodmatch.external.adapter.ExternalSourceMappings;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AniListExternalSearchProvider implements ExternalSearchProvider {

    private static final String ATTRIBUTION = "Metadata from AniList";
    private static final Set<MediaType> SUPPORTED_MEDIA_TYPES = Set.of(MediaType.FILM, MediaType.SERIES, MediaType.BOOK);
    private static final Set<String> ANIME_SERIES_FORMATS =
            Set.of("TV", "TV_SHORT", "OVA", "ONA", "SPECIAL", "SHORT", "MUSIC");
    private static final Set<String> BOOK_FORMATS = Set.of("MANGA", "NOVEL", "ONE_SHOT");
    private static final int FETCH_MULTIPLIER = 4;
    private static final int MIN_FETCH_LIMIT = 20;
    private static final int MAX_SUBJECT_VALUES = 10;
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    @Inject
    AniListGateway aniListGateway;

    @Override
    public ExternalSearchSourceName sourceName() {
        return ExternalSearchSourceName.ANILIST;
    }

    @Override
    public Set<MediaType> supportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }

    @Override
    public List<ExternalSearchResult> search(ExternalSearchRequest request) {
        return aniListGateway.searchMedia(request.mediaType(), request.query(), fetchLimit(request.limit())).stream()
                .filter(item -> item.id() > 0)
                .map(this::toResult)
                .filter(Objects::nonNull)
                .filter(result -> result.mediaType() == request.mediaType())
                .filter(result -> result.title() != null && !result.title().isBlank())
                .sorted((first, second) -> Integer.compare(seriesResultPriority(first), seriesResultPriority(second)))
                .limit(request.limit())
                .toList();
    }

    private int fetchLimit(int requestedLimit) {
        return Math.max(MIN_FETCH_LIMIT, requestedLimit * FETCH_MULTIPLIER);
    }

    private int seriesResultPriority(ExternalSearchResult result) {
        if (result.mediaType() != MediaType.SERIES) {
            return 0;
        }
        return result.externalSubjects().contains("Format: MUSIC") ? 1 : 0;
    }

    private ExternalSearchResult toResult(AniListGateway.AniListMedia item) {
        MediaType mediaType = mapMediaType(item);
        if (mediaType == null) {
            return null;
        }

        return new ExternalSearchResult(
                ExternalSearchSourceName.ANILIST,
                ExternalSourceMappings.toMappingSource(ExternalSearchSourceName.ANILIST, mediaType),
                String.valueOf(item.id()),
                mediaType,
                preferredTitle(item.title()),
                originalTitle(item.title()),
                creatorNames(item, mediaType),
                stripHtml(item.description()),
                item.startYear(),
                coverUrl(item.coverImage()),
                blankToNull(item.siteUrl()),
                sanitizeTextList(item.genres()),
                externalSubjects(item),
                List.of(),
                ATTRIBUTION,
                List.of());
    }

    private MediaType mapMediaType(AniListGateway.AniListMedia item) {
        String type = normalize(item.type());
        String format = normalize(item.format());

        if ("ANIME".equals(type)) {
            if ("MOVIE".equals(format)) {
                return MediaType.FILM;
            }
            if (ANIME_SERIES_FORMATS.contains(format)) {
                return MediaType.SERIES;
            }
            return null;
        }

        if ("MANGA".equals(type) && BOOK_FORMATS.contains(format)) {
            return MediaType.BOOK;
        }

        return null;
    }

    private String preferredTitle(AniListGateway.AniListTitle title) {
        if (title == null) {
            return null;
        }
        String romaji = blankToNull(title.romaji());
        if (romaji != null) {
            return romaji;
        }
        return blankToNull(title.english());
    }

    private String originalTitle(AniListGateway.AniListTitle title) {
        if (title == null) {
            return null;
        }
        String nativeTitle = blankToNull(title.nativeTitle());
        String preferredTitle = preferredTitle(title);
        return nativeTitle != null && !nativeTitle.equals(preferredTitle) ? nativeTitle : null;
    }

    private List<String> creatorNames(AniListGateway.AniListMedia item, MediaType mediaType) {
        if (mediaType == MediaType.FILM || mediaType == MediaType.SERIES) {
            return sanitizeTextList(item.studios());
        }
        return sanitizeTextList(item.staffNames());
    }

    private String coverUrl(AniListGateway.AniListCoverImage coverImage) {
        if (coverImage == null) {
            return null;
        }
        String large = blankToNull(coverImage.large());
        return large != null ? large : blankToNull(coverImage.medium());
    }

    private List<String> externalSubjects(AniListGateway.AniListMedia item) {
        ArrayList<String> subjects = new ArrayList<>();
        addLabeledSubject(subjects, "Format", item.format());
        addLabeledSubject(subjects, "Status", item.status());
        addSeasonSubject(subjects, item.season(), item.seasonYear());
        for (String tag : sanitizeTextList(item.tags())) {
            if (subjects.size() >= MAX_SUBJECT_VALUES) {
                break;
            }
            subjects.add(tag);
        }
        return subjects.stream().distinct().limit(MAX_SUBJECT_VALUES).toList();
    }

    private void addLabeledSubject(List<String> subjects, String label, String value) {
        String normalized = blankToNull(value);
        if (normalized != null) {
            subjects.add(label + ": " + normalized);
        }
    }

    private void addSeasonSubject(List<String> subjects, String season, Integer seasonYear) {
        String normalizedSeason = blankToNull(season);
        if (normalizedSeason != null && seasonYear != null) {
            subjects.add("Season: " + normalizedSeason + " " + seasonYear);
        } else if (normalizedSeason != null) {
            subjects.add("Season: " + normalizedSeason);
        } else if (seasonYear != null) {
            subjects.add("Season Year: " + seasonYear);
        }
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

    private String normalize(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase(java.util.Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
