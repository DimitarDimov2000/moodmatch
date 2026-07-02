package com.moodmatch.service;

import java.security.Principal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.moodmatch.entity.AppUser;
import com.moodmatch.entity.AuthProvider;
import com.moodmatch.repository.AppUserRepository;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotAuthorizedException;

@ApplicationScoped
public class OidcCurrentUserProvider {

    @Inject
    AppUserRepository appUserRepository;

    @Inject
    MoodMatchAuthConfiguration authConfiguration;

    @Inject
    SecurityIdentityProvider securityIdentityProvider;

    @Transactional
    public AppUser getCurrentUser() {
        SecurityIdentity securityIdentity = securityIdentityProvider.getSecurityIdentity();
        if (securityIdentity == null || securityIdentity.isAnonymous()) {
            throw new NotAuthorizedException("Bearer token required.");
        }

        TokenUserProfile tokenUserProfile = TokenUserProfile.fromSecurityIdentity(
                securityIdentity, authConfiguration.getOidcProvider());

        return appUserRepository
                .findByProviderAndProviderSubject(tokenUserProfile.provider(), tokenUserProfile.providerSubject())
                .map(existingUser -> syncUser(existingUser, tokenUserProfile))
                .orElseGet(() -> createUser(tokenUserProfile));
    }

    private AppUser createUser(TokenUserProfile tokenUserProfile) {
        AppUser user = new AppUser();
        user.setProvider(tokenUserProfile.provider());
        user.setProviderSubject(tokenUserProfile.providerSubject());
        tokenUserProfile.email().ifPresent(user::setEmail);
        tokenUserProfile.displayName().ifPresent(user::setDisplayName);
        tokenUserProfile.avatarUrl().ifPresent(user::setAvatarUrl);
        appUserRepository.persist(user);
        return user;
    }

    private AppUser syncUser(AppUser user, TokenUserProfile tokenUserProfile) {
        updateIfChanged(user.getEmail(), tokenUserProfile.email()).ifPresent(user::setEmail);
        updateIfChanged(user.getDisplayName(), tokenUserProfile.displayName()).ifPresent(user::setDisplayName);
        updateIfChanged(user.getAvatarUrl(), tokenUserProfile.avatarUrl()).ifPresent(user::setAvatarUrl);
        return user;
    }

    private Optional<String> updateIfChanged(String existingValue, Optional<String> incomingValue) {
        return incomingValue.filter(value -> !Objects.equals(existingValue, value));
    }

    record TokenUserProfile(
            AuthProvider provider,
            String providerSubject,
            Optional<String> email,
            Optional<String> displayName,
            Optional<String> avatarUrl) {

        static TokenUserProfile fromSecurityIdentity(SecurityIdentity securityIdentity, AuthProvider provider) {
            String providerSubject = firstNonBlankClaim(securityIdentity, "sub")
                    .orElseGet(() -> principalName(securityIdentity)
                            .orElseThrow(() -> new NotAuthorizedException("Authenticated token subject is missing.")));

            return new TokenUserProfile(
                    provider,
                    providerSubject,
                    firstNonBlankClaim(securityIdentity, "email"),
                    firstNonBlankClaim(securityIdentity, "name", "display_name", "preferred_username"),
                    firstNonBlankClaim(securityIdentity, "picture", "avatar_url"));
        }

        private static Optional<String> principalName(SecurityIdentity securityIdentity) {
            Principal principal = securityIdentity.getPrincipal();
            if (principal == null) {
                return Optional.empty();
            }

            return normalize(principal.getName());
        }

        private static Optional<String> firstNonBlankClaim(SecurityIdentity securityIdentity, String... claimNames) {
            for (String claimName : claimNames) {
                Optional<String> value = readClaim(securityIdentity, claimName);
                if (value.isPresent()) {
                    return value;
                }
            }

            return Optional.empty();
        }

        private static Optional<String> readClaim(SecurityIdentity securityIdentity, String claimName) {
            Object attribute = securityIdentity.getAttribute(claimName);
            if (attribute != null) {
                return normalize(attribute.toString());
            }

            Map<String, Object> attributes = securityIdentity.getAttributes();
            if (attributes != null && attributes.containsKey(claimName)) {
                Object value = attributes.get(claimName);
                if (value != null) {
                    return normalize(value.toString());
                }
            }

            return Optional.empty();
        }

        private static Optional<String> normalize(String value) {
            if (value == null) {
                return Optional.empty();
            }

            String trimmed = value.trim();
            return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
        }
    }
}
