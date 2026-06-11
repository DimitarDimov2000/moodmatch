package com.moodmatch.mapper;

import java.util.Comparator;
import java.util.List;

import com.moodmatch.dto.media.MediaResponse;
import com.moodmatch.entity.MediaItem;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MediaItemMapper {

    private static final Comparator<com.moodmatch.entity.Tag> TAG_COMPARATOR =
            Comparator.comparing(com.moodmatch.entity.Tag::getCategory)
                    .thenComparing(com.moodmatch.entity.Tag::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(com.moodmatch.entity.Tag::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<com.moodmatch.entity.MediaExternalRef> EXTERNAL_REF_COMPARATOR =
            Comparator.comparing(com.moodmatch.entity.MediaExternalRef::getSourceName)
                    .thenComparing(com.moodmatch.entity.MediaExternalRef::getExternalId, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(
                            com.moodmatch.entity.MediaExternalRef::getId,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    @Inject
    TagMapper tagMapper;

    @Inject
    MediaExternalRefMapper mediaExternalRefMapper;

    public MediaResponse toResponse(MediaItem mediaItem) {
        if (mediaItem == null) {
            return null;
        }

        List<com.moodmatch.dto.tag.TagResponse> tags = mediaItem.getMediaTags() == null
                ? List.of()
                : mediaItem.getMediaTags().stream()
                        .map(com.moodmatch.entity.MediaTag::getTag)
                        .filter(java.util.Objects::nonNull)
                        .sorted(TAG_COMPARATOR)
                        .map(tagMapper::toResponse)
                        .toList();

        List<com.moodmatch.dto.external.ExternalReferenceResponse> externalReferences =
                mediaItem.getExternalReferences() == null
                        ? List.of()
                        : mediaItem.getExternalReferences().stream()
                                .filter(java.util.Objects::nonNull)
                                .sorted(EXTERNAL_REF_COMPARATOR)
                                .map(mediaExternalRefMapper::toResponse)
                                .toList();

        return new MediaResponse(
                mediaItem.getId(),
                mediaItem.getTitle(),
                mediaItem.getOriginalTitle(),
                mediaItem.getDescription(),
                mediaItem.getMediaType(),
                mediaItem.getConsumptionStatus(),
                mediaItem.isFavourite(),
                mediaItem.getRating(),
                mediaItem.getSourceType(),
                mediaItem.getSourceNote(),
                mediaItem.getCommitmentLevel(),
                mediaItem.getReleaseYear(),
                mediaItem.getCoverUrl(),
                mediaItem.getExternalSourceName(),
                mediaItem.getExternalSourceId(),
                mediaItem.getExternalSourceUrl(),
                mediaItem.getMetadataOrigin(),
                tags,
                externalReferences,
                mediaItem.getCreatedAt(),
                mediaItem.getUpdatedAt());
    }
}
