package com.moodmatch.auth;

import com.moodmatch.entity.AppUser;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.NotAuthorizedException;

@RequestScoped
public class AuthenticatedUserContext {

    private AppUser currentUser;

    public AppUser getCurrentUser() {
        if (currentUser == null) {
            throw new NotAuthorizedException("Bearer token required.");
        }

        return currentUser;
    }

    public void setCurrentUser(AppUser currentUser) {
        this.currentUser = currentUser;
    }
}
