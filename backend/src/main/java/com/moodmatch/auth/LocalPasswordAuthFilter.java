package com.moodmatch.auth;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.moodmatch.exception.ErrorResponse;
import com.moodmatch.service.MoodMatchAuthConfiguration;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class LocalPasswordAuthFilter implements ContainerRequestFilter {

    private static final List<String> PROTECTED_EXACT_PATHS = List.of(
            "api/profile",
            "api/candidates",
            "api/matches",
            "api/external/search",
            "api/auth/me",
            "api/auth/logout",
            "api/media");

    private static final List<String> PROTECTED_PATH_PREFIXES = List.of("api/media/");

    @Inject
    MoodMatchAuthConfiguration authConfiguration;

    @Inject
    AuthTokenService authTokenService;

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!authConfiguration.isLocalPasswordMode() || !isProtectedPath(normalizePath(requestContext.getUriInfo().getPath()))) {
            return;
        }

        Optional<String> token = extractBearerToken(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION));
        if (token.isEmpty()) {
            abortUnauthorized(requestContext);
            return;
        }

        authTokenService.findUserForToken(token.get())
                .ifPresentOrElse(authenticatedUserContext::setCurrentUser, () -> abortUnauthorized(requestContext));
    }

    private boolean isProtectedPath(String path) {
        return PROTECTED_EXACT_PATHS.contains(path)
                || PROTECTED_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }

        return path.startsWith("/") ? path.substring(1) : path;
    }

    private Optional<String> extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return Optional.empty();
        }

        String prefix = "Bearer ";
        if (!authorizationHeader.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return Optional.empty();
        }

        String token = authorizationHeader.substring(prefix.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    private void abortUnauthorized(ContainerRequestContext requestContext) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("UNAUTHORIZED", "Invalid or missing bearer token.", List.of()))
                .build());
    }
}
