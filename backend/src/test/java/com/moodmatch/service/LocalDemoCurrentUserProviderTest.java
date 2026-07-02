package com.moodmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.moodmatch.entity.AppUser;
import com.moodmatch.entity.AuthProvider;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class LocalDemoCurrentUserProviderTest {

    @Inject
    CurrentUserProvider currentUserProvider;

    @AfterEach
    void resetCurrentUser() {
        TestCurrentUserProvider.useLocalDemoUser();
    }

    @Test
    @TestTransaction
    void shouldResolveLocalDemoUserByDefaultInTestMode() {
        AppUser currentUser = currentUserProvider.getCurrentUser();

        assertEquals(LocalDemoCurrentUserProvider.LOCAL_DEMO_USER_ID, currentUser.getId());
        assertEquals(AuthProvider.LOCAL, currentUser.getProvider());
        assertEquals(LocalDemoCurrentUserProvider.LOCAL_DEMO_PROVIDER_SUBJECT, currentUser.getProviderSubject());
        assertEquals(LocalDemoCurrentUserProvider.LOCAL_DEMO_EMAIL, currentUser.getEmail());
        assertEquals(LocalDemoCurrentUserProvider.LOCAL_DEMO_DISPLAY_NAME, currentUser.getDisplayName());
    }
}
