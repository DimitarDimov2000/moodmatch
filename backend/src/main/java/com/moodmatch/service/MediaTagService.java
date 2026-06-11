package com.moodmatch.service;

import java.util.LinkedHashSet;
import java.util.Set;

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
        Set<Tag> resolvedTags = tagService.findOrCreateTags(request);
        mediaItem.getMediaTags().clear();

        Set<MediaTag> replacementLinks = new LinkedHashSet<>();
        for (Tag tag : resolvedTags) {
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
