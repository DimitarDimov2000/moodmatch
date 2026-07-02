package com.moodmatch.dto.external;

import java.util.List;

import com.moodmatch.entity.MediaType;
import com.moodmatch.external.adapter.ExternalSearchSourceName;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExternalImportRequest(
        @NotNull ExternalSearchSourceName source,
        @NotBlank @Size(max = 255) String externalId,
        @NotNull MediaType mediaType,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 255) String originalTitle,
        List<@Size(max = 255) String> creatorNames,
        String description,
        Integer releaseYear,
        @Size(max = 4000) String coverUrl,
        @Size(max = 4000) String sourceUrl,
        List<@Size(max = 255) String> externalGenres,
        List<@Size(max = 255) String> externalSubjects,
        @Size(max = 255) String attribution) {}
