package com.moodmatch.service;

import java.util.UUID;

import com.moodmatch.entity.AppUser;
import com.moodmatch.entity.AuthProvider;
import com.moodmatch.repository.AppUserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class LocalDemoCurrentUserProvider {

    static final UUID LOCAL_DEMO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final AuthProvider LOCAL_DEMO_PROVIDER = AuthProvider.LOCAL;
    static final String LOCAL_DEMO_PROVIDER_SUBJECT = "local-demo-user";
    static final String LOCAL_DEMO_EMAIL = "local-demo@example.local";
    static final String LOCAL_DEMO_DISPLAY_NAME = "Local Demo User";

    @Inject
    AppUserRepository appUserRepository;

    @Inject
    LocalDemoUserIdentitySource localDemoUserIdentitySource;

    @Transactional
    public AppUser getCurrentUser() {
        LocalDemoUserIdentity identity = localDemoUserIdentitySource.getIdentity();
        return appUserRepository
                .findByProviderAndProviderSubject(identity.provider(), identity.providerSubject())
                .orElseGet(() -> createLocalDemoUser(identity));
    }

    private AppUser createLocalDemoUser(LocalDemoUserIdentity identity) {
        // Development/test fallback for flows that do not need a real local password session.
        AppUser user = new AppUser();
        user.setId(identity.id());
        user.setProvider(identity.provider());
        user.setProviderSubject(identity.providerSubject());
        user.setEmail(identity.email());
        user.setDisplayName(identity.displayName());
        appUserRepository.persist(user);
        return user;
    }
}
