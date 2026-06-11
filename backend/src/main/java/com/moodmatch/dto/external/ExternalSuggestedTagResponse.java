package com.moodmatch.dto.external;

import java.util.UUID;

import com.moodmatch.entity.TagCategory;
import com.moodmatch.entity.TagMappingConfidence;

public record ExternalSuggestedTagResponse(
        UUID tagId,
        String tagName,
        TagCategory tagCategory,
        String sourceValue,
        String reason,
        TagMappingConfidence confidence) {}
