package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Maps LinkedResourceNotFoundException to HTTP 422 Unprocessable Entity.
 * Triggered when a sensor is registered with a roomId that does not exist.
 *
 * Why 422 over 404:
 * The HTTP 404 means the *endpoint* or *resource URI* was not found.
 * Here, the endpoint /sensors is perfectly valid and found.
 * The problem is the payload references a resource (roomId) that doesn't exist —
 * the request is syntactically correct JSON but semantically invalid.
 * 422 precisely communicates: "I understood the request, but the content is unprocessable."
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        return Response.status(422) // Unprocessable Entity
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "error", "LINKED_RESOURCE_NOT_FOUND",
                        "status", 422,
                        "message", exception.getMessage(),
                        "hint", "Ensure the roomId exists before registering a sensor."
                ))
                .build();
    }
}
