package com.moodmatch.mapper;

import com.moodmatch.dto.external.ExternalReferenceResponse;
import com.moodmatch.entity.MediaExternalRef;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MediaExternalRefMapper {

    public ExternalReferenceResponse toResponse(MediaExternalRef externalRef) {
        if (externalRef == null) {
            return null;
        }

        return new ExternalReferenceResponse(
                externalRef.getId(),
                externalRef.getSourceName(),
                externalRef.getExternalId(),
                externalRef.getExternalUrl(),
                externalRef.getAttributionText(),
                externalRef.getSourcePayloadHash(),
                externalRef.getCreatedAt());
    }
}
