package com.moodmatch.resource;

import java.net.URI;

import com.moodmatch.dto.external.ExternalImportRequest;
import com.moodmatch.dto.external.ExternalImportResponse;
import com.moodmatch.dto.external.ExternalResolveUrlRequest;
import com.moodmatch.dto.external.ExternalSearchResponse;
import com.moodmatch.dto.external.ExternalSearchResultResponse;
import com.moodmatch.service.ExternalImportService;
import com.moodmatch.service.ExternalResolveUrlService;
import com.moodmatch.service.ExternalSearchService;

import jakarta.validation.Valid;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;

import org.jboss.resteasy.reactive.RestQuery;

@Path("/api/external")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ExternalSearchResource {

    @Inject
    ExternalSearchService externalSearchService;

    @Inject
    ExternalImportService externalImportService;

    @Inject
    ExternalResolveUrlService externalResolveUrlService;

    @GET
    @Path("/search")
    public ExternalSearchResponse search(
            @RestQuery("query") @NotBlank @Size(max = 200) String query,
            @RestQuery("mediaType") @NotBlank String mediaType,
            @RestQuery("source") @Size(max = 50) String source,
            @RestQuery("limit") @Min(1) Integer limit) {
        return externalSearchService.search(query, mediaType, source, limit);
    }

    @POST
    @Path("/import")
    public Response importMedia(@NotNull @Valid ExternalImportRequest request, @Context UriInfo uriInfo) {
        ExternalImportResponse response = externalImportService.importMedia(request);
        if (!response.created()) {
            return Response.ok(response).build();
        }

        URI location = uriInfo.getBaseUriBuilder()
                .path("/api/media/")
                .path(response.media().id().toString())
                .build();
        return Response.created(location).entity(response).build();
    }

    @POST
    @Path("/resolve-url")
    public ExternalSearchResultResponse resolveUrl(@NotNull @Valid ExternalResolveUrlRequest request) {
        return externalResolveUrlService.resolve(request);
    }
}
