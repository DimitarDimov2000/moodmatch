package com.moodmatch.service;

import com.moodmatch.entity.AppUser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ModeAwareCurrentUserProvider implements CurrentUserProvider {

    @Inject
    MoodMatchAuthConfiguration authConfiguration;

    @Inject
    LocalDemoCurrentUserProvider localDemoCurrentUserProvider;

    @Inject
    OidcCurrentUserProvider oidcCurrentUserProvider;

    @Inject
    LocalPasswordCurrentUserProvider localPasswordCurrentUserProvider;

    @Override
    public AppUser getCurrentUser() {
        return switch (authConfiguration.getAuthMode()) {
            case LOCAL_DEMO -> localDemoCurrentUserProvider.getCurrentUser();
            case LOCAL_PASSWORD -> localPasswordCurrentUserProvider.getCurrentUser();
            case OIDC -> oidcCurrentUserProvider.getCurrentUser();
        };
    }
}
