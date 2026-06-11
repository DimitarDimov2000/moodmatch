package com.moodmatch.dto.matching;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.moodmatch.dto.tag.TagResponse;
import com.moodmatch.entity.CommitmentLevel;
import com.moodmatch.entity.ConsumptionStatus;
import com.moodmatch.entity.MediaType;

public record MatchingMediaResponse(
        UUID id,
        String title,
        MediaType mediaType,
        ConsumptionStatus consumptionStatus,
        CommitmentLevel commitmentLevel,
        boolean isFavourite,
        Integer rating,
        Integer releaseYear,
        String coverUrl,
        List<TagResponse> tags,
        Instant createdAt,
        Instant updatedAt) {}
