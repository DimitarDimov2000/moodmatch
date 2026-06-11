package com.moodmatch.repository;

import java.util.List;
import java.util.UUID;

import com.moodmatch.entity.MediaTag;
import com.moodmatch.entity.MediaTagId;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MediaTagRepository implements PanacheRepositoryBase<MediaTag, MediaTagId> {

    public List<MediaTag> findByMediaId(UUID mediaId) {
        return list("mediaItem.id", mediaId);
    }

    public List<MediaTag> findByTagId(UUID tagId) {
        return list("tag.id", tagId);
    }

    public boolean existsByMediaIdAndTagId(UUID mediaId, UUID tagId) {
        return count("mediaItem.id = ?1 and tag.id = ?2", mediaId, tagId) > 0;
    }
}
