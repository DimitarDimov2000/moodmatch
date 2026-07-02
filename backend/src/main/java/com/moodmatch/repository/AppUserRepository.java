package com.moodmatch.repository;

import java.util.Optional;
import java.util.UUID;

import com.moodmatch.entity.AppUser;
import com.moodmatch.entity.AuthProvider;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AppUserRepository implements PanacheRepositoryBase<AppUser, UUID> {

    public Optional<AppUser> findByProviderAndProviderSubject(AuthProvider provider, String providerSubject) {
        return find("provider = ?1 and providerSubject = ?2", provider, providerSubject).firstResultOptional();
    }
}
