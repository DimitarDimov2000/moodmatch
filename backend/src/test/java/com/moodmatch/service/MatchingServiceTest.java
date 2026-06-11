package com.moodmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.moodmatch.dto.matching.MatchResultResponse;
import com.moodmatch.dto.matching.MatchingResponse;
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

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
class MatchingServiceTest {

    @Inject
    MatchingService matchingService;

    @Inject
    MediaService mediaService;

    @Inject
    TagService tagService;

    @Inject
    EntityManager entityManager;

    @Test
    @TestTransaction
    void shouldCalculateDeterministicScoresWithPrecisionAdjustedOverlap() {
        TagResponse scienceFiction = createTag("Science-Fiction", TagCategory.GENRE);
        TagResponse time = createTag("Zeit", TagCategory.THEME);
        TagResponse suspense = createTag("spannend", TagCategory.TONE);
        TagResponse mystery = createTag("Mystery", TagCategory.GENRE);
        TagResponse comedy = createTag("Comedy", TagCategory.GENRE);

        createMediaWithTags("Interstellar", ConsumptionStatus.CONSUMED, 5, true, List.of(scienceFiction.id(), time.id()));
        createMediaWithTags("Dark", ConsumptionStatus.CONSUMED, 4, false, List.of(time.id(), suspense.id()));
        createMediaWithTags("Arrival", ConsumptionStatus.CONSUMED, 5, false, List.of(suspense.id()));

        createMediaWithTags("The Expanse", ConsumptionStatus.WANT_TO_CONSUME, null, false, List.of(time.id(), suspense.id()));
        createMediaWithTags("Severance", ConsumptionStatus.WANT_TO_CONSUME, null, false, List.of(time.id(), mystery.id()));
        createMediaWithTags("Ted Lasso", ConsumptionStatus.WANT_TO_CONSUME, null, false, List.of(comedy.id()));

        MatchingResponse response = matchingService.calculateMatches();

        assertFalse(response.scoresSuppressed());
        assertEquals("The Expanse", response.matches().getFirst().candidate().media().title());
        assertEquals("Severance", response.matches().get(1).candidate().media().title());
        assertEquals("Ted Lasso", response.matches().get(2).candidate().media().title());

        MatchResultResponse topMatch = response.matches().getFirst();
        assertEquals(new BigDecimal("15.75"), topMatch.rawScore());
        assertEquals(new BigDecimal("1.0000"), topMatch.precisionFactor());
        assertEquals(new BigDecimal("15.75"), topMatch.adjustedScore());
        assertEquals(new BigDecimal("100.00"), topMatch.relativeScore());
        assertEquals(2, topMatch.matchingTags().size());

        MatchResultResponse partialMatch = response.matches().get(1);
        assertEquals(new BigDecimal("10.35"), partialMatch.rawScore());
        assertEquals(new BigDecimal("0.5000"), partialMatch.precisionFactor());
        assertEquals(new BigDecimal("5.18"), partialMatch.adjustedScore());
        assertEquals(new BigDecimal("32.86"), partialMatch.relativeScore());
        assertTrue(partialMatch.explanationMessage().contains("lowered precision"));

        MatchResultResponse noOverlap = response.matches().get(2);
        assertEquals(new BigDecimal("0.00"), noOverlap.rawScore());
        assertEquals(new BigDecimal("0.0000"), noOverlap.precisionFactor());
        assertEquals(new BigDecimal("0.00"), noOverlap.adjustedScore());
        assertNull(noOverlap.relativeScore());
        assertTrue(noOverlap.explanationMessage().contains("none overlap"));
    }

    @Test
    @TestTransaction
    void shouldSuppressScoresWhenProfileDataIsInsufficient() {
        TagResponse theme = createTag("Identität", TagCategory.THEME);

        createMediaWithTags("Dark Matter", ConsumptionStatus.CONSUMED, 5, false, List.of(theme.id()));
        createMediaWithTags("Silo", ConsumptionStatus.CONSUMED, 4, false, List.of(theme.id()));
        createMediaWithTags("The Leftovers", ConsumptionStatus.WANT_TO_CONSUME, null, false, List.of(theme.id()));

        MatchingResponse response = matchingService.calculateMatches();

        assertTrue(response.scoresSuppressed());
        assertEquals(1, response.matches().size());
        assertNull(response.matches().getFirst().rawScore());
        assertNull(response.matches().getFirst().precisionFactor());
        assertNull(response.matches().getFirst().adjustedScore());
        assertNull(response.matches().getFirst().relativeScore());
        assertTrue(response.explanationMessage().contains("Scores are suppressed"));
    }

    @Test
    @TestTransaction
    void shouldHandleIncompleteAndNoOverlapCandidatesClearly() {
        TagResponse theme = createTag("Überleben", TagCategory.THEME);
        TagResponse tone = createTag("intensiv", TagCategory.TONE);
        TagResponse comedy = createTag("Comedy", TagCategory.GENRE);

        createMediaWithTags("Dune", ConsumptionStatus.CONSUMED, 5, false, List.of(theme.id()));
        createMediaWithTags("Children of Men", ConsumptionStatus.CONSUMED, 5, false, List.of(theme.id(), tone.id()));
        createMediaWithTags("The Road", ConsumptionStatus.CONSUMED, 4, false, List.of(theme.id()));

        createMediaWithTags("Silo", ConsumptionStatus.WANT_TO_CONSUME, null, false, List.of(theme.id()));
        createMediaWithTags("Parks and Recreation", ConsumptionStatus.WANT_TO_CONSUME, null, false, List.of(comedy.id()));
        createMediaWithTags("Untitled Wishlist", ConsumptionStatus.WANT_TO_CONSUME, null, false, List.of());

        MatchingResponse response = matchingService.calculateMatches();

        assertFalse(response.scoresSuppressed());
        assertTrue(response.explanationMessage().contains("Relative percentages are unavailable"));

        MatchResultResponse meaningful = response.matches().getFirst();
        assertEquals("Silo", meaningful.candidate().media().title());
        assertNull(meaningful.relativeScore());
        assertTrue(meaningful.explanationMessage().contains("relative percentage is unavailable"));

        MatchResultResponse noOverlap = response.matches().stream()
                .filter(match -> "Parks and Recreation".equals(match.candidate().media().title()))
                .findFirst()
                .orElseThrow();
        assertTrue(noOverlap.candidate().isCompleteForMatching());
        assertEquals(new BigDecimal("0.00"), noOverlap.adjustedScore());
        assertTrue(noOverlap.explanationMessage().contains("none overlap"));

        MatchResultResponse incomplete = response.matches().stream()
                .filter(match -> "Untitled Wishlist".equals(match.candidate().media().title()))
                .findFirst()
                .orElseThrow();
        assertFalse(incomplete.candidate().isCompleteForMatching());
        assertEquals(new BigDecimal("0.00"), incomplete.adjustedScore());
        assertTrue(incomplete.explanationMessage().contains("no confirmed candidate tags"));
    }

    @Test
    @TestTransaction
    void shouldBreakScoreTiesByCreatedAtThenId() {
        TagResponse theme = createTag("Freundschaft", TagCategory.THEME);

        createMediaWithTags("Avatar", ConsumptionStatus.CONSUMED, 5, false, List.of(theme.id()));
        createMediaWithTags("Inside Out", ConsumptionStatus.CONSUMED, 5, false, List.of(theme.id()));
        createMediaWithTags("Ted Lasso", ConsumptionStatus.CONSUMED, 4, false, List.of(theme.id()));

        MediaResponse firstCandidate =
                createMediaWithTags("Candidate One", ConsumptionStatus.WANT_TO_CONSUME, null, false, List.of(theme.id()));
        MediaResponse secondCandidate =
                createMediaWithTags("Candidate Two", ConsumptionStatus.WANT_TO_CONSUME, null, false, List.of(theme.id()));

        Timestamp sharedTimestamp = Timestamp.from(Instant.parse("2026-01-01T00:00:00Z"));
        alignCreatedAt(firstCandidate.id(), sharedTimestamp);
        alignCreatedAt(secondCandidate.id(), sharedTimestamp);
        entityManager.flush();
        entityManager.clear();

        MatchingResponse response = matchingService.calculateMatches();

        List<UUID> firstTwoIds = response.matches().stream()
                .map(match -> match.candidate().media().id())
                .limit(2)
                .toList();
        List<UUID> expectedOrder = List.of(firstCandidate.id(), secondCandidate.id()).stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        assertEquals(expectedOrder, firstTwoIds);
    }

    private void alignCreatedAt(UUID mediaId, Timestamp createdAt) {
        entityManager.createNativeQuery("update media_items set created_at = ?1, updated_at = ?2 where id = ?3")
                .setParameter(1, createdAt)
                .setParameter(2, createdAt)
                .setParameter(3, mediaId)
                .executeUpdate();
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
                CommitmentLevel.MEDIUM,
                2024,
                null,
                MetadataOrigin.MANUAL));

        return mediaService.replaceMediaTags(media.id(), new ReplaceMediaTagsRequest(tagIds));
    }
}
