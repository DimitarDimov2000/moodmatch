package com.moodmatch.external.rawg;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

@Mock
@ApplicationScoped
public class TestRawgGateway implements RawgGateway {

    private static final List<RawgGame> DEFAULT_RESULTS = List.of(new RawgGame(
            3498,
            "elden-ring",
            "Elden Ring",
            "Rise, Tarnished, and be guided by grace.",
            "2022-02-25",
            "https://media.rawg.io/media/games/elden-ring.jpg",
            List.of("Action", "RPG"),
            List.of("PC", "PlayStation 5"),
            List.of("Singleplayer", "Open World"),
            List.of("FromSoftware"),
            List.of("Bandai Namco Entertainment")));

    private static final AtomicReference<List<RawgGame>> RESULTS = new AtomicReference<>(DEFAULT_RESULTS);

    public static void reset() {
        RESULTS.set(DEFAULT_RESULTS);
    }

    public static void useResults(List<RawgGame> results) {
        RESULTS.set(List.copyOf(results));
    }

    @Override
    public List<RawgGame> searchGames(String apiKey, String query, int limit) {
        return RESULTS.get().stream().limit(limit).toList();
    }
}
