package com.moodmatch.service;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.moodmatch.entity.AppUser;
import com.moodmatch.entity.AuthProvider;
import com.moodmatch.repository.AppUserRepository;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@Mock
@ApplicationScoped
public class TestCurrentUserProvider implements CurrentUserProvider {

    private static final TestUserIdentity LOCAL_DEMO_USER = new TestUserIdentity(
            LocalDemoCurrentUserProvider.LOCAL_DEMO_USER_ID,
            LocalDemoCurrentUserProvider.LOCAL_DEMO_PROVIDER,
            LocalDemoCurrentUserProvider.LOCAL_DEMO_PROVIDER_SUBJECT,
            LocalDemoCurrentUserProvider.LOCAL_DEMO_EMAIL,
            LocalDemoCurrentUserProvider.LOCAL_DEMO_DISPLAY_NAME);
    private static final TestUserIdentity USER_A = new TestUserIdentity(
            UUID.fromString("10000000-0000-0000-0000-000000000001"),
            AuthProvider.OIDC,
            "test-user-a",
            "user-a@example.local",
            "Test User A");
    private static final TestUserIdentity USER_B = new TestUserIdentity(
            UUID.fromString("20000000-0000-0000-0000-000000000001"),
            AuthProvider.OIDC,
            "test-user-b",
            "user-b@example.local",
            "Test User B");

    private static final AtomicReference<TestUserIdentity> ACTIVE_USER = new AtomicReference<>(LOCAL_DEMO_USER);

    @Inject
    AppUserRepository appUserRepository;

    public static void useLocalDemoUser() {
        ACTIVE_USER.set(LOCAL_DEMO_USER);
    }

    public static void useUserA() {
        ACTIVE_USER.set(USER_A);
    }

    public static void useUserB() {
        ACTIVE_USER.set(USER_B);
    }

    @Override
    @Transactional
    public AppUser getCurrentUser() {
        TestUserIdentity identity = ACTIVE_USER.get();
        return appUserRepository
                .findByProviderAndProviderSubject(identity.provider(), identity.providerSubject())
                .orElseGet(() -> createUser(identity));
    }

    private AppUser createUser(TestUserIdentity identity) {
        AppUser user = new AppUser();
        user.setId(identity.id());
        user.setProvider(identity.provider());
        user.setProviderSubject(identity.providerSubject());
        user.setEmail(identity.email());
        user.setDisplayName(identity.displayName());
        appUserRepository.persist(user);
        return user;
    }

    private record TestUserIdentity(
            UUID id, AuthProvider provider, String providerSubject, String email, String displayName) {}
}
