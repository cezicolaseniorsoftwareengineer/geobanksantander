package com.santander.geobank.domain.services;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import com.santander.geobank.domain.model.Branch;
import com.santander.geobank.domain.model.BranchStatus;
import com.santander.geobank.domain.model.BranchType;

/**
 * Domain service encapsulating banking-specific business rules.
 * Contains pure domain logic without infrastructure dependencies.
 *
 * Design Principles:
 * - No framework dependencies (pure Java)
 * - Stateless operations
 * - Business rule validation
 * - Domain invariant enforcement
 *
 * Architecture Alignment:
 * - Hexagonal Architecture: inner hexagon (domain layer)
 * - DDD: Domain Service pattern for cross-entity logic
 * - Clean Architecture: entities + use cases layer
 *
 * @author Banking Domain Team
 * @since 1.0.0
 */
public class BranchBusinessRules {

    private static final int MIN_OPERATIONAL_HOURS = 4;
    private static final int MAX_OPERATIONAL_HOURS = 24;
    private static final double MIN_BRANCH_DISTANCE_KM = 0.5; // Minimum distance between branches

    /**
     * Validate if branch can be registered at given location.
     * Enforces business rules for branch network optimization.
     */
    public ValidationResult validateBranchRegistration(
            Branch newBranch,
            Iterable<Branch> existingBranches) {

        Objects.requireNonNull(newBranch, "Branch cannot be null");
        Objects.requireNonNull(existingBranches, "Existing branches cannot be null");

        // Rule 1: Check minimum distance from other branches
        for (Branch existing : existingBranches) {
            if (!existing.isOperational()) {
                continue;
            }

            double distance = newBranch.distanceTo(existing.getLocation()).getKilometers();
            if (distance < MIN_BRANCH_DISTANCE_KM) {
                return ValidationResult.failure(
                        "Branch too close to existing branch " + existing.getName() +
                                " (distance: " + String.format("%.2f", distance) + " km, minimum: " +
                                MIN_BRANCH_DISTANCE_KM + " km)");
            }
        }

        // Rule 2: Validate branch type for location density
        long nearbyBranches = countBranchesInRadius(newBranch, existingBranches, 5.0);
        if (nearbyBranches >= 10 && newBranch.getType() == BranchType.TRADITIONAL) {
            return ValidationResult.failure(
                    "Area is saturated. Consider ATM or express branch instead.");
        }

        return ValidationResult.success();
    }

    /**
     * Determine if branch should be temporarily closed based on operational
     * criteria.
     */
    public boolean shouldTemporarilyClose(Branch branch, LocalDateTime assessmentTime) {
        Objects.requireNonNull(branch, "Branch cannot be null");
        Objects.requireNonNull(assessmentTime, "Assessment time cannot be null");

        // Rule: Only operational branches can be evaluated for closure
        if (!branch.isOperational()) {
            return false;
        }

        // Rule: Check if outside operational hours
        LocalTime currentTime = assessmentTime.toLocalTime();
        if (!isWithinOperationalHours(branch.getType(), currentTime)) {
            return true;
        }

        return false;
    }

    /**
     * Calculate optimal branch type for a location based on area characteristics.
     */
    public BranchType recommendBranchType(
            double populationDensity,
            double commercialActivityIndex,
            int nearbyBranchCount) {

        if (populationDensity < 100) {
            // Rural area - ATM sufficient
            return BranchType.ATM_ONLY;
        }

        if (nearbyBranchCount >= 5) {
            // High density area - express services
            return BranchType.EXPRESS;
        }

        if (commercialActivityIndex > 0.7) {
            // Business district - premium or traditional
            return BranchType.PREMIUM;
        }

        // Default to traditional full service
        return BranchType.TRADITIONAL;
    }

    /**
     * Validate branch status transition according to banking regulations.
     */
    public ValidationResult validateStatusTransition(
            BranchStatus currentStatus,
            BranchStatus newStatus) {

        Objects.requireNonNull(currentStatus, "Current status cannot be null");
        Objects.requireNonNull(newStatus, "New status cannot be null");

        // Rule: Cannot reopen permanently closed branch
        if (currentStatus == BranchStatus.PERMANENTLY_CLOSED) {
            return ValidationResult.failure(
                    "Cannot change status of permanently closed branch");
        }

        // Rule: Direct transition to permanent closure requires approval
        if (currentStatus == BranchStatus.ACTIVE &&
                newStatus == BranchStatus.PERMANENTLY_CLOSED) {
            return ValidationResult.failure(
                    "Direct transition from ACTIVE to PERMANENTLY_CLOSED requires " +
                            "temporary closure period for regulatory compliance");
        }

        return ValidationResult.success();
    }

    /**
     * Calculate priority score for branch based on business criteria.
     */
    public int calculateBusinessPriority(
            Branch branch,
            double customerFootfall,
            double transactionVolume) {

        Objects.requireNonNull(branch, "Branch cannot be null");

        if (!branch.isOperational()) {
            return 0;
        }

        int basePriority = branch.getPriorityScore();

        // Adjust based on performance metrics
        int footfallBonus = (int) (customerFootfall / 100);
        int volumeBonus = (int) (transactionVolume / 1_000_000);

        return basePriority + footfallBonus + volumeBonus;
    }

    /**
     * Determine if branch meets regulatory compliance requirements.
     */
    public ValidationResult validateRegulatoryCompliance(Branch branch) {
        Objects.requireNonNull(branch, "Branch cannot be null");

        // Rule: Branch must have valid contact information
        if (branch.getContactPhone().isEmpty()) {
            return ValidationResult.failure(
                    "Branch must have contact phone for regulatory compliance");
        }

        // Rule: Branch location must be accessible (basic validation)
        double lat = branch.getLocation().latitude();
        double lon = branch.getLocation().longitude();
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            return ValidationResult.failure(
                    "Branch location coordinates are invalid");
        }

        return ValidationResult.success();
    }

    // Private helper methods

    private long countBranchesInRadius(
            Branch center,
            Iterable<Branch> branches,
            double radiusKm) {

        long count = 0;
        for (Branch branch : branches) {
            if (branch.isOperational() &&
                    branch.isWithinRadius(center.getLocation(), radiusKm)) {
                count++;
            }
        }
        return count;
    }

    private boolean isWithinOperationalHours(BranchType type, LocalTime time) {
        // Simplified operational hours - in production would be configurable
        return switch (type) {
            case TRADITIONAL -> time.isAfter(LocalTime.of(9, 0)) &&
                    time.isBefore(LocalTime.of(17, 0));
            case PREMIUM -> time.isAfter(LocalTime.of(8, 0)) &&
                    time.isBefore(LocalTime.of(18, 0));
            case ATM_ONLY -> true; // 24/7
            case DIGITAL, EXPRESS -> time.isAfter(LocalTime.of(10, 0)) &&
                    time.isBefore(LocalTime.of(16, 0));
        };
    }

    /**
     * Validation result value object.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String message) {
            Objects.requireNonNull(message, "Failure message cannot be null");
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public String getMessageOrDefault(String defaultMessage) {
            return message != null ? message : defaultMessage;
        }
    }
}
