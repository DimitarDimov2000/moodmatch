package com.moodmatch.exception;

import java.util.List;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

abstract class ApiExceptionMapperSupport {

    protected Response buildResponse(Response.Status status, String code, String message, List<ErrorDetail> details) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(code, message, details == null ? List.of() : List.copyOf(details)))
                .build();
    }
}
