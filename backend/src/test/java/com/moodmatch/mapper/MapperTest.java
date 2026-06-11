package com.moodmatch.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moodmatch.dto.external.ExternalTagMappingResponse;
import com.moodmatch.dto.media.MediaResponse;
import com.moodmatch.dto.tag.TagResponse;
import com.moodmatch.entity.CommitmentLevel;
import com.moodmatch.entity.ConsumptionStatus;
import com.moodmatch.entity.ExternalSourceName;
import com.moodmatch.entity.ExternalTagMapping;
import com.moodmatch.entity.MediaExternalRef;
import com.moodmatch.entity.MediaItem;
import com.moodmatch.entity.MediaTag;
import com.moodmatch.entity.MediaTagId;
import com.moodmatch.entity.MediaType;
import com.moodmatch.entity.MetadataOrigin;
import com.moodmatch.entity.SourceType;
import com.moodmatch.entity.Tag;
import com.moodmatch.entity.TagCategory;
import com.moodmatch.entity.TagMappingConfidence;

class MapperTest {

    private TagMapper tagMapper;
    private MediaExternalRefMapper mediaExternalRefMapper;
    private MediaItemMapper mediaItemMapper;
    private ExternalTagMappingMapper externalTagMappingMapper;

    @BeforeEach
    void setUp() {
        tagMapper = new TagMapper();
        mediaExternalRefMapper = new MediaExternalRefMapper();
        mediaItemMapper = new MediaItemMapper();
        mediaItemMapper.tagMapper = tagMapper;
        mediaItemMapper.mediaExternalRefMapper = mediaExternalRefMapper;
        externalTagMappingMapper = new ExternalTagMappingMapper();
        externalTagMappingMapper.tagMapper = tagMapper;
    }

    @Test
    void shouldMapTagToResponse() {
        Tag tag = buildTag(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Science-Fiction",
                TagCategory.GENRE,
                Instant.parse("2026-01-01T10:15:30Z"),
                Instant.parse("2026-01-02T10:15:30Z"));

        TagResponse response = tagMapper.toResponse(tag);

        assertNotNull(response);
        assertEquals(tag.getId(), response.id());
        assertEquals("Science-Fiction", response.name());
        assertEquals(TagCategory.GENRE, response.category());
        assertEquals(tag.getCreatedAt(), response.createdAt());
        assertEquals(tag.getUpdatedAt(), response.updatedAt());
    }

    @Test
    void shouldMapMediaItemToResponseWithDeterministicOrdering() {
        Tag genre = buildTag(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Science-Fiction",
                TagCategory.GENRE,
                Instant.parse("2026-01-03T10:15:30Z"),
                Instant.parse("2026-01-04T10:15:30Z"));
        Tag tone = buildTag(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "melancholisch",
                TagCategory.TONE,
                Instant.parse("2026-01-05T10:15:30Z"),
                Instant.parse("2026-01-06T10:15:30Z"));

        MediaItem mediaItem = new MediaItem();
        mediaItem.setId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        mediaItem.setTitle("Interstellar");
        mediaItem.setOriginalTitle("Interstellar");
        mediaItem.setDescription("Space, time, and family.");
        mediaItem.setMediaType(MediaType.FILM);
        mediaItem.setConsumptionStatus(ConsumptionStatus.CONSUMED);
        mediaItem.setFavourite(true);
        mediaItem.setRating(5);
        mediaItem.setSourceType(SourceType.MANUAL);
        mediaItem.setSourceNote("Own collection");
        mediaItem.setCommitmentLevel(CommitmentLevel.LONG);
        mediaItem.setReleaseYear(2014);
        mediaItem.setCoverUrl("https://example.com/cover.jpg");
        mediaItem.setExternalSourceName(ExternalSourceName.TMDB);
        mediaItem.setExternalSourceId("157336");
        mediaItem.setExternalSourceUrl("https://www.themoviedb.org/movie/157336");
        mediaItem.setMetadataOrigin(MetadataOrigin.IMPORTED_AND_EDITED);
        mediaItem.setCreatedAt(Instant.parse("2026-01-07T10:15:30Z"));
        mediaItem.setUpdatedAt(Instant.parse("2026-01-08T10:15:30Z"));

        MediaTag toneLink = buildMediaTag(mediaItem, tone);
        MediaTag genreLink = buildMediaTag(mediaItem, genre);
        mediaItem.setMediaTags(new LinkedHashSet<>(Set.of(toneLink, genreLink)));

        MediaExternalRef rawgRef = buildExternalRef(
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                mediaItem,
                ExternalSourceName.RAWG,
                "rawg-1",
                "https://rawg.io/games/example",
                "RAWG",
                "hash-b",
                Instant.parse("2026-01-10T10:15:30Z"));
        MediaExternalRef tmdbRef = buildExternalRef(
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                mediaItem,
                ExternalSourceName.TMDB,
                "157336",
                "https://www.themoviedb.org/movie/157336",
                "TMDB",
                "hash-a",
                Instant.parse("2026-01-09T10:15:30Z"));
        mediaItem.setExternalReferences(new LinkedHashSet<>(Set.of(rawgRef, tmdbRef)));

        MediaResponse response = mediaItemMapper.toResponse(mediaItem);

        assertNotNull(response);
        assertEquals(mediaItem.getId(), response.id());
        assertEquals(2, response.tags().size());
        assertEquals("Science-Fiction", response.tags().getFirst().name());
        assertEquals("melancholisch", response.tags().get(1).name());
        assertEquals(2, response.externalReferences().size());
        assertEquals(ExternalSourceName.RAWG, response.externalReferences().get(1).sourceName());
        assertEquals(ExternalSourceName.TMDB, response.externalReferences().getFirst().sourceName());
        assertTrue(response.isFavourite());
    }

    @Test
    void shouldMapExternalTagMappingToResponse() {
        Tag tag = buildTag(
                UUID.fromString("77777777-7777-7777-7777-777777777777"),
                "Adventure",
                TagCategory.GENRE,
                Instant.parse("2026-02-01T10:15:30Z"),
                Instant.parse("2026-02-02T10:15:30Z"));

        ExternalTagMapping mapping = new ExternalTagMapping();
        mapping.setId(UUID.fromString("88888888-8888-8888-8888-888888888888"));
        mapping.setSourceName(ExternalSourceName.TMDB);
        mapping.setExternalField("genre");
        mapping.setExternalValue("Adventure");
        mapping.setTag(tag);
        mapping.setConfidence(TagMappingConfidence.HIGH);
        mapping.setCreatedAt(Instant.parse("2026-02-03T10:15:30Z"));
        mapping.setUpdatedAt(Instant.parse("2026-02-04T10:15:30Z"));

        ExternalTagMappingResponse response = externalTagMappingMapper.toResponse(mapping);

        assertNotNull(response);
        assertEquals(mapping.getId(), response.id());
        assertEquals("genre", response.externalField());
        assertEquals("Adventure", response.externalValue());
        assertEquals("Adventure", response.tag().name());
        assertEquals(TagMappingConfidence.HIGH, response.confidence());
    }

    @Test
    void shouldReturnEmptyCollectionsForMissingAssociations() {
        MediaItem mediaItem = new MediaItem();
        mediaItem.setId(UUID.fromString("99999999-9999-9999-9999-999999999999"));
        mediaItem.setTitle("Untitled");
        mediaItem.setMediaType(MediaType.BOOK);
        mediaItem.setConsumptionStatus(ConsumptionStatus.WANT_TO_CONSUME);
        mediaItem.setSourceType(SourceType.UNKNOWN);
        mediaItem.setCommitmentLevel(CommitmentLevel.UNKNOWN);
        mediaItem.setMetadataOrigin(MetadataOrigin.MANUAL);
        mediaItem.setMediaTags(null);
        mediaItem.setExternalReferences(null);

        MediaResponse response = mediaItemMapper.toResponse(mediaItem);

        assertNotNull(response);
        assertTrue(response.tags().isEmpty());
        assertTrue(response.externalReferences().isEmpty());
        assertFalse(response.isFavourite());
    }

    private Tag buildTag(UUID id, String name, TagCategory category, Instant createdAt, Instant updatedAt) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        tag.setCategory(category);
        tag.setCreatedAt(createdAt);
        tag.setUpdatedAt(updatedAt);
        return tag;
    }

    private MediaTag buildMediaTag(MediaItem mediaItem, Tag tag) {
        MediaTag mediaTag = new MediaTag();
        MediaTagId id = new MediaTagId();
        id.setMediaId(mediaItem.getId());
        id.setTagId(tag.getId());
        mediaTag.setId(id);
        mediaTag.setMediaItem(mediaItem);
        mediaTag.setTag(tag);
        mediaTag.setCreatedAt(Instant.parse("2026-03-01T10:15:30Z"));
        return mediaTag;
    }

    private MediaExternalRef buildExternalRef(
            UUID id,
            MediaItem mediaItem,
            ExternalSourceName sourceName,
            String externalId,
            String externalUrl,
            String attributionText,
            String sourcePayloadHash,
            Instant createdAt) {
        MediaExternalRef externalRef = new MediaExternalRef();
        externalRef.setId(id);
        externalRef.setMediaItem(mediaItem);
        externalRef.setSourceName(sourceName);
        externalRef.setExternalId(externalId);
        externalRef.setExternalUrl(externalUrl);
        externalRef.setAttributionText(attributionText);
        externalRef.setSourcePayloadHash(sourcePayloadHash);
        externalRef.setCreatedAt(createdAt);
        externalRef.setUpdatedAt(createdAt);
        return externalRef;
    }
}
