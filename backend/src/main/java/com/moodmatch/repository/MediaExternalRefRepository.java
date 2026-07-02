package com.moodmatch.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.moodmatch.entity.ExternalSourceName;
import com.moodmatch.entity.MediaExternalRef;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MediaExternalRefRepository implements PanacheRepositoryBase<MediaExternalRef, UUID> {

    public Optional<MediaExternalRef> findBySourceNameAndExternalId(
            UUID userId, ExternalSourceName sourceName, String externalId) {
        return find(
                        "mediaItem.owner.id = ?1 and sourceName = ?2 and externalId = ?3",
                        userId,
                        sourceName,
                        externalId)
                .firstResultOptional();
    }

    public List<MediaExternalRef> findByMediaId(UUID userId, UUID mediaId) {
        return list("mediaItem.owner.id = ?1 and mediaItem.id = ?2", userId, mediaId);
    }
}
