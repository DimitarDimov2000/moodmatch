package com.moodmatch.service;

import java.util.UUID;

import com.moodmatch.entity.AuthProvider;

public record LocalDemoUserIdentity(
        UUID id,
        AuthProvider provider,
        String providerSubject,
        String email,
        String displayName) {}
