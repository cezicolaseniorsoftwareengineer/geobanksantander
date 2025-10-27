package com.santander.geobank.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for branch registration request containing mandatory fields
 * with comprehensive validation for geographic coordinates and banking
 * metadata.
 *
 * Validates Brazilian coordinate boundaries and banking business rules.
 */
public record BranchRegistrationRequest(

        @NotBlank(message = "Branch ID cannot be blank") @Size(min = 3, max = 20, message = "Branch ID must be between 3 and 20 characters") @Pattern(regexp = "^[A-Z0-9]+$", message = "Branch ID must contain only uppercase letters and numbers") String id,

        @NotBlank(message = "Branch name cannot be blank") @Size(min = 2, max = 100, message = "Branch name must be between 2 and 100 characters") String name,

        @NotNull(message = "Latitude cannot be null") @DecimalMin(value = "-33.751", message = "Latitude must be within Brazilian territory bounds (min: -33.751)") @DecimalMax(value = "5.271", message = "Latitude must be within Brazilian territory bounds (max: 5.271)") BigDecimal latitude,

        @NotNull(message = "Longitude cannot be null") @DecimalMin(value = "-73.983", message = "Longitude must be within Brazilian territory bounds (min: -73.983)") @DecimalMax(value = "-34.793", message = "Longitude must be within Brazilian territory bounds (max: -34.793)") BigDecimal longitude,

        @NotNull(message = "Branch type cannot be null") @Pattern(regexp = "^(AGENCY|ATM|SELF_SERVICE|DIGITAL_POINT)$", message = "Branch type must be one of: AGENCY, ATM, SELF_SERVICE, DIGITAL_POINT") String type,

        @NotNull(message = "Branch status cannot be null") @Pattern(regexp = "^(ACTIVE|INACTIVE|MAINTENANCE)$", message = "Branch status must be one of: ACTIVE, INACTIVE, MAINTENANCE") String status,

        @Size(max = 500, message = "Address cannot exceed 500 characters") String address,

        @Pattern(regexp = "^\\d{2}:\\d{2}-\\d{2}:\\d{2}$", message = "Operating hours must follow format HH:MM-HH:MM") String operatingHours) {

    /**
     * Validates business rules beyond annotation constraints.
     *
     * @return validation error message or null if valid
     */
    public String validateBusinessRules() {
        // Validate coordinate precision (max 6 decimal places for banking precision)
        if (hasExcessivePrecision(latitude) || hasExcessivePrecision(longitude)) {
            return "Coordinates cannot have more than 6 decimal places";
        }

        // Validate operating hours format if provided
        if (operatingHours != null && !isValidOperatingHours(operatingHours)) {
            return "Operating hours must have start time before end time";
        }

        return null;
    }

    private boolean hasExcessivePrecision(BigDecimal coordinate) {
        return coordinate.scale() > 6;
    }

    private boolean isValidOperatingHours(String hours) {
        if (hours == null || !hours.matches("^\\d{2}:\\d{2}-\\d{2}:\\d{2}$")) {
            return false;
        }

        String[] parts = hours.split("-");
        String startTime = parts[0];
        String endTime = parts[1];

        return startTime.compareTo(endTime) < 0;
    }
}

