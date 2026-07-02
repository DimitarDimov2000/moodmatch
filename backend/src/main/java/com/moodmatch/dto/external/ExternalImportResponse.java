package com.moodmatch.dto.external;

import com.moodmatch.dto.media.MediaResponse;

public record ExternalImportResponse(MediaResponse media, boolean created, String message) {}
