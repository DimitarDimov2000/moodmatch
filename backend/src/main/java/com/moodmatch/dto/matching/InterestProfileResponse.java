package com.moodmatch.dto.matching;

import java.util.List;

public record InterestProfileResponse(
        boolean isReadyForMatching,
        int profileRelevantMediaCount,
        int requiredProfileRelevantMediaCount,
        String explanationMessage,
        List<InterestProfileMediaContributionResponse> contributingMedia,
        List<InterestProfileTagWeightResponse> weightedTags) {}
