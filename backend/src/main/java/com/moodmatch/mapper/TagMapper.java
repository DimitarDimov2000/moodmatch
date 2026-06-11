package com.moodmatch.mapper;

import com.moodmatch.dto.tag.TagResponse;
import com.moodmatch.entity.Tag;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TagMapper {

    public TagResponse toResponse(Tag tag) {
        if (tag == null) {
            return null;
        }

        return new TagResponse(
                tag.getId(),
                tag.getName(),
                tag.getCategory(),
                tag.getCreatedAt(),
                tag.getUpdatedAt());
    }
}
