package com.santander.geobank.infrastructure.persistence.entities;

/**
 * JPA enum mapping for BranchStatus domain enum.
 * Ensures database compatibility with different SQL dialects.
 */
public enum BranchStatusEntity {
    ACTIVE,
    INACTIVE,
    MAINTENANCE
}

