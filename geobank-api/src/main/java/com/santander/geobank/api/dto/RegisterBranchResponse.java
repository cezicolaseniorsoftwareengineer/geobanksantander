
package com.santander.geobank.api.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for branch registration.
 *
 * Returns confirmation information according to GeoBank specification.
 */
public record RegisterBranchResponse(
        String id,
        String name,
        String address,
        Double longitude,
        Double latitude,
        String status,
        LocalDateTime createdAt,
        String message) {

    /**
     * Factory method for success response.
     */
    public static RegisterBranchResponse success(
            String id,
            String name,
            String address,
            Double longitude,
            Double latitude) {
        return new RegisterBranchResponse(
                id,
                name,
                address,
                longitude,
                latitude,
                "REGISTERED",
                LocalDateTime.now(),
                "Branch successfully registered");
    }

    /**
     * Factory method for error response.
     */
    public static RegisterBranchResponse error(String message) {
        return new RegisterBranchResponse(
                null,
                null,
                null,
                null,
                null,
                "ERROR",
                LocalDateTime.now(),
                message);
    }
}

