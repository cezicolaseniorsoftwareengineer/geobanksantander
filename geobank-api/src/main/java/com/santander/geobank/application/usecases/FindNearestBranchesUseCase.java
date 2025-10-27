package com.santander.geobank.application.usecases;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.santander.geobank.domain.events.UserProximityQueriedEvent;
import com.santander.geobank.domain.model.Branch;
import com.santander.geobank.domain.model.BranchId;
import com.santander.geobank.domain.model.BranchType;
import com.santander.geobank.domain.model.Distance;
import com.santander.geobank.domain.model.GeoPoint;
import com.santander.geobank.domain.ports.BranchRepository;
import com.santander.geobank.domain.ports.CachePort;
import com.santander.geobank.domain.ports.DomainEventPublisher;
import com.santander.geobank.domain.services.BranchDistanceCalculator;

/**
 * FindNearestBranchesUseCase - Use Case for finding branches near user location
 *
 * Implements intelligent caching and performance optimization for branch
 * proximity queries.
 * Supports filtering by type, service requirements, and custom search radius.
 * */
public class FindNearestBranchesUseCase {

    private final BranchRepository branchRepository;
    private final BranchDistanceCalculator distanceCalculator;
    private final CachePort cachePort;
    private final DomainEventPublisher eventPublisher;

    private static final double DEFAULT_RADIUS_KM = 10.0;
    private static final int DEFAULT_MAX_RESULTS = 10;
    private static final int CACHE_TTL_SECONDS = 300; // 5 minutes

    public FindNearestBranchesUseCase(BranchRepository branchRepository,
            BranchDistanceCalculator distanceCalculator,
            CachePort cachePort,
            DomainEventPublisher eventPublisher) {
        this.branchRepository = Objects.requireNonNull(branchRepository, "Branch repository cannot be null");
        this.distanceCalculator = Objects.requireNonNull(distanceCalculator, "Distance calculator cannot be null");
        this.cachePort = Objects.requireNonNull(cachePort, "Cache port cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "Event publisher cannot be null");
    }

    /**
     * Execute branch proximity search
     *
     * @param query search query with location and filters
     * @return search results with distances and metadata
     */
    public FindNearestBranchesResponse execute(FindNearestBranchesQuery query) {
        Objects.requireNonNull(query, "Search query cannot be null");

        Instant startTime = Instant.now();
        String correlationId = UUID.randomUUID().toString();

        try {
            // Validate query
            validateQuery(query);

            // Try cache first
            String cacheKey = buildCacheKey(query);
            Optional<FindNearestBranchesResponse> cachedResult = cachePort.get(cacheKey,
                    FindNearestBranchesResponse.class);

            if (cachedResult.isPresent()) {
                publishQueryEvent(query, cachedResult.get().branches().size(),
                        Duration.between(startTime, Instant.now()).toMillis(),
                        true, correlationId);
                return cachedResult.get();
            }

            // Execute search
            FindNearestBranchesResponse response = performSearch(query, correlationId);

            // Cache result
            cachePort.put(cacheKey, response, CACHE_TTL_SECONDS);

            // Publish event
            long executionTimeMs = Duration.between(startTime, Instant.now()).toMillis();
            publishQueryEvent(query, response.branches().size(), executionTimeMs, false, correlationId);

            return response;

        } catch (Exception e) {
            throw new BranchSearchException("Failed to search for branches: " + e.getMessage(), e);
        }
    }

    /**
     * Perform the actual search without cache
     */
    private FindNearestBranchesResponse performSearch(FindNearestBranchesQuery query, String correlationId) {
        GeoPoint userLocation = query.userLocation();
        double radiusKm = query.radiusKm().orElse(DEFAULT_RADIUS_KM);
        int maxResults = query.maxResults().orElse(DEFAULT_MAX_RESULTS);

        // Get branches within radius
        List<Branch> candidateBranches;

        if (query.branchTypes().isPresent() && !query.branchTypes().get().isEmpty()) {
            candidateBranches = branchRepository.findWithinRadiusByTypes(
                    userLocation, radiusKm, query.branchTypes().get());
        } else {
            candidateBranches = branchRepository.findWithinRadius(userLocation, radiusKm);
        }

        // Filter by service if specified
        if (query.serviceType().isPresent()) {
            candidateBranches = candidateBranches.stream()
                    .filter(branch -> branch.supportsService(query.serviceType().get()))
                    .collect(Collectors.toList());
        }

        // Calculate distances and sort
        List<Branch> nearestBranches = distanceCalculator.findNearestBranches(
                userLocation, candidateBranches, radiusKm, maxResults);

        // Build response
        List<BranchWithDistance> branchesWithDistance = nearestBranches.stream()
                .map(branch -> {
                    Distance distance = distanceCalculator.calculateDistances(
                            userLocation, List.of(branch)).values().iterator().next();

                    return new BranchWithDistance(
                            branch.getId().toString(),
                            branch.getName(),
                            branch.getLocation().latitude(),
                            branch.getLocation().longitude(),
                            branch.getType().name(),
                            branch.getAddress(),
                            branch.getContactPhone().orElse(null),
                            distance.getKilometers(),
                            branch.isOperational());
                })
                .collect(Collectors.toList());

        // Calculate search statistics
        var searchStats = distanceCalculator.getSearchStatistics(userLocation, candidateBranches, radiusKm);

        return new FindNearestBranchesResponse(
                branchesWithDistance,
                query.userLocation().latitude(),
                query.userLocation().longitude(),
                radiusKm,
                maxResults,
                searchStats.branchesFound(),
                searchStats.averageDistance(),
                searchStats.networkDensity(),
                correlationId);
    }

    /**
     * Validate search query
     */
    private void validateQuery(FindNearestBranchesQuery query) {
        if (query.userLocation() == null) {
            throw new BranchSearchException("User location is required");
        }

        if (query.radiusKm().isPresent() && query.radiusKm().get() <= 0) {
            throw new BranchSearchException("Search radius must be positive");
        }

        if (query.radiusKm().isPresent() && query.radiusKm().get() > 100) {
            throw new BranchSearchException("Search radius cannot exceed 100 km");
        }

        if (query.maxResults().isPresent() && query.maxResults().get() <= 0) {
            throw new BranchSearchException("Max results must be positive");
        }

        if (query.maxResults().isPresent() && query.maxResults().get() > 50) {
            throw new BranchSearchException("Max results cannot exceed 50");
        }
    }

    /**
     * Build cache key for query
     */
    private String buildCacheKey(FindNearestBranchesQuery query) {
        StringBuilder keyBuilder = new StringBuilder("nearest:");
        keyBuilder.append(String.format("%.6f,%.6f",
                query.userLocation().latitude(), query.userLocation().longitude()));
        keyBuilder.append(":r").append(query.radiusKm().orElse(DEFAULT_RADIUS_KM));
        keyBuilder.append(":m").append(query.maxResults().orElse(DEFAULT_MAX_RESULTS));

        if (query.branchTypes().isPresent()) {
            String types = query.branchTypes().get().stream()
                    .map(Enum::name)
                    .sorted()
                    .collect(Collectors.joining(","));
            keyBuilder.append(":t").append(types);
        }

        if (query.serviceType().isPresent()) {
            keyBuilder.append(":s").append(query.serviceType().get());
        }

        return keyBuilder.toString();
    }

    /**
     * Publish query event for analytics
     */
    private void publishQueryEvent(FindNearestBranchesQuery query, int branchCount,
            long executionTimeMs, boolean cacheHit, String correlationId) {
        try {
            // Create mock branch IDs based on branchCount
            List<BranchId> foundBranchIds = new ArrayList<>();
            for (int i = 0; i < branchCount && i < 10; i++) {
                foundBranchIds.add(new BranchId("branch-" + i));
            }

            UserProximityQueriedEvent event = UserProximityQueriedEvent.create(
                    query.userLocation().latitude(),
                    query.userLocation().longitude(),
                    query.radiusKm().orElse(DEFAULT_RADIUS_KM),
                    query.maxResults().orElse(DEFAULT_MAX_RESULTS),
                    foundBranchIds,
                    executionTimeMs,
                    cacheHit,
                    correlationId,
                    query.sessionId().orElse("anonymous"));

            eventPublisher.publish(event);
        } catch (Exception e) {
            // Log event publishing failure but don't fail the search
            System.err.println("Failed to publish query event: " + e.getMessage());
        }
    }

    /**
     * Search query record
     */
    public record FindNearestBranchesQuery(
            GeoPoint userLocation,
            Optional<Double> radiusKm,
            Optional<Integer> maxResults,
            Optional<Set<BranchType>> branchTypes,
            Optional<String> serviceType,
            Optional<String> sessionId) {
        public FindNearestBranchesQuery {
            Objects.requireNonNull(userLocation, "User location cannot be null");
        }

        /**
         * Create simple query with just location
         */
        public static FindNearestBranchesQuery create(double latitude, double longitude) {
            return new FindNearestBranchesQuery(
                    new GeoPoint(latitude, longitude),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty());
        }

        /**
         * Create query with location and radius
         */
        public static FindNearestBranchesQuery create(double latitude, double longitude,
                double radiusKm, int maxResults) {
            return new FindNearestBranchesQuery(
                    new GeoPoint(latitude, longitude),
                    Optional.of(radiusKm),
                    Optional.of(maxResults),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty());
        }
    }

    /**
     * Branch with distance information
     */
    public record BranchWithDistance(
            String id,
            String name,
            double latitude,
            double longitude,
            String type,
            String address,
            String contactPhone,
            double distanceKm,
            boolean isOperational) {
    }

    /**
     * Search response record
     */
    public record FindNearestBranchesResponse(
            List<BranchWithDistance> branches,
            double userLatitude,
            double userLongitude,
            double searchRadiusKm,
            int maxResults,
            int totalBranchesFound,
            double averageDistanceKm,
            double networkDensity,
            String correlationId) {
    }

    /**
     * Branch search exception
     */
    public static class BranchSearchException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public BranchSearchException(String message) {
            super(message);
        }

        public BranchSearchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

