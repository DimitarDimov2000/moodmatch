package com.moodmatch.dto.tag;

import java.time.Instant;
import java.util.UUID;

import com.moodmatch.entity.TagCategory;

public record TagResponse(
        UUID id,
        String name,
        TagCategory category,
        Instant createdAt,
        Instant updatedAt) {}
