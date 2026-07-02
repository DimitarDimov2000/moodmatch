package com.moodmatch.external.adapter;

public interface ExternalUrlResolver {

    ExternalSearchSourceName sourceName();

    default boolean isConfigured() {
        return true;
    }

    default String configurationErrorMessage() {
        return "Source %s is not configured.".formatted(sourceName());
    }

    ExternalSearchResult resolve(String urlOrId);
}
