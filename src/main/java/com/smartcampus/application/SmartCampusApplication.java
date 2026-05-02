package com.smartcampus.application;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;
import com.smartcampus.exception.*;
import com.smartcampus.filter.LoggingFilter;

/**
 * JAX-RS Application entry point.
 * @ApplicationPath sets the base URI for all resources.
 *
 * Lifecycle Note: By default, JAX-RS creates a new resource class instance per request (request-scoped).
 * This means resource classes are NOT singletons. We therefore use a shared DataStore singleton
 * with ConcurrentHashMap to safely manage in-memory state across all requests.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // Resources
        classes.add(DiscoveryResource.class);
        classes.add(RoomResource.class);
        classes.add(SensorResource.class);
        // Exception Mappers
        classes.add(RoomNotEmptyExceptionMapper.class);
        classes.add(LinkedResourceNotFoundExceptionMapper.class);
        classes.add(SensorUnavailableExceptionMapper.class);
        classes.add(GlobalExceptionMapper.class);
        // Filters
        classes.add(LoggingFilter.class);
        return classes;
    }
}
