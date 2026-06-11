package com.moodmatch.dto.external;

import java.time.Instant;
import java.util.UUID;

import com.moodmatch.entity.ExternalSourceName;

public record ExternalReferenceResponse(
        UUID id,
        ExternalSourceName sourceName,
        String externalId,
        String externalUrl,
        String attributionText,
        String sourcePayloadHash,
        Instant createdAt) {}
