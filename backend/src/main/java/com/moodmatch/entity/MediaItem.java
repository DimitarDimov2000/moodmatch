package com.moodmatch.entity;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "media_items")
public class MediaItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser owner;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "original_title")
    private String originalTitle;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Enumerated(EnumType.STRING)
    @Column(name = "consumption_status", nullable = false)
    private ConsumptionStatus consumptionStatus;

    @Column(name = "is_favourite", nullable = false)
    private boolean isFavourite = false;

    @Column(name = "rating")
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType = SourceType.UNKNOWN;

    @Column(name = "source_note")
    private String sourceNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "commitment_level", nullable = false)
    private CommitmentLevel commitmentLevel = CommitmentLevel.UNKNOWN;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "cover_url", columnDefinition = "text")
    private String coverUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "external_source_name")
    private ExternalSourceName externalSourceName;

    @Column(name = "external_source_id")
    private String externalSourceId;

    @Column(name = "external_source_url", columnDefinition = "text")
    private String externalSourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "metadata_origin", nullable = false)
    private MetadataOrigin metadataOrigin = MetadataOrigin.MANUAL;

    // Confirmed local tags only. Suggested external tags belong in import flow DTOs later.
    @OneToMany(mappedBy = "mediaItem", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private Set<MediaTag> mediaTags = new LinkedHashSet<>();

    @OneToMany(mappedBy = "mediaItem", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private Set<MediaExternalRef> externalReferences = new LinkedHashSet<>();

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public ConsumptionStatus getConsumptionStatus() {
        return consumptionStatus;
    }

    public void setConsumptionStatus(ConsumptionStatus consumptionStatus) {
        this.consumptionStatus = consumptionStatus;
    }

    public boolean isFavourite() {
        return isFavourite;
    }

    public void setFavourite(boolean favourite) {
        isFavourite = favourite;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceNote() {
        return sourceNote;
    }

    public void setSourceNote(String sourceNote) {
        this.sourceNote = sourceNote;
    }

    public CommitmentLevel getCommitmentLevel() {
        return commitmentLevel;
    }

    public void setCommitmentLevel(CommitmentLevel commitmentLevel) {
        this.commitmentLevel = commitmentLevel;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public ExternalSourceName getExternalSourceName() {
        return externalSourceName;
    }

    public void setExternalSourceName(ExternalSourceName externalSourceName) {
        this.externalSourceName = externalSourceName;
    }

    public String getExternalSourceId() {
        return externalSourceId;
    }

    public void setExternalSourceId(String externalSourceId) {
        this.externalSourceId = externalSourceId;
    }

    public String getExternalSourceUrl() {
        return externalSourceUrl;
    }

    public void setExternalSourceUrl(String externalSourceUrl) {
        this.externalSourceUrl = externalSourceUrl;
    }

    public MetadataOrigin getMetadataOrigin() {
        return metadataOrigin;
    }

    public void setMetadataOrigin(MetadataOrigin metadataOrigin) {
        this.metadataOrigin = metadataOrigin;
    }

    public Set<MediaTag> getMediaTags() {
        return mediaTags;
    }

    public void setMediaTags(Set<MediaTag> mediaTags) {
        this.mediaTags = mediaTags;
    }

    public Set<MediaExternalRef> getExternalReferences() {
        return externalReferences;
    }

    public void setExternalReferences(Set<MediaExternalRef> externalReferences) {
        this.externalReferences = externalReferences;
    }
}
