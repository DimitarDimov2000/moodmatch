package com.moodmatch.resource;

import com.moodmatch.dto.matching.InterestProfileResponse;
import com.moodmatch.service.InterestProfileService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/profile")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProfileResource {

    @Inject
    InterestProfileService interestProfileService;

    @GET
    public InterestProfileResponse getProfile() {
        return interestProfileService.calculateInterestProfile();
    }
}
