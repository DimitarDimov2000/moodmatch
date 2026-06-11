package com.moodmatch.entity;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "tags",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tags_name_category", columnNames = {"name", "category"})
        })
public class Tag extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private TagCategory category;

    @OneToMany(mappedBy = "tag", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private Set<MediaTag> mediaTags = new LinkedHashSet<>();

    @OneToMany(mappedBy = "tag", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private Set<ExternalTagMapping> externalMappings = new LinkedHashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TagCategory getCategory() {
        return category;
    }

    public void setCategory(TagCategory category) {
        this.category = category;
    }

    public Set<MediaTag> getMediaTags() {
        return mediaTags;
    }

    public void setMediaTags(Set<MediaTag> mediaTags) {
        this.mediaTags = mediaTags;
    }

    public Set<ExternalTagMapping> getExternalMappings() {
        return externalMappings;
    }

    public void setExternalMappings(Set<ExternalTagMapping> externalMappings) {
        this.externalMappings = externalMappings;
    }
}
