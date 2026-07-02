package com.moodmatch.service;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.moodmatch.entity.AuthProvider;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

@Mock
@ApplicationScoped
public class TestLocalDemoUserIdentitySource implements LocalDemoUserIdentitySource {

    private static final LocalDemoUserIdentity LOCAL_DEMO_USER = new LocalDemoUserIdentity(
            LocalDemoCurrentUserProvider.LOCAL_DEMO_USER_ID,
            LocalDemoCurrentUserProvider.LOCAL_DEMO_PROVIDER,
            LocalDemoCurrentUserProvider.LOCAL_DEMO_PROVIDER_SUBJECT,
            LocalDemoCurrentUserProvider.LOCAL_DEMO_EMAIL,
            LocalDemoCurrentUserProvider.LOCAL_DEMO_DISPLAY_NAME);
    private static final LocalDemoUserIdentity USER_A = new LocalDemoUserIdentity(
            UUID.fromString("10000000-0000-0000-0000-000000000001"),
            AuthProvider.OIDC,
            "test-user-a",
            "user-a@example.local",
            "Test User A");
    private static final LocalDemoUserIdentity USER_B = new LocalDemoUserIdentity(
            UUID.fromString("20000000-0000-0000-0000-000000000001"),
            AuthProvider.OIDC,
            "test-user-b",
            "user-b@example.local",
            "Test User B");

    private static final AtomicReference<LocalDemoUserIdentity> ACTIVE_USER = new AtomicReference<>(LOCAL_DEMO_USER);

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
    public LocalDemoUserIdentity getIdentity() {
        return ACTIVE_USER.get();
    }
}
