package com.santander.geobank.application.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for standardized error responses with detailed validation information.
 * Provides consistent error structure across all API endpoints.
 */
public record ErrorResponse(
        String error,
        String message,
        Integer status,
        String path,
        LocalDateTime timestamp,
        List<ValidationError> validationErrors) {

    /**
     * Individual validation error detail.
     */
    public record ValidationError(
            String field,
            Object rejectedValue,
            String message) {
    }

    /**
     * Creates validation error response.
     *
     * @param message          error message
     * @param status           HTTP status code
     * @param path             request path
     * @param validationErrors list of field errors
     * @return validation error response
     */
    public static ErrorResponse validationError(
            String message, Integer status, String path,
            List<ValidationError> validationErrors) {
        return new ErrorResponse(
                "Validation Failed",
                message,
                status,
                path,
                LocalDateTime.now(),
                validationErrors);
    }

    /**
     * Creates business logic error response.
     *
     * @param message error message
     * @param status  HTTP status code
     * @param path    request path
     * @return business error response
     */
    public static ErrorResponse businessError(String message, Integer status, String path) {
        return new ErrorResponse(
                "Business Rule Violation",
                message,
                status,
                path,
                LocalDateTime.now(),
                List.of());
    }

    /**
     * Creates internal server error response.
     *
     * @param message error message
     * @param path    request path
     * @return server error response
     */
    public static ErrorResponse serverError(String message, String path) {
        return new ErrorResponse(
                "Internal Server Error",
                message,
                500,
                path,
                LocalDateTime.now(),
                List.of());
    }
}

