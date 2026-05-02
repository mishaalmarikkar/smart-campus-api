# Smart Campus Sensor & Room Management API

**Module:** 5COSC022W Client-Server Architectures  
**Student:** Mishaal Marikkar  
**Technology:** JAX-RS (Jersey 2.x) + Grizzly Embedded HTTP Server  
**Base URI:** `http://localhost:8080/api/v1`

---

## API Design Overview

This project implements a RESTful web service for managing the University of Westminster's Smart Campus infrastructure. The API exposes three primary resource collections:

| Resource | Path | Description |
|---|---|---|
| Discovery | `GET /api/v1` | API metadata and HATEOAS navigation links |
| Rooms | `/api/v1/rooms` | Campus room lifecycle management |
| Sensors | `/api/v1/sensors` | IoT sensor registration and monitoring |
| Readings | `/api/v1/sensors/{id}/readings` | Historical sensor data (sub-resource) |

All data is stored in-memory using thread-safe `ConcurrentHashMap` structures. No database is used. The server starts on port **8080** using the Grizzly embedded HTTP container.

---

## Project Structure

```
smart-campus-api/
├── pom.xml
└── src/main/java/com/smartcampus/
    ├── application/
    │   ├── Main.java                          # Entry point, starts Grizzly server
    │   ├── SmartCampusApplication.java        # @ApplicationPath("/api/v1")
    │   └── DataStore.java                     # Singleton ConcurrentHashMap store
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── resource/
    │   ├── DiscoveryResource.java             # GET /api/v1
    │   ├── RoomResource.java                  # /api/v1/rooms
    │   ├── SensorResource.java                # /api/v1/sensors
    │   └── SensorReadingResource.java         # Sub-resource: /sensors/{id}/readings
    ├── exception/
    │   ├── RoomNotEmptyException.java         # 409 Conflict
    │   ├── RoomNotEmptyExceptionMapper.java
    │   ├── LinkedResourceNotFoundException.java   # 422 Unprocessable Entity
    │   ├── LinkedResourceNotFoundExceptionMapper.java
    │   ├── SensorUnavailableException.java    # 403 Forbidden
    │   ├── SensorUnavailableExceptionMapper.java
    │   └── GlobalExceptionMapper.java         # 500 catch-all
    └── filter/
        └── LoggingFilter.java                 # Request + Response logging
```

---

## How to Build and Run

### Prerequisites
- Java 11+
- Apache Maven 3.6+

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/mishaalmarikkar/smart-campus-api.git
cd smart-campus-api

# 2. Build the project
mvn clean install -DskipTests

# 3. Run the server
mvn exec:java -Dexec.mainClass="com.smartcampus.application.Main"
```

The server starts at `http://localhost:8080/api/v1`  
Press **ENTER** in the terminal to stop the server.

---

## Endpoint Reference

| Method | Path | Description | Status |
|--------|------|-------------|--------|
| GET | `/api/v1` | Discovery + HATEOAS links | 200 |
| GET | `/api/v1/rooms` | List all rooms | 200 |
| POST | `/api/v1/rooms` | Create a room | 201 |
| GET | `/api/v1/rooms/{id}` | Get room by ID | 200 |
| DELETE | `/api/v1/rooms/{id}` | Delete room (no sensors) | 200 |
| GET | `/api/v1/sensors` | List all sensors (optional `?type=`) | 200 |
| POST | `/api/v1/sensors` | Register a sensor | 201 |
| GET | `/api/v1/sensors/{id}` | Get sensor by ID | 200 |
| GET | `/api/v1/sensors/{id}/readings` | Get reading history | 200 |
| POST | `/api/v1/sensors/{id}/readings` | Add a new reading | 201 |

## Error Codes Reference

| HTTP Code | Scenario |
|-----------|----------|
| 404 | Resource not found |
| 409 Conflict | Deleting a room that still has sensors |
| 415 Unsupported Media Type | Wrong Content-Type on POST |
| 422 Unprocessable Entity | Sensor registered with non-existent roomId |
| 403 Forbidden | Posting a reading to a MAINTENANCE sensor |
| 500 Internal Server Error | Any unexpected runtime exception |

---

## Sample curl Commands

### 1. Discover API
```bash
curl -X GET http://localhost:8080/api/v1 -H "Accept: application/json"
```

### 2. Create a Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"HALL-01","name":"Main Hall","capacity":120}'
```

### 3. Get all Rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms -H "Accept: application/json"
```

### 4. Register a Sensor with valid roomId
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-002","type":"Temperature","status":"ACTIVE","currentValue":21.0,"roomId":"LAB-101"}'
```

### 5. Register a Sensor with invalid roomId (422 Error)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-003","type":"Temperature","status":"ACTIVE","currentValue":21.0,"roomId":"DOES-NOT-EXIST"}'
```

### 6. Filter Sensors by Type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2" -H "Accept: application/json"
```

### 7. Post a Reading to a Sensor
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.7}'
```

### 8. Get Reading History
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings -H "Accept: application/json"
```

### 9. Delete a Room without sensors (success)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/HALL-01 -H "Accept: application/json"
```

### 10. Delete a Room with sensors (409 Conflict)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 -H "Accept: application/json"
```

---

## Report: Answers to Conceptual Questions

### Part 1.1 — JAX-RS Resource Lifecycle & Concurrency

By default, JAX-RS creates a **new instance of every resource class for each incoming HTTP request** (request-scoped). This means the runtime instantiates the resource, handles the request, and discards the instance without retaining state between calls.

This has a critical implication for in-memory data management. Since resource classes are not singletons, they cannot hold shared state as instance fields. The solution used in this project is a **singleton `DataStore` class** using `ConcurrentHashMap` structures. A `ConcurrentHashMap` is essential because multiple requests can arrive simultaneously on different threads, and a plain `HashMap` is not thread-safe. `ConcurrentHashMap` achieves thread safety using segment-level locking, allowing concurrent reads while serialising writes to the same bucket.

---

### Part 1.2 — HATEOAS and Hypermedia

HATEOAS (Hypermedia As The Engine Of Application State) is a REST constraint where API responses include **navigational links** pointing to related actions and resources. This is considered a hallmark of advanced REST design because it eliminates the need for clients to hardcode URIs or consult static documentation. The client discovers what it can do next directly from the response — similar to how a web browser follows hyperlinks. If the server changes a URL, clients that follow links adapt automatically, whereas clients with hardcoded URLs break.

---

### Part 2.1 — Returning IDs vs Full Objects in Lists

Returning **only IDs** minimises payload size but forces the client to make N additional requests to fetch details — a classic N+1 problem. **Returning full objects** reduces total HTTP calls, improves performance, and simplifies client-side logic. This project returns full objects as the collection size is moderate and clients typically need the full data.

---

### Part 2.2 — Idempotency of DELETE

DELETE in this implementation is **idempotent in terms of server state**. The first DELETE removes the room and returns 200 OK. A second DELETE on the same room returns 404 Not Found. The server state after both calls is identical — the room does not exist. RFC 9110 confirms idempotency refers to server-side effect, not the response code.

---

### Part 3.1 — @Consumes and Content-Type Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation declares that the endpoint only accepts `Content-Type: application/json`. If a client sends `text/plain` or `application/xml`, JAX-RS performs content negotiation before the method executes and automatically returns **HTTP 415 Unsupported Media Type** without invoking any application code.

---

### Part 3.2 — @QueryParam vs Path Parameter for Filtering

`@QueryParam` (e.g., `GET /sensors?type=CO2`) is designed for filtering, searching, and pagination — operations that narrow a collection without identifying a specific resource. Path parameters identify a specific unique resource. Using `/sensors/type/CO2` implies CO2 is a distinct resource, which is semantically misleading. Query parameters also allow multiple filters naturally: `?type=CO2&status=ACTIVE`.

---

### Part 4.1 — Sub-Resource Locator Pattern

The sub-resource locator pattern delegates handling of a URI path segment to a separate dedicated class. Benefits include Single Responsibility Principle (each class handles one resource type), reduced cognitive load, better testability, contextual initialisation, and scalability. New sub-resources like `/sensors/{id}/alerts` can be added without modifying existing classes.

---

### Part 5.2 — Why 422 is More Accurate Than 404

HTTP 404 means the URI endpoint itself could not be located. The endpoint `POST /sensors` is perfectly valid and reachable. HTTP 422 Unprocessable Entity means the server understood the content type and parsed the body successfully, but the payload is semantically invalid — it references a non-existent resource. A 404 would mislead the developer into thinking their URL is wrong.

---

### Part 5.4 — Security Risks of Exposing Stack Traces

Exposing raw Java stack traces reveals: internal package and class names (enabling targeted attacks), third-party library versions (enabling CVE lookups), internal file system paths, application logic and control flow, and potentially database structure. The `GlobalExceptionMapper` addresses all these risks by logging the full trace server-side only and returning a clean generic JSON 500 response to the client.

---

### Part 5.5 — Why Filters for Cross-Cutting Concerns

Using JAX-RS filters for logging is superior to manual `Logger.info()` calls because: it follows the DRY principle (logic exists in one place), separates concerns (resource methods express business logic only), guarantees coverage (filters apply to every request automatically), allows composability (same mechanism handles logging, auth, CORS), and is centrally configurable (changing log format requires editing one class).
