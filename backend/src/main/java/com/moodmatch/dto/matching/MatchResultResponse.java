package com.moodmatch.dto.matching;

import java.math.BigDecimal;
import java.util.List;

import com.moodmatch.dto.tag.TagResponse;

public record MatchResultResponse(
        CandidateMediaResponse candidate,
        int candidateTagCount,
        int matchingTagCount,
        List<MatchTagExplanationResponse> matchingTags,
        List<TagResponse> extraCandidateTags,
        BigDecimal rawScore,
        BigDecimal precisionFactor,
        BigDecimal adjustedScore,
        BigDecimal relativeScore,
        String explanationMessage,
        String candidateTagsNote) {}
