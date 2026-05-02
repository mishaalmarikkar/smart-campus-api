package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Maps RoomNotEmptyException to HTTP 409 Conflict.
 * Triggered when a deletion is attempted on a room that still has sensors assigned.
 */
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "error", "ROOM_NOT_EMPTY",
                        "status", 409,
                        "message", exception.getMessage(),
                        "hint", "Please remove or reassign all sensors before deleting this room."
                ))
                .build();
    }
}
