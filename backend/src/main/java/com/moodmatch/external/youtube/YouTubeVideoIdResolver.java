package com.moodmatch.external.youtube;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

import com.moodmatch.exception.BusinessRuleViolationException;

final class YouTubeVideoIdResolver {

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{11}$");

    String resolveVideoId(String rawValue) {
        String normalized = blankToNull(rawValue);
        if (normalized == null) {
            throw invalidValue();
        }
        if (VIDEO_ID_PATTERN.matcher(normalized).matches()) {
            return normalized;
        }

        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw invalidValue();
        }

        String host = blankToNull(uri.getHost());
        if (host == null) {
            throw invalidValue();
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (normalizedHost.startsWith("www.")) {
            normalizedHost = normalizedHost.substring(4);
        }
        if (normalizedHost.equals("m.youtube.com")) {
            normalizedHost = "youtube.com";
        }

        if (normalizedHost.equals("youtu.be")) {
            return validateCandidate(firstPathSegment(uri.getPath()));
        }
        if (!normalizedHost.equals("youtube.com")) {
            throw invalidValue();
        }

        String normalizedPath = blankToNull(uri.getPath());
        if (normalizedPath == null) {
            throw invalidValue();
        }
        if ("/watch".equals(normalizedPath)) {
            return validateCandidate(queryValue(uri.getRawQuery(), "v"));
        }
        if (normalizedPath.startsWith("/shorts/")) {
            return validateCandidate(firstPathSegment(normalizedPath.substring("/shorts/".length())));
        }
        if (normalizedPath.startsWith("/embed/")) {
            return validateCandidate(firstPathSegment(normalizedPath.substring("/embed/".length())));
        }

        throw invalidValue();
    }

    private String validateCandidate(String candidate) {
        if (candidate != null && VIDEO_ID_PATTERN.matcher(candidate).matches()) {
            return candidate;
        }
        throw invalidValue();
    }

    private String firstPathSegment(String path) {
        String normalized = blankToNull(path);
        if (normalized == null) {
            return null;
        }
        String withoutLeadingSlash = normalized.startsWith("/") ? normalized.substring(1) : normalized;
        int separatorIndex = withoutLeadingSlash.indexOf('/');
        return separatorIndex >= 0 ? withoutLeadingSlash.substring(0, separatorIndex) : withoutLeadingSlash;
    }

    private String queryValue(String rawQuery, String key) {
        String normalizedQuery = blankToNull(rawQuery);
        if (normalizedQuery == null) {
            return null;
        }

        for (String pair : normalizedQuery.split("&")) {
            int separatorIndex = pair.indexOf('=');
            String candidateKey = separatorIndex >= 0 ? pair.substring(0, separatorIndex) : pair;
            if (!key.equals(candidateKey)) {
                continue;
            }
            String value = separatorIndex >= 0 ? pair.substring(separatorIndex + 1) : "";
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BusinessRuleViolationException invalidValue() {
        return new BusinessRuleViolationException("Enter a valid YouTube URL or video ID.");
    }
}
