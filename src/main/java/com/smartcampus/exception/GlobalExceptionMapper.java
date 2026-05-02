package com.smartcampus.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof RoomNotEmptyException) {
            return new RoomNotEmptyExceptionMapper().toResponse((RoomNotEmptyException) exception);
        }
        if (exception instanceof LinkedResourceNotFoundException) {
            return new LinkedResourceNotFoundExceptionMapper().toResponse((LinkedResourceNotFoundException) exception);
        }
        if (exception instanceof SensorUnavailableException) {
            return new SensorUnavailableExceptionMapper().toResponse((SensorUnavailableException) exception);
        }
        if (exception instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) exception;
            Response original = wae.getResponse();
            return Response.status(original.getStatus())
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of(
                            "status", original.getStatus(),
                            "error", original.getStatusInfo().getReasonPhrase(),
                            "message", exception.getMessage() != null ? exception.getMessage() : original.getStatusInfo().getReasonPhrase()
                    ))
                    .build();
        }
        LOGGER.log(Level.SEVERE, "Unhandled exception: " + exception.getMessage(), exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "error", "INTERNAL_SERVER_ERROR",
                        "status", 500,
                        "message", "An unexpected error occurred. Please contact the system administrator.",
                        "hint", "No internal details are disclosed for security reasons."
                ))
                .build();
    }
}