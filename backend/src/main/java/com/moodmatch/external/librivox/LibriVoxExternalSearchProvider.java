package com.moodmatch.external.librivox;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.moodmatch.entity.MediaType;
import com.moodmatch.external.adapter.ExternalSearchProvider;
import com.moodmatch.external.adapter.ExternalSearchRequest;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;
import com.moodmatch.external.adapter.ExternalSourceMappings;
import com.moodmatch.external.librivox.LibriVoxGateway.LibriVoxAudiobook;
import com.moodmatch.external.librivox.LibriVoxGateway.LibriVoxAuthor;
import com.moodmatch.external.librivox.LibriVoxGateway.LibriVoxGenre;
import com.moodmatch.external.librivox.LibriVoxGateway.LibriVoxReader;
import com.moodmatch.external.librivox.LibriVoxGateway.LibriVoxSection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LibriVoxExternalSearchProvider implements ExternalSearchProvider {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final String ATTRIBUTION = "LibriVox public domain audiobook catalog";
    private static final Set<MediaType> SUPPORTED_MEDIA_TYPES = Set.of(MediaType.AUDIOBOOK);

    @Inject
    LibriVoxGateway libriVoxGateway;

    @Override
    public ExternalSearchSourceName sourceName() {
        return ExternalSearchSourceName.LIBRIVOX;
    }

    @Override
    public Set<MediaType> supportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }

    @Override
    public List<ExternalSearchResult> search(ExternalSearchRequest request) {
        return libriVoxGateway.searchAudiobooks(request.query(), request.limit()).stream()
                .map(this::toResult)
                .filter(result -> result.externalId() != null && !result.externalId().isBlank())
                .toList();
    }

    private ExternalSearchResult toResult(LibriVoxAudiobook item) {
        return new ExternalSearchResult(
                ExternalSearchSourceName.LIBRIVOX,
                ExternalSourceMappings.toMappingSource(ExternalSearchSourceName.LIBRIVOX, MediaType.AUDIOBOOK),
                item.id(),
                MediaType.AUDIOBOOK,
                item.title().trim(),
                null,
                buildCreatorNames(item.authors(), item.sections()),
                normalizeDescription(item.description()),
                parseReleaseYear(item.copyrightYear()),
                buildCoverUrl(item),
                blankToNull(item.urlLibrivox()),
                sanitizeTextList(item.genres().stream().map(LibriVoxGenre::name).toList()),
                buildSubjects(item.language()),
                List.of(),
                ATTRIBUTION,
                List.of());
    }

    private List<String> buildCreatorNames(List<LibriVoxAuthor> authors, List<LibriVoxSection> sections) {
        List<String> authorNames = sanitizeTextList(authors.stream()
                .map(this::formatAuthorName)
                .toList());
        List<String> readerNames = sanitizeTextList(sections.stream()
                .flatMap(section -> section.readers().stream())
                .map(LibriVoxReader::displayName)
                .toList());

        java.util.ArrayList<String> creatorNames = new java.util.ArrayList<>();
        if (!authorNames.isEmpty()) {
            creatorNames.add("Author: " + String.join(", ", authorNames));
        }
        if (readerNames.size() == 1) {
            creatorNames.add("Reader: " + readerNames.getFirst());
        } else if (!readerNames.isEmpty()) {
            creatorNames.add("Readers: " + summarizeReaders(readerNames));
        }
        return List.copyOf(creatorNames);
    }

    private String summarizeReaders(List<String> readerNames) {
        if (readerNames.size() <= 3) {
            return String.join(", ", readerNames);
        }
        return String.join(", ", readerNames.subList(0, 3)) + ", +" + (readerNames.size() - 3) + " more";
    }

    private List<String> buildSubjects(String language) {
        String normalizedLanguage = blankToNull(language);
        if (normalizedLanguage == null) {
            return List.of();
        }
        return List.of(normalizedLanguage);
    }

    private List<String> sanitizeTextList(List<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String formatAuthorName(LibriVoxAuthor author) {
        String firstName = blankToNull(author.firstName());
        String lastName = blankToNull(author.lastName());
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return firstName != null ? firstName : lastName;
    }

    private String normalizeDescription(String description) {
        String normalized = blankToNull(description);
        if (normalized == null) {
            return null;
        }

        String withoutBreaks = normalized.replaceAll("(?i)<br\\s*/?>", "\n");
        String withoutTags = HTML_TAG_PATTERN.matcher(withoutBreaks).replaceAll("");
        String compactWhitespace = withoutTags
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&rsquo;", "'")
                .replace("&ldquo;", "\"")
                .replace("&rdquo;", "\"")
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        return compactWhitespace.isBlank() ? null : compactWhitespace;
    }

    private Integer parseReleaseYear(String copyrightYear) {
        String normalized = blankToNull(copyrightYear);
        if (normalized == null) {
            return null;
        }

        try {
            return Integer.valueOf(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String buildCoverUrl(LibriVoxAudiobook item) {
        String coverArtJpg = blankToNull(item.coverArtJpg());
        if (coverArtJpg != null) {
            return coverArtJpg;
        }
        return blankToNull(item.coverArtThumbnail());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
