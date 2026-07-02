package com.moodmatch.external.demo;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.moodmatch.entity.MediaType;
import com.moodmatch.external.adapter.ExternalSearchProvider;
import com.moodmatch.external.adapter.ExternalSearchRequest;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;
import com.moodmatch.external.adapter.ExternalSourceMappings;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DemoExternalSearchProvider implements ExternalSearchProvider {

    static final String ATTRIBUTION = "MoodMatch Demo Provider (offline)";

    private static final Set<MediaType> SUPPORTED_MEDIA_TYPES = Set.of(MediaType.values());
    private static final List<DemoCatalogEntry> CATALOG = List.of(
            new DemoCatalogEntry(
                    "demo-film-arrival",
                    MediaType.FILM,
                    "Arrival",
                    null,
                    List.of(),
                    "A linguist races to understand visitors whose arrival changes how humanity thinks about time.",
                    2016,
                    "https://demo.moodmatch.local/covers/arrival.jpg",
                    "https://demo.moodmatch.local/items/demo-film-arrival",
                    List.of("Science-Fiction", "Drama"),
                    List.of("Zeit", "Identitaet", "Entdeckung")),
            new DemoCatalogEntry(
                    "demo-series-dark",
                    MediaType.SERIES,
                    "Dark",
                    null,
                    List.of(),
                    "A small-town mystery unravels across generations, time loops, and family secrets.",
                    2017,
                    "https://demo.moodmatch.local/covers/dark.jpg",
                    "https://demo.moodmatch.local/items/demo-series-dark",
                    List.of("Science-Fiction", "Mystery"),
                    List.of("Zeit", "Familie", "duester")),
            new DemoCatalogEntry(
                    "demo-book-dune",
                    MediaType.BOOK,
                    "Dune",
                    null,
                    List.of("Frank Herbert"),
                    "A sprawling desert saga about prophecy, power, survival, and political destiny.",
                    1965,
                    "https://demo.moodmatch.local/covers/dune.jpg",
                    "https://demo.moodmatch.local/items/demo-book-dune",
                    List.of("Science-Fiction", "Adventure"),
                    List.of("Macht", "Ueberleben", "Fantasiewelt")),
            new DemoCatalogEntry(
                    "demo-game-outer-wilds",
                    MediaType.GAME,
                    "Outer Wilds",
                    null,
                    List.of(),
                    "An open exploration game about curiosity, wonder, and a repeating solar-system mystery.",
                    2019,
                    "https://demo.moodmatch.local/covers/outer-wilds.jpg",
                    "https://demo.moodmatch.local/items/demo-game-outer-wilds",
                    List.of("Adventure"),
                    List.of("Entdeckung", "Staunen", "Weltraum")),
            new DemoCatalogEntry(
                    "demo-film-severance-preview",
                    MediaType.FILM,
                    "Severance Preview Reel",
                    null,
                    List.of(),
                    "A fictional preview entry used to prove deterministic title matching in the demo adapter.",
                    2022,
                    "https://demo.moodmatch.local/covers/severance-preview.jpg",
                    "https://demo.moodmatch.local/items/demo-film-severance-preview",
                    List.of("Drama"),
                    List.of("Identitaet", "Grossstadt", "nachdenklich")));

    @Override
    public ExternalSearchSourceName sourceName() {
        return ExternalSearchSourceName.DEMO;
    }

    @Override
    public Set<MediaType> supportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }

    @Override
    public List<ExternalSearchResult> search(ExternalSearchRequest request) {
        String normalizedQuery = request.query().trim().toLowerCase(Locale.ROOT);

        return CATALOG.stream()
                .filter(entry -> entry.mediaType() == request.mediaType())
                .map(entry -> new RankedResult(toScore(entry, normalizedQuery), toResult(entry)))
                .filter(rankedResult -> rankedResult.score() > 0)
                .sorted(Comparator.comparingInt(RankedResult::score)
                        .reversed()
                        .thenComparing(rankedResult -> rankedResult.result().title(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(rankedResult -> rankedResult.result().externalId(), String.CASE_INSENSITIVE_ORDER))
                .limit(request.limit())
                .map(RankedResult::result)
                .toList();
    }

    private int toScore(DemoCatalogEntry entry, String normalizedQuery) {
        String title = entry.title().toLowerCase(Locale.ROOT);
        if (title.startsWith(normalizedQuery)) {
            return 3;
        }
        if (title.contains(normalizedQuery)) {
            return 2;
        }

        if (containsIgnoreCase(entry.description(), normalizedQuery)
                || entry.externalGenres().stream().anyMatch(value -> containsIgnoreCase(value, normalizedQuery))
                || entry.externalSubjects().stream().anyMatch(value -> containsIgnoreCase(value, normalizedQuery))) {
            return 1;
        }

        return 0;
    }

    private boolean containsIgnoreCase(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private ExternalSearchResult toResult(DemoCatalogEntry entry) {
        return new ExternalSearchResult(
                ExternalSearchSourceName.DEMO,
                ExternalSourceMappings.toMappingSource(ExternalSearchSourceName.DEMO, entry.mediaType()),
                entry.externalId(),
                entry.mediaType(),
                entry.title(),
                entry.originalTitle(),
                List.copyOf(entry.creatorNames()),
                entry.description(),
                entry.releaseYear(),
                entry.coverUrl(),
                entry.sourceUrl(),
                List.copyOf(entry.externalGenres()),
                List.copyOf(entry.externalSubjects()),
                List.of(),
                ATTRIBUTION,
                List.of());
    }

    private record DemoCatalogEntry(
            String externalId,
            MediaType mediaType,
            String title,
            String originalTitle,
            List<String> creatorNames,
            String description,
            Integer releaseYear,
            String coverUrl,
            String sourceUrl,
            List<String> externalGenres,
            List<String> externalSubjects) {}

    private record RankedResult(int score, ExternalSearchResult result) {}
}
