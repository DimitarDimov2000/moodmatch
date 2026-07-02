package com.moodmatch.external.anilist;

import java.util.List;

import com.moodmatch.entity.MediaType;

public interface AniListGateway {

    List<AniListMedia> searchMedia(MediaType mediaType, String query, int limit);

    record AniListMedia(
            int id,
            String type,
            String format,
            String status,
            String season,
            Integer seasonYear,
            Integer startYear,
            AniListTitle title,
            String description,
            AniListCoverImage coverImage,
            String siteUrl,
            List<String> genres,
            List<String> tags,
            List<String> studios,
            List<String> staffNames) {}

    record AniListTitle(String romaji, String english, String nativeTitle) {}

    record AniListCoverImage(String large, String medium) {}
}
