package com.santander.geobank.domain.model;

/**
 * BranchStatus - Value Object representing the operational status of a branch
 *
 * Enumeration of possible branch operational states with business rules.
 * Controls service availability and operational capabilities.
 * */
public enum BranchStatus {

    /**
     * Branch is fully operational and accepting all services
     */
    ACTIVE("Active", "Branch is fully operational", true, true),

    /**
     * Branch is temporarily closed (maintenance, holidays, etc.)
     */
    TEMPORARILY_CLOSED("Temporarily Closed", "Branch is temporarily unavailable", false, true),

    /**
     * Branch is permanently closed and will not reopen
     */
    PERMANENTLY_CLOSED("Permanently Closed", "Branch is permanently closed", false, false),

    /**
     * Branch is under maintenance but may have limited services
     */
    UNDER_MAINTENANCE("Under Maintenance", "Branch is under maintenance", false, true),

    /**
     * Branch is planned but not yet operational
     */
    PLANNED("Planned", "Branch is planned but not yet open", false, true);

    private final String displayName;
    private final String description;
    private final boolean isOperational;
    private final boolean canReopen;

    BranchStatus(String displayName, String description, boolean isOperational, boolean canReopen) {
        this.displayName = displayName;
        this.description = description;
        this.isOperational = isOperational;
        this.canReopen = canReopen;
    }

    /**
     * Get display name for UI
     *
     * @return human-readable display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get description of status
     *
     * @return detailed description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if branch is operational in this status
     *
     * @return true if branch can provide services
     */
    public boolean isOperational() {
        return isOperational;
    }

    /**
     * Check if branch can be reopened from this status
     *
     * @return true if status allows reopening
     */
    public boolean canReopen() {
        return canReopen;
    }

    /**
     * Check if status allows emergency services
     *
     * @return true if emergency services available
     */
    public boolean allowsEmergencyServices() {
        return this == ACTIVE || this == UNDER_MAINTENANCE;
    }

    /**
     * Check if status allows ATM services
     *
     * @return true if ATM services available
     */
    public boolean allowsATMServices() {
        return this != PERMANENTLY_CLOSED;
    }

    /**
     * Get next valid status transitions
     *
     * @return array of valid next statuses
     */
    public BranchStatus[] getValidTransitions() {
        return switch (this) {
            case PLANNED -> new BranchStatus[] { ACTIVE, PERMANENTLY_CLOSED };
            case ACTIVE -> new BranchStatus[] { TEMPORARILY_CLOSED, UNDER_MAINTENANCE, PERMANENTLY_CLOSED };
            case TEMPORARILY_CLOSED -> new BranchStatus[] { ACTIVE, UNDER_MAINTENANCE, PERMANENTLY_CLOSED };
            case UNDER_MAINTENANCE -> new BranchStatus[] { ACTIVE, TEMPORARILY_CLOSED, PERMANENTLY_CLOSED };
            case PERMANENTLY_CLOSED -> new BranchStatus[] {}; // No transitions allowed
        };
    }

    /**
     * Check if transition to another status is valid
     *
     * @param newStatus target status
     * @return true if transition is allowed
     */
    public boolean canTransitionTo(BranchStatus newStatus) {
        if (newStatus == null) {
            return false;
        }

        BranchStatus[] validTransitions = getValidTransitions();
        for (BranchStatus validStatus : validTransitions) {
            if (validStatus == newStatus) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse BranchStatus from string
     *
     * @param value string representation
     * @return BranchStatus instance
     * @throws IllegalArgumentException if invalid value
     */
    public static BranchStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("BranchStatus value cannot be null or empty");
        }

        String normalized = value.trim().toUpperCase().replace(" ", "_");

        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try matching by display name
            for (BranchStatus status : values()) {
                if (status.displayName.equalsIgnoreCase(value.trim())) {
                    return status;
                }
            }
            throw new IllegalArgumentException(
                    String.format("Unknown BranchStatus: %s. Valid statuses: %s",
                            value, java.util.Arrays.toString(values())));
        }
    }

    /**
     * Get status severity level for alerting
     *
     * @return severity level (0=normal, 1=warning, 2=critical)
     */
    public int getSeverityLevel() {
        return switch (this) {
            case ACTIVE, PLANNED -> 0;
            case TEMPORARILY_CLOSED, UNDER_MAINTENANCE -> 1;
            case PERMANENTLY_CLOSED -> 2;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}

