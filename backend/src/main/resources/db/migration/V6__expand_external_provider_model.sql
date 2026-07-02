ALTER TABLE media_items DROP CONSTRAINT chk_media_items_media_type;

ALTER TABLE media_items
    ADD CONSTRAINT chk_media_items_media_type
        CHECK (media_type IN ('FILM', 'SERIES', 'BOOK', 'GAME', 'PODCAST', 'AUDIOBOOK', 'VIDEO'));

ALTER TABLE media_items DROP CONSTRAINT chk_media_items_external_source_name;

ALTER TABLE media_items
    ADD CONSTRAINT chk_media_items_external_source_name
        CHECK (
            external_source_name IS NULL
                OR external_source_name IN (
                    'DEMO',
                    'TMDB',
                    'OPEN_LIBRARY',
                    'RAWG',
                    'LIBRIVOX',
                    'PODCAST_INDEX',
                    'ANILIST',
                    'YOUTUBE',
                    'WIKIDATA',
                    'IGDB',
                    'GOOGLE_BOOKS',
                    'TVMAZE'
                )
            );

ALTER TABLE media_external_refs DROP CONSTRAINT chk_media_external_refs_source_name;

ALTER TABLE media_external_refs
    ADD CONSTRAINT chk_media_external_refs_source_name
        CHECK (source_name IN (
            'DEMO',
            'TMDB',
            'OPEN_LIBRARY',
            'RAWG',
            'LIBRIVOX',
            'PODCAST_INDEX',
            'ANILIST',
            'YOUTUBE',
            'WIKIDATA',
            'IGDB',
            'GOOGLE_BOOKS',
            'TVMAZE'
        ));

ALTER TABLE external_tag_mappings DROP CONSTRAINT chk_external_tag_mappings_source_name;

ALTER TABLE external_tag_mappings
    ADD CONSTRAINT chk_external_tag_mappings_source_name
        CHECK (source_name IN (
            'DEMO',
            'TMDB',
            'OPEN_LIBRARY',
            'RAWG',
            'LIBRIVOX',
            'PODCAST_INDEX',
            'ANILIST',
            'YOUTUBE',
            'WIKIDATA',
            'IGDB',
            'GOOGLE_BOOKS',
            'TVMAZE'
        ));
