package com.moodmatch.dto.external;

import java.time.Instant;
import java.util.UUID;

import com.moodmatch.dto.tag.TagResponse;
import com.moodmatch.entity.ExternalSourceName;
import com.moodmatch.entity.TagMappingConfidence;

public record ExternalTagMappingResponse(
        UUID id,
        ExternalSourceName sourceName,
        String externalField,
        String externalValue,
        TagResponse tag,
        TagMappingConfidence confidence,
        Instant createdAt,
        Instant updatedAt) {}
