package com.moodmatch.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.moodmatch.entity.ExternalTagMapping;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSuggestedTag;
import com.moodmatch.repository.ExternalTagMappingRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class ExternalTagSuggestionService {

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

        collectSuggestions(
                suggestions,
                result.mappingSource(),
                "genre",
                externalMetadataTagNormalizer.normalizeGenres(result.externalGenres()));
        collectSuggestions(
                suggestions,
                result.mappingSource(),
                "subject",
                externalMetadataTagNormalizer.normalizeSubjects(result.externalSubjects()));

        return suggestions.values().stream()
                .sorted(Comparator.comparing(ExternalSuggestedTag::tagCategory)
                        .thenComparing(ExternalSuggestedTag::tagName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ExternalSuggestedTag::sourceValue, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ExternalSuggestedTag::tagId))
                .toList();
    }

    private void collectSuggestions(
            Map<String, ExternalSuggestedTag> suggestions,
            com.moodmatch.entity.ExternalSourceName source,
            String externalField,
            List<ExternalMetadataTagNormalizer.NormalizedExternalValue> values) {
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
    }

    private String normalizeLookupValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase(Locale.ROOT);
    }
}
