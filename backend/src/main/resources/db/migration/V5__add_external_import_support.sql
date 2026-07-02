ALTER TABLE media_items DROP CONSTRAINT chk_media_items_external_source_name;

ALTER TABLE media_items
    ADD CONSTRAINT chk_media_items_external_source_name
        CHECK (
            external_source_name IS NULL
                OR external_source_name IN ('DEMO', 'TMDB', 'OPEN_LIBRARY', 'RAWG', 'WIKIDATA', 'IGDB', 'GOOGLE_BOOKS', 'TVMAZE')
            );

ALTER TABLE media_external_refs DROP CONSTRAINT uk_media_external_refs_source_external_id;

ALTER TABLE media_external_refs
    ADD CONSTRAINT uk_media_external_refs_media_source_external UNIQUE (media_id, source_name, external_id);

ALTER TABLE media_external_refs DROP CONSTRAINT chk_media_external_refs_source_name;

ALTER TABLE media_external_refs
    ADD CONSTRAINT chk_media_external_refs_source_name
        CHECK (source_name IN ('DEMO', 'TMDB', 'OPEN_LIBRARY', 'RAWG', 'WIKIDATA', 'IGDB', 'GOOGLE_BOOKS', 'TVMAZE'));

ALTER TABLE external_tag_mappings DROP CONSTRAINT chk_external_tag_mappings_source_name;

ALTER TABLE external_tag_mappings
    ADD CONSTRAINT chk_external_tag_mappings_source_name
        CHECK (source_name IN ('DEMO', 'TMDB', 'OPEN_LIBRARY', 'RAWG', 'WIKIDATA', 'IGDB', 'GOOGLE_BOOKS', 'TVMAZE'));
