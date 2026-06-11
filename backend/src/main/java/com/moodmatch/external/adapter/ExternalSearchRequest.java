package com.moodmatch.external.adapter;

import com.moodmatch.entity.MediaType;

public record ExternalSearchRequest(
        String query,
        MediaType mediaType,
        ExternalSearchSourceName source,
        int limit) {}
