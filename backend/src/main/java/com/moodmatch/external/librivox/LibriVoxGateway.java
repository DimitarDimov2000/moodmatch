package com.moodmatch.external.librivox;

import java.util.List;

public interface LibriVoxGateway {

    List<LibriVoxAudiobook> searchAudiobooks(String query, int limit);

    record LibriVoxAudiobook(
            String id,
            String title,
            String description,
            String language,
            String copyrightYear,
            String urlLibrivox,
            String coverArtJpg,
            String coverArtThumbnail,
            List<LibriVoxAuthor> authors,
            List<LibriVoxGenre> genres,
            List<LibriVoxSection> sections) {}

    record LibriVoxAuthor(String id, String firstName, String lastName) {}

    record LibriVoxGenre(String id, String name) {}

    record LibriVoxSection(String id, List<LibriVoxReader> readers) {}

    record LibriVoxReader(String readerId, String displayName) {}
}
