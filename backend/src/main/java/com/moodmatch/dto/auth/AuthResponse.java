package com.moodmatch.dto.auth;

public record AuthResponse(String token, AuthUserResponse user) {}
