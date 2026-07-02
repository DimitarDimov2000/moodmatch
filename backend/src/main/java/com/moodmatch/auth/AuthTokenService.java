package com.moodmatch.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.moodmatch.entity.AppUser;
import com.moodmatch.entity.AuthSession;
import com.moodmatch.repository.AuthSessionRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AuthTokenService {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    @ConfigProperty(name = "moodmatch.auth.token.ttl-hours", defaultValue = "24")
    long tokenTtlHours;

    @Inject
    AuthSessionRepository authSessionRepository;

    @Transactional
    public String createToken(AppUser user) {
        String rawToken = generateRawToken();
        AuthSession session = new AuthSession();
        session.setUser(user);
        session.setTokenHash(hashToken(rawToken));
        session.setExpiresAt(Instant.now().plus(Duration.ofHours(tokenTtlHours)));
        authSessionRepository.persist(session);
        return rawToken;
    }

    @Transactional
    public Optional<AppUser> findUserForToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        return authSessionRepository
                .findActiveByTokenHash(hashToken(rawToken), now)
                .map(session -> {
                    session.setLastUsedAt(now);
                    return session.getUser();
                });
    }

    @Transactional
    public void revokeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        authSessionRepository
                .findActiveByTokenHash(hashToken(rawToken), Instant.now())
                .ifPresent(session -> session.setRevokedAt(Instant.now()));
    }

    public String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Token hashing is unavailable.", exception);
        }
    }

    private String generateRawToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}
