package com.smartcampus.resource;

import com.smartcampus.application.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sub-resource class for /api/v1/sensors/{sensorId}/readings.
 * Instantiated via the sub-resource locator in SensorResource.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final Sensor sensor;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(Sensor sensor) {
        this.sensor = sensor;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings — fetches historical readings for this sensor.
     */
    @GET
    public Response getReadings() {
        List<SensorReading> readings = store.getSensorReadings().get(sensor.getId());
        if (readings == null) {
            return Response.ok(List.of()).build();
        }
        return Response.ok(readings).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings — appends a new reading.
     * Side Effect: Updates the parent sensor's currentValue to maintain consistency.
     * Constraint: Sensors in MAINTENANCE status cannot accept new readings.
     */
    @POST
    public Response addReading(SensorReading reading) {
        // State constraint: MAINTENANCE sensors cannot accept readings
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensor.getId() + "' is currently in MAINTENANCE mode and cannot accept new readings."
            );
        }

        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Reading body is required."))
                    .build();
        }

        // Assign UUID if not provided
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }
        // Auto-set timestamp if not provided
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        List<SensorReading> readings = store.getSensorReadings().get(sensor.getId());
        readings.add(reading);

        // Side effect: update the parent sensor's currentValue
        sensor.setCurrentValue(reading.getValue());

        Map<String, Object> response = Map.of(
                "message", "Reading recorded successfully.",
                "readingId", reading.getId(),
                "sensorId", sensor.getId(),
                "updatedCurrentValue", reading.getValue()
        );

        return Response.status(Response.Status.CREATED).entity(response).build();
    }
}
