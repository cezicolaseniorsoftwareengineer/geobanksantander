package com.santander.geobank.domain.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

import com.santander.geobank.domain.model.Branch;
import com.santander.geobank.domain.model.BranchType;
import com.santander.geobank.domain.model.Distance;
import com.santander.geobank.domain.model.GeoPoint;

/**
 * BranchDistanceCalculator - Domain Service for geographical calculations
 *
 * Provides advanced distance calculations and branch proximity analysis.
 * Implements multiple algorithms and ranking strategies for optimal branch
 * selection.
 * */
public class BranchDistanceCalculator {

    private static final double DEFAULT_SEARCH_RADIUS_KM = 50.0;

    /**
     * Calculate distances from user location to all branches
     *
     * @param userLocation user's geographical position
     * @param branches     list of branches to calculate distances to
     * @return map of branch ID to distance, sorted by distance
     */
    public Map<String, Distance> calculateDistances(GeoPoint userLocation, List<Branch> branches) {
        Objects.requireNonNull(userLocation, "User location cannot be null");
        Objects.requireNonNull(branches, "Branches list cannot be null");

        return branches.parallelStream()
                .filter(Branch::isOperational)
                .collect(Collectors.toMap(
                        branch -> branch.getId().toString(),
                        branch -> calculateDistance(userLocation, branch.getLocation()),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
    }

    /**
     * Find nearest branches within specified radius
     *
     * @param userLocation user's geographical position
     * @param branches     list of branches to search
     * @param radiusKm     search radius in kilometers
     * @param maxResults   maximum number of results to return
     * @return list of branches sorted by distance
     */
    public List<Branch> findNearestBranches(GeoPoint userLocation, List<Branch> branches,
            double radiusKm, int maxResults) {
        Objects.requireNonNull(userLocation, "User location cannot be null");
        Objects.requireNonNull(branches, "Branches list cannot be null");

        if (radiusKm <= 0) {
            throw new IllegalArgumentException("Radius must be positive");
        }

        if (maxResults <= 0) {
            throw new IllegalArgumentException("Max results must be positive");
        }

        return branches.parallelStream()
                .filter(Branch::isOperational)
                .filter(branch -> branch.isWithinRadius(userLocation, radiusKm))
                .sorted((b1, b2) -> {
                    Distance d1 = calculateDistance(userLocation, b1.getLocation());
                    Distance d2 = calculateDistance(userLocation, b2.getLocation());

                    // Primary sort: distance
                    int distanceComparison = d1.compareTo(d2);
                    if (distanceComparison != 0) {
                        return distanceComparison;
                    }

                    // Secondary sort: branch priority (type-based)
                    return Integer.compare(b2.getPriorityScore(), b1.getPriorityScore());
                })
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * Find nearest branches with default radius
     *
     * @param userLocation user's geographical position
     * @param branches     list of branches to search
     * @param maxResults   maximum number of results
     * @return list of nearest branches
     */
    public List<Branch> findNearestBranches(GeoPoint userLocation, List<Branch> branches, int maxResults) {
        return findNearestBranches(userLocation, branches, DEFAULT_SEARCH_RADIUS_KM, maxResults);
    }

    /**
     * Find branches by service type within radius
     *
     * @param userLocation user's geographical position
     * @param branches     list of branches to search
     * @param serviceType  required service type
     * @param radiusKm     search radius in kilometers
     * @param maxResults   maximum number of results
     * @return list of branches supporting the service
     */
    public List<Branch> findBranchesByService(GeoPoint userLocation, List<Branch> branches,
            String serviceType, double radiusKm, int maxResults) {
        Objects.requireNonNull(serviceType, "Service type cannot be null");

        return findNearestBranches(userLocation, branches, radiusKm, Integer.MAX_VALUE)
                .stream()
                .filter(branch -> branch.supportsService(serviceType))
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * Find branches by type within radius
     *
     * @param userLocation user's geographical position
     * @param branches     list of branches to search
     * @param branchTypes  desired branch types
     * @param radiusKm     search radius in kilometers
     * @param maxResults   maximum number of results
     * @return list of branches of specified types
     */
    public List<Branch> findBranchesByType(GeoPoint userLocation, List<Branch> branches,
            Set<BranchType> branchTypes, double radiusKm, int maxResults) {
        Objects.requireNonNull(branchTypes, "Branch types cannot be null");

        if (branchTypes.isEmpty()) {
            return Collections.emptyList();
        }

        return findNearestBranches(userLocation, branches, radiusKm, Integer.MAX_VALUE)
                .stream()
                .filter(branch -> branchTypes.contains(branch.getType()))
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * Calculate network density for a given area
     *
     * @param centerLocation center of analysis area
     * @param branches       list of branches to analyze
     * @param radiusKm       analysis radius in kilometers
     * @return network density score (branches per square km)
     */
    public double calculateNetworkDensity(GeoPoint centerLocation, List<Branch> branches, double radiusKm) {
        Objects.requireNonNull(centerLocation, "Center location cannot be null");
        Objects.requireNonNull(branches, "Branches list cannot be null");

        if (radiusKm <= 0) {
            throw new IllegalArgumentException("Radius must be positive");
        }

        long branchesInArea = branches.stream()
                .filter(Branch::isOperational)
                .filter(branch -> branch.isWithinRadius(centerLocation, radiusKm))
                .count();

        double areaKm2 = Math.PI * radiusKm * radiusKm;
        return branchesInArea / areaKm2;
    }

    /**
     * Find coverage gaps in branch network
     *
     * @param branches                list of branches to analyze
     * @param minimumCoverageRadiusKm minimum coverage radius per branch
     * @return list of areas with insufficient coverage
     */
    public List<GeoPoint> findCoverageGaps(List<Branch> branches, double minimumCoverageRadiusKm) {
        Objects.requireNonNull(branches, "Branches list cannot be null");

        if (minimumCoverageRadiusKm <= 0) {
            throw new IllegalArgumentException("Minimum coverage radius must be positive");
        }

        // Simplified gap analysis - in production would use more sophisticated spatial
        // algorithms
        List<GeoPoint> operationalLocations = branches.stream()
                .filter(Branch::isOperational)
                .map(Branch::getLocation)
                .collect(Collectors.toList());

        List<GeoPoint> potentialGaps = new ArrayList<>();

        // Generate grid points for analysis
        if (!operationalLocations.isEmpty()) {
            double minLat = operationalLocations.stream().mapToDouble(GeoPoint::latitude).min().orElse(0);
            double maxLat = operationalLocations.stream().mapToDouble(GeoPoint::latitude).max().orElse(0);
            double minLon = operationalLocations.stream().mapToDouble(GeoPoint::longitude).min().orElse(0);
            double maxLon = operationalLocations.stream().mapToDouble(GeoPoint::longitude).max().orElse(0);

            double stepSize = minimumCoverageRadiusKm / 111.0; // Approximate degrees per km

            for (double lat = minLat; lat <= maxLat; lat += stepSize) {
                for (double lon = minLon; lon <= maxLon; lon += stepSize) {
                    GeoPoint testPoint = new GeoPoint(lat, lon);

                    boolean hasCoverage = operationalLocations.stream()
                            .anyMatch(branchLocation -> branchLocation.distanceTo(testPoint)
                                    .getKilometers() <= minimumCoverageRadiusKm);

                    if (!hasCoverage) {
                        potentialGaps.add(testPoint);
                    }
                }
            }
        }

        return potentialGaps;
    }

    /**
     * Calculate precise distance using Haversine formula
     *
     * @param from source location
     * @param to   destination location
     * @return calculated distance
     */
    private Distance calculateDistance(GeoPoint from, GeoPoint to) {
        return from.distanceTo(to);
    }

    /**
     * Calculate bearing from one point to another
     *
     * @param from source location
     * @param to   destination location
     * @return bearing in degrees (0-360)
     */
    public double calculateBearing(GeoPoint from, GeoPoint to) {
        Objects.requireNonNull(from, "Source location cannot be null");
        Objects.requireNonNull(to, "Destination location cannot be null");

        double lat1Rad = Math.toRadians(from.latitude());
        double lat2Rad = Math.toRadians(to.latitude());
        double deltaLonRad = Math.toRadians(to.longitude() - from.longitude());

        double y = Math.sin(deltaLonRad) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad);

        double bearingRad = Math.atan2(y, x);
        double bearingDeg = Math.toDegrees(bearingRad);

        return (bearingDeg + 360) % 360;
    }

    /**
     * Get search statistics for analysis
     *
     * @param userLocation user's geographical position
     * @param branches     list of branches analyzed
     * @param radiusKm     search radius used
     * @return search statistics
     */
    public SearchStatistics getSearchStatistics(GeoPoint userLocation, List<Branch> branches, double radiusKm) {
        Objects.requireNonNull(userLocation, "User location cannot be null");
        Objects.requireNonNull(branches, "Branches list cannot be null");

        List<Branch> branchesInRadius = findNearestBranches(userLocation, branches, radiusKm, Integer.MAX_VALUE);

        Map<BranchType, Long> typeDistribution = branchesInRadius.stream()
                .collect(Collectors.groupingBy(Branch::getType, Collectors.counting()));

        OptionalDouble avgDistance = branchesInRadius.stream()
                .mapToDouble(branch -> calculateDistance(userLocation, branch.getLocation()).getKilometers())
                .average();

        double networkDensity = calculateNetworkDensity(userLocation, branches, radiusKm);

        return new SearchStatistics(
                branchesInRadius.size(),
                avgDistance.orElse(0.0),
                typeDistribution,
                networkDensity);
    }

    /**
     * Search statistics record
     */
    public record SearchStatistics(
            int branchesFound,
            double averageDistance,
            Map<BranchType, Long> typeDistribution,
            double networkDensity) {
    }
}

