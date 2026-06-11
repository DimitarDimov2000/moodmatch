package com.moodmatch.resource;

import java.util.List;

import com.moodmatch.dto.tag.TagResponse;
import com.moodmatch.service.TagService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/tags")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TagResource {

    @Inject
    TagService tagService;

    @GET
    public List<TagResponse> listTags() {
        return tagService.listTags();
    }
}
