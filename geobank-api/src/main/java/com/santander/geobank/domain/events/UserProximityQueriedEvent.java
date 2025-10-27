package com.santander.geobank.domain.events;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.santander.geobank.domain.model.BranchId;

/**
 * UserProximityQueriedEvent - Domain event for proximity queries
 *
 * Published when a user queries for nearby branches.
 * Used for analytics and performance monitoring.
 * */
@SuppressWarnings("java:S1116") // Collection is populated by caller, not modified here
public record UserProximityQueriedEvent(
        double userLatitude,
        double userLongitude,
        double radiusKm,
        int maxResults,
        List<BranchId> foundBranches,
        long executionTimeMs,
        boolean cacheHit,
        LocalDateTime occurredAt,
        String correlationId,
        String sessionId) {

    /**
     * Constructor with validation
     */
    public UserProximityQueriedEvent {
        Objects.requireNonNull(foundBranches, "Found branches list cannot be null");
        Objects.requireNonNull(occurredAt, "Occurred timestamp cannot be null");
        Objects.requireNonNull(correlationId, "Correlation ID cannot be null");

        if (userLatitude < -90.0 || userLatitude > 90.0) {
            throw new IllegalArgumentException("Invalid user latitude: " + userLatitude);
        }

        if (userLongitude < -180.0 || userLongitude > 180.0) {
            throw new IllegalArgumentException("Invalid user longitude: " + userLongitude);
        }

        if (radiusKm <= 0) {
            throw new IllegalArgumentException("Radius must be positive: " + radiusKm);
        }

        if (maxResults <= 0) {
            throw new IllegalArgumentException("Max results must be positive: " + maxResults);
        }

        if (executionTimeMs < 0) {
            throw new IllegalArgumentException("Execution time cannot be negative: " + executionTimeMs);
        }

        // Create defensive copy of the list
        foundBranches = List.copyOf(foundBranches);
    }

    /**
     * Create event for successful query
     *
     * @param userLatitude    user's latitude
     * @param userLongitude   user's longitude
     * @param radiusKm        search radius
     * @param maxResults      maximum results requested
     * @param foundBranches   branches found in query
     * @param executionTimeMs query execution time
     * @param cacheHit        whether result came from cache
     * @param correlationId   correlation identifier
     * @param sessionId       user session identifier
     * @return domain event
     */
    public static UserProximityQueriedEvent create(double userLatitude, double userLongitude,
            double radiusKm, int maxResults,
            List<BranchId> foundBranches, long executionTimeMs,
            boolean cacheHit, String correlationId, String sessionId) {
        return new UserProximityQueriedEvent(
                userLatitude, userLongitude, radiusKm, maxResults,
                foundBranches, executionTimeMs, cacheHit,
                LocalDateTime.now(), correlationId, sessionId);
    }

    /**
     * Get event type identifier
     *
     * @return event type
     */
    public String getEventType() {
        return "USER_PROXIMITY_QUERIED";
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
        return false; // Query events are for analytics, not critical
    }

    /**
     * Get query performance category
     *
     * @return performance category (FAST, NORMAL, SLOW)
     */
    public String getPerformanceCategory() {
        if (executionTimeMs < 50) {
            return "FAST";
        } else if (executionTimeMs < 200) {
            return "NORMAL";
        } else {
            return "SLOW";
        }
    }

    /**
     * Get number of branches found
     *
     * @return branch count
     */
    public int getBranchCount() {
        return foundBranches.size();
    }

    /**
     * Check if query had good coverage (found branches within reasonable count)
     *
     * @return true if good coverage
     */
    public boolean hasGoodCoverage() {
        int branchCount = getBranchCount();
        return branchCount >= 1 && branchCount <= maxResults;
    }

    /**
     * Get cache efficiency indicator
     *
     * @return cache hit rate category
     */
    public String getCacheEfficiency() {
        return cacheHit ? "HIT" : "MISS";
    }
}

