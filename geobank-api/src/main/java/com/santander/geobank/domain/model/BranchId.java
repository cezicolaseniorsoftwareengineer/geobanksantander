package com.santander.geobank.domain.model;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * BranchId - Value Object representing unique branch identifier
 *
 * Immutable identifier for banking branches with validation.
 * Supports both UUID and custom format identifiers.
 * */
public record BranchId(String value) {

    private static final Pattern BRANCH_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{4,12}$");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /**
     * Constructor with validation
     */
    public BranchId {
        Objects.requireNonNull(value, "BranchId value cannot be null");

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("BranchId value cannot be empty");
        }

        // Validate format: either UUID or branch code
        if (!UUID_PATTERN.matcher(trimmed).matches() &&
                !BRANCH_CODE_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    String.format("Invalid BranchId format: %s. Must be UUID or 4-12 alphanumeric characters",
                            trimmed));
        }

        // Normalize to uppercase for branch codes
        value = isUUID(trimmed) ? trimmed : trimmed.toUpperCase();
    }

    /**
     * Generate new UUID-based BranchId
     *
     * @return new BranchId with UUID
     */
    public static BranchId generate() {
        return new BranchId(UUID.randomUUID().toString());
    }

    /**
     * Create BranchId from branch code
     *
     * @param branchCode banking branch code
     * @return BranchId instance
     */
    public static BranchId fromBranchCode(String branchCode) {
        Objects.requireNonNull(branchCode, "Branch code cannot be null");
        return new BranchId(branchCode.trim().toUpperCase());
    }

    /**
     * Create BranchId from UUID string
     *
     * @param uuid UUID string
     * @return BranchId instance
     */
    public static BranchId fromUUID(String uuid) {
        Objects.requireNonNull(uuid, "UUID cannot be null");
        // Validate UUID format
        UUID.fromString(uuid); // Throws exception if invalid
        return new BranchId(uuid);
    }

    /**
     * Create BranchId from UUID object
     *
     * @param uuid UUID object
     * @return BranchId instance
     */
    public static BranchId fromUUID(UUID uuid) {
        Objects.requireNonNull(uuid, "UUID cannot be null");
        return new BranchId(uuid.toString());
    }

    /**
     * Check if the value is a UUID format
     *
     * @return true if UUID format
     */
    public boolean isUUID() {
        return isUUID(this.value);
    }

    /**
     * Check if the value is a branch code format
     *
     * @return true if branch code format
     */
    public boolean isBranchCode() {
        return !isUUID() && BRANCH_CODE_PATTERN.matcher(this.value).matches();
    }

    /**
     * Get short display version of the ID
     *
     * @return shortened ID for display
     */
    public String getShortDisplay() {
        if (isUUID()) {
            return value.substring(0, 8) + "...";
        }
        return value;
    }

    /**
     * Convert to UUID if possible
     *
     * @return UUID object if value is UUID format
     * @throws IllegalStateException if not UUID format
     */
    public UUID toUUID() {
        if (!isUUID()) {
            throw new IllegalStateException("BranchId is not in UUID format: " + value);
        }
        return UUID.fromString(value);
    }

    private static boolean isUUID(String value) {
        return UUID_PATTERN.matcher(value).matches();
    }

    @Override
    public String toString() {
        return value;
    }
}

