package com.moodmatch.dto.media;

import java.util.List;
import java.util.UUID;

import com.moodmatch.dto.tag.CreateTagRequest;

public record ReplaceMediaTagsRequest(
        List<UUID> tagIds,
        List<CreateTagRequest> createTags) {}
