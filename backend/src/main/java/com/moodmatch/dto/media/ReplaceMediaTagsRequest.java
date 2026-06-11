package com.moodmatch.dto.media;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ReplaceMediaTagsRequest(List<@NotNull UUID> tagIds) {}
