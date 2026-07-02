package com.moodmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.moodmatch.dto.external.ExternalReferenceResponse;
import com.moodmatch.entity.CommitmentLevel;
import com.moodmatch.entity.ConsumptionStatus;
import com.moodmatch.entity.ExternalSourceName;
import com.moodmatch.entity.MediaExternalRef;
import com.moodmatch.entity.MediaItem;
import com.moodmatch.entity.MediaType;
import com.moodmatch.entity.MetadataOrigin;
import com.moodmatch.entity.SourceType;
import com.moodmatch.repository.MediaExternalRefRepository;
import com.moodmatch.repository.MediaItemRepository;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class ExternalReferenceServiceTest {

    @Inject
    ExternalReferenceService externalReferenceService;

    @Inject
    MediaItemRepository mediaItemRepository;

    @Inject
    MediaExternalRefRepository mediaExternalRefRepository;

    @Inject
    CurrentUserProvider currentUserProvider;

    @AfterEach
    void resetCurrentUser() {
        TestCurrentUserProvider.useLocalDemoUser();
    }

    @Test
    @TestTransaction
    void shouldLookupExternalReferencesBySourceAndMedia() {
        MediaItem mediaItem = new MediaItem();
        mediaItem.setOwner(currentUserProvider.getCurrentUser());
        mediaItem.setTitle("Control");
        mediaItem.setMediaType(MediaType.GAME);
        mediaItem.setConsumptionStatus(ConsumptionStatus.CONSUMED);
        mediaItem.setRating(5);
        mediaItem.setFavourite(true);
        mediaItem.setSourceType(SourceType.EXTERNAL_SEARCH);
        mediaItem.setCommitmentLevel(CommitmentLevel.MEDIUM);
        mediaItem.setMetadataOrigin(MetadataOrigin.IMPORTED);
        mediaItemRepository.persist(mediaItem);

        MediaExternalRef externalRef = new MediaExternalRef();
        externalRef.setMediaItem(mediaItem);
        externalRef.setSourceName(ExternalSourceName.RAWG);
        externalRef.setExternalId("rawg-control");
        externalRef.setExternalUrl("https://rawg.io/games/control");
        externalRef.setAttributionText("RAWG");
        externalRef.setSourcePayloadHash("hash-123");
        mediaExternalRefRepository.persist(externalRef);

        ExternalReferenceResponse lookedUp = externalReferenceService
                .findBySourceAndExternalId(ExternalSourceName.RAWG, "rawg-control")
                .orElseThrow();
        List<ExternalReferenceResponse> byMedia = externalReferenceService.listByMediaId(mediaItem.getId());

        assertEquals(ExternalSourceName.RAWG, lookedUp.sourceName());
        assertEquals("rawg-control", lookedUp.externalId());
        assertEquals(1, byMedia.size());
        assertEquals(lookedUp.id(), byMedia.getFirst().id());
        assertTrue(externalReferenceService.findBySourceAndExternalId(ExternalSourceName.TMDB, "missing").isEmpty());
    }

    @Test
    @TestTransaction
    void shouldScopeExternalReferencesByCurrentUser() {
        TestCurrentUserProvider.useUserA();

        MediaItem mediaItem = new MediaItem();
        mediaItem.setOwner(currentUserProvider.getCurrentUser());
        mediaItem.setTitle("Alan Wake 2");
        mediaItem.setMediaType(MediaType.GAME);
        mediaItem.setConsumptionStatus(ConsumptionStatus.WANT_TO_CONSUME);
        mediaItem.setSourceType(SourceType.EXTERNAL_SEARCH);
        mediaItem.setCommitmentLevel(CommitmentLevel.MEDIUM);
        mediaItem.setMetadataOrigin(MetadataOrigin.IMPORTED);
        mediaItemRepository.persist(mediaItem);

        MediaExternalRef externalRef = new MediaExternalRef();
        externalRef.setMediaItem(mediaItem);
        externalRef.setSourceName(ExternalSourceName.RAWG);
        externalRef.setExternalId("rawg-alan-wake-2");
        externalRef.setExternalUrl("https://rawg.io/games/alan-wake-2");
        mediaExternalRefRepository.persist(externalRef);

        TestCurrentUserProvider.useUserB();

        assertTrue(externalReferenceService
                .findBySourceAndExternalId(ExternalSourceName.RAWG, "rawg-alan-wake-2")
                .isEmpty());
        assertTrue(externalReferenceService.listByMediaId(mediaItem.getId()).isEmpty());

    }
}
