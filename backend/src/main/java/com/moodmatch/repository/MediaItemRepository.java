package com.moodmatch.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.moodmatch.entity.ConsumptionStatus;
import com.moodmatch.entity.MediaItem;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MediaItemRepository implements PanacheRepositoryBase<MediaItem, UUID> {

    public List<MediaItem> findByConsumptionStatus(ConsumptionStatus consumptionStatus) {
        return list("consumptionStatus", consumptionStatus);
    }

    public List<MediaItem> listAllWithAssociations() {
        return list(
                "select distinct mediaItem from MediaItem mediaItem "
                        + "left join fetch mediaItem.mediaTags mediaTags "
                        + "left join fetch mediaTags.tag "
                        + "left join fetch mediaItem.externalReferences "
                        + "order by mediaItem.title asc, mediaItem.id asc");
    }

    public Optional<MediaItem> findByIdWithAssociations(UUID id) {
        return find(
                        "select distinct mediaItem from MediaItem mediaItem "
                                + "left join fetch mediaItem.mediaTags mediaTags "
                                + "left join fetch mediaTags.tag "
                                + "left join fetch mediaItem.externalReferences "
                                + "where mediaItem.id = ?1",
                        id)
                .list()
                .stream()
                .findFirst();
    }
}
