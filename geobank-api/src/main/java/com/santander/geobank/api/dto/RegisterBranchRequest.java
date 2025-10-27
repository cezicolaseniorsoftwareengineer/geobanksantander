
package com.santander.geobank.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for branch registration request.
 *
 * Implements validation for:
 * - Longitude and latitude
 * - Required fields for branch identification
 * - Integration with authentication system
 * 
 * @since 1.0.0
 */
public record RegisterBranchRequest(

        @NotBlank(message = "Branch name is required") @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters") String name,

        @NotBlank(message = "Address is required") @Size(min = 5, max = 200, message = "Address must be between 5 and 200 characters") String address,

        @NotNull(message = "Longitude is required") @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180") @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180") Double longitude,

        @NotNull(message = "Latitude is required") @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90") @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90") Double latitude) {

    /**
     * Additional validation for Brazilian territory coordinates.
     * Brazil: Latitude: -33.75 to 5.27, Longitude: -73.98 to -28.84
     */
    public boolean isValidBrazilianCoordinates() {
        return latitude >= -33.75 && latitude <= 5.27 &&
                longitude >= -73.98 && longitude <= -28.84;
    }

    /**
     * Verifies if coordinates are globally valid.
     */
    public boolean hasValidCoordinates() {
        return longitude != null && latitude != null &&
                longitude >= -180.0 && longitude <= 180.0 &&
                latitude >= -90.0 && latitude <= 90.0;
    }

    /**
     * Converts to standardized coordinate representation.
     */
    public String getCoordinatesString() {
        return String.format(java.util.Locale.US, "(%.6f, %.6f)", longitude, latitude);
    }
}
