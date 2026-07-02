package com.moodmatch.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.moodmatch.entity.ConsumptionStatus;
import com.moodmatch.entity.ExternalSourceName;
import com.moodmatch.entity.MediaItem;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MediaItemRepository implements PanacheRepositoryBase<MediaItem, UUID> {

    public List<MediaItem> findByConsumptionStatus(UUID userId, ConsumptionStatus consumptionStatus) {
        return list("owner.id = ?1 and consumptionStatus = ?2", userId, consumptionStatus);
    }

    public List<MediaItem> listByConsumptionStatusWithTags(UUID userId, ConsumptionStatus consumptionStatus) {
        return list(
                "select distinct mediaItem from MediaItem mediaItem "
                        + "left join fetch mediaItem.mediaTags mediaTags "
                        + "left join fetch mediaTags.tag "
                        + "where mediaItem.owner.id = ?1 and mediaItem.consumptionStatus = ?2 "
                        + "order by mediaItem.createdAt asc, mediaItem.id asc",
                userId,
                consumptionStatus);
    }

    public List<MediaItem> listAllWithAssociations(UUID userId) {
        return list(
                "select distinct mediaItem from MediaItem mediaItem "
                        + "left join fetch mediaItem.mediaTags mediaTags "
                        + "left join fetch mediaTags.tag "
                        + "left join fetch mediaItem.externalReferences "
                        + "where mediaItem.owner.id = ?1 "
                        + "order by mediaItem.title asc, mediaItem.id asc",
                userId);
    }

    public Optional<MediaItem> findByIdWithAssociations(UUID userId, UUID id) {
        return find(
                        "select distinct mediaItem from MediaItem mediaItem "
                                + "left join fetch mediaItem.mediaTags mediaTags "
                                + "left join fetch mediaTags.tag "
                                + "left join fetch mediaItem.externalReferences "
                                + "where mediaItem.owner.id = ?1 and mediaItem.id = ?2",
                        userId,
                        id)
                .list()
                .stream()
                .findFirst();
    }

    public Optional<MediaItem> findByExternalSourceWithAssociations(
            UUID userId, ExternalSourceName sourceName, String externalId) {
        return find(
                        "select distinct mediaItem from MediaItem mediaItem "
                                + "left join fetch mediaItem.mediaTags mediaTags "
                                + "left join fetch mediaTags.tag "
                                + "left join fetch mediaItem.externalReferences "
                                + "where mediaItem.owner.id = ?1 "
                                + "and mediaItem.externalSourceName = ?2 "
                                + "and mediaItem.externalSourceId = ?3",
                        userId,
                        sourceName,
                        externalId)
                .list()
                .stream()
                .findFirst();
    }
}
