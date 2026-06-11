package com.moodmatch.repository;

import java.util.List;
import java.util.UUID;

import com.moodmatch.entity.ExternalSourceName;
import com.moodmatch.entity.ExternalTagMapping;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ExternalTagMappingRepository implements PanacheRepositoryBase<ExternalTagMapping, UUID> {

    public List<ExternalTagMapping> findBySourceAndFieldAndValue(
            ExternalSourceName sourceName, String externalField, String externalValue) {
        return list(
                "sourceName = ?1 and externalField = ?2 and externalValue = ?3",
                sourceName,
                externalField,
                externalValue);
    }

    public List<ExternalTagMapping> findByTagId(UUID tagId) {
        return list("tag.id", tagId);
    }
}
