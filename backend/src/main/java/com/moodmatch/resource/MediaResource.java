package com.moodmatch.resource;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.moodmatch.dto.media.CreateMediaRequest;
import com.moodmatch.dto.media.MediaResponse;
import com.moodmatch.dto.media.ReplaceMediaTagsRequest;
import com.moodmatch.dto.media.UpdateMediaConsumptionStatusRequest;
import com.moodmatch.dto.media.UpdateMediaFavouriteRequest;
import com.moodmatch.dto.media.UpdateMediaRequest;
import com.moodmatch.service.MediaService;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;

@Path("/api/media")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MediaResource {

    @Inject
    MediaService mediaService;

    @GET
    public List<MediaResponse> listMedia() {
        return mediaService.listMedia();
    }

    @GET
    @Path("/{id}")
    public MediaResponse getMediaById(@PathParam("id") UUID id) {
        return mediaService.getMediaById(id);
    }

    @POST
    public Response createMedia(@NotNull @Valid CreateMediaRequest request, @Context UriInfo uriInfo) {
        MediaResponse created = mediaService.createMedia(request);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.id().toString()).build();
        return Response.created(location).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public MediaResponse updateMedia(@PathParam("id") UUID id, @NotNull @Valid UpdateMediaRequest request) {
        return mediaService.updateMedia(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response deleteMedia(@PathParam("id") UUID id) {
        mediaService.deleteMedia(id);
        return Response.noContent().build();
    }

    @PATCH
    @Path("/{id}/status")
    public MediaResponse updateMediaStatus(
            @PathParam("id") UUID id, @NotNull @Valid UpdateMediaConsumptionStatusRequest request) {
        return mediaService.updateMediaConsumptionStatus(id, request);
    }

    @PATCH
    @Path("/{id}/favorite")
    public MediaResponse updateMediaFavorite(
            @PathParam("id") UUID id, @NotNull @Valid UpdateMediaFavouriteRequest request) {
        return mediaService.updateMediaFavourite(id, request);
    }

    @PUT
    @Path("/{id}/tags")
    public MediaResponse replaceMediaTags(@PathParam("id") UUID id, @NotNull @Valid ReplaceMediaTagsRequest request) {
        return mediaService.replaceMediaTags(id, request);
    }
}
