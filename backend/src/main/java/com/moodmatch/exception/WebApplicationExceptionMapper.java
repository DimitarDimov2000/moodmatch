package com.moodmatch.exception;

import java.util.List;

import org.jboss.logging.Logger;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper extends ApiExceptionMapperSupport
        implements ExceptionMapper<WebApplicationException> {

    private static final Logger LOG = Logger.getLogger(WebApplicationExceptionMapper.class);

    @Override
    public Response toResponse(WebApplicationException exception) {
        int statusCode = exception.getResponse() == null ? Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()
                : exception.getResponse().getStatus();
        Response.Status status = Response.Status.fromStatusCode(statusCode);
        Response.Status resolvedStatus = status == null ? Response.Status.INTERNAL_SERVER_ERROR : status;

        if (resolvedStatus.getStatusCode() >= 500) {
            LOG.error("Request failed with web application exception.", exception);
            return buildResponse(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "INTERNAL_SERVER_ERROR",
                    "An unexpected error occurred.",
                    List.of());
        }

        if (resolvedStatus == Response.Status.BAD_REQUEST) {
            return buildResponse(
                    Response.Status.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request could not be processed.",
                    List.of());
        }

        if (resolvedStatus == Response.Status.NOT_FOUND) {
            return buildResponse(
                    Response.Status.NOT_FOUND,
                    "RESOURCE_NOT_FOUND",
                    "Resource not found.",
                    List.of());
        }

        return buildResponse(
                resolvedStatus,
                "REQUEST_ERROR",
                exception.getMessage() == null || exception.getMessage().isBlank()
                        ? "Request could not be processed."
                        : exception.getMessage(),
                List.of());
    }
}
