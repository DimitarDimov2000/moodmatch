package com.moodmatch.dto.external;

import com.moodmatch.external.adapter.ExternalSearchSourceName;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExternalResolveUrlRequest(
        @NotNull ExternalSearchSourceName source,
        @NotBlank @Size(max = 4000) String url) {}
