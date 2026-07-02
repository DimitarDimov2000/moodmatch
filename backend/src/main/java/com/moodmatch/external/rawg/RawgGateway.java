package com.moodmatch.external.rawg;

import java.util.List;

public interface RawgGateway {

    List<RawgGame> searchGames(String apiKey, String query, int limit);

    record RawgGame(
            int id,
            String slug,
            String name,
            String descriptionRaw,
            String released,
            String backgroundImage,
            List<String> genres,
            List<String> platforms,
            List<String> tags,
            List<String> developers,
            List<String> publishers) {}
}
