package com.moodmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ExternalMetadataTagNormalizerTest {

    private final ExternalMetadataTagNormalizer normalizer = new ExternalMetadataTagNormalizer();

    @Test
    void shouldCanonicalizeSynonymsAndCollapseDuplicates() {
        List<ExternalMetadataTagNormalizer.NormalizedExternalValue> values = normalizer.normalizeGenres(List.of(
                "  Sci-Fi  ",
                "science-fiction",
                "Science Fiction",
                "Kids",
                "children",
                "TV",
                "television"));

        assertEquals(3, values.size());
        assertIterableEquals(
                List.of("science fiction", "children", "television"),
                values.stream().map(ExternalMetadataTagNormalizer.NormalizedExternalValue::canonicalValue).toList());
        assertTrue(values.getFirst().lookupValues().contains("science-fiction"));
        assertTrue(values.getFirst().lookupValues().contains("sci-fi"));
        assertTrue(values.get(1).lookupValues().contains("kids"));
        assertTrue(values.get(2).lookupValues().contains("tv"));
    }

    @Test
    void shouldDropNoisySubjectsAndKeepOnlyUsefulValues() {
        List<ExternalMetadataTagNormalizer.NormalizedExternalValue> values = normalizer.normalizeSubjects(List.of(
                "Format: TV",
                "Status: FINISHED",
                " Season: Spring 2013 ",
                "Survival",
                "Kids",
                "Language: en",
                "television"));

        assertIterableEquals(
                List.of("survival", "children", "television"),
                values.stream().map(ExternalMetadataTagNormalizer.NormalizedExternalValue::canonicalValue).toList());
    }
}
