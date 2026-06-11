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
            ExternalSourceName sourceName, String externalId) {
        return find("sourceName = ?1 and externalId = ?2", sourceName, externalId).firstResultOptional();
    }

    public List<MediaExternalRef> findByMediaId(UUID mediaId) {
        return list("mediaItem.id", mediaId);
    }
}
