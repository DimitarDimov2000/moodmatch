package com.moodmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.moodmatch.dto.media.CreateMediaRequest;
import com.moodmatch.dto.media.MediaResponse;
import com.moodmatch.dto.media.ReplaceMediaTagsRequest;
import com.moodmatch.dto.media.UpdateMediaConsumptionStatusRequest;
import com.moodmatch.dto.media.UpdateMediaFavouriteRequest;
import com.moodmatch.dto.tag.CreateTagRequest;
import com.moodmatch.dto.tag.TagResponse;
import com.moodmatch.entity.CommitmentLevel;
import com.moodmatch.entity.ConsumptionStatus;
import com.moodmatch.entity.MediaType;
import com.moodmatch.entity.MetadataOrigin;
import com.moodmatch.entity.SourceType;
import com.moodmatch.entity.TagCategory;
import com.moodmatch.exception.BusinessRuleViolationException;
import com.moodmatch.repository.TagRepository;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class MediaServiceTest {

    @Inject
    MediaService mediaService;

    @Inject
    TagService tagService;

    @Inject
    TagRepository tagRepository;

    @Test
    @TestTransaction
    void shouldCreateListGetAndDeleteMedia() {
        MediaResponse created = mediaService.createMedia(buildConsumedRequest("Interstellar", 5, true));

        assertNotNull(created.id());
        assertEquals("Interstellar", created.title());
        assertTrue(created.isFavourite());

        List<MediaResponse> listed = mediaService.listMedia();
        assertEquals(1, listed.size());
        assertEquals(created.id(), listed.getFirst().id());

        MediaResponse fetched = mediaService.getMediaById(created.id());
        assertEquals(created.id(), fetched.id());
        assertEquals(ConsumptionStatus.CONSUMED, fetched.consumptionStatus());

        mediaService.deleteMedia(created.id());

        assertTrue(mediaService.listMedia().isEmpty());
    }

    @Test
    @TestTransaction
    void shouldRejectFavouriteDuringCreateWhenRatingIsTooLow() {
        assertThrows(
                BusinessRuleViolationException.class,
                () -> mediaService.createMedia(buildConsumedRequest("Low Rated Favourite", 3, true)));
    }

    @Test
    @TestTransaction
    void shouldRejectFavouritePatchWhenCurrentRatingIsTooLow() {
        MediaResponse created = mediaService.createMedia(buildConsumedRequest("Arrival", 3, false));

        assertThrows(
                BusinessRuleViolationException.class,
                () -> mediaService.updateMediaFavourite(created.id(), new UpdateMediaFavouriteRequest(true)));
    }

    @Test
    @TestTransaction
    void shouldRequireConfirmationWhenLeavingConsumedStatus() {
        MediaResponse created = mediaService.createMedia(buildConsumedRequest("The Expanse", 5, true));

        assertThrows(
                BusinessRuleViolationException.class,
                () -> mediaService.updateMediaConsumptionStatus(
                        created.id(),
                        new UpdateMediaConsumptionStatusRequest(
                                ConsumptionStatus.WANT_TO_CONSUME, null, false, false)));

        MediaResponse updated = mediaService.updateMediaConsumptionStatus(
                created.id(),
                new UpdateMediaConsumptionStatusRequest(ConsumptionStatus.WANT_TO_CONSUME, null, false, true));

        assertEquals(ConsumptionStatus.WANT_TO_CONSUME, updated.consumptionStatus());
        assertNull(updated.rating());
        assertFalse(updated.isFavourite());
    }

    @Test
    @TestTransaction
    void shouldReplaceMediaTagsUsingExistingAndCreatedTagsWithoutDuplicates() {
        MediaResponse created = mediaService.createMedia(new CreateMediaRequest(
                "Dune",
                null,
                "Epic science fiction.",
                MediaType.BOOK,
                ConsumptionStatus.WANT_TO_CONSUME,
                false,
                null,
                SourceType.MANUAL,
                null,
                CommitmentLevel.LONG,
                1965,
                null,
                MetadataOrigin.MANUAL));

        TagResponse existingGenre = tagService.createTagIfNeeded(new CreateTagRequest("Science-Fiction", TagCategory.GENRE));

        MediaResponse updated = mediaService.replaceMediaTags(
                created.id(),
                new ReplaceMediaTagsRequest(List.of(existingGenre.id())));

        assertEquals(1, updated.tags().size());
        assertEquals("Science-Fiction", updated.tags().getFirst().name());

        TagResponse spaceOpera = tagService.createTagIfNeeded(new CreateTagRequest("Space Opera", TagCategory.GENRE));
        MediaResponse replaced = mediaService.replaceMediaTags(
                created.id(),
                new ReplaceMediaTagsRequest(List.of(existingGenre.id(), existingGenre.id(), spaceOpera.id())));

        assertEquals(2, replaced.tags().size());
        assertEquals("Science-Fiction", replaced.tags().getFirst().name());
        assertEquals("Space Opera", replaced.tags().get(1).name());
        assertEquals(2, tagRepository.count());
    }

    @Test
    @TestTransaction
    void shouldPersistExpandedMediaTypes() {
        MediaResponse created = mediaService.createMedia(new CreateMediaRequest(
                "The Fellowship of the Ring",
                null,
                "Unabridged audiobook edition.",
                MediaType.AUDIOBOOK,
                ConsumptionStatus.WANT_TO_CONSUME,
                false,
                null,
                SourceType.MANUAL,
                null,
                CommitmentLevel.LONG,
                1954,
                null,
                MetadataOrigin.MANUAL));

        assertEquals(MediaType.AUDIOBOOK, created.mediaType());
    }

    private CreateMediaRequest buildConsumedRequest(String title, int rating, boolean isFavourite) {
        return new CreateMediaRequest(
                title,
                title,
                "Description for " + title,
                MediaType.FILM,
                ConsumptionStatus.CONSUMED,
                isFavourite,
                rating,
                SourceType.MANUAL,
                "Own library",
                CommitmentLevel.LONG,
                2020,
                "https://example.com/" + title.toLowerCase().replace(' ', '-') + ".jpg",
                MetadataOrigin.MANUAL);
    }
}
