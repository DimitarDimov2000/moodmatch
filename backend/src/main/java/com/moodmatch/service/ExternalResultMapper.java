package com.moodmatch.service;

import java.util.List;

import com.moodmatch.dto.external.ExternalSearchResultResponse;
import com.moodmatch.dto.external.ExternalSuggestedTagResponse;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSuggestedTag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ExternalResultMapper {

    @Inject
    ExternalTagSuggestionService externalTagSuggestionService;

    public ExternalSearchResult enrichSuggestions(ExternalSearchResult result) {
        List<ExternalSuggestedTag> suggestions = externalTagSuggestionService.buildSuggestions(result);
        return new ExternalSearchResult(
                result.source(),
                result.mappingSource(),
                result.externalId(),
                result.mediaType(),
                result.title(),
                result.originalTitle(),
                result.creatorNames(),
                result.description(),
                result.releaseYear(),
                result.coverUrl(),
                result.sourceUrl(),
                result.externalGenres(),
                result.externalSubjects(),
                suggestions,
                result.attribution(),
                result.warnings());
    }

    public ExternalSearchResultResponse toResponse(ExternalSearchResult result) {
        return new ExternalSearchResultResponse(
                result.source(),
                result.externalId(),
                result.mediaType(),
                result.title(),
                result.originalTitle(),
                List.copyOf(result.creatorNames()),
                result.description(),
                result.releaseYear(),
                result.coverUrl(),
                result.sourceUrl(),
                List.copyOf(result.externalGenres()),
                List.copyOf(result.externalSubjects()),
                result.suggestedTags().stream().map(this::toResponse).toList(),
                result.attribution(),
                List.copyOf(result.warnings()));
    }

    private ExternalSuggestedTagResponse toResponse(ExternalSuggestedTag suggestedTag) {
        return new ExternalSuggestedTagResponse(
                suggestedTag.tagId(),
                suggestedTag.tagName(),
                suggestedTag.tagCategory(),
                suggestedTag.sourceValue(),
                suggestedTag.reason(),
                suggestedTag.confidence());
    }
}
