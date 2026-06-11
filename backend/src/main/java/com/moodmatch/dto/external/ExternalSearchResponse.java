package com.moodmatch.dto.external;

import java.util.List;

import com.moodmatch.entity.MediaType;
import com.moodmatch.external.adapter.ExternalSearchSourceName;

public record ExternalSearchResponse(
        String query,
        MediaType mediaType,
        ExternalSearchSourceName source,
        List<ExternalSearchResultResponse> results,
        List<String> warnings) {}
