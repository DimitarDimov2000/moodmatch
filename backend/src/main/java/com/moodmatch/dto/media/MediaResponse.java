package com.moodmatch.dto.media;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.moodmatch.dto.external.ExternalReferenceResponse;
import com.moodmatch.dto.tag.TagResponse;
import com.moodmatch.entity.CommitmentLevel;
import com.moodmatch.entity.ConsumptionStatus;
import com.moodmatch.entity.ExternalSourceName;
import com.moodmatch.entity.MediaType;
import com.moodmatch.entity.MetadataOrigin;
import com.moodmatch.entity.SourceType;

public record MediaResponse(
        UUID id,
        String title,
        String originalTitle,
        String description,
        MediaType mediaType,
        ConsumptionStatus consumptionStatus,
        boolean isFavourite,
        Integer rating,
        SourceType sourceType,
        String sourceNote,
        CommitmentLevel commitmentLevel,
        Integer releaseYear,
        String coverUrl,
        ExternalSourceName externalSourceName,
        String externalSourceId,
        String externalSourceUrl,
        MetadataOrigin metadataOrigin,
        List<TagResponse> tags,
        List<ExternalReferenceResponse> externalReferences,
        Instant createdAt,
        Instant updatedAt) {}
