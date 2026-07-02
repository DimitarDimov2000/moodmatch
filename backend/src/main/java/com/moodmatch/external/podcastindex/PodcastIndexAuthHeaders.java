package com.moodmatch.external.podcastindex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

record PodcastIndexAuthHeaders(
        String apiKey,
        String authDate,
        String authorization,
        String userAgent) {

    static final String DEFAULT_USER_AGENT = "MoodMatch/1.0";

    static PodcastIndexAuthHeaders create(String apiKey, String apiSecret, long epochSeconds) {
        String authDate = String.valueOf(epochSeconds);
        return new PodcastIndexAuthHeaders(
                apiKey,
                authDate,
                sha1Hex(apiKey + apiSecret + authDate),
                DEFAULT_USER_AGENT);
    }

    private static String sha1Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is not available.", exception);
        }
    }
}
