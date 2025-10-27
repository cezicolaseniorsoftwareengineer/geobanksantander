package com.santander.geobank.infrastructure.persistence.entities;

/**
 * JPA enum mapping for BranchType domain enum.
 * Ensures database compatibility with different SQL dialects.
 */
public enum BranchTypeEntity {
    AGENCY,
    ATM,
    SELF_SERVICE,
    DIGITAL_POINT
}

