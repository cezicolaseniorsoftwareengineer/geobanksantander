package com.santander.geobank.domain.events;

import java.time.LocalDateTime;
import java.util.Objects;

import com.santander.geobank.domain.model.Branch;
import com.santander.geobank.domain.model.BranchId;

/**
 * BranchRegisteredEvent - Domain event for branch registration
 *
 * Published when a new branch is registered in the system.
 * Triggers cache invalidation and network analysis.
 * */
public record BranchRegisteredEvent(
        BranchId branchId,
        String branchName,
        String branchType,
        double latitude,
        double longitude,
        LocalDateTime occurredAt,
        String correlationId) {

    /**
     * Constructor with validation
     */
    public BranchRegisteredEvent {
        Objects.requireNonNull(branchId, "Branch ID cannot be null");
        Objects.requireNonNull(branchName, "Branch name cannot be null");
        Objects.requireNonNull(branchType, "Branch type cannot be null");
        Objects.requireNonNull(occurredAt, "Occurred timestamp cannot be null");
        Objects.requireNonNull(correlationId, "Correlation ID cannot be null");

        if (branchName.trim().isEmpty()) {
            throw new IllegalArgumentException("Branch name cannot be empty");
        }

        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Invalid latitude: " + latitude);
        }

        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Invalid longitude: " + longitude);
        }
    }

    /**
     * Create event from Branch entity
     *
     * @param branch        branch entity
     * @param correlationId correlation identifier
     * @return domain event
     */
    public static BranchRegisteredEvent from(Branch branch, String correlationId) {
        Objects.requireNonNull(branch, "Branch cannot be null");
        Objects.requireNonNull(correlationId, "Correlation ID cannot be null");

        return new BranchRegisteredEvent(
                branch.getId(),
                branch.getName(),
                branch.getType().name(),
                branch.getLocation().latitude(),
                branch.getLocation().longitude(),
                LocalDateTime.now(),
                correlationId);
    }

    /**
     * Get event type identifier
     *
     * @return event type
     */
    public String getEventType() {
        return "BRANCH_REGISTERED";
    }

    /**
     * Get event version for schema evolution
     *
     * @return event version
     */
    public String getVersion() {
        return "1.0";
    }

    /**
     * Check if this is a critical event requiring immediate processing
     *
     * @return true if critical
     */
    public boolean isCritical() {
        return true; // Branch registration is always critical for cache invalidation
    }
}

