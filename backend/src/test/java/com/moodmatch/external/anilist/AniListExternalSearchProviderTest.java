package com.moodmatch.external.anilist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.moodmatch.entity.MediaType;
import com.moodmatch.external.adapter.ExternalSearchRequest;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;

class AniListExternalSearchProviderTest {

    @Test
    void shouldMapAnimeMovieSearchResultsIntoFilmResults() {
        AniListExternalSearchProvider provider = providerWith(List.of(animeMovie()));

        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("spirited away", MediaType.FILM, ExternalSearchSourceName.ANILIST, 5));

        assertEquals(1, results.size());
        ExternalSearchResult result = results.getFirst();
        assertEquals(ExternalSearchSourceName.ANILIST, result.source());
        assertEquals("199", result.externalId());
        assertEquals(MediaType.FILM, result.mediaType());
        assertEquals("Sen to Chihiro no Kamikakushi", result.title());
        assertEquals("千と千尋の神隠し", result.originalTitle());
        assertEquals(List.of("Studio Ghibli"), result.creatorNames());
        assertEquals("A young girl enters a world of spirits & witches.", result.description());
        assertEquals(2001, result.releaseYear());
        assertEquals("https://img.anilist.co/large.jpg", result.coverUrl());
        assertEquals("https://anilist.co/anime/199", result.sourceUrl());
        assertEquals(List.of("Adventure", "Fantasy"), result.externalGenres());
        assertEquals(List.of("Format: MOVIE", "Status: FINISHED", "Supernatural", "Coming of Age"),
                result.externalSubjects());
        assertEquals("Metadata from AniList", result.attribution());
    }

    @Test
    void shouldMapAnimeSeriesFormatsIntoSeriesResults() {
        AniListExternalSearchProvider provider = providerWith(List.of(animeSeries()));

        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("attack on titan", MediaType.SERIES, ExternalSearchSourceName.ANILIST, 5));

        assertEquals(1, results.size());
        ExternalSearchResult result = results.getFirst();
        assertEquals(MediaType.SERIES, result.mediaType());
        assertEquals("Shingeki no Kyojin", result.title());
        assertEquals(List.of("Wit Studio"), result.creatorNames());
        assertEquals(List.of("Format: TV", "Status: FINISHED", "Season: SPRING 2013", "Survival"),
                result.externalSubjects());
    }

    @Test
    void shouldFetchExtraAniListResultsAndFilterToRequestedAnimeSeries() {
        CapturingAniListGateway gateway = new CapturingAniListGateway(List.of(animeMovie(), animeMusicVideo(), animeSeries()));
        AniListExternalSearchProvider provider = new AniListExternalSearchProvider();
        provider.aniListGateway = gateway;

        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("demon slayer", MediaType.SERIES, ExternalSearchSourceName.ANILIST, 1));

        assertTrue(gateway.lastLimit() > 1);
        assertEquals(1, results.size());
        assertEquals(MediaType.SERIES, results.getFirst().mediaType());
        assertEquals("Shingeki no Kyojin", results.getFirst().title());
    }

    @Test
    void shouldFilterAnimeMovieSearchResultsToFilm() {
        AniListExternalSearchProvider provider = providerWith(List.of(animeSeries(), animeMovie()));

        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("spirited away", MediaType.FILM, ExternalSearchSourceName.ANILIST, 5));

        assertEquals(1, results.size());
        assertEquals(MediaType.FILM, results.getFirst().mediaType());
        assertEquals("Sen to Chihiro no Kamikakushi", results.getFirst().title());
    }

    @Test
    void shouldMapAniListMusicFormatAsSeriesWhenReturnedByAnimeSearch() {
        AniListExternalSearchProvider provider = providerWith(List.of(animeMusicVideo()));

        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("anime music clip", MediaType.SERIES, ExternalSearchSourceName.ANILIST, 5));

        assertEquals(1, results.size());
        assertEquals(MediaType.SERIES, results.getFirst().mediaType());
        assertEquals("Anime Music Clip", results.getFirst().title());
    }

    @Test
    void shouldMapMangaAndLightNovelFormatsIntoBookResults() {
        AniListExternalSearchProvider provider = providerWith(List.of(manga(), novel()));

        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("berserk", MediaType.BOOK, ExternalSearchSourceName.ANILIST, 5));

        assertEquals(2, results.size());
        assertEquals(MediaType.BOOK, results.getFirst().mediaType());
        assertEquals("Berserk", results.getFirst().title());
        assertEquals(List.of("Kentaro Miura"), results.getFirst().creatorNames());
        assertEquals("Format: MANGA", results.getFirst().externalSubjects().getFirst());
        assertEquals(MediaType.BOOK, results.get(1).mediaType());
        assertEquals("Format: NOVEL", results.get(1).externalSubjects().getFirst());
    }

    @Test
    void shouldStripHtmlDescriptions() {
        AniListExternalSearchProvider provider = new AniListExternalSearchProvider();

        assertEquals("Line one. Line two & more.", provider.stripHtml("<p>Line one.</p><br><i>Line two &amp; more.</i>"));
        assertNull(provider.stripHtml(" "));
    }

    @Test
    void shouldIgnoreUnsupportedAniListTypesAndReturnEmptyResults() {
        AniListExternalSearchProvider provider = providerWith(List.of(new AniListGateway.AniListMedia(
                1,
                "UNKNOWN",
                "TV",
                "FINISHED",
                null,
                null,
                2020,
                new AniListGateway.AniListTitle("Music Video", null, null),
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of())));

        List<ExternalSearchResult> results = provider.search(
                new ExternalSearchRequest("unknown", MediaType.SERIES, ExternalSearchSourceName.ANILIST, 5));

        assertTrue(results.isEmpty());
    }

    private AniListExternalSearchProvider providerWith(List<AniListGateway.AniListMedia> media) {
        AniListExternalSearchProvider provider = new AniListExternalSearchProvider();
        provider.aniListGateway = (mediaType, query, limit) -> media.stream().limit(limit).toList();
        return provider;
    }

    private AniListGateway.AniListMedia animeMovie() {
        return new AniListGateway.AniListMedia(
                199,
                "ANIME",
                "MOVIE",
                "FINISHED",
                null,
                null,
                2001,
                new AniListGateway.AniListTitle("Sen to Chihiro no Kamikakushi", "Spirited Away", "千と千尋の神隠し"),
                "<p>A young girl enters a world of <b>spirits</b> &amp; witches.</p>",
                new AniListGateway.AniListCoverImage("https://img.anilist.co/large.jpg", "https://img.anilist.co/medium.jpg"),
                "https://anilist.co/anime/199",
                List.of("Adventure", "Fantasy"),
                List.of("Supernatural", "Coming of Age"),
                List.of("Studio Ghibli"),
                List.of("Hayao Miyazaki"));
    }

    private AniListGateway.AniListMedia animeSeries() {
        return new AniListGateway.AniListMedia(
                16498,
                "ANIME",
                "TV",
                "FINISHED",
                "SPRING",
                2013,
                2013,
                new AniListGateway.AniListTitle("Shingeki no Kyojin", "Attack on Titan", "進撃の巨人"),
                "Humanity fights giants.",
                new AniListGateway.AniListCoverImage(null, "https://img.anilist.co/aot.jpg"),
                "https://anilist.co/anime/16498",
                List.of("Action", "Drama"),
                List.of("Survival"),
                List.of("Wit Studio"),
                List.of());
    }

    private AniListGateway.AniListMedia animeMusicVideo() {
        return new AniListGateway.AniListMedia(
                999,
                "ANIME",
                "MUSIC",
                "FINISHED",
                null,
                null,
                2020,
                new AniListGateway.AniListTitle("Anime Music Clip", null, null),
                "A short anime music format item.",
                null,
                "https://anilist.co/anime/999",
                List.of("Music"),
                List.of(),
                List.of("Animation Studio"),
                List.of());
    }

    private AniListGateway.AniListMedia manga() {
        return new AniListGateway.AniListMedia(
                30002,
                "MANGA",
                "MANGA",
                "RELEASING",
                null,
                null,
                1989,
                new AniListGateway.AniListTitle("Berserk", null, "ベルセルク"),
                "A dark fantasy manga.",
                null,
                "https://anilist.co/manga/30002",
                List.of("Action", "Fantasy"),
                List.of("Seinen"),
                List.of(),
                List.of("Kentaro Miura"));
    }

    private AniListGateway.AniListMedia novel() {
        return new AniListGateway.AniListMedia(
                1,
                "MANGA",
                "NOVEL",
                "FINISHED",
                null,
                null,
                2006,
                new AniListGateway.AniListTitle("Spice and Wolf", null, "狼と香辛料"),
                "A light novel.",
                null,
                "https://anilist.co/manga/1",
                List.of("Adventure"),
                List.of("Economics"),
                List.of(),
                List.of("Isuna Hasekura"));
    }

    private static final class CapturingAniListGateway implements AniListGateway {

        private final List<AniListMedia> results;
        private int lastLimit;

        private CapturingAniListGateway(List<AniListMedia> results) {
            this.results = results;
        }

        @Override
        public List<AniListMedia> searchMedia(MediaType mediaType, String query, int limit) {
            lastLimit = limit;
            return results.stream().limit(limit).toList();
        }

        private int lastLimit() {
            return lastLimit;
        }
    }
}
