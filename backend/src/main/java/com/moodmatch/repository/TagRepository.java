package com.moodmatch.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.moodmatch.entity.Tag;
import com.moodmatch.entity.TagCategory;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TagRepository implements PanacheRepositoryBase<Tag, UUID> {

    public Optional<Tag> findByNameAndCategory(String name, TagCategory category) {
        return find("name = ?1 and category = ?2", name, category).firstResultOptional();
    }

    public Optional<Tag> findByNameIgnoreCaseAndCategory(String name, TagCategory category) {
        return find("lower(name) = lower(?1) and category = ?2", name, category).firstResultOptional();
    }

    public List<Tag> findByCategory(TagCategory category) {
        return list("category", category);
    }

    public List<Tag> listAllOrdered() {
        return list("order by category asc, lower(name) asc, id asc");
    }

    public List<Tag> listByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return list("id in ?1", ids);
    }
}
