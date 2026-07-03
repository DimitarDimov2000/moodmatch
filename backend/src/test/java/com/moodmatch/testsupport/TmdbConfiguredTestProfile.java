package com.moodmatch.testsupport;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class TmdbConfiguredTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("moodmatch.external.tmdb.api-key", "test-tmdb-key");
    }
}
