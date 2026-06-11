package com.moodmatch.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.moodmatch.dto.matching.CandidateMediaResponse;
import com.moodmatch.dto.matching.CandidateSelectionResponse;
import com.moodmatch.dto.matching.InterestProfileResponse;
import com.moodmatch.dto.matching.MatchResultResponse;
import com.moodmatch.dto.matching.MatchTagExplanationResponse;
import com.moodmatch.dto.matching.MatchingResponse;
import com.moodmatch.dto.tag.TagResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

/**
 * Scoring reference:
 * - weighted tag overlap is explainable, but it rewards over-tagged candidates too generously on its own.
 * - Jaccard similarity is symmetric and does not preserve MoodMatch's category and rating weights.
 * - cosine similarity is harder to explain to users and adds complexity the closed tag taxonomy does not need.
 * - MoodMatch uses precision-adjusted weighted overlap because it is deterministic, easy to explain, and penalizes over-tagging.
 */
@ApplicationScoped
public class MatchingService {

    static final String CANDIDATE_TAGS_NOTE =
            "Candidate tags are user-estimated expectations until the item is consumed and rated.";
    static final String SCORING_METHOD_NOTE =
            "Uses precision-adjusted weighted overlap instead of plain weighted overlap, Jaccard, or cosine similarity.";

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private static final Comparator<MatchTagExplanationResponse> MATCHING_TAG_COMPARATOR = Comparator
            .comparing(MatchTagExplanationResponse::profileWeight, Comparator.reverseOrder())
            .thenComparing(match -> match.tag().category())
            .thenComparing(match -> match.tag().name(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(match -> match.tag().id(), Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<CandidateScore> SCORE_ORDER = Comparator
            .comparing(CandidateScore::adjustedScoreExact, Comparator.reverseOrder())
            .thenComparing(score -> score.candidate().media().createdAt(), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(score -> score.candidate().media().id(), Comparator.nullsLast(Comparator.naturalOrder()));

    @Inject
    InterestProfileService interestProfileService;

    @Inject
    CandidateService candidateService;

    @Transactional(TxType.SUPPORTS)
    public MatchingResponse calculateMatches() {
        InterestProfileResponse interestProfile = interestProfileService.calculateInterestProfile();
        CandidateSelectionResponse candidateSelection = candidateService.listCandidates();

        if (!interestProfile.isReadyForMatching()) {
            return new MatchingResponse(
                    interestProfile,
                    true,
                    interestProfile.explanationMessage(),
                    SCORING_METHOD_NOTE,
                    candidateSelection.candidates().stream()
                            .map(candidate -> buildSuppressedResult(candidate, interestProfile.explanationMessage()))
                            .toList());
        }

        Map<UUID, BigDecimal> profileWeights = buildProfileWeightMap(interestProfile);
        List<CandidateScore> scoredCandidates = candidateSelection.candidates().stream()
                .map(candidate -> scoreCandidate(candidate, profileWeights))
                .sorted(SCORE_ORDER)
                .toList();

        List<CandidateScore> meaningfulCandidates =
                scoredCandidates.stream().filter(CandidateScore::isMeaningful).toList();
        BigDecimal maxAdjustedScore = meaningfulCandidates.stream()
                .map(CandidateScore::adjustedScoreExact)
                .max(Comparator.naturalOrder())
                .orElse(ZERO);
        boolean relativeScoreAvailable =
                meaningfulCandidates.size() >= 2 && maxAdjustedScore.compareTo(ZERO) > 0;

        List<MatchResultResponse> matches = scoredCandidates.stream()
                .map(score -> buildMatchResult(score, relativeScoreAvailable, meaningfulCandidates.size(), maxAdjustedScore))
                .toList();

        return new MatchingResponse(
                interestProfile,
                false,
                buildMatchingExplanation(candidateSelection.candidates().size(), meaningfulCandidates.size()),
                SCORING_METHOD_NOTE,
                matches);
    }

    private Map<UUID, BigDecimal> buildProfileWeightMap(InterestProfileResponse interestProfile) {
        Map<UUID, BigDecimal> weights = new LinkedHashMap<>();
        interestProfile.weightedTags().forEach(weight -> weights.put(weight.tag().id(), weight.weight()));
        return weights;
    }

    private CandidateScore scoreCandidate(CandidateMediaResponse candidate, Map<UUID, BigDecimal> profileWeights) {
        List<TagResponse> candidateTags = candidate.media().tags();
        int candidateTagCount = candidateTags.size();

        if (!candidate.isCompleteForMatching()) {
            return new CandidateScore(candidate, candidateTagCount, 0, List.of(), List.of(), ZERO, ZERO, ZERO, false);
        }

        BigDecimal rawScore = ZERO;
        List<MatchTagExplanationResponse> matchingTags = new ArrayList<>();
        List<TagResponse> extraCandidateTags = new ArrayList<>();

        for (TagResponse tag : candidateTags) {
            BigDecimal profileWeight = profileWeights.get(tag.id());
            if (profileWeight != null && profileWeight.compareTo(ZERO) > 0) {
                rawScore = rawScore.add(profileWeight);
                matchingTags.add(new MatchTagExplanationResponse(tag, InterestProfileService.roundForOutput(profileWeight, 2)));
            } else {
                extraCandidateTags.add(tag);
            }
        }

        matchingTags.sort(MATCHING_TAG_COMPARATOR);
        int matchingTagCount = matchingTags.size();
        BigDecimal precisionFactor = candidateTagCount == 0
                ? ZERO
                : BigDecimal.valueOf(matchingTagCount).divide(BigDecimal.valueOf(candidateTagCount), 8, RoundingMode.HALF_UP);
        BigDecimal adjustedScore = rawScore.multiply(precisionFactor);

        return new CandidateScore(
                candidate,
                candidateTagCount,
                matchingTagCount,
                matchingTags,
                List.copyOf(extraCandidateTags),
                rawScore,
                precisionFactor,
                adjustedScore,
                candidateTagCount > 0 && adjustedScore.compareTo(ZERO) > 0);
    }

    private MatchResultResponse buildSuppressedResult(CandidateMediaResponse candidate, String profileMessage) {
        String explanationMessage = candidate.isCompleteForMatching()
                ? profileMessage
                : profileMessage + " This candidate also needs at least one confirmed tag.";

        return new MatchResultResponse(
                candidate,
                candidate.media().tags().size(),
                0,
                List.of(),
                candidate.media().tags(),
                null,
                null,
                null,
                null,
                explanationMessage,
                CANDIDATE_TAGS_NOTE);
    }

    private MatchResultResponse buildMatchResult(
            CandidateScore score,
            boolean relativeScoreAvailable,
            int meaningfulCandidateCount,
            BigDecimal maxAdjustedScore) {
        BigDecimal relativeScore = relativeScoreAvailable && score.isMeaningful()
                ? score.adjustedScoreExact()
                        .divide(maxAdjustedScore, 8, RoundingMode.HALF_UP)
                        .multiply(ONE_HUNDRED)
                : null;

        return new MatchResultResponse(
                score.candidate(),
                score.candidateTagCount(),
                score.matchingTagCount(),
                score.matchingTags(),
                score.extraCandidateTags(),
                InterestProfileService.roundForOutput(score.rawScoreExact(), 2),
                InterestProfileService.roundForOutput(score.precisionFactorExact(), 4),
                InterestProfileService.roundForOutput(score.adjustedScoreExact(), 2),
                InterestProfileService.roundForOutput(relativeScore, 2),
                buildCandidateExplanation(score, relativeScoreAvailable, meaningfulCandidateCount),
                CANDIDATE_TAGS_NOTE);
    }

    private String buildMatchingExplanation(int candidateCount, int meaningfulCandidateCount) {
        if (candidateCount == 0) {
            return "No WANT_TO_CONSUME candidates are available for matching yet.";
        }
        if (meaningfulCandidateCount < 2) {
            return "Relative percentages are unavailable until at least two candidates have a positive adjusted score.";
        }
        return "Scores were calculated deterministically from weighted tag overlap and candidate-tag precision.";
    }

    private String buildCandidateExplanation(
            CandidateScore score, boolean relativeScoreAvailable, int meaningfulCandidateCount) {
        if (!score.candidate().isCompleteForMatching()) {
            return "This candidate cannot be matched yet because it has no confirmed candidate tags.";
        }
        if (score.matchingTagCount() == 0) {
            return "This candidate has confirmed tags, but none overlap with your current interest profile.";
        }
        if (!relativeScoreAvailable && meaningfulCandidateCount < 2) {
            return "Matched on %d of %d candidate tags, but relative percentage is unavailable because fewer than two candidates have a meaningful score."
                    .formatted(score.matchingTagCount(), score.candidateTagCount());
        }
        if (score.matchingTagCount() == score.candidateTagCount()) {
            return "Matched on all %d candidate tags."
                    .formatted(score.candidateTagCount());
        }
        return "Matched on %d of %d candidate tags; extra unmatched candidate tags lowered precision."
                .formatted(score.matchingTagCount(), score.candidateTagCount());
    }

    private record CandidateScore(
            CandidateMediaResponse candidate,
            int candidateTagCount,
            int matchingTagCount,
            List<MatchTagExplanationResponse> matchingTags,
            List<TagResponse> extraCandidateTags,
            BigDecimal rawScoreExact,
            BigDecimal precisionFactorExact,
            BigDecimal adjustedScoreExact,
            boolean isMeaningful) {}
}
