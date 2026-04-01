package com.redhat.filter;

import java.io.IOException;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.dto.ErrorResponse;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class SecurityErrorResponseFilter implements ContainerResponseFilter {

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        int status = responseContext.getStatus();

        if (responseContext.getEntity() != null) {
            return;
        }

        if (status == 401) {
            responseContext.setEntity(
                    new ErrorResponse(
                            OffsetDateTime.now().toString(),
                            401,
                            "Unauthorized",
                            "A valid Bearer token is required to access this resource.",
                            requestContext.getUriInfo().getPath()
                    ),
                    null,
                    MediaType.APPLICATION_JSON_TYPE
            );
            return;
        }

        if (status == 403) {
            responseContext.setEntity(
                    new ErrorResponse(
                            OffsetDateTime.now().toString(),
                            403,
                            "Forbidden",
                            "The token was accepted, but it does not contain the required role for this resource.",
                            requestContext.getUriInfo().getPath()
                    ),
                    null,
                    MediaType.APPLICATION_JSON_TYPE
            );
        }
    }
}