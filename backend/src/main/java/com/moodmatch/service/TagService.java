package com.moodmatch.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.moodmatch.dto.media.ReplaceMediaTagsRequest;
import com.moodmatch.dto.tag.CreateTagRequest;
import com.moodmatch.dto.tag.TagResponse;
import com.moodmatch.entity.Tag;
import com.moodmatch.entity.TagCategory;
import com.moodmatch.exception.ResourceNotFoundException;
import com.moodmatch.mapper.TagMapper;
import com.moodmatch.repository.TagRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class TagService {

    @Inject
    TagRepository tagRepository;

    @Inject
    TagMapper tagMapper;

    @Transactional(TxType.SUPPORTS)
    public List<TagResponse> listTags() {
        return tagRepository.listAllOrdered().stream().map(tagMapper::toResponse).toList();
    }

    @Transactional
    public TagResponse createTagIfNeeded(CreateTagRequest request) {
        return tagMapper.toResponse(findOrCreateTag(request));
    }

    Tag requireTag(UUID tagId) {
        return tagRepository
                .findByIdOptional(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId));
    }

    Set<Tag> findOrCreateTags(ReplaceMediaTagsRequest request) {
        LinkedHashSet<Tag> resolvedTags = new LinkedHashSet<>();
        resolvedTags.addAll(findExistingTags(request == null ? null : request.tagIds()));

        if (request == null || request.createTags() == null) {
            return resolvedTags;
        }

        Map<TagKey, CreateTagRequest> uniqueCreateRequests = new LinkedHashMap<>();
        for (CreateTagRequest createTagRequest : request.createTags()) {
            if (createTagRequest == null) {
                continue;
            }

            String normalizedName = normalizeTagName(createTagRequest.name());
            TagKey key = new TagKey(normalizedName, createTagRequest.category());
            uniqueCreateRequests.putIfAbsent(key, new CreateTagRequest(normalizedName, createTagRequest.category()));
        }

        for (CreateTagRequest createTagRequest : uniqueCreateRequests.values()) {
            resolvedTags.add(findOrCreateTag(createTagRequest));
        }

        return resolvedTags;
    }

    private List<Tag> findExistingTags(List<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }

        List<UUID> uniqueIds = tagIds.stream().filter(Objects::nonNull).distinct().toList();
        List<Tag> tags = new ArrayList<>(tagRepository.listByIds(uniqueIds));
        if (tags.size() != uniqueIds.size()) {
            Set<UUID> resolvedIds = tags.stream().map(Tag::getId).collect(java.util.stream.Collectors.toSet());
            UUID missingId = uniqueIds.stream().filter(id -> !resolvedIds.contains(id)).findFirst().orElse(null);
            throw new ResourceNotFoundException("Tag not found: " + missingId);
        }

        tags.sort(java.util.Comparator.comparing(tag -> uniqueIds.indexOf(tag.getId())));
        return tags;
    }

    private Tag findOrCreateTag(CreateTagRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Tag request must not be null.");
        }

        String normalizedName = normalizeTagName(request.name());
        TagCategory category = Objects.requireNonNull(request.category(), "Tag category must not be null.");

        return tagRepository.findByNameIgnoreCaseAndCategory(normalizedName, category).orElseGet(() -> {
            Tag tag = new Tag();
            tag.setName(normalizedName);
            tag.setCategory(category);
            tagRepository.persist(tag);
            return tag;
        });
    }

    private String normalizeTagName(String rawName) {
        if (rawName == null) {
            throw new IllegalArgumentException("Tag name must not be null.");
        }

        String normalized = rawName.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Tag name must not be blank.");
        }
        return normalized;
    }

    private record TagKey(String normalizedName, TagCategory category) {
        private TagKey {
            normalizedName = normalizedName.toLowerCase(Locale.ROOT);
            category = Objects.requireNonNull(category, "Tag category must not be null.");
        }
    }
}
