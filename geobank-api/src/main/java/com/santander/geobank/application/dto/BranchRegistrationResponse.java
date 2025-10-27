package com.santander.geobank.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for branch registration response containing confirmation data.
 * Immutable record providing registration status and metadata.
 */
public record BranchRegistrationResponse(
        String id,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        String type,
        String status,
        String address,
        String operatingHours,
        LocalDateTime registeredAt,
        String message) {

    /**
     * Creates successful registration response.
     *
     * @param id             branch identifier
     * @param name           branch name
     * @param latitude       coordinate
     * @param longitude      coordinate
     * @param type           branch type
     * @param status         branch status
     * @param address        optional address
     * @param operatingHours optional hours
     * @param registeredAt   registration timestamp
     * @return success response
     */
    public static BranchRegistrationResponse success(
            String id, String name, BigDecimal latitude, BigDecimal longitude,
            String type, String status, String address, String operatingHours,
            LocalDateTime registeredAt) {
        return new BranchRegistrationResponse(
                id, name, latitude, longitude, type, status,
                address, operatingHours, registeredAt,
                "Branch registered successfully");
    }
}

