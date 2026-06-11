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

    public List<Tag> findByCategory(TagCategory category) {
        return list("category", category);
    }
}
