package com.moodmatch.external.podcastindex;

import java.util.List;

public interface PodcastIndexGateway {

    List<PodcastFeed> searchShows(String apiKey, String apiSecret, String query, int limit);

    record PodcastFeed(
            long id,
            String title,
            String url,
            String originalUrl,
            String link,
            String description,
            String author,
            String ownerName,
            String image,
            String artwork,
            Long newestItemPubdate,
            String language,
            Integer explicit,
            String medium,
            List<String> categories) {}
}
