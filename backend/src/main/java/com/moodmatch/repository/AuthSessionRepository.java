package com.moodmatch.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.moodmatch.entity.AuthSession;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthSessionRepository implements PanacheRepositoryBase<AuthSession, UUID> {

    public Optional<AuthSession> findActiveByTokenHash(String tokenHash, Instant now) {
        return find(
                        "select session from AuthSession session "
                                + "join fetch session.user "
                                + "where session.tokenHash = ?1 "
                                + "and session.revokedAt is null "
                                + "and session.expiresAt > ?2",
                        tokenHash,
                        now)
                .firstResultOptional();
    }
}
