package com.moodmatch.testsupport;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class YouTubeMissingConfigTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("moodmatch.external.youtube.api-key", "__missing_youtube_config__");
    }
}
