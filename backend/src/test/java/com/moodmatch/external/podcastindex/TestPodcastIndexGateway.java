package com.moodmatch.external.podcastindex;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

@Mock
@ApplicationScoped
public class TestPodcastIndexGateway implements PodcastIndexGateway {

    private static final List<PodcastFeed> DEFAULT_RESULTS = List.of(new PodcastFeed(
            75075L,
            "Lex Fridman Podcast",
            "https://lexfridman.com/feed/podcast/",
            "https://lexfridman.com/feed/podcast/",
            "https://lexfridman.com/podcast/",
            "<p>Conversations about science, technology, history, philosophy, and the nature of intelligence.</p>",
            "Lex Fridman",
            "Lex Fridman",
            "https://image.simplecastcdn.com/images/lex-fridman-image.jpg",
            "https://image.simplecastcdn.com/images/lex-fridman.jpg",
            1_719_838_400L,
            "en",
            0,
            "podcast",
            List.of("Technology", "Science")));

    private static final AtomicReference<List<PodcastFeed>> RESULTS = new AtomicReference<>(DEFAULT_RESULTS);

    public static void reset() {
        RESULTS.set(DEFAULT_RESULTS);
    }

    public static void useResults(List<PodcastFeed> results) {
        RESULTS.set(List.copyOf(results));
    }

    @Override
    public List<PodcastFeed> searchShows(String apiKey, String apiSecret, String query, int limit) {
        return RESULTS.get().stream().limit(limit).toList();
    }
}
