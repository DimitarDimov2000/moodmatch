package com.moodmatch.resource;

import com.moodmatch.dto.matching.CandidateSelectionResponse;
import com.moodmatch.service.CandidateService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/candidates")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CandidateResource {

    @Inject
    CandidateService candidateService;

    @GET
    public CandidateSelectionResponse listCandidates() {
        return candidateService.listCandidates();
    }
}
