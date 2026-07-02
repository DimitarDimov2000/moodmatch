package com.moodmatch.external.adapter;

import java.util.List;

import com.moodmatch.entity.ExternalSourceName;
import com.moodmatch.entity.MediaType;

public record ExternalSearchResult(
        ExternalSearchSourceName source,
        ExternalSourceName mappingSource,
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
        List<ExternalSuggestedTag> suggestedTags,
        String attribution,
        List<String> warnings) {}
