package com.santander.geobank.domain.ports.input;

import java.util.Optional;

import com.santander.geobank.domain.model.BranchId;

/**
 * Enhanced Input Port for Branch Registration Use Case
 *
 * @deprecated Interfaces currently not implemented - planned for future
 *             releases
 */
@Deprecated(since = "1.0.0", forRemoval = false)
interface RegisterBranchUseCase {

    /**
     * Register a new banking branch with comprehensive validation
     *
     * Performs business rule validation, geographic verification,
     * and regulatory compliance checks before persistence.
     *
     * @param request Branch registration data with geo coordinates
     * @return Registration result with validation status and audit trail
     * @throws BranchValidationException if business rules violated
     * @throws DuplicateBranchException  if branch already exists
     */
    BranchRegistrationResult register(BranchRegistrationRequest request);

    /**
     * Validate branch registration data without persisting
     *
     * Dry-run validation for UI feedback and pre-submission checks.
     *
     * @param request Branch data to validate
     * @return Validation result with detailed feedback for each field
     */
    BranchValidationResult validate(BranchRegistrationRequest request);

    /**
     * Register branch asynchronously for bulk operations
     *
     * @param request Branch registration data
     * @return Future containing registration result
     */
    java.util.concurrent.CompletableFuture<BranchRegistrationResult> registerAsync(BranchRegistrationRequest request);
}

/**
 * Enhanced Input Port for Branch Query Operations
 */
/**
 * Query Branch Use Case Interface
 * 
 * @deprecated Not implemented - planned for future releases
 */
@Deprecated(since = "1.0.0", forRemoval = false)
interface QueryBranchUseCase {

    /**
     * Find branches within specified distance radius
     *
     * Uses optimized spatial indexing for sub-100ms response times.
     *
     * @param request Distance calculation request with geo point and radius
     * @return List of branches sorted by distance with travel time estimates
     */
    BranchDistanceResult findBranchesWithinDistance(DistanceCalculationRequest request);

    /**
     * Get branch by unique identifier with caching
     *
     * @param branchId Branch identifier
     * @return Branch data from cache or database
     */
    Optional<BranchData> findById(BranchId branchId);

    /**
     * Search branches by multiple criteria with pagination
     *
     * @param criteria Search parameters (name, region, services, status)
     * @return Paginated branch results with faceted search support
     */
    BranchSearchResult searchBranches(BranchSearchCriteria criteria);

    /**
     * Get all active branches for administrative operations
     *
     * @return List of all active branches with basic data
     */
    java.util.List<BranchSummary> findAllActive();

    /**
     * Find branches by geographic region
     *
     * @param region Geographic region identifier
     * @return Branches within specified region
     */
    java.util.List<BranchData> findByRegion(GeographicRegion region);
}

/**
 * Enhanced Input Port for Branch Management Operations
 */
/**
 * Manage Branch Use Case Interface
 * 
 * @deprecated Not implemented - planned for future releases
 */
@Deprecated(since = "1.0.0", forRemoval = false)
interface ManageBranchUseCase {

    /**
     * Update branch information with optimistic locking
     *
     * @param request Update request with version for concurrency control
     * @return Update result with validation status and new version
     */
    BranchUpdateResult updateBranch(BranchUpdateRequest request);

    /**
     * Deactivate branch with business rule validation
     *
     * Ensures no active transactions or pending operations before deactivation.
     *
     * @param branchId Branch to deactivate
     * @param reason   Deactivation reason for audit trail
     * @return Deactivation result with validation status
     */
    BranchDeactivationResult deactivateBranch(BranchId branchId, String reason);

    /**
     * Activate previously deactivated branch
     *
     * @param branchId Branch to activate
     * @param reason   Activation reason for audit trail
     * @return Activation result with validation status
     */
    BranchActivationResult activateBranch(BranchId branchId, String reason);

    /**
     * Update branch geolocation with spatial validation
     *
     * @param branchId    Branch identifier
     * @param newLocation New geographic coordinates
     * @return Update result with geographic validation status
     */
    BranchLocationUpdateResult updateLocation(BranchId branchId, GeoPoint newLocation);

    /**
     * Bulk update branch data for administrative operations
     *
     * @param updates List of branch updates
     * @return Bulk operation result with individual status for each branch
     */
    BranchBulkUpdateResult bulkUpdate(java.util.List<BranchUpdateRequest> updates);
}

// Placeholder classes for type safety - to be implemented
class BranchRegistrationRequest {
}

class BranchRegistrationResult {
}

class BranchValidationResult {
}

class DistanceCalculationRequest {
}

class BranchDistanceResult {
}

class BranchData {
}

class BranchSearchCriteria {
}

class BranchSearchResult {
}

class BranchSummary {
}

class GeographicRegion {
}

class BranchUpdateRequest {
}

class BranchUpdateResult {
}

class BranchDeactivationResult {
}

class BranchActivationResult {
}

class GeoPoint {
}

class BranchLocationUpdateResult {
}

class BranchBulkUpdateResult {
}

// Exception classes
/**
 * Branch validation exception
 * 
 * @deprecated Not implemented - planned for future releases
 */
@Deprecated(since = "1.0.0", forRemoval = false)
class BranchValidationException extends Exception {
    public BranchValidationException(String message) {
        super(message);
    }
}

/**
 * Duplicate branch exception
 * 
 * @deprecated Not implemented - planned for future releases
 */
@Deprecated(since = "1.0.0", forRemoval = false)
class DuplicateBranchException extends Exception {
    public DuplicateBranchException(String message) {
        super(message);
    }
}

