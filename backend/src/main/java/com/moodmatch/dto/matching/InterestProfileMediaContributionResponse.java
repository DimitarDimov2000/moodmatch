package com.moodmatch.dto.matching;

import java.math.BigDecimal;
import java.util.List;

public record InterestProfileMediaContributionResponse(
        MatchingMediaResponse media,
        BigDecimal ratingWeight,
        BigDecimal favouriteFactor,
        List<InterestProfileTagContributionResponse> tagContributions) {}
