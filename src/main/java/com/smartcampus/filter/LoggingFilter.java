package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Cross-cutting logging filter that intercepts every HTTP request and response.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter so a single
 * class handles the full request/response lifecycle for observability.
 *
 * Why filters over manual Logger.info() in every resource method?
 * - DRY principle: one place to change logging behaviour across the entire API
 * - Separation of concerns: resource methods focus on business logic only
 * - Guaranteed coverage: a new resource class gets logging automatically
 * - Consistent format: all log entries look the same regardless of who wrote the resource
 * - Easier to extend: add auth checks, rate limiting, or correlation IDs in one place
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Logs incoming request: HTTP method and URI.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format("[REQUEST]  %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()));
    }

    /**
     * Logs outgoing response: HTTP status code.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format("[RESPONSE] %s %s -> HTTP %d",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus()));
    }
}
