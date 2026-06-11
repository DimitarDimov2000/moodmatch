package com.moodmatch.external.adapter;

import java.util.List;
import java.util.Set;

import com.moodmatch.entity.MediaType;

public interface ExternalSearchProvider {

    ExternalSearchSourceName sourceName();

    Set<MediaType> supportedMediaTypes();

    List<ExternalSearchResult> search(ExternalSearchRequest request);
}
