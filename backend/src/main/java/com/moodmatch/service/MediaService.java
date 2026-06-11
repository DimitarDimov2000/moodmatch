package com.moodmatch.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.moodmatch.dto.media.CreateMediaRequest;
import com.moodmatch.dto.media.MediaResponse;
import com.moodmatch.dto.media.ReplaceMediaTagsRequest;
import com.moodmatch.dto.media.UpdateMediaConsumptionStatusRequest;
import com.moodmatch.dto.media.UpdateMediaFavouriteRequest;
import com.moodmatch.dto.media.UpdateMediaRequest;
import com.moodmatch.entity.ConsumptionStatus;
import com.moodmatch.entity.MediaItem;
import com.moodmatch.entity.MetadataOrigin;
import com.moodmatch.exception.BusinessRuleViolationException;
import com.moodmatch.exception.ResourceNotFoundException;
import com.moodmatch.mapper.MediaItemMapper;
import com.moodmatch.repository.MediaItemRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class MediaService {

    @Inject
    MediaItemRepository mediaItemRepository;

    @Inject
    MediaItemMapper mediaItemMapper;

    @Inject
    MediaTagService mediaTagService;

    @Transactional
    public MediaResponse createMedia(CreateMediaRequest request) {
        Objects.requireNonNull(request, "Create media request must not be null.");

        MediaItem mediaItem = new MediaItem();
        applyMediaFields(mediaItem, request);
        mediaItemRepository.persist(mediaItem);
        return mediaItemMapper.toResponse(mediaItem);
    }

    @Transactional(TxType.SUPPORTS)
    public List<MediaResponse> listMedia() {
        return mediaItemRepository.listAllWithAssociations().stream().map(mediaItemMapper::toResponse).toList();
    }

    @Transactional(TxType.SUPPORTS)
    public MediaResponse getMediaById(UUID id) {
        return mediaItemMapper.toResponse(getMediaEntity(id));
    }

    @Transactional
    public MediaResponse updateMedia(UUID id, UpdateMediaRequest request) {
        Objects.requireNonNull(request, "Update media request must not be null.");

        MediaItem mediaItem = getMediaEntity(id);
        applyMediaFields(mediaItem, request);
        return mediaItemMapper.toResponse(mediaItem);
    }

    @Transactional
    public void deleteMedia(UUID id) {
        MediaItem mediaItem = getMediaEntity(id);
        mediaItemRepository.delete(mediaItem);
    }

    @Transactional
    public MediaResponse updateMediaConsumptionStatus(UUID id, UpdateMediaConsumptionStatusRequest request) {
        Objects.requireNonNull(request, "Update media consumption status request must not be null.");

        MediaItem mediaItem = getMediaEntity(id);
        ConsumptionStatus previousStatus = mediaItem.getConsumptionStatus();
        ConsumptionStatus nextStatus =
                Objects.requireNonNull(request.consumptionStatus(), "Consumption status must not be null.");

        if (previousStatus == ConsumptionStatus.CONSUMED
                && nextStatus != ConsumptionStatus.CONSUMED
                && !request.confirmDestructiveChange()) {
            throw new BusinessRuleViolationException(
                    "Changing a consumed item to a different status requires confirmDestructiveChange=true.");
        }

        mediaItem.setConsumptionStatus(nextStatus);
        if (nextStatus == ConsumptionStatus.CONSUMED) {
            mediaItem.setRating(requireConsumedRating(request.rating()));
            applyFavourite(mediaItem, request.isFavourite());
        } else {
            mediaItem.setRating(null);
            mediaItem.setFavourite(false);
        }

        return mediaItemMapper.toResponse(mediaItem);
    }

    @Transactional
    public MediaResponse updateMediaFavourite(UUID id, UpdateMediaFavouriteRequest request) {
        Objects.requireNonNull(request, "Update media favourite request must not be null.");

        MediaItem mediaItem = getMediaEntity(id);
        applyFavourite(mediaItem, request.isFavourite());
        return mediaItemMapper.toResponse(mediaItem);
    }

    @Transactional
    public MediaResponse replaceMediaTags(UUID id, ReplaceMediaTagsRequest request) {
        MediaItem mediaItem = getMediaEntity(id);
        mediaTagService.replaceTags(mediaItem, request);
        return mediaItemMapper.toResponse(mediaItem);
    }

    private MediaItem getMediaEntity(UUID id) {
        Objects.requireNonNull(id, "Media id must not be null.");
        return mediaItemRepository
                .findByIdWithAssociations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media item not found: " + id));
    }

    private void applyMediaFields(MediaItem mediaItem, CreateMediaRequest request) {
        mediaItem.setTitle(request.title());
        mediaItem.setOriginalTitle(request.originalTitle());
        mediaItem.setDescription(request.description());
        mediaItem.setMediaType(request.mediaType());
        mediaItem.setSourceType(request.sourceType());
        mediaItem.setSourceNote(request.sourceNote());
        mediaItem.setCommitmentLevel(request.commitmentLevel());
        mediaItem.setReleaseYear(request.releaseYear());
        mediaItem.setCoverUrl(request.coverUrl());
        mediaItem.setMetadataOrigin(request.metadataOrigin() == null ? MetadataOrigin.MANUAL : request.metadataOrigin());
        applyConsumptionState(mediaItem, request.consumptionStatus(), request.rating(), request.isFavourite());
    }

    private void applyMediaFields(MediaItem mediaItem, UpdateMediaRequest request) {
        mediaItem.setTitle(request.title());
        mediaItem.setOriginalTitle(request.originalTitle());
        mediaItem.setDescription(request.description());
        mediaItem.setMediaType(request.mediaType());
        mediaItem.setSourceType(request.sourceType());
        mediaItem.setSourceNote(request.sourceNote());
        mediaItem.setCommitmentLevel(request.commitmentLevel());
        mediaItem.setReleaseYear(request.releaseYear());
        mediaItem.setCoverUrl(request.coverUrl());
        mediaItem.setMetadataOrigin(
                request.metadataOrigin() == null ? mediaItem.getMetadataOrigin() : request.metadataOrigin());
        applyConsumptionState(mediaItem, request.consumptionStatus(), request.rating(), request.isFavourite());
    }

    private void applyConsumptionState(
            MediaItem mediaItem, ConsumptionStatus consumptionStatus, Integer rating, boolean isFavourite) {
        mediaItem.setConsumptionStatus(consumptionStatus);

        if (consumptionStatus == ConsumptionStatus.CONSUMED) {
            mediaItem.setRating(requireConsumedRating(rating));
            applyFavourite(mediaItem, isFavourite);
            return;
        }

        if (rating != null) {
            throw new BusinessRuleViolationException("Rating is only allowed for consumed media.");
        }
        if (isFavourite) {
            throw new BusinessRuleViolationException(
                    "Favourite=true is only allowed when status is CONSUMED and rating is at least 4.");
        }

        mediaItem.setRating(null);
        mediaItem.setFavourite(false);
    }

    private Integer requireConsumedRating(Integer rating) {
        if (rating == null) {
            throw new BusinessRuleViolationException("Rating is required when status is CONSUMED.");
        }
        return rating;
    }

    private void applyFavourite(MediaItem mediaItem, boolean isFavourite) {
        if (isFavourite
                && (mediaItem.getConsumptionStatus() != ConsumptionStatus.CONSUMED
                        || mediaItem.getRating() == null
                        || mediaItem.getRating() < 4)) {
            throw new BusinessRuleViolationException(
                    "Favourite=true is only allowed when status is CONSUMED and rating is at least 4.");
        }

        mediaItem.setFavourite(isFavourite);
    }
}
