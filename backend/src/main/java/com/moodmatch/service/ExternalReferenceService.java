package com.moodmatch.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.moodmatch.dto.external.ExternalReferenceResponse;
import com.moodmatch.entity.ExternalSourceName;
import com.moodmatch.mapper.MediaExternalRefMapper;
import com.moodmatch.repository.MediaExternalRefRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional.TxType;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ExternalReferenceService {

    @Inject
    MediaExternalRefRepository mediaExternalRefRepository;

    @Inject
    MediaExternalRefMapper mediaExternalRefMapper;

    @Inject
    CurrentUserProvider currentUserProvider;

    @Transactional(TxType.SUPPORTS)
    public Optional<ExternalReferenceResponse> findBySourceAndExternalId(
            ExternalSourceName sourceName, String externalId) {
        return mediaExternalRefRepository
                .findBySourceNameAndExternalId(currentUserProvider.getCurrentUser().getId(), sourceName, externalId)
                .map(mediaExternalRefMapper::toResponse);
    }

    @Transactional(TxType.SUPPORTS)
    public List<ExternalReferenceResponse> listByMediaId(UUID mediaId) {
        return mediaExternalRefRepository
                .findByMediaId(currentUserProvider.getCurrentUser().getId(), mediaId)
                .stream()
                .map(mediaExternalRefMapper::toResponse)
                .toList();
    }
}
