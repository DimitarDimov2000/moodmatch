package com.moodmatch.external.adapter;

import java.util.UUID;

import com.moodmatch.entity.TagCategory;
import com.moodmatch.entity.TagMappingConfidence;

public record ExternalSuggestedTag(
        UUID tagId,
        String tagName,
        TagCategory tagCategory,
        String sourceValue,
        String reason,
        TagMappingConfidence confidence) {}
