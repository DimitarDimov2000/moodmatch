package com.moodmatch.resource;

import java.util.List;

import com.moodmatch.dto.tag.CreateTagRequest;
import com.moodmatch.dto.tag.TagResponse;
import com.moodmatch.service.TagService;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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

    @POST
    public TagResponse createTag(@NotNull @Valid CreateTagRequest request) {
        return tagService.createTagIfNeeded(request);
    }
}
