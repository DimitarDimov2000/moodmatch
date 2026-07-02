package com.moodmatch.auth;

import java.time.Instant;
import java.util.Locale;

import com.moodmatch.dto.auth.AuthResponse;
import com.moodmatch.dto.auth.AuthUserResponse;
import com.moodmatch.dto.auth.LoginRequest;
import com.moodmatch.dto.auth.RegisterRequest;
import com.moodmatch.entity.AppUser;
import com.moodmatch.entity.AuthProvider;
import com.moodmatch.repository.AppUserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class AuthService {

    @Inject
    AppUserRepository appUserRepository;

    @Inject
    PasswordHashingService passwordHashingService;

    @Inject
    AuthTokenService authTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (appUserRepository.findByEmail(email).isPresent()) {
            throw new WebApplicationException("Email is already registered.", Response.Status.CONFLICT);
        }

        AppUser user = new AppUser();
        user.setProvider(AuthProvider.LOCAL);
        user.setProviderSubject(email);
        user.setEmail(email);
        user.setDisplayName(normalizeDisplayName(request.displayName()));
        user.setPasswordHash(passwordHashingService.hash(request.password()));
        user.setLastLoginAt(Instant.now());
        appUserRepository.persist(user);

        return new AuthResponse(authTokenService.createToken(user), toUserResponse(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        AppUser user = appUserRepository
                .findByEmail(email)
                .filter(candidate -> passwordHashingService.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new WebApplicationException("Invalid email or password.", Response.Status.UNAUTHORIZED));

        user.setLastLoginAt(Instant.now());
        return new AuthResponse(authTokenService.createToken(user), toUserResponse(user));
    }

    public AuthUserResponse toUserResponse(AppUser user) {
        return new AuthUserResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }

        String trimmed = displayName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
