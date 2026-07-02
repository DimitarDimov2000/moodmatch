package com.moodmatch.testsupport;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class OidcAuthTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "moodmatch.auth.mode", "oidc",
                "moodmatch.auth.private-endpoint-policy", "authenticated",
                "moodmatch.auth.oidc.enabled", "false",
                "moodmatch.auth.oidc.provider", "OIDC",
                "quarkus.oidc.auth-server-url", "http://localhost/oidc-disabled",
                "quarkus.oidc.client-id", "moodmatch-test-client",
                "quarkus.oidc.token.audience", "moodmatch-test-client");
    }
}
