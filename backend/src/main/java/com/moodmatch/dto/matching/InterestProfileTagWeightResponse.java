package com.moodmatch.dto.matching;

import java.math.BigDecimal;

import com.moodmatch.dto.tag.TagResponse;

public record InterestProfileTagWeightResponse(TagResponse tag, BigDecimal weight) {}
