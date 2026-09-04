package com.orders.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.Map;

@Provider
public class InvalidCurrencyExceptionMapper implements ExceptionMapper<InvalidCurrencyException> {

    @Override
    public Response toResponse(InvalidCurrencyException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "error", "INVALID_CURRENCY",
                        "message", exception.getMessage(),
                        "timestamp", Instant.now().toString()
                ))
                .build();
    }
}
