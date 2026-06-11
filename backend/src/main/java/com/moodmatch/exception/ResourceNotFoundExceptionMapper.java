package com.moodmatch.exception;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ResourceNotFoundExceptionMapper extends ApiExceptionMapperSupport
        implements ExceptionMapper<ResourceNotFoundException> {

    @Override
    public Response toResponse(ResourceNotFoundException exception) {
        return buildResponse(
                Response.Status.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                exception.getMessage(),
                List.of());
    }
}
