package com.moodmatch.service;

import java.util.List;

import com.moodmatch.dto.matching.CandidateMediaResponse;
import com.moodmatch.dto.matching.CandidateSelectionResponse;
import com.moodmatch.entity.ConsumptionStatus;
import com.moodmatch.mapper.MatchingMapper;
import com.moodmatch.repository.MediaItemRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class CandidateService {

    @Inject
    MediaItemRepository mediaItemRepository;

    @Inject
    MatchingMapper matchingMapper;

    @Inject
    CurrentUserProvider currentUserProvider;

    @Transactional(TxType.SUPPORTS)
    public CandidateSelectionResponse listCandidates() {
        List<CandidateMediaResponse> candidates = mediaItemRepository
                .listByConsumptionStatusWithTags(
                        currentUserProvider.getCurrentUser().getId(), ConsumptionStatus.WANT_TO_CONSUME)
                .stream()
                .map(mediaItem -> new CandidateMediaResponse(
                        matchingMapper.toMatchingMediaResponse(mediaItem),
                        mediaItem.getMediaTags() != null && mediaItem.getMediaTags().stream()
                                .map(com.moodmatch.entity.MediaTag::getTag)
                                .anyMatch(java.util.Objects::nonNull)))
                .toList();

        return new CandidateSelectionResponse(candidates);
    }
}
