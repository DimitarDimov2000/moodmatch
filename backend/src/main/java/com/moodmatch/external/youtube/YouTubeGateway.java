package com.moodmatch.external.youtube;

import java.util.List;
import java.util.Optional;

public interface YouTubeGateway {

    Optional<YouTubeVideo> fetchVideo(String apiKey, String videoId);

    Optional<String> fetchCategoryLabel(String apiKey, String categoryId);

    record YouTubeVideo(
            String id,
            String title,
            String description,
            String channelTitle,
            String publishedAt,
            String categoryId,
            List<String> tags,
            ThumbnailSet thumbnails) {}

    record ThumbnailSet(
            String defaultUrl,
            String mediumUrl,
            String highUrl,
            String standardUrl,
            String maxresUrl) {}
}
