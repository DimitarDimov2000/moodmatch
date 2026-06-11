package com.moodmatch.dto.media;

import com.moodmatch.entity.CommitmentLevel;
import com.moodmatch.entity.ConsumptionStatus;
import com.moodmatch.entity.MediaType;
import com.moodmatch.entity.MetadataOrigin;
import com.moodmatch.entity.SourceType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateMediaRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 255) String originalTitle,
        String description,
        @NotNull MediaType mediaType,
        @NotNull ConsumptionStatus consumptionStatus,
        boolean isFavourite,
        @Min(1) @Max(5) Integer rating,
        @NotNull SourceType sourceType,
        @Size(max = 2000) String sourceNote,
        @NotNull CommitmentLevel commitmentLevel,
        Integer releaseYear,
        @Size(max = 4000) String coverUrl,
        MetadataOrigin metadataOrigin) {}
