package com.moodmatch.external.adapter;

import com.moodmatch.entity.ExternalSourceName;
import com.moodmatch.entity.MediaType;

public final class ExternalSourceMappings {

    private ExternalSourceMappings() {}

    public static ExternalSourceName toEntitySource(ExternalSearchSourceName source) {
        return switch (source) {
            case DEMO -> ExternalSourceName.DEMO;
            case TMDB -> ExternalSourceName.TMDB;
            case OPEN_LIBRARY -> ExternalSourceName.OPEN_LIBRARY;
        };
    }

    public static ExternalSourceName toMappingSource(ExternalSearchSourceName source, MediaType mediaType) {
        return switch (source) {
            case TMDB -> ExternalSourceName.TMDB;
            case OPEN_LIBRARY -> ExternalSourceName.OPEN_LIBRARY;
            case DEMO -> switch (mediaType) {
                case FILM, SERIES -> ExternalSourceName.TMDB;
                case BOOK -> ExternalSourceName.OPEN_LIBRARY;
                case GAME -> ExternalSourceName.RAWG;
            };
        };
    }
}
