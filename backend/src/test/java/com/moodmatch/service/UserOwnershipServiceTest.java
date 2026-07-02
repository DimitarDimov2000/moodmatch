package com.moodmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.moodmatch.dto.matching.CandidateSelectionResponse;
import com.moodmatch.dto.matching.InterestProfileResponse;
import com.moodmatch.dto.matching.MatchingResponse;
import com.moodmatch.dto.media.CreateMediaRequest;
import com.moodmatch.dto.media.MediaResponse;
import com.moodmatch.dto.media.ReplaceMediaTagsRequest;
import com.moodmatch.dto.media.UpdateMediaRequest;
import com.moodmatch.dto.tag.CreateTagRequest;
import com.moodmatch.dto.tag.TagResponse;
import com.moodmatch.entity.CommitmentLevel;
import com.moodmatch.entity.ConsumptionStatus;
import com.moodmatch.entity.MediaType;
import com.moodmatch.entity.MetadataOrigin;
import com.moodmatch.entity.SourceType;
import com.moodmatch.entity.TagCategory;
import com.moodmatch.exception.ResourceNotFoundException;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class UserOwnershipServiceTest {

    @Inject
    MediaService mediaService;

    @Inject
    TagService tagService;

    @Inject
    InterestProfileService interestProfileService;

    @Inject
    CandidateService candidateService;

    @Inject
    MatchingService matchingService;

    @AfterEach
    void resetCurrentUser() {
        TestCurrentUserProvider.useLocalDemoUser();
    }

    @Test
    @TestTransaction
    void shouldIsolateMediaVisibilityAcrossUsers() {
        TestCurrentUserProvider.useUserA();
        MediaResponse userAMedia = mediaService.createMedia(buildMediaRequest(
                "Arrival", ConsumptionStatus.WANT_TO_CONSUME, null, false, MediaType.FILM));

        TestCurrentUserProvider.useUserB();

        assertTrue(mediaService.listMedia().isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> mediaService.getMediaById(userAMedia.id()));
    }

    @Test
    @TestTransaction
    void shouldRejectCrossUserMediaAccessForUpdateAndDelete() {
        TestCurrentUserProvider.useUserA();
        MediaResponse userAMedia =
                mediaService.createMedia(buildMediaRequest("Dune", ConsumptionStatus.WANT_TO_CONSUME, null, false, MediaType.BOOK));

        TestCurrentUserProvider.useUserB();

        assertThrows(
                ResourceNotFoundException.class,
                () -> mediaService.updateMedia(
                        userAMedia.id(),
                        new UpdateMediaRequest(
                                "Dune Messiah",
                                null,
                                "Should not be updated by another user.",
                                MediaType.BOOK,
                                ConsumptionStatus.WANT_TO_CONSUME,
                                false,
                                null,
                                SourceType.MANUAL,
                                null,
                                CommitmentLevel.LONG,
                                1969,
                                null,
                                MetadataOrigin.MANUAL)));
        assertThrows(ResourceNotFoundException.class, () -> mediaService.deleteMedia(userAMedia.id()));

        TestCurrentUserProvider.useUserA();
        assertEquals("Dune", mediaService.getMediaById(userAMedia.id()).title());
    }

    @Test
    @TestTransaction
    void shouldScopeProfileCandidatesAndMatchesByCurrentUser() {
        TagResponse identity = createTag("Identity", TagCategory.THEME);
        TagResponse melancholy = createTag("Melancholy", TagCategory.TONE);
        TagResponse politics = createTag("Politics+", TagCategory.THEME);
        TagResponse comedy = createTag("Comedy+", TagCategory.GENRE);

        TestCurrentUserProvider.useUserA();
        createMediaWithTags("Arrival", ConsumptionStatus.CONSUMED, 5, true, List.of(identity.id(), melancholy.id()));
        createMediaWithTags("Severance", ConsumptionStatus.CONSUMED, 4, false, List.of(identity.id()));
        createMediaWithTags("Dark", ConsumptionStatus.CONSUMED, 5, false, List.of(identity.id()));
        createMediaWithTags("Solaris", ConsumptionStatus.WANT_TO_CONSUME, null, false, List.of(identity.id()));

        TestCurrentUserProvider.useUserB();
        createMediaWithTags("The Crown", ConsumptionStatus.CONSUMED, 5, false, List.of(politics.id()));
        createMediaWithTags("Borgen", ConsumptionStatus.CONSUMED, 4, false, List.of(politics.id()));
        createMediaWithTags("The West Wing", ConsumptionStatus.CONSUMED, 5, false, List.of(politics.id()));
        createMediaWithTags("Abbott Elementary", ConsumptionStatus.WANT_TO_CONSUME, null, false, List.of(comedy.id()));

        TestCurrentUserProvider.useUserA();
        InterestProfileResponse userAProfile = interestProfileService.calculateInterestProfile();
        CandidateSelectionResponse userACandidates = candidateService.listCandidates();
        MatchingResponse userAMatches = matchingService.calculateMatches();

        assertTrue(userAProfile.isReadyForMatching());
        assertEquals(List.of("Identity", "Melancholy"), userAProfile.weightedTags().stream()
                .map(weight -> weight.tag().name())
                .toList());
        assertEquals(List.of("Solaris"), userACandidates.candidates().stream()
                .map(candidate -> candidate.media().title())
                .toList());
        assertEquals(List.of("Solaris"), userAMatches.matches().stream()
                .map(match -> match.candidate().media().title())
                .toList());

        TestCurrentUserProvider.useUserB();
        InterestProfileResponse userBProfile = interestProfileService.calculateInterestProfile();
        CandidateSelectionResponse userBCandidates = candidateService.listCandidates();
        MatchingResponse userBMatches = matchingService.calculateMatches();

        assertTrue(userBProfile.isReadyForMatching());
        assertEquals(List.of("Politics+"), userBProfile.weightedTags().stream()
                .map(weight -> weight.tag().name())
                .toList());
        assertEquals(List.of("Abbott Elementary"), userBCandidates.candidates().stream()
                .map(candidate -> candidate.media().title())
                .toList());
        assertEquals(List.of("Abbott Elementary"), userBMatches.matches().stream()
                .map(match -> match.candidate().media().title())
                .toList());
    }

    private TagResponse createTag(String name, TagCategory category) {
        return tagService.createTagIfNeeded(new CreateTagRequest(name, category));
    }

    private MediaResponse createMediaWithTags(
            String title,
            ConsumptionStatus status,
            Integer rating,
            boolean isFavourite,
            List<UUID> tagIds) {
        MediaResponse media = mediaService.createMedia(buildMediaRequest(title, status, rating, isFavourite, MediaType.SERIES));
        return mediaService.replaceMediaTags(media.id(), new ReplaceMediaTagsRequest(tagIds));
    }

    private CreateMediaRequest buildMediaRequest(
            String title, ConsumptionStatus status, Integer rating, boolean isFavourite, MediaType mediaType) {
        return new CreateMediaRequest(
                title,
                title,
                "Description for " + title,
                mediaType,
                status,
                isFavourite,
                rating,
                SourceType.MANUAL,
                "Manual",
                CommitmentLevel.MEDIUM,
                2024,
                null,
                MetadataOrigin.MANUAL);
    }
}
