package com.redhat.exception;

import java.time.OffsetDateTime;

import com.redhat.dto.ErrorResponse;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CBVApiExceptionMapper implements ExceptionMapper<CBVApiException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(CBVApiException exception) {
        String error = switch (exception.getStatus()) {
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            default -> "Downstream API Error";
        };

        String message = switch (exception.getStatus()) {
            case 401 -> "cbv-client could not access cbv-api because no valid access token was accepted.";
            case 403 -> "cbv-client access token was accepted by cbv-api, but it does not contain the required role.";
            default -> exception.getDownstreamMessage();
        };

        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now().toString(),
                exception.getStatus(),
                error,
                message,
                uriInfo.getPath()
        );

        return Response.status(exception.getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
