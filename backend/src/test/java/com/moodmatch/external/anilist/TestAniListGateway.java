package com.moodmatch.external.anilist;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.moodmatch.entity.MediaType;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

@Mock
@ApplicationScoped
public class TestAniListGateway implements AniListGateway {

    private static final List<AniListMedia> DEFAULT_RESULTS = List.of(
            new AniListMedia(
                    16498,
                    "ANIME",
                    "TV",
                    "FINISHED",
                    "SPRING",
                    2013,
                    2013,
                    new AniListTitle("Shingeki no Kyojin", "Attack on Titan", "進撃の巨人"),
                    "<p>Humanity fights titans beyond the walls.</p>",
                    new AniListCoverImage("https://img.anilist.co/aot-large.jpg", "https://img.anilist.co/aot-medium.jpg"),
                    "https://anilist.co/anime/16498",
                    List.of("Action", "Drama"),
                    List.of("Survival", "Military"),
                    List.of("Wit Studio"),
                    List.of()),
            new AniListMedia(
                    30002,
                    "MANGA",
                    "MANGA",
                    "RELEASING",
                    null,
                    null,
                    1989,
                    new AniListTitle("Berserk", null, "ベルセルク"),
                    "<p>A dark fantasy manga.</p>",
                    new AniListCoverImage("https://img.anilist.co/berserk-large.jpg", null),
                    "https://anilist.co/manga/30002",
                    List.of("Action", "Fantasy"),
                    List.of("Seinen", "Revenge"),
                    List.of(),
                    List.of("Kentaro Miura")));

    private static final AtomicReference<List<AniListMedia>> RESULTS = new AtomicReference<>(DEFAULT_RESULTS);

    public static void reset() {
        RESULTS.set(DEFAULT_RESULTS);
    }

    public static void useResults(List<AniListMedia> results) {
        RESULTS.set(List.copyOf(results));
    }

    @Override
    public List<AniListMedia> searchMedia(MediaType mediaType, String query, int limit) {
        return RESULTS.get().stream()
                .filter(item -> mediaType == MediaType.BOOK ? "MANGA".equals(item.type()) : "ANIME".equals(item.type()))
                .limit(limit)
                .toList();
    }
}
