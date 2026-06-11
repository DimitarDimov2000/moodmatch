package com.moodmatch.resource;

import com.moodmatch.dto.external.ExternalSearchResponse;
import com.moodmatch.service.ExternalSearchService;

import jakarta.inject.Inject;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestQuery;

@Path("/api/external")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ExternalSearchResource {

    @Inject
    ExternalSearchService externalSearchService;

    @GET
    @Path("/search")
    public ExternalSearchResponse search(
            @RestQuery("query") @NotBlank @Size(max = 200) String query,
            @RestQuery("mediaType") @NotBlank String mediaType,
            @RestQuery("source") @Size(max = 50) String source,
            @RestQuery("limit") @Min(1) Integer limit) {
        return externalSearchService.search(query, mediaType, source, limit);
    }
}
