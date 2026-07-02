package com.moodmatch.service;

import java.util.UUID;

import com.moodmatch.entity.AppUser;
import com.moodmatch.entity.AuthProvider;
import com.moodmatch.repository.AppUserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class LocalDemoCurrentUserProvider implements CurrentUserProvider {

    static final UUID LOCAL_DEMO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final AuthProvider LOCAL_DEMO_PROVIDER = AuthProvider.LOCAL;
    static final String LOCAL_DEMO_PROVIDER_SUBJECT = "local-demo-user";
    static final String LOCAL_DEMO_EMAIL = "local-demo@example.local";
    static final String LOCAL_DEMO_DISPLAY_NAME = "Local Demo User";

    @Inject
    AppUserRepository appUserRepository;

    @Override
    @Transactional
    public AppUser getCurrentUser() {
        return appUserRepository
                .findByProviderAndProviderSubject(LOCAL_DEMO_PROVIDER, LOCAL_DEMO_PROVIDER_SUBJECT)
                .orElseGet(this::createLocalDemoUser);
    }

    private AppUser createLocalDemoUser() {
        // Temporary development fallback until real OIDC/Google auth resolves the current user.
        AppUser user = new AppUser();
        user.setId(LOCAL_DEMO_USER_ID);
        user.setProvider(LOCAL_DEMO_PROVIDER);
        user.setProviderSubject(LOCAL_DEMO_PROVIDER_SUBJECT);
        user.setEmail(LOCAL_DEMO_EMAIL);
        user.setDisplayName(LOCAL_DEMO_DISPLAY_NAME);
        appUserRepository.persist(user);
        return user;
    }
}
