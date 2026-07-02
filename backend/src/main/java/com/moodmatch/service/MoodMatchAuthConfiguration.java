package com.moodmatch.service;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.moodmatch.entity.AuthProvider;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MoodMatchAuthConfiguration {

    @ConfigProperty(name = "moodmatch.auth.mode", defaultValue = "local-password")
    String authMode;

    @ConfigProperty(name = "moodmatch.auth.oidc.provider", defaultValue = "OIDC")
    AuthProvider oidcProvider;

    public AuthMode getAuthMode() {
        return AuthMode.fromConfig(authMode);
    }

    public boolean isLocalDemoMode() {
        return getAuthMode() == AuthMode.LOCAL_DEMO;
    }

    public boolean isOidcMode() {
        return getAuthMode() == AuthMode.OIDC;
    }

    public boolean isLocalPasswordMode() {
        return getAuthMode() == AuthMode.LOCAL_PASSWORD;
    }

    public AuthProvider getOidcProvider() {
        return oidcProvider;
    }
}
