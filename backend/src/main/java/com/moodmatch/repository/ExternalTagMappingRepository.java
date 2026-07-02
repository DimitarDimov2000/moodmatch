package com.moodmatch.repository;

import java.util.List;
import java.util.Locale;
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

    public List<ExternalTagMapping> findBySourceAndFieldAndLowercaseValues(
            ExternalSourceName sourceName, String externalField, List<String> externalValues) {
        if (externalValues == null || externalValues.isEmpty()) {
            return List.of();
        }

        List<String> normalizedValues = externalValues.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
        if (normalizedValues.isEmpty()) {
            return List.of();
        }

        return list(
                "sourceName = ?1 and externalField = ?2 and lower(externalValue) in ?3",
                sourceName,
                externalField,
                normalizedValues);
    }

    public List<ExternalTagMapping> findByTagId(UUID tagId) {
        return list("tag.id", tagId);
    }
}
