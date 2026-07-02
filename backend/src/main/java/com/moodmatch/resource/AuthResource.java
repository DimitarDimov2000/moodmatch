package com.moodmatch.resource;

import com.moodmatch.auth.AuthService;
import com.moodmatch.auth.AuthTokenService;
import com.moodmatch.dto.auth.AuthResponse;
import com.moodmatch.dto.auth.AuthUserResponse;
import com.moodmatch.dto.auth.LoginRequest;
import com.moodmatch.dto.auth.RegisterRequest;
import com.moodmatch.service.CurrentUserProvider;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @Inject
    AuthTokenService authTokenService;

    @Inject
    CurrentUserProvider currentUserProvider;

    @POST
    @Path("/register")
    public AuthResponse register(@NotNull @Valid RegisterRequest request) {
        return authService.register(request);
    }

    @POST
    @Path("/login")
    public AuthResponse login(@NotNull @Valid LoginRequest request) {
        return authService.login(request);
    }

    @GET
    @Path("/me")
    public AuthUserResponse me() {
        return authService.toUserResponse(currentUserProvider.getCurrentUser());
    }

    @POST
    @Path("/logout")
    @Consumes(MediaType.WILDCARD)
    public Response logout(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        authTokenService.revokeToken(extractBearerToken(authorizationHeader));
        return Response.noContent().build();
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }

        String prefix = "Bearer ";
        if (!authorizationHeader.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return null;
        }

        String token = authorizationHeader.substring(prefix.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
