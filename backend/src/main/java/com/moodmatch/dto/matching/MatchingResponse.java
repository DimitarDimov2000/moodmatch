package com.moodmatch.dto.matching;

import java.util.List;

public record MatchingResponse(
        InterestProfileResponse interestProfile,
        boolean scoresSuppressed,
        String explanationMessage,
        String scoringMethodNote,
        List<MatchResultResponse> matches) {}
