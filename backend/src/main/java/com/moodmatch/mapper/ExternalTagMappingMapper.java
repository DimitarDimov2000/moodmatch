package com.moodmatch.mapper;

import com.moodmatch.dto.external.ExternalTagMappingResponse;
import com.moodmatch.entity.ExternalTagMapping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ExternalTagMappingMapper {

    @Inject
    TagMapper tagMapper;

    public ExternalTagMappingResponse toResponse(ExternalTagMapping mapping) {
        if (mapping == null) {
            return null;
        }

        return new ExternalTagMappingResponse(
                mapping.getId(),
                mapping.getSourceName(),
                mapping.getExternalField(),
                mapping.getExternalValue(),
                tagMapper.toResponse(mapping.getTag()),
                mapping.getConfidence(),
                mapping.getCreatedAt(),
                mapping.getUpdatedAt());
    }
}
