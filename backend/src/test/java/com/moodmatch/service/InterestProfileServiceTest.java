package com.moodmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.moodmatch.dto.matching.InterestProfileResponse;
import com.moodmatch.dto.matching.InterestProfileTagWeightResponse;
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

@QuarkusTest
class InterestProfileServiceTest {

    @Inject
    InterestProfileService interestProfileService;

    @Inject
    MediaService mediaService;

    @Inject
    TagService tagService;

    @Test
    @TestTransaction
    void shouldAggregateProfileWeightsAndIgnoreNonRelevantMedia() {
        TagResponse genre = createTag("Science-Fiction", TagCategory.GENRE);
        TagResponse theme = createTag("Zeit", TagCategory.THEME);
        TagResponse tone = createTag("melancholisch", TagCategory.TONE);

        createMediaWithTags("Interstellar", ConsumptionStatus.CONSUMED, 4, false, List.of(genre.id(), theme.id()));
        createMediaWithTags("Dark", ConsumptionStatus.CONSUMED, 5, true, List.of(theme.id(), tone.id()));
        createMediaWithTags("Arrival", ConsumptionStatus.CONSUMED, 5, false, List.of(theme.id()));

        createMediaWithTags("Low Rated", ConsumptionStatus.CONSUMED, 3, false, List.of(theme.id()));
        createMediaWithTags("Queued", ConsumptionStatus.WANT_TO_CONSUME, null, false, List.of(theme.id()));
        createMediaWithTags("Tagless", ConsumptionStatus.CONSUMED, 5, false, List.of());

        InterestProfileResponse profile = interestProfileService.calculateInterestProfile();

        assertTrue(profile.isReadyForMatching());
        assertEquals(3, profile.profileRelevantMediaCount());
        assertEquals(3, profile.contributingMedia().size());

        Map<String, InterestProfileTagWeightResponse> weightsByTag = profile.weightedTags().stream()
                .collect(Collectors.toMap(weight -> weight.tag().name(), Function.identity()));

        assertEquals(new BigDecimal("14.85"), weightsByTag.get("Zeit").weight());
        assertEquals(new BigDecimal("4.50"), weightsByTag.get("melancholisch").weight());
        assertEquals(new BigDecimal("1.20"), weightsByTag.get("Science-Fiction").weight());
        assertEquals("Zeit", profile.weightedTags().getFirst().tag().name());
    }

    @Test
    @TestTransaction
    void shouldRemainNotReadyWhenFewerThanThreeRelevantMediaExist() {
        TagResponse theme = createTag("Identität", TagCategory.THEME);

        createMediaWithTags("Severance", ConsumptionStatus.CONSUMED, 5, false, List.of(theme.id()));
        createMediaWithTags("Dark Matter", ConsumptionStatus.CONSUMED, 4, false, List.of(theme.id()));
        createMediaWithTags("Wishlist", ConsumptionStatus.WANT_TO_CONSUME, null, false, List.of(theme.id()));

        InterestProfileResponse profile = interestProfileService.calculateInterestProfile();

        assertFalse(profile.isReadyForMatching());
        assertEquals(2, profile.profileRelevantMediaCount());
        assertTrue(profile.explanationMessage().contains("Current count: 2"));
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
                MediaType.SERIES,
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
