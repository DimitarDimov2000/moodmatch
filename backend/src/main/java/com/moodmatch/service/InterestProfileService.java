package com.moodmatch.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.moodmatch.dto.matching.InterestProfileMediaContributionResponse;
import com.moodmatch.dto.matching.InterestProfileResponse;
import com.moodmatch.dto.matching.InterestProfileTagContributionResponse;
import com.moodmatch.dto.matching.InterestProfileTagWeightResponse;
import com.moodmatch.dto.matching.MatchingMediaResponse;
import com.moodmatch.dto.tag.TagResponse;
import com.moodmatch.entity.ConsumptionStatus;
import com.moodmatch.entity.TagCategory;
import com.moodmatch.mapper.MatchingMapper;
import com.moodmatch.repository.MediaItemRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class InterestProfileService {

    static final int MINIMUM_PROFILE_RELEVANT_MEDIA = 3;

    private static final BigDecimal FAVOURITE_FACTOR = decimal("1.5");
    private static final BigDecimal NON_FAVOURITE_FACTOR = BigDecimal.ONE;

    private static final Map<Integer, BigDecimal> RATING_WEIGHTS = Map.of(4, decimal("1.2"), 5, decimal("1.5"));

    private static final Map<TagCategory, BigDecimal> CATEGORY_WEIGHTS = buildCategoryWeights();

    private static final Comparator<InterestProfileTagWeightResponse> WEIGHTED_TAG_COMPARATOR = Comparator
            .comparing(InterestProfileTagWeightResponse::weight, Comparator.reverseOrder())
            .thenComparing(weight -> weight.tag().category())
            .thenComparing(weight -> weight.tag().name(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(weight -> weight.tag().id(), Comparator.nullsLast(Comparator.naturalOrder()));

    @Inject
    MediaItemRepository mediaItemRepository;

    @Inject
    MatchingMapper matchingMapper;

    @Transactional(TxType.SUPPORTS)
    public InterestProfileResponse calculateInterestProfile() {
        List<com.moodmatch.entity.MediaItem> relevantMedia = mediaItemRepository
                .listByConsumptionStatusWithTags(ConsumptionStatus.CONSUMED)
                .stream()
                .filter(this::isProfileRelevant)
                .toList();

        Map<UUID, WeightedTagAccumulator> accumulatedWeights = new LinkedHashMap<>();
        List<InterestProfileMediaContributionResponse> contributingMedia = new ArrayList<>();

        for (com.moodmatch.entity.MediaItem mediaItem : relevantMedia) {
            MatchingMediaResponse media = matchingMapper.toMatchingMediaResponse(mediaItem);
            BigDecimal ratingWeight = requireRatingWeight(media.rating());
            BigDecimal favouriteFactor = media.isFavourite() ? FAVOURITE_FACTOR : NON_FAVOURITE_FACTOR;

            List<InterestProfileTagContributionResponse> tagContributions = media.tags().stream()
                    .map(tag -> buildTagContribution(tag, ratingWeight, favouriteFactor, accumulatedWeights))
                    .toList();

            contributingMedia.add(new InterestProfileMediaContributionResponse(
                    media,
                    roundForOutput(ratingWeight, 2),
                    roundForOutput(favouriteFactor, 2),
                    tagContributions));
        }

        List<InterestProfileTagWeightResponse> weightedTags = accumulatedWeights.values().stream()
                .map(accumulator ->
                        new InterestProfileTagWeightResponse(accumulator.tag(), roundForOutput(accumulator.weight(), 2)))
                .sorted(WEIGHTED_TAG_COMPARATOR)
                .toList();

        boolean isReadyForMatching = relevantMedia.size() >= MINIMUM_PROFILE_RELEVANT_MEDIA;
        String explanationMessage = isReadyForMatching
                ? "Profile built from %d profile-relevant media items.".formatted(relevantMedia.size())
                : "Scores are suppressed until at least %d consumed items rated 4 or 5 with confirmed tags are available. Current count: %d."
                        .formatted(MINIMUM_PROFILE_RELEVANT_MEDIA, relevantMedia.size());

        return new InterestProfileResponse(
                isReadyForMatching,
                relevantMedia.size(),
                MINIMUM_PROFILE_RELEVANT_MEDIA,
                explanationMessage,
                contributingMedia,
                weightedTags);
    }

    private boolean isProfileRelevant(com.moodmatch.entity.MediaItem mediaItem) {
        return mediaItem != null
                && mediaItem.getConsumptionStatus() == ConsumptionStatus.CONSUMED
                && requireRatingWeight(mediaItem.getRating()) != null
                && mediaItem.getMediaTags() != null
                && mediaItem.getMediaTags().stream()
                        .map(com.moodmatch.entity.MediaTag::getTag)
                        .anyMatch(java.util.Objects::nonNull);
    }

    private InterestProfileTagContributionResponse buildTagContribution(
            TagResponse tag,
            BigDecimal ratingWeight,
            BigDecimal favouriteFactor,
            Map<UUID, WeightedTagAccumulator> accumulatedWeights) {
        BigDecimal contributionWeight = ratingWeight.multiply(favouriteFactor).multiply(categoryWeight(tag.category()));

        WeightedTagAccumulator accumulator =
                accumulatedWeights.computeIfAbsent(tag.id(), ignored -> new WeightedTagAccumulator(tag, BigDecimal.ZERO));
        accumulator.add(contributionWeight);

        return new InterestProfileTagContributionResponse(tag, roundForOutput(contributionWeight, 2));
    }

    private BigDecimal requireRatingWeight(Integer rating) {
        return rating == null ? null : RATING_WEIGHTS.get(rating);
    }

    private BigDecimal categoryWeight(TagCategory category) {
        return CATEGORY_WEIGHTS.getOrDefault(category, BigDecimal.ZERO);
    }

    private static Map<TagCategory, BigDecimal> buildCategoryWeights() {
        Map<TagCategory, BigDecimal> weights = new EnumMap<>(TagCategory.class);
        weights.put(TagCategory.THEME, decimal("3.0"));
        weights.put(TagCategory.EXPERIENCE, decimal("2.5"));
        weights.put(TagCategory.TONE, decimal("2.0"));
        weights.put(TagCategory.SETTING, decimal("1.5"));
        weights.put(TagCategory.GENRE, decimal("1.0"));
        return Map.copyOf(weights);
    }

    static BigDecimal roundForOutput(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private static final class WeightedTagAccumulator {
        private final TagResponse tag;
        private BigDecimal weight;

        private WeightedTagAccumulator(TagResponse tag, BigDecimal weight) {
            this.tag = tag;
            this.weight = weight;
        }

        private TagResponse tag() {
            return tag;
        }

        private BigDecimal weight() {
            return weight;
        }

        private void add(BigDecimal contribution) {
            weight = weight.add(contribution);
        }
    }
}
