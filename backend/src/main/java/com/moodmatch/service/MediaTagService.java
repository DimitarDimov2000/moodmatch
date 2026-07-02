package com.moodmatch.service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.moodmatch.dto.media.ReplaceMediaTagsRequest;
import com.moodmatch.entity.MediaItem;
import com.moodmatch.entity.MediaTag;
import com.moodmatch.entity.MediaTagId;
import com.moodmatch.entity.Tag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MediaTagService {

    @Inject
    TagService tagService;

    void replaceTags(MediaItem mediaItem, ReplaceMediaTagsRequest request) {
        Set<Tag> resolvedTags = tagService.requireTags(request);
        syncResolvedTags(mediaItem, resolvedTags, true);
    }

    void addResolvedTags(MediaItem mediaItem, Set<Tag> resolvedTags) {
        syncResolvedTags(mediaItem, resolvedTags, false);
    }

    private void syncResolvedTags(MediaItem mediaItem, Set<Tag> resolvedTags, boolean removeMissing) {
        Set<UUID> replacementTagIds = resolvedTags.stream().map(Tag::getId).collect(java.util.stream.Collectors.toSet());

        if (removeMissing) {
            mediaItem.getMediaTags().removeIf(mediaTag -> {
                Tag currentTag = mediaTag.getTag();
                return currentTag == null || !replacementTagIds.contains(currentTag.getId());
            });
        }

        Set<UUID> existingTagIds = mediaItem.getMediaTags().stream()
                .map(MediaTag::getTag)
                .filter(java.util.Objects::nonNull)
                .map(Tag::getId)
                .collect(java.util.stream.Collectors.toSet());

        Set<MediaTag> replacementLinks = new LinkedHashSet<>();
        for (Tag tag : resolvedTags) {
            if (existingTagIds.contains(tag.getId())) {
                continue;
            }

            MediaTag mediaTag = new MediaTag();
            MediaTagId id = new MediaTagId();
            id.setMediaId(mediaItem.getId());
            id.setTagId(tag.getId());
            mediaTag.setId(id);
            mediaTag.setMediaItem(mediaItem);
            mediaTag.setTag(tag);
            replacementLinks.add(mediaTag);
        }

        mediaItem.getMediaTags().addAll(replacementLinks);
    }
}
