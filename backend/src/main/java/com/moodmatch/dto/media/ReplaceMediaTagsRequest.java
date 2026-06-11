package com.moodmatch.dto.media;

import java.util.List;
import java.util.UUID;

import com.moodmatch.dto.tag.CreateTagRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ReplaceMediaTagsRequest(
        List<@NotNull UUID> tagIds,
        List<@NotNull @Valid CreateTagRequest> createTags) {}
