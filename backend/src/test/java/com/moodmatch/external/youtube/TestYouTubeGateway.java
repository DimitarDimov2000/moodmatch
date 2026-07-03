package com.moodmatch.external.youtube;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

@Mock
@ApplicationScoped
public class TestYouTubeGateway implements YouTubeGateway {

    private static final YouTubeVideo DEFAULT_VIDEO = new YouTubeVideo(
            "abc123XYZ_0",
            "VueConf 2024 Keynote",
            "A practical keynote about building resilient frontend systems.",
            "MoodMatch Dev",
            "2024-05-20T10:30:00Z",
            "27",
            java.util.List.of("Vue 3", "Tutorial", "Frontend"),
            new ThumbnailSet(
                    "https://img.youtube.test/default.jpg",
                    "https://img.youtube.test/medium.jpg",
                    "https://img.youtube.test/high.jpg",
                    "https://img.youtube.test/standard.jpg",
                    "https://img.youtube.test/maxres.jpg"));

    private static final AtomicReference<Optional<YouTubeVideo>> VIDEO =
            new AtomicReference<>(Optional.of(DEFAULT_VIDEO));
    private static final AtomicReference<java.util.List<YouTubeVideo>> SEARCH_RESULTS =
            new AtomicReference<>(java.util.List.of(DEFAULT_VIDEO));
    private static final AtomicReference<Map<String, String>> CATEGORY_LABELS =
            new AtomicReference<>(Map.of("27", "Education"));
    private static final AtomicReference<String> LAST_SEARCH_ORDER = new AtomicReference<>("relevance");
    private static final AtomicReference<String> LAST_FETCHED_VIDEO_ID = new AtomicReference<>(DEFAULT_VIDEO.id());
    private static final AtomicReference<RuntimeException> FETCH_VIDEO_EXCEPTION = new AtomicReference<>();

    public static void reset() {
        VIDEO.set(Optional.of(DEFAULT_VIDEO));
        SEARCH_RESULTS.set(java.util.List.of(DEFAULT_VIDEO));
        CATEGORY_LABELS.set(Map.of("27", "Education"));
        LAST_SEARCH_ORDER.set("relevance");
        LAST_FETCHED_VIDEO_ID.set(DEFAULT_VIDEO.id());
        FETCH_VIDEO_EXCEPTION.set(null);
    }

    public static void useVideo(YouTubeVideo video) {
        VIDEO.set(Optional.ofNullable(video));
    }

    public static void useSearchResults(java.util.List<YouTubeVideo> results) {
        SEARCH_RESULTS.set(java.util.List.copyOf(results));
    }

    public static void useMissingVideo() {
        VIDEO.set(Optional.empty());
    }

    public static void useCategoryLabels(Map<String, String> categoryLabels) {
        CATEGORY_LABELS.set(Map.copyOf(categoryLabels));
    }

    public static String lastSearchOrder() {
        return LAST_SEARCH_ORDER.get();
    }

    public static String lastFetchedVideoId() {
        return LAST_FETCHED_VIDEO_ID.get();
    }

    public static void useVideoFetchFailure(RuntimeException exception) {
        FETCH_VIDEO_EXCEPTION.set(exception);
    }

    @Override
    public Optional<YouTubeVideo> fetchVideo(String apiKey, String videoId) {
        LAST_FETCHED_VIDEO_ID.set(videoId);
        RuntimeException exception = FETCH_VIDEO_EXCEPTION.get();
        if (exception != null) {
            throw exception;
        }
        return VIDEO.get().filter(video -> videoId.equals(video.id()));
    }

    @Override
    public java.util.List<YouTubeVideo> searchVideos(String apiKey, String query, int maxResults, String order) {
        LAST_SEARCH_ORDER.set(order);
        return SEARCH_RESULTS.get().stream().limit(maxResults).toList();
    }

    @Override
    public Optional<String> fetchCategoryLabel(String apiKey, String categoryId) {
        return Optional.ofNullable(CATEGORY_LABELS.get().get(categoryId));
    }
}
