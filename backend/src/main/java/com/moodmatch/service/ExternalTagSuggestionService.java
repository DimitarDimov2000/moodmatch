package com.moodmatch.service;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.moodmatch.entity.ExternalTagMapping;
import com.moodmatch.entity.MediaType;
import com.moodmatch.entity.TagCategory;
import com.moodmatch.entity.TagMappingConfidence;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSuggestedTag;
import com.moodmatch.repository.ExternalTagMappingRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class ExternalTagSuggestionService {

    private static final int MAX_SUGGESTIONS = 10;

    private static final Set<String> USEFUL_FALLBACK_VALUES = Set.of(
            "science fiction",
            "children",
            "anime",
            "manga",
            "education",
            "technology",
            "action",
            "adventure",
            "drama",
            "fantasy",
            "comedy",
            "thriller",
            "mystery",
            "horror",
            "romance",
            "rpg",
            "open world",
            "politics",
            "survival",
            "family",
            "space",
            "identity",
            "dark",
            "light",
            "epic",
            "historical",
            "urban",
            "nature",
            "relaxing",
            "intense",
            "challenging",
            "tutorial",
            "frontend");

    @Inject
    ExternalTagMappingRepository externalTagMappingRepository;

    @Inject
    ExternalMetadataTagNormalizer externalMetadataTagNormalizer;

    @Transactional(TxType.SUPPORTS)
    public List<ExternalSuggestedTag> buildSuggestions(ExternalSearchResult result) {
        if (result.mappingSource() == null) {
            return List.of();
        }

        Map<String, ExternalSuggestedTag> suggestions = new LinkedHashMap<>();
        Set<String> mappedCanonicalValues = new LinkedHashSet<>();
        List<ExternalMetadataTagNormalizer.NormalizedExternalValue> genreValues =
                externalMetadataTagNormalizer.normalizeGenres(result.externalGenres());
        List<ExternalMetadataTagNormalizer.NormalizedExternalValue> subjectValues =
                externalMetadataTagNormalizer.normalizeSubjects(result.externalSubjects());

        mappedCanonicalValues.addAll(collectMappedSuggestions(
                suggestions,
                result.mappingSource(),
                "genre",
                genreValues));
        mappedCanonicalValues.addAll(collectMappedSuggestions(
                suggestions,
                result.mappingSource(),
                "subject",
                subjectValues));

        List<ExternalSuggestedTag> mappedSuggestions = suggestions.values().stream()
                .sorted(Comparator.comparing(ExternalSuggestedTag::tagCategory)
                        .thenComparing(ExternalSuggestedTag::tagName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ExternalSuggestedTag::sourceValue, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ExternalSuggestedTag::tagId))
                .limit(MAX_SUGGESTIONS)
                .toList();
        if (mappedSuggestions.size() >= MAX_SUGGESTIONS) {
            return mappedSuggestions;
        }

        LinkedHashMap<String, ExternalSuggestedTag> combined = new LinkedHashMap<>();
        mappedSuggestions.forEach(suggestion -> combined.put(dedupeKey(suggestion), suggestion));
        collectFallbackSuggestions(combined, mappedCanonicalValues, genreValues, TagCategory.GENRE, true, result.mediaType());
        collectFallbackSuggestions(combined, mappedCanonicalValues, subjectValues, TagCategory.THEME, false, result.mediaType());

        return combined.values().stream().limit(MAX_SUGGESTIONS).toList();
    }

    private Set<String> collectMappedSuggestions(
            Map<String, ExternalSuggestedTag> suggestions,
            com.moodmatch.entity.ExternalSourceName source,
            String externalField,
            List<ExternalMetadataTagNormalizer.NormalizedExternalValue> values) {
        Set<String> mappedCanonicalValues = new LinkedHashSet<>();
        List<ExternalTagMapping> mappings = externalTagMappingRepository.findBySourceAndFieldAndLowercaseValues(
                source,
                externalField,
                values.stream().flatMap(value -> value.lookupValues().stream()).distinct().toList());

        for (ExternalMetadataTagNormalizer.NormalizedExternalValue value : values) {
            for (ExternalTagMapping mapping :
                    mappings.stream()
                            .filter(candidate -> value.lookupValues().contains(normalizeLookupValue(candidate.getExternalValue())))
                            .toList()) {
                String key = mapping.getTag().getId().toString();
                mappedCanonicalValues.add(value.canonicalValue());
                suggestions.putIfAbsent(
                        key,
                        new ExternalSuggestedTag(
                                mapping.getTag().getId(),
                                mapping.getTag().getName(),
                                mapping.getTag().getCategory(),
                                value.originalValue(),
                                "Mapped from external " + externalField + " value.",
                                mapping.getConfidence()));
            }
        }
        return mappedCanonicalValues;
    }

    private void collectFallbackSuggestions(
            LinkedHashMap<String, ExternalSuggestedTag> suggestions,
            Set<String> mappedCanonicalValues,
            List<ExternalMetadataTagNormalizer.NormalizedExternalValue> values,
            TagCategory category,
            boolean fromGenre,
            MediaType mediaType) {
        for (ExternalMetadataTagNormalizer.NormalizedExternalValue value : values) {
            if (suggestions.size() >= MAX_SUGGESTIONS || mappedCanonicalValues.contains(value.canonicalValue())) {
                continue;
            }
            if (!isUsefulFallbackValue(value, fromGenre, mediaType)) {
                continue;
            }

            ExternalSuggestedTag suggestion = new ExternalSuggestedTag(
                    fallbackId(category, value.canonicalValue()),
                    displayName(value.canonicalValue()),
                    category,
                    value.originalValue(),
                    "Suggested from normalized external metadata.",
                    TagMappingConfidence.LOW);
            suggestions.putIfAbsent(dedupeKey(suggestion), suggestion);
        }
    }

    private boolean isUsefulFallbackValue(
            ExternalMetadataTagNormalizer.NormalizedExternalValue value,
            boolean fromGenre,
            MediaType mediaType) {
        if (fromGenre) {
            return true;
        }
        if (USEFUL_FALLBACK_VALUES.contains(value.canonicalValue())) {
            return true;
        }
        if (mediaType == MediaType.GAME) {
            return value.canonicalValue().equals("open world") || value.canonicalValue().equals("rpg");
        }

        return value.canonicalValue().split(" ").length <= 3
                && value.canonicalValue().length() <= 32
                && value.canonicalValue().matches("[a-z0-9]+(?: [a-z0-9]+)*");
    }

    private String dedupeKey(ExternalSuggestedTag suggestion) {
        return suggestion.tagCategory().name() + "::" + suggestion.tagName().trim().toLowerCase(Locale.ROOT);
    }

    private UUID fallbackId(TagCategory category, String canonicalValue) {
        return UUID.nameUUIDFromBytes(
                ("external-fallback-tag::" + category.name() + "::" + canonicalValue).getBytes(StandardCharsets.UTF_8));
    }

    private String displayName(String canonicalValue) {
        if ("rpg".equals(canonicalValue)) {
            return "RPG";
        }
        if ("science fiction".equals(canonicalValue)) {
            return "Science Fiction";
        }
        String[] words = canonicalValue.split(" ");
        StringBuilder builder = new StringBuilder(canonicalValue.length());
        for (String word : words) {
            if (!builder.isEmpty()) {
                builder.append(" ");
            }
            builder.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.toString();
    }

    private String normalizeLookupValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase(Locale.ROOT);
    }
}
