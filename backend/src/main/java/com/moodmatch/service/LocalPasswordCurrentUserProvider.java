package com.moodmatch.service;

import com.moodmatch.auth.AuthenticatedUserContext;
import com.moodmatch.entity.AppUser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LocalPasswordCurrentUserProvider {

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    public AppUser getCurrentUser() {
        return authenticatedUserContext.getCurrentUser();
    }
}
