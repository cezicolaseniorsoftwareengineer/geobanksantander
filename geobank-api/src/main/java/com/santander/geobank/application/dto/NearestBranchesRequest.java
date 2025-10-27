package com.santander.geobank.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for nearest branches search request with coordinate validation
 * and configurable search parameters.
 */
public record NearestBranchesRequest(

        @NotNull(message = "Latitude cannot be null") @DecimalMin(value = "-33.751", message = "Latitude must be within Brazilian territory bounds (min: -33.751)") @DecimalMax(value = "5.271", message = "Latitude must be within Brazilian territory bounds (max: 5.271)") BigDecimal latitude,

        @NotNull(message = "Longitude cannot be null") @DecimalMin(value = "-73.983", message = "Longitude must be within Brazilian territory bounds (min: -73.983)") @DecimalMax(value = "-34.793", message = "Longitude must be within Brazilian territory bounds (max: -34.793)") BigDecimal longitude,

        @Min(value = 1, message = "Limit must be at least 1") @Max(value = 50, message = "Limit cannot exceed 50 branches") Integer limit,

        @DecimalMin(value = "0.1", message = "Max distance must be at least 0.1 km") @DecimalMax(value = "100.0", message = "Max distance cannot exceed 100 km") BigDecimal maxDistanceKm) {

    /**
     * Creates request with default values for optional parameters.
     *
     * @param latitude  search center latitude
     * @param longitude search center longitude
     * @return request with defaults: limit=10, maxDistance=25km
     */
    public static NearestBranchesRequest withDefaults(BigDecimal latitude, BigDecimal longitude) {
        return new NearestBranchesRequest(latitude, longitude, 10, new BigDecimal("25.0"));
    }

    /**
     * Validates business rules for search parameters.
     *
     * @return validation error message or null if valid
     */
    public String validateBusinessRules() {
        // Validate coordinate precision (max 6 decimal places for performance)
        if (hasExcessivePrecision(latitude) || hasExcessivePrecision(longitude)) {
            return "Coordinates cannot have more than 6 decimal places";
        }

        // Apply defaults if null
        if (limit == null || maxDistanceKm == null) {
            return "Limit and maxDistanceKm must be specified";
        }

        return null;
    }

    private boolean hasExcessivePrecision(BigDecimal coordinate) {
        return coordinate.scale() > 6;
    }
}

