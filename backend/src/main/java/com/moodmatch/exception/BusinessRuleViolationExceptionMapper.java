package com.moodmatch.exception;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BusinessRuleViolationExceptionMapper extends ApiExceptionMapperSupport
        implements ExceptionMapper<BusinessRuleViolationException> {

    @Override
    public Response toResponse(BusinessRuleViolationException exception) {
        return buildResponse(
                Response.Status.BAD_REQUEST,
                "BUSINESS_RULE_VIOLATION",
                exception.getMessage(),
                List.of());
    }
}
