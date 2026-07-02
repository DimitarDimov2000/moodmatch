package com.moodmatch.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.moodmatch.dto.external.ExternalImportRequest;
import com.moodmatch.dto.external.ExternalImportResponse;
import com.moodmatch.entity.CommitmentLevel;
import com.moodmatch.entity.ConsumptionStatus;
import com.moodmatch.entity.MediaExternalRef;
import com.moodmatch.entity.MediaItem;
import com.moodmatch.entity.MediaType;
import com.moodmatch.entity.MetadataOrigin;
import com.moodmatch.entity.SourceType;
import com.moodmatch.entity.Tag;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSourceMappings;
import com.moodmatch.mapper.MediaItemMapper;
import com.moodmatch.repository.MediaItemRepository;
import com.moodmatch.repository.TagRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ExternalImportService {

    @Inject
    MediaItemRepository mediaItemRepository;

    @Inject
    MediaItemMapper mediaItemMapper;

    @Inject
    MediaTagService mediaTagService;

    @Inject
    ExternalTagSuggestionService externalTagSuggestionService;

    @Inject
    TagRepository tagRepository;

    @Inject
    CurrentUserProvider currentUserProvider;

    @Transactional
    public ExternalImportResponse importMedia(ExternalImportRequest request) {
        Objects.requireNonNull(request, "External import request must not be null.");

        MediaItem existing = mediaItemRepository
                .findByExternalSourceWithAssociations(
                        currentUserProvider.getCurrentUser().getId(),
                        ExternalSourceMappings.toEntitySource(request.source()),
                        request.externalId())
                .orElse(null);
        if (existing != null) {
            return new ExternalImportResponse(
                    mediaItemMapper.toResponse(existing),
                    false,
                    "This item is already in your media library.");
        }

        MediaItem mediaItem = new MediaItem();
        mediaItem.setId(UUID.randomUUID());
        mediaItem.setOwner(currentUserProvider.getCurrentUser());
        mediaItem.setTitle(request.title().trim());
        mediaItem.setOriginalTitle(blankToNull(request.originalTitle()));
        mediaItem.setDescription(resolveImportedDescription(request));
        mediaItem.setMediaType(request.mediaType());
        mediaItem.setConsumptionStatus(ConsumptionStatus.WANT_TO_CONSUME);
        mediaItem.setFavourite(false);
        mediaItem.setRating(null);
        mediaItem.setSourceType(SourceType.EXTERNAL_SEARCH);
        mediaItem.setSourceNote("Imported from %s".formatted(request.source()));
        mediaItem.setCommitmentLevel(inferCommitmentLevel(request.mediaType()));
        mediaItem.setReleaseYear(request.releaseYear());
        mediaItem.setCoverUrl(blankToNull(request.coverUrl()));
        mediaItem.setExternalSourceName(ExternalSourceMappings.toEntitySource(request.source()));
        mediaItem.setExternalSourceId(request.externalId().trim());
        mediaItem.setExternalSourceUrl(blankToNull(request.sourceUrl()));
        mediaItem.setMetadataOrigin(MetadataOrigin.IMPORTED);

        attachExternalReference(mediaItem, request);
        attachMappedTags(mediaItem, request);
        mediaItemRepository.persist(mediaItem);

        return new ExternalImportResponse(
                mediaItemMapper.toResponse(mediaItem),
                true,
                "Imported into your media library.");
    }

    private void attachExternalReference(MediaItem mediaItem, ExternalImportRequest request) {
        MediaExternalRef externalRef = new MediaExternalRef();
        externalRef.setMediaItem(mediaItem);
        externalRef.setSourceName(ExternalSourceMappings.toEntitySource(request.source()));
        externalRef.setExternalId(request.externalId().trim());
        externalRef.setExternalUrl(blankToNull(request.sourceUrl()));
        externalRef.setAttributionText(blankToNull(request.attribution()));
        externalRef.setSourcePayloadHash(payloadHash(request));
        mediaItem.getExternalReferences().add(externalRef);
    }

    private void attachMappedTags(MediaItem mediaItem, ExternalImportRequest request) {
        ExternalSearchResult searchResult = new ExternalSearchResult(
                request.source(),
                ExternalSourceMappings.toMappingSource(request.source(), request.mediaType()),
                request.externalId().trim(),
                request.mediaType(),
                request.title().trim(),
                blankToNull(request.originalTitle()),
                safeList(request.creatorNames()),
                blankToNull(request.description()),
                request.releaseYear(),
                blankToNull(request.coverUrl()),
                blankToNull(request.sourceUrl()),
                safeList(request.externalGenres()),
                safeList(request.externalSubjects()),
                List.of(),
                blankToNull(request.attribution()),
                List.of());
        Set<Tag> mappedTags = new LinkedHashSet<>(tagRepository
                .listByIds(externalTagSuggestionService.buildSuggestions(searchResult).stream()
                        .map(suggestedTag -> suggestedTag.tagId())
                        .distinct()
                        .toList()));
        mediaTagService.addResolvedTags(mediaItem, mappedTags);
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String payloadHash(ExternalImportRequest request) {
        String payload = String.join(
                "::",
                request.source().name(),
                request.externalId().trim(),
                request.mediaType().name(),
                request.title().trim(),
                String.join("|", safeList(request.creatorNames())),
                String.valueOf(request.releaseYear()),
                String.join("|", safeList(request.externalGenres())),
                String.join("|", safeList(request.externalSubjects())),
                String.valueOf(blankToNull(request.sourceUrl())));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private CommitmentLevel inferCommitmentLevel(MediaType mediaType) {
        return switch (mediaType) {
            case FILM -> CommitmentLevel.MEDIUM;
            case SERIES, BOOK, GAME, PODCAST, AUDIOBOOK, VIDEO -> CommitmentLevel.LONG;
        };
    }

    private String resolveImportedDescription(ExternalImportRequest request) {
        String description = blankToNull(request.description());
        if (description != null) {
            return description;
        }
        if (request.mediaType() != MediaType.BOOK) {
            return null;
        }

        List<String> creatorNames = safeList(request.creatorNames());
        if (!creatorNames.isEmpty() && request.releaseYear() != null) {
            return "Book by %s. First published in %s."
                    .formatted(String.join(", ", creatorNames), request.releaseYear());
        }
        if (!creatorNames.isEmpty()) {
            return "Book by %s.".formatted(String.join(", ", creatorNames));
        }
        if (request.releaseYear() != null) {
            return "Book first published in %s.".formatted(request.releaseYear());
        }
        return "Book imported from %s.".formatted(request.source());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
