package com.moodmatch.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class MediaTagId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "media_id", nullable = false)
    private UUID mediaId;

    @Column(name = "tag_id", nullable = false)
    private UUID tagId;

    public UUID getMediaId() {
        return mediaId;
    }

    public void setMediaId(UUID mediaId) {
        this.mediaId = mediaId;
    }

    public UUID getTagId() {
        return tagId;
    }

    public void setTagId(UUID tagId) {
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MediaTagId that)) {
            return false;
        }
        return Objects.equals(mediaId, that.mediaId) && Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mediaId, tagId);
    }
}
