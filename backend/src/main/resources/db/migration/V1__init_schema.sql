CREATE TABLE media_items (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    original_title VARCHAR(255),
    description TEXT,
    media_type VARCHAR(32) NOT NULL,
    consumption_status VARCHAR(32) NOT NULL,
    is_favourite BOOLEAN NOT NULL DEFAULT FALSE,
    rating INTEGER,
    source_type VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    source_note VARCHAR(255),
    commitment_level VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    release_year INTEGER,
    cover_url TEXT,
    external_source_name VARCHAR(32),
    external_source_id VARCHAR(255),
    external_source_url TEXT,
    metadata_origin VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_media_items_media_type
            CHECK (media_type IN ('FILM', 'SERIES', 'BOOK', 'GAME')),
    CONSTRAINT chk_media_items_consumption_status
            CHECK (consumption_status IN ('CONSUMED', 'WANT_TO_CONSUME', 'NOT_INTERESTED', 'ABANDONED')),
    CONSTRAINT chk_media_items_rating_range
            CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),
    CONSTRAINT chk_media_items_source_type
            CHECK (source_type IN ('FRIEND', 'SOCIAL_MEDIA', 'ARTICLE', 'PLATFORM', 'MANUAL', 'EXTERNAL_SEARCH', 'UNKNOWN')),
    CONSTRAINT chk_media_items_commitment_level
            CHECK (commitment_level IN ('SHORT', 'MEDIUM', 'LONG', 'UNKNOWN')),
    CONSTRAINT chk_media_items_external_source_name
            CHECK (external_source_name IS NULL OR external_source_name IN ('TMDB', 'OPEN_LIBRARY', 'RAWG', 'WIKIDATA', 'IGDB', 'GOOGLE_BOOKS', 'TVMAZE')),
    CONSTRAINT chk_media_items_metadata_origin
            CHECK (metadata_origin IN ('MANUAL', 'IMPORTED', 'IMPORTED_AND_EDITED')),
    CONSTRAINT chk_media_items_favourite_rule
            CHECK (NOT is_favourite OR (consumption_status = 'CONSUMED' AND rating >= 4))
);

CREATE TABLE tags (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_tags_name_category UNIQUE (name, category),
    CONSTRAINT chk_tags_category
            CHECK (category IN ('GENRE', 'THEME', 'SETTING', 'TONE', 'EXPERIENCE'))
);

CREATE TABLE media_tags (
    media_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_media_tags PRIMARY KEY (media_id, tag_id),
    CONSTRAINT fk_media_tags_media_items FOREIGN KEY (media_id) REFERENCES media_items (id),
    CONSTRAINT fk_media_tags_tags FOREIGN KEY (tag_id) REFERENCES tags (id)
);

CREATE TABLE media_external_refs (
    id UUID PRIMARY KEY,
    media_id UUID NOT NULL,
    source_name VARCHAR(32) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    external_url TEXT,
    attribution_text TEXT,
    source_payload_hash VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_media_external_refs_source_external_id UNIQUE (source_name, external_id),
    CONSTRAINT fk_media_external_refs_media_items FOREIGN KEY (media_id) REFERENCES media_items (id),
    CONSTRAINT chk_media_external_refs_source_name
            CHECK (source_name IN ('TMDB', 'OPEN_LIBRARY', 'RAWG', 'WIKIDATA', 'IGDB', 'GOOGLE_BOOKS', 'TVMAZE'))
);

CREATE TABLE external_tag_mappings (
    id UUID PRIMARY KEY,
    source_name VARCHAR(32) NOT NULL,
    external_field VARCHAR(255) NOT NULL,
    external_value VARCHAR(255) NOT NULL,
    tag_id UUID NOT NULL,
    confidence VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_external_tag_mappings_tags FOREIGN KEY (tag_id) REFERENCES tags (id),
    CONSTRAINT chk_external_tag_mappings_source_name
            CHECK (source_name IN ('TMDB', 'OPEN_LIBRARY', 'RAWG', 'WIKIDATA', 'IGDB', 'GOOGLE_BOOKS', 'TVMAZE')),
    CONSTRAINT chk_external_tag_mappings_confidence
            CHECK (confidence IN ('HIGH', 'MEDIUM', 'LOW'))
);

CREATE INDEX idx_media_external_refs_media_id ON media_external_refs (media_id);
CREATE INDEX idx_external_tag_mappings_tag_id ON external_tag_mappings (tag_id);
CREATE INDEX idx_media_items_media_type ON media_items (media_type);
CREATE INDEX idx_media_items_consumption_status ON media_items (consumption_status);
