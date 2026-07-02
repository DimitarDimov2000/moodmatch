package com.moodmatch.service;

public enum AuthMode {
    LOCAL_DEMO("local-demo"),
    OIDC("oidc");

    private final String configValue;

    AuthMode(String configValue) {
        this.configValue = configValue;
    }

    public static AuthMode fromConfig(String rawValue) {
        for (AuthMode mode : values()) {
            if (mode.configValue.equalsIgnoreCase(rawValue)) {
                return mode;
            }
        }

        throw new IllegalArgumentException("Unsupported moodmatch.auth.mode: " + rawValue);
    }
}
