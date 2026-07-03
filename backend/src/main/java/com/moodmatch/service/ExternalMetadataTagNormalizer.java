package com.moodmatch.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ExternalMetadataTagNormalizer {

    private static final int MAX_GENRE_VALUES = 8;
    private static final int MAX_SUBJECT_VALUES = 10;
    private static final int MAX_VALUE_LENGTH = 60;

    private static final Set<String> NOISY_SUBJECT_PREFIXES = Set.of(
            "format:",
            "status:",
            "season:",
            "season year:",
            "language:",
            "explicit:",
            "feed type:",
            "category:",
            "channel:");

    private static final Set<String> NOISY_SUBJECT_VALUES = Set.of(
            "pc",
            "mac",
            "macos",
            "linux",
            "ios",
            "android",
            "playstation",
            "playstation 4",
            "playstation 5",
            "xbox",
            "xbox one",
            "xbox series s x",
            "nintendo switch");

    private static final Map<String, String> CANONICAL_SYNONYMS = Map.ofEntries(
            Map.entry("science fiction", "science fiction"),
            Map.entry("sciencefiction", "science fiction"),
            Map.entry("sci fi", "science fiction"),
            Map.entry("scifi", "science fiction"),
            Map.entry("sci-fi", "science fiction"),
            Map.entry("children", "children"),
            Map.entry("childrens", "children"),
            Map.entry("kid", "children"),
            Map.entry("kids", "children"),
            Map.entry("anime", "anime"),
            Map.entry("manga", "manga"),
            Map.entry("education", "education"),
            Map.entry("educational", "education"),
            Map.entry("technology", "technology"),
            Map.entry("tech", "technology"),
            Map.entry("action", "action"),
            Map.entry("adventure", "adventure"),
            Map.entry("drama", "drama"),
            Map.entry("fantasy", "fantasy"),
            Map.entry("comedy", "comedy"),
            Map.entry("thriller", "thriller"),
            Map.entry("mystery", "mystery"),
            Map.entry("horror", "horror"),
            Map.entry("romance", "romance"),
            Map.entry("rpg", "rpg"),
            Map.entry("role playing", "rpg"),
            Map.entry("roleplaying", "rpg"),
            Map.entry("open world", "open world"),
            Map.entry("openworld", "open world"),
            Map.entry("television", "television"),
            Map.entry("tv", "television"));

    public List<NormalizedExternalValue> normalizeGenres(List<String> values) {
        return normalize(values, MAX_GENRE_VALUES, false);
    }

    public List<NormalizedExternalValue> normalizeSubjects(List<String> values) {
        return normalize(values, MAX_SUBJECT_VALUES, true);
    }

    private List<NormalizedExternalValue> normalize(List<String> values, int maxValues, boolean filterNoisySubjects) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        LinkedHashMap<String, NormalizedExternalValue> byCanonicalValue = new LinkedHashMap<>();
        for (String value : values) {
            String displayValue = normalizeDisplayValue(value);
            if (displayValue == null) {
                continue;
            }
            if (filterNoisySubjects && isNoisySubject(displayValue)) {
                continue;
            }

            String canonicalValue = canonicalValue(displayValue);
            if (canonicalValue == null) {
                continue;
            }

            byCanonicalValue.computeIfAbsent(
                    canonicalValue,
                    ignored -> new NormalizedExternalValue(
                            displayValue,
                            canonicalValue,
                            lookupValues(displayValue, canonicalValue)));
            if (byCanonicalValue.size() >= maxValues) {
                break;
            }
        }

        return List.copyOf(byCanonicalValue.values());
    }

    private String normalizeDisplayValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()
                || normalized.length() > MAX_VALUE_LENGTH
                || normalized.contains("://")
                || normalized.toLowerCase(Locale.ROOT).startsWith("www.")) {
            return null;
        }
        return normalized;
    }

    private boolean isNoisySubject(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (NOISY_SUBJECT_PREFIXES.stream().anyMatch(normalized::startsWith)) {
            return true;
        }

        String canonicalValue = canonicalValue(value);
        return canonicalValue == null || NOISY_SUBJECT_VALUES.contains(canonicalValue);
    }

    private String canonicalValue(String value) {
        String normalized = value.toLowerCase(Locale.ROOT)
                .replace('\u2010', '-')
                .replace('\u2011', '-')
                .replace('\u2012', '-')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('_', ' ')
                .replace('/', ' ')
                .replace('&', ' ')
                .replace("'", "")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s-]", " ")
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isEmpty()) {
            return null;
        }

        String compact = normalized.replace(" ", "");
        String synonym = CANONICAL_SYNONYMS.get(normalized);
        if (synonym != null) {
            return synonym;
        }
        synonym = CANONICAL_SYNONYMS.get(compact);
        if (synonym != null) {
            return synonym;
        }
        return normalized;
    }

    private Set<String> lookupValues(String displayValue, String canonicalValue) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        addLookupValue(values, displayValue);
        addLookupValue(values, canonicalValue);
        addLookupValue(values, canonicalValue.replace(' ', '-'));
        addLookupValue(values, canonicalValue.replace(" ", ""));

        if (Objects.equals(canonicalValue, "science fiction")) {
            addLookupValue(values, "science-fiction");
            addLookupValue(values, "science fiction");
            addLookupValue(values, "sciencefiction");
            addLookupValue(values, "sci-fi");
            addLookupValue(values, "sci fi");
            addLookupValue(values, "scifi");
        } else if (Objects.equals(canonicalValue, "children")) {
            addLookupValue(values, "children");
            addLookupValue(values, "childrens");
            addLookupValue(values, "kid");
            addLookupValue(values, "kids");
        } else if (Objects.equals(canonicalValue, "television")) {
            addLookupValue(values, "television");
            addLookupValue(values, "tv");
        }

        return Set.copyOf(values);
    }

    private void addLookupValue(Set<String> values, String candidate) {
        String normalized = normalizeDisplayValue(candidate);
        if (normalized == null) {
            return;
        }
        values.add(normalized.toLowerCase(Locale.ROOT));
    }

    public record NormalizedExternalValue(
            String originalValue,
            String canonicalValue,
            Set<String> lookupValues) {}
}
