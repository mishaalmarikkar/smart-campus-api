package com.smartcampus.resource;

import com.smartcampus.application.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the /api/v1/rooms collection.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    /**
     * GET /api/v1/rooms — returns all rooms.
     */
    @GET
    public Response getAllRooms() {
        Collection<Room> rooms = store.getRooms().values();
        return Response.ok(rooms).build();
    }

    /**
     * POST /api/v1/rooms — creates a new room.
     * Returns 201 Created with Location header.
     */
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Room ID is required."))
                    .build();
        }
        if (store.getRooms().containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "A room with ID '" + room.getId() + "' already exists."))
                    .build();
        }
        store.getRooms().put(room.getId(), room);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Room created successfully.");
        response.put("roomId", room.getId());
        response.put("_links", Map.of("self", "/api/v1/rooms/" + room.getId()));

        return Response.status(Response.Status.CREATED)
                .header("Location", "/api/v1/rooms/" + room.getId())
                .entity(response)
                .build();
    }

    /**
     * GET /api/v1/rooms/{roomId} — returns a specific room.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Room with ID '" + roomId + "' not found."))
                    .build();
        }
        return Response.ok(room).build();
    }

    /**
     * DELETE /api/v1/rooms/{roomId} — deletes a room.
     * Business Rule: Cannot delete a room that still has sensors assigned.
     * Idempotent: second delete on non-existent room returns 404 (not the same as first delete).
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Room with ID '" + roomId + "' not found."))
                    .build();
        }
        // Safety check: prevent orphan sensors
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Room '" + roomId + "' cannot be deleted. It still has " +
                    room.getSensorIds().size() + " active sensor(s) assigned: " + room.getSensorIds()
            );
        }
        store.getRooms().remove(roomId);
        return Response.ok(Map.of("message", "Room '" + roomId + "' has been successfully decommissioned.")).build();
    }
}
