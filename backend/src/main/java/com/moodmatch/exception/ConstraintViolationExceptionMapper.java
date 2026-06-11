package com.moodmatch.exception;

import java.util.Comparator;
import java.util.List;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ConstraintViolationExceptionMapper extends ApiExceptionMapperSupport
        implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<ErrorDetail> details = exception.getConstraintViolations().stream()
                .map(violation -> new ErrorDetail(extractField(violation.getPropertyPath().toString()), violation.getMessage()))
                .sorted(Comparator.comparing(ErrorDetail::field, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(ErrorDetail::message, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        return buildResponse(
                Response.Status.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Validation failed.",
                details);
    }

    private String extractField(String propertyPath) {
        if (propertyPath == null || propertyPath.isBlank()) {
            return null;
        }

        String[] segments = propertyPath.split("\\.");
        if (segments.length <= 2) {
            return segments[segments.length - 1];
        }

        return String.join(".", java.util.Arrays.copyOfRange(segments, 2, segments.length));
    }
}
