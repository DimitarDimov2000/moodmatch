package com.moodmatch.mapper;

import java.util.Comparator;
import java.util.List;

import com.moodmatch.dto.matching.MatchingMediaResponse;
import com.moodmatch.entity.MediaItem;
import com.moodmatch.entity.Tag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MatchingMapper {

    private static final Comparator<Tag> TAG_COMPARATOR = Comparator.comparing(Tag::getCategory)
            .thenComparing(Tag::getName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Tag::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    @Inject
    TagMapper tagMapper;

    public MatchingMediaResponse toMatchingMediaResponse(MediaItem mediaItem) {
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

        return new MatchingMediaResponse(
                mediaItem.getId(),
                mediaItem.getTitle(),
                mediaItem.getMediaType(),
                mediaItem.getConsumptionStatus(),
                mediaItem.getCommitmentLevel(),
                mediaItem.isFavourite(),
                mediaItem.getRating(),
                mediaItem.getReleaseYear(),
                mediaItem.getCoverUrl(),
                tags,
                mediaItem.getCreatedAt(),
                mediaItem.getUpdatedAt());
    }
}
