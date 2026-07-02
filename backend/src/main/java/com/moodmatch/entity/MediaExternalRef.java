package com.moodmatch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "media_external_refs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_media_external_refs_media_source_external",
                        columnNames = {"media_id", "source_name", "external_id"})
        })
public class MediaExternalRef extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    private MediaItem mediaItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_name", nullable = false)
    private ExternalSourceName sourceName;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "external_url")
    private String externalUrl;

    @Column(name = "attribution_text")
    private String attributionText;

    @Column(name = "source_payload_hash")
    private String sourcePayloadHash;

    public MediaItem getMediaItem() {
        return mediaItem;
    }

    public void setMediaItem(MediaItem mediaItem) {
        this.mediaItem = mediaItem;
    }

    public ExternalSourceName getSourceName() {
        return sourceName;
    }

    public void setSourceName(ExternalSourceName sourceName) {
        this.sourceName = sourceName;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public String getAttributionText() {
        return attributionText;
    }

    public void setAttributionText(String attributionText) {
        this.attributionText = attributionText;
    }

    public String getSourcePayloadHash() {
        return sourcePayloadHash;
    }

    public void setSourcePayloadHash(String sourcePayloadHash) {
        this.sourcePayloadHash = sourcePayloadHash;
    }
}
