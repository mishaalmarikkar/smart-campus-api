package com.smartcampus.application;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;
import com.smartcampus.exception.RoomNotEmptyExceptionMapper;
import com.smartcampus.exception.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.exception.SensorUnavailableExceptionMapper;
import com.smartcampus.exception.GlobalExceptionMapper;
import com.smartcampus.filter.LoggingFilter;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static final String BASE_URI = "http://0.0.0.0:8080/api/v1/";

    public static void main(String[] args) throws IOException {

        ResourceConfig config = new ResourceConfig()
                // Register resources explicitly (avoids @ApplicationPath conflict)
                .register(DiscoveryResource.class)
                .register(RoomResource.class)
                .register(SensorResource.class)
                // Exception mappers
                .register(RoomNotEmptyExceptionMapper.class)
                .register(LinkedResourceNotFoundExceptionMapper.class)
                .register(SensorUnavailableExceptionMapper.class)
                .register(GlobalExceptionMapper.class)
                // Filters
                .register(LoggingFilter.class)
                // JSON support
                .register(JacksonFeature.class);

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);

        LOGGER.info("Smart Campus API started at: http://localhost:8080/api/v1");
        LOGGER.info("Press ENTER to stop...");
        System.in.read();
        server.shutdownNow();
    }
}