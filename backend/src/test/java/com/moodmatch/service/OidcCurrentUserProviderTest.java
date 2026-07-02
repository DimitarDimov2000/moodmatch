package com.moodmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.security.Permission;
import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moodmatch.entity.AppUser;
import com.moodmatch.entity.AuthProvider;
import com.moodmatch.repository.AppUserRepository;

import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

class OidcCurrentUserProviderTest {

    private OidcCurrentUserProvider oidcCurrentUserProvider;
    private InMemoryAppUserRepository appUserRepository;
    private MutableSecurityIdentityProvider securityIdentityProvider;

    @BeforeEach
    void setUp() {
        appUserRepository = new InMemoryAppUserRepository();
        securityIdentityProvider = new MutableSecurityIdentityProvider();

        MoodMatchAuthConfiguration authConfiguration = new MoodMatchAuthConfiguration();
        authConfiguration.authMode = "oidc";
        authConfiguration.oidcProvider = AuthProvider.OIDC;

        oidcCurrentUserProvider = new OidcCurrentUserProvider();
        oidcCurrentUserProvider.appUserRepository = appUserRepository;
        oidcCurrentUserProvider.authConfiguration = authConfiguration;
        oidcCurrentUserProvider.securityIdentityProvider = securityIdentityProvider;
    }

    @Test
    void shouldCreateAppUserFromTokenClaimsOnFirstAuthenticatedAccess() {
        securityIdentityProvider.securityIdentity = new StubSecurityIdentity(
                "google-user-123",
                Map.of(
                        "sub", "google-user-123",
                        "email", "oidc-user@example.com",
                        "name", "OIDC User",
                        "picture", "https://images.example.com/oidc-user.png"),
                false);

        AppUser currentUser = oidcCurrentUserProvider.getCurrentUser();

        assertNotNull(currentUser.getId());
        assertEquals(AuthProvider.OIDC, currentUser.getProvider());
        assertEquals("google-user-123", currentUser.getProviderSubject());
        assertEquals("oidc-user@example.com", currentUser.getEmail());
        assertEquals("OIDC User", currentUser.getDisplayName());
        assertEquals("https://images.example.com/oidc-user.png", currentUser.getAvatarUrl());
        assertEquals(1, appUserRepository.size());
    }

    @Test
    void shouldReuseExistingAppUserAndOnlyUpdateNonBlankClaimFields() {
        securityIdentityProvider.securityIdentity = new StubSecurityIdentity(
                "google-user-123",
                Map.of(
                        "sub", "google-user-123",
                        "email", "first@example.com",
                        "name", "First Name",
                        "picture", "https://images.example.com/first.png"),
                false);

        AppUser createdUser = oidcCurrentUserProvider.getCurrentUser();

        securityIdentityProvider.securityIdentity = new StubSecurityIdentity(
                "google-user-123",
                Map.of(
                        "sub", "google-user-123",
                        "email", "   ",
                        "name", "Updated Name",
                        "picture", "https://images.example.com/updated.png"),
                false);

        AppUser updatedUser = oidcCurrentUserProvider.getCurrentUser();

        assertEquals(createdUser.getId(), updatedUser.getId());
        assertEquals(1, appUserRepository.size());
        assertEquals("first@example.com", updatedUser.getEmail());
        assertEquals("Updated Name", updatedUser.getDisplayName());
        assertEquals("https://images.example.com/updated.png", updatedUser.getAvatarUrl());
    }

    private static final class MutableSecurityIdentityProvider implements SecurityIdentityProvider {

        private SecurityIdentity securityIdentity = new StubSecurityIdentity("anonymous", Map.of(), true);

        @Override
        public SecurityIdentity getSecurityIdentity() {
            return securityIdentity;
        }
    }

    private static final class InMemoryAppUserRepository extends AppUserRepository {

        private final Map<String, AppUser> usersByProviderKey = new HashMap<>();

        @Override
        public Optional<AppUser> findByProviderAndProviderSubject(AuthProvider provider, String providerSubject) {
            return Optional.ofNullable(usersByProviderKey.get(key(provider, providerSubject)));
        }

        @Override
        public void persist(AppUser user) {
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            if (user.getCreatedAt() == null) {
                user.setCreatedAt(Instant.now());
            }
            user.setUpdatedAt(Instant.now());
            usersByProviderKey.put(key(user.getProvider(), user.getProviderSubject()), user);
        }

        int size() {
            return usersByProviderKey.size();
        }

        private String key(AuthProvider provider, String providerSubject) {
            return provider + "::" + providerSubject;
        }
    }

    private static final class StubSecurityIdentity implements SecurityIdentity {

        private final Principal principal;
        private final Map<String, Object> attributes;
        private final boolean anonymous;

        private StubSecurityIdentity(String principalName, Map<String, Object> attributes, boolean anonymous) {
            this.principal = () -> principalName;
            this.attributes = attributes;
            this.anonymous = anonymous;
        }

        @Override
        public Principal getPrincipal() {
            return principal;
        }

        @Override
        public boolean isAnonymous() {
            return anonymous;
        }

        @Override
        public Set<String> getRoles() {
            return Set.of();
        }

        @Override
        public boolean hasRole(String role) {
            return false;
        }

        @Override
        public Set<Permission> getPermissions() {
            return Set.of();
        }

        @Override
        public <T extends Credential> T getCredential(Class<T> credentialType) {
            return null;
        }

        @Override
        public Set<Credential> getCredentials() {
            return Set.of();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getAttribute(String name) {
            return (T) attributes.get(name);
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public Uni<Boolean> checkPermission(Permission permission) {
            return Uni.createFrom().item(false);
        }
    }
}
