package com.moodmatch.dto.matching;

import java.math.BigDecimal;

import com.moodmatch.dto.tag.TagResponse;

public record InterestProfileTagContributionResponse(TagResponse tag, BigDecimal contributionWeight) {}
