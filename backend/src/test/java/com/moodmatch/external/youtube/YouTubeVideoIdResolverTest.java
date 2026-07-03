package com.moodmatch.external.youtube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.moodmatch.exception.BusinessRuleViolationException;

class YouTubeVideoIdResolverTest {

    private final YouTubeVideoIdResolver resolver = new YouTubeVideoIdResolver();

    @Test
    void shouldResolveCommonYouTubeWatchUrlsShortUrlsShortsAndRawIds() {
        assertEquals("abc123XYZ_0", resolver.resolveVideoId("https://www.youtube.com/watch?v=abc123XYZ_0"));
        assertEquals("abc123XYZ_0", resolver.resolveVideoId("https://www.youtube.com/watch?v=abc123XYZ_0&feature=share"));
        assertEquals("abc123XYZ_0", resolver.resolveVideoId("https://youtu.be/abc123XYZ_0?t=15"));
        assertEquals("abc123XYZ_0", resolver.resolveVideoId("https://youtu.be/abc123XYZ_0/"));
        assertEquals("abc123XYZ_0", resolver.resolveVideoId("https://youtube.com/shorts/abc123XYZ_0"));
        assertEquals("abc123XYZ_0", resolver.resolveVideoId("https://www.youtube.com/shorts/abc123XYZ_0?si=share"));
        assertEquals("abc123XYZ_0", resolver.resolveVideoId("https://www.youtube.com/embed/abc123XYZ_0"));
        assertEquals("abc123XYZ_0", resolver.resolveVideoId("abc123XYZ_0"));
        assertEquals("abc123XYZ_0", resolver.resolveVideoId("  abc123XYZ_0  "));
    }

    @Test
    void shouldRejectInvalidOrUnsupportedValues() {
        BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                () -> resolver.resolveVideoId("https://example.com/watch?v=abc123XYZ_0"));

        assertEquals("Enter a valid YouTube URL or video ID.", exception.getMessage());
    }
}
