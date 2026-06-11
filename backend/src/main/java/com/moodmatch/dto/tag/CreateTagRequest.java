package com.moodmatch.dto.tag;

import com.moodmatch.entity.TagCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull TagCategory category) {}
