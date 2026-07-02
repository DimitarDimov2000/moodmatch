package com.moodmatch.external.librivox;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.moodmatch.external.librivox.LibriVoxGateway.LibriVoxAudiobook;
import com.moodmatch.external.librivox.LibriVoxGateway.LibriVoxAuthor;
import com.moodmatch.external.librivox.LibriVoxGateway.LibriVoxGenre;
import com.moodmatch.external.librivox.LibriVoxGateway.LibriVoxReader;
import com.moodmatch.external.librivox.LibriVoxGateway.LibriVoxSection;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

@Mock
@ApplicationScoped
public class TestLibriVoxGateway implements LibriVoxGateway {

    private static final List<LibriVoxAudiobook> DEFAULT_RESULTS = List.of(new LibriVoxAudiobook(
            "253",
            "Pride and Prejudice",
            "Jane Austen's classic novel about wit, family, and first impressions.",
            "English",
            "1813",
            "https://librivox.org/pride-and-prejudice-by-jane-austen/",
            "https://archive.org/covers/pride.jpg",
            "https://archive.org/covers/pride-thumb.jpg",
            List.of(new LibriVoxAuthor("155", "Jane", "Austen")),
            List.of(new LibriVoxGenre("27", "Romance")),
            List.of(
                    new LibriVoxSection("1", List.of(new LibriVoxReader("30", "Annie Coleman Rothenberg"))),
                    new LibriVoxSection("2", List.of(new LibriVoxReader("30", "Annie Coleman Rothenberg"))))));

    private static final AtomicReference<List<LibriVoxAudiobook>> RESULTS = new AtomicReference<>(DEFAULT_RESULTS);

    public static void reset() {
        RESULTS.set(DEFAULT_RESULTS);
    }

    public static void useResults(List<LibriVoxAudiobook> results) {
        RESULTS.set(List.copyOf(results));
    }

    @Override
    public List<LibriVoxAudiobook> searchAudiobooks(String query, int limit) {
        return RESULTS.get().stream().limit(limit).toList();
    }
}
