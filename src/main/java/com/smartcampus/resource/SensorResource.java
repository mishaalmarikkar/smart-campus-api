package com.smartcampus.resource;

import com.smartcampus.application.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the /api/v1/sensors collection.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    /**
     * GET /api/v1/sensors — returns all sensors, with optional ?type= filter.
     * Example: GET /api/v1/sensors?type=CO2
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        Collection<Sensor> all = store.getSensors().values();
        if (type != null && !type.isBlank()) {
            List<Sensor> filtered = all.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
            return Response.ok(filtered).build();
        }
        return Response.ok(all).build();
    }

    /**
     * POST /api/v1/sensors — registers a new sensor.
     * Validates that the provided roomId exists before registering.
     */
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Sensor ID is required."))
                    .build();
        }
        if (store.getSensors().containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Sensor with ID '" + sensor.getId() + "' already exists."))
                    .build();
        }
        // Validate that the referenced roomId actually exists
        if (sensor.getRoomId() == null || !store.getRooms().containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Cannot register sensor: roomId '" + sensor.getRoomId() + "' does not exist in the system."
            );
        }

        store.getSensors().put(sensor.getId(), sensor);
        store.getSensorReadings().put(sensor.getId(), new ArrayList<>());

        // Link the sensor to the room
        store.getRooms().get(sensor.getRoomId()).getSensorIds().add(sensor.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sensor registered successfully.");
        response.put("sensorId", sensor.getId());
        response.put("_links", Map.of(
                "self", "/api/v1/sensors/" + sensor.getId(),
                "readings", "/api/v1/sensors/" + sensor.getId() + "/readings"
        ));

        return Response.status(Response.Status.CREATED)
                .header("Location", "/api/v1/sensors/" + sensor.getId())
                .entity(response)
                .build();
    }

    /**
     * GET /api/v1/sensors/{sensorId} — returns a specific sensor.
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor with ID '" + sensorId + "' not found."))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    /**
     * Sub-Resource Locator: delegates /sensors/{sensorId}/readings to SensorReadingResource.
     * This pattern promotes separation of concerns and keeps each class focused.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor with ID '" + sensorId + "' not found.");
        }
        return new SensorReadingResource(sensor);
    }
}
