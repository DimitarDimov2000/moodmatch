package com.moodmatch.resource;

import com.moodmatch.dto.matching.MatchingResponse;
import com.moodmatch.service.MatchingService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/matches")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MatchingResource {

    @Inject
    MatchingService matchingService;

    @GET
    public MatchingResponse calculateMatches() {
        return matchingService.calculateMatches();
    }
}
