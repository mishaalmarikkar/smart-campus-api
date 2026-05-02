package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Maps SensorUnavailableException to HTTP 403 Forbidden.
 * Triggered when a POST reading is attempted on a sensor in MAINTENANCE status.
 */
@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException exception) {
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "error", "SENSOR_UNAVAILABLE",
                        "status", 403,
                        "message", exception.getMessage(),
                        "hint", "Sensor must be in ACTIVE or OFFLINE status to accept readings."
                ))
                .build();
    }
}
