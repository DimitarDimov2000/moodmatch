package com.moodmatch.dto.external;

import java.util.List;

import com.moodmatch.entity.MediaType;
import com.moodmatch.external.adapter.ExternalSearchSourceName;

public record ExternalSearchResultResponse(
        ExternalSearchSourceName source,
        String externalId,
        MediaType mediaType,
        String title,
        String originalTitle,
        List<String> creatorNames,
        String description,
        Integer releaseYear,
        String coverUrl,
        String sourceUrl,
        List<String> externalGenres,
        List<String> externalSubjects,
        List<ExternalSuggestedTagResponse> suggestedTags,
        String attribution,
        List<String> warnings) {}
