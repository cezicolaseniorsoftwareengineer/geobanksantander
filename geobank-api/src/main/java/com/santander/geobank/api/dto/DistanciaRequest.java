package com.santander.geobank.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO for distance query between user and branches.
 *
 * Implements validations according to challenge specification:
 * - Mandatory X and Y coordinates
 * - User position for distance calculation
 * * @since 1.0.0
 */
public record DistanciaRequest(

        @NotNull(message = "User X coordinate (longitude) is required") @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180") @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180") Double posX,

        @NotNull(message = "User Y coordinate (latitude) is required") @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90") @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90") Double posY,

        @Positive(message = "Limit must be greater than zero") Integer limite) {

    /**
     * Constructor with default limit of 10 branches.
     */
    public DistanciaRequest(Double posX, Double posY) {
        this(posX, posY, 10);
    }

    /**
     * Verifies if coordinates are valid.
     */
    public boolean hasValidCoordinates() {
        return posX != null && posY != null &&
                posX >= -180.0 && posX <= 180.0 &&
                posY >= -90.0 && posY <= 90.0;
    }

    /**
     * Returns safe limit (maximum 100 branches).
     */
    public int getLimiteSeguro() {
        if (limite == null || limite <= 0) {
            return 10; // Default
        }
        return Math.min(limite, 100); // Maximum 100
    }

    /**
     * Converts to coordinate representation.
     */
    public String getCoordinatesString() {
        return String.format(java.util.Locale.US, "User at (%.6f, %.6f)", posX, posY);
    }
}
