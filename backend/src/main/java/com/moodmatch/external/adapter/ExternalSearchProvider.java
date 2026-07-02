package com.moodmatch.external.adapter;

import java.util.List;
import java.util.Set;

import com.moodmatch.entity.MediaType;

public interface ExternalSearchProvider {

    ExternalSearchSourceName sourceName();

    Set<MediaType> supportedMediaTypes();

    default boolean isConfigured() {
        return true;
    }

    default String configurationErrorMessage() {
        return "Source %s is not configured.".formatted(sourceName());
    }

    List<ExternalSearchResult> search(ExternalSearchRequest request);
}
