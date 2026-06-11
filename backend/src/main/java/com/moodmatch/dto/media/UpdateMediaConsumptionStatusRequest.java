package com.moodmatch.dto.media;

import com.moodmatch.entity.ConsumptionStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateMediaConsumptionStatusRequest(
        @NotNull ConsumptionStatus consumptionStatus,
        @Min(1) @Max(5) Integer rating,
        boolean isFavourite,
        boolean confirmDestructiveChange) {}
