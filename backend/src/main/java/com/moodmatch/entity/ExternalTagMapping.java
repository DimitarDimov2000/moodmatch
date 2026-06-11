package com.moodmatch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "external_tag_mappings")
public class ExternalTagMapping extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "source_name", nullable = false)
    private ExternalSourceName sourceName;

    @Column(name = "external_field", nullable = false)
    private String externalField;

    @Column(name = "external_value", nullable = false)
    private String externalValue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence", nullable = false)
    private TagMappingConfidence confidence;

    public ExternalSourceName getSourceName() {
        return sourceName;
    }

    public void setSourceName(ExternalSourceName sourceName) {
        this.sourceName = sourceName;
    }

    public String getExternalField() {
        return externalField;
    }

    public void setExternalField(String externalField) {
        this.externalField = externalField;
    }

    public String getExternalValue() {
        return externalValue;
    }

    public void setExternalValue(String externalValue) {
        this.externalValue = externalValue;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public TagMappingConfidence getConfidence() {
        return confidence;
    }

    public void setConfidence(TagMappingConfidence confidence) {
        this.confidence = confidence;
    }
}
