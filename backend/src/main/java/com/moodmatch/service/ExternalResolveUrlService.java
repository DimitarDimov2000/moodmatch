package com.moodmatch.service;

import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.moodmatch.dto.external.ExternalResolveUrlRequest;
import com.moodmatch.dto.external.ExternalSearchResultResponse;
import com.moodmatch.exception.BusinessRuleViolationException;
import com.moodmatch.external.adapter.ExternalSearchResult;
import com.moodmatch.external.adapter.ExternalSearchSourceName;
import com.moodmatch.external.adapter.ExternalUrlResolver;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class ExternalResolveUrlService {

    @Inject
    Instance<ExternalUrlResolver> externalUrlResolvers;

    @Inject
    ExternalResultMapper externalResultMapper;

    @Transactional(TxType.SUPPORTS)
    public ExternalSearchResultResponse resolve(ExternalResolveUrlRequest request) {
        Objects.requireNonNull(request, "External resolve request must not be null.");
        ExternalSearchSourceName source = validateSource(request.source());
        ExternalUrlResolver resolver = resolveResolver(source);
        ensureConfigured(resolver);

        ExternalSearchResult result = externalResultMapper.enrichSuggestions(resolver.resolve(request.url()));
        return externalResultMapper.toResponse(result);
    }

    private ExternalSearchSourceName validateSource(ExternalSearchSourceName source) {
        if (source == null) {
            throw new BusinessRuleViolationException("Source must not be null.");
        }
        if (source == ExternalSearchSourceName.AUTOMATIC || source == ExternalSearchSourceName.DEMO) {
            throw new BusinessRuleViolationException("Source %s does not support URL resolution.".formatted(source));
        }
        return source;
    }

    private ExternalUrlResolver resolveResolver(ExternalSearchSourceName source) {
        return findResolver(source)
                .orElseThrow(() -> new BusinessRuleViolationException("Source does not support URL resolution: " + source));
    }

    private Optional<ExternalUrlResolver> findResolver(ExternalSearchSourceName source) {
        return externalUrlResolvers.stream()
                .filter(resolver -> resolver.sourceName() == source)
                .min(Comparator.comparing(resolver -> resolver.getClass().getName().toLowerCase(Locale.ROOT)));
    }

    private void ensureConfigured(ExternalUrlResolver resolver) {
        if (!resolver.isConfigured()) {
            throw new BusinessRuleViolationException(resolver.configurationErrorMessage());
        }
    }
}
