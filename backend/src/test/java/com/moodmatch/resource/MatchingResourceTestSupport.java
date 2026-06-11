package com.moodmatch.resource;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.moodmatch.dto.media.CreateMediaRequest;
import com.moodmatch.dto.media.MediaResponse;
import com.moodmatch.dto.media.ReplaceMediaTagsRequest;
import com.moodmatch.dto.tag.CreateTagRequest;
import com.moodmatch.dto.tag.TagResponse;
import com.moodmatch.entity.CommitmentLevel;
import com.moodmatch.entity.ConsumptionStatus;
import com.moodmatch.entity.MediaType;
import com.moodmatch.entity.MetadataOrigin;
import com.moodmatch.entity.SourceType;
import com.moodmatch.entity.TagCategory;
import com.moodmatch.service.MediaService;
import com.moodmatch.service.TagService;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

abstract class MatchingResourceTestSupport {

    @Inject
    EntityManager entityManager;

    @Inject
    MediaService mediaService;

    @Inject
    TagService tagService;

    @BeforeEach
    void cleanDatabaseBefore() {
        cleanDatabase();
    }

    @AfterEach
    void cleanDatabaseAfter() {
        cleanDatabase();
    }

    protected TagResponse createTag(String name, TagCategory category) {
        return tagService.createTagIfNeeded(new CreateTagRequest(name, category));
    }

    protected MediaResponse createMediaWithTags(
            String title,
            ConsumptionStatus status,
            Integer rating,
            boolean isFavourite,
            CommitmentLevel commitmentLevel,
            List<UUID> tagIds) {
        MediaResponse media = mediaService.createMedia(new CreateMediaRequest(
                title,
                title,
                "Description for " + title,
                MediaType.FILM,
                status,
                isFavourite,
                rating,
                SourceType.MANUAL,
                "Manual",
                commitmentLevel,
                2024,
                null,
                MetadataOrigin.MANUAL));

        return mediaService.replaceMediaTags(media.id(), new ReplaceMediaTagsRequest(tagIds));
    }

    private void cleanDatabase() {
        QuarkusTransaction.requiringNew().run(() -> {
            entityManager.createNativeQuery("DELETE FROM media_tags").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM media_external_refs").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM media_items").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM external_tag_mappings").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM tags").executeUpdate();
        });
    }
}
