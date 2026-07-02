package com.moodmatch.external.podcastindex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PodcastIndexAuthHeadersTest {

    @Test
    void shouldBuildDeterministicPodcastIndexAuthHeaders() {
        PodcastIndexAuthHeaders headers = PodcastIndexAuthHeaders.create("test-key", "test-secret", 1_700_000_000L);

        assertEquals("test-key", headers.apiKey());
        assertEquals("1700000000", headers.authDate());
        assertEquals("2782ad65bd878a76107dd3f1cdbfabe647607c5d", headers.authorization());
        assertEquals("MoodMatch/1.0", headers.userAgent());
    }
}
