package com.moodmatch.testsupport;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class LocalPasswordAuthTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.datasource.jdbc.url",
                "jdbc:h2:mem:moodmatch-auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
                "quarkus.oidc.enabled", "false",
                "moodmatch.auth.mode", "local-password",
                "moodmatch.auth.token.ttl-hours", "24",
                "moodmatch.private-endpoint-policy", "permit",
                "moodmatch.auth.private-endpoint-policy", "permit",
                "quarkus.http.auth.permission.moodmatch-private.policy", "permit");
    }
}
