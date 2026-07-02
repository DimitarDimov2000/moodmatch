package com.moodmatch.dto.auth;

import java.util.UUID;

public record AuthUserResponse(UUID id, String email, String displayName) {}
