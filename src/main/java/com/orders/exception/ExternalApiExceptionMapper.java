package com.orders.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.Map;

@Provider
public class ExternalApiExceptionMapper implements ExceptionMapper<ExternalApiException> {

    @Override
    public Response toResponse(ExternalApiException exception) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "error", "EXTERNAL_API_ERROR",
                        "message", "Failed to reach exchange rate service: " + exception.getMessage(),
                        "timestamp", Instant.now().toString()
                ))
                .build();
    }
}
