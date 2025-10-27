package com.santander.geobank.application.usecases;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.santander.geobank.domain.model.Branch;
import com.santander.geobank.domain.model.Distance;
import com.santander.geobank.domain.model.GeoPoint;
import com.santander.geobank.domain.ports.BranchRepository;
import com.santander.geobank.domain.ports.CachePort;
import com.santander.geobank.domain.services.BranchDistanceCalculator;

/**
 * Use Case for analyzing branch network density and coverage gaps.
 * Provides strategic insights for branch placement and network optimization.
 *
 * Performance: Leverages spatial indexing and caching for large-scale analysis.
 * Business Value: Identifies underserved areas and network optimization
 * opportunities.
 */
@Service
public class BranchNetworkAnalysisUseCase {

    private final BranchRepository branchRepository;
    private final BranchDistanceCalculator distanceCalculator;
    private final CachePort cachePort;

    private static final String CACHE_KEY_PREFIX = "network_analysis:";
    private static final String DENSITY_CACHE_KEY = CACHE_KEY_PREFIX + "density:";
    private static final String COVERAGE_CACHE_KEY = CACHE_KEY_PREFIX + "coverage:";
    private static final int CACHE_TTL_HOURS = 6;

    public BranchNetworkAnalysisUseCase(
            BranchRepository branchRepository,
            BranchDistanceCalculator distanceCalculator,
            CachePort cachePort) {
        this.branchRepository = branchRepository;
        this.distanceCalculator = distanceCalculator;
        this.cachePort = cachePort;
    }

    /**
     * Analyzes branch network density within specified region.
     * Calculates branches per square kilometer and identifies clustering patterns.
     *
     * @param centerLat region center latitude
     * @param centerLon region center longitude
     * @param radiusKm  analysis radius in kilometers
     * @return network density analysis
     */
    public NetworkDensityAnalysis analyzeDensity(BigDecimal centerLat, BigDecimal centerLon, BigDecimal radiusKm) {
        String cacheKey = DENSITY_CACHE_KEY + centerLat + ":" + centerLon + ":" + radiusKm;

        // Try cache first for performance
        var cached = cachePort.get(cacheKey, NetworkDensityAnalysis.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        GeoPoint center = new GeoPoint(centerLat.doubleValue(), centerLon.doubleValue());
        double radiusKmDouble = radiusKm.doubleValue();

        // Find all branches within radius using domain service
        List<Branch> branchesInRadius = distanceCalculator.findNearestBranches(
                center, branchRepository.findAll(), radiusKmDouble, Integer.MAX_VALUE);

        // Calculate density metrics
        BigDecimal areaKm2 = calculateCircleArea(radiusKm);
        BigDecimal density = branchesInRadius.isEmpty() ? BigDecimal.ZERO
                : new BigDecimal(branchesInRadius.size()).divide(areaKm2, 4, RoundingMode.HALF_UP);

        // Analyze branch type distribution
        Map<String, Long> typeDistribution = branchesInRadius.stream()
                .collect(Collectors.groupingBy(
                        branch -> branch.getType().name(),
                        Collectors.counting()));

        // Calculate network density using domain service
        double clusteringCoefficient = distanceCalculator.calculateNetworkDensity(center, branchesInRadius, 1.0);

        NetworkDensityAnalysis analysis = new NetworkDensityAnalysis(
                center,
                new Distance(radiusKmDouble),
                areaKm2,
                branchesInRadius.size(),
                density,
                typeDistribution,
                BigDecimal.valueOf(clusteringCoefficient),
                categorizeDensity(density));

        // Cache result for performance
        cachePort.put(cacheKey, analysis, CACHE_TTL_HOURS);

        // Publish analysis event for monitoring
        publishNetworkAnalysisEvent("DENSITY_ANALYSIS", center, analysis.totalBranches());

        return analysis;
    }

    /**
     * Identifies coverage gaps in branch network using grid-based analysis.
     * Finds areas with insufficient branch coverage for strategic planning.
     *
     * @param bounds        geographic bounds for analysis
     * @param maxDistanceKm maximum acceptable distance to nearest branch
     * @param gridSizeKm    grid cell size for analysis
     * @return coverage gap analysis
     */
    public CoverageGapAnalysis analyzeCoverageGaps(
            GeographicBounds bounds, BigDecimal maxDistanceKm, BigDecimal gridSizeKm) {

        String cacheKey = COVERAGE_CACHE_KEY + bounds.hashCode() + ":" + maxDistanceKm + ":" + gridSizeKm;

        var cached = cachePort.get(cacheKey, CoverageGapAnalysis.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        List<Branch> allBranches = branchRepository.findAll();
        List<GeoPoint> underservedAreas = findUnderservedAreas(bounds, allBranches, maxDistanceKm, gridSizeKm);

        // Calculate coverage metrics
        BigDecimal totalArea = calculateRectangleArea(bounds);
        BigDecimal underservedArea = new BigDecimal(underservedAreas.size())
                .multiply(gridSizeKm.multiply(gridSizeKm));

        BigDecimal coveragePercentage = totalArea.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : BigDecimal.ONE.subtract(underservedArea.divide(totalArea, 4, RoundingMode.HALF_UP))
                        .multiply(new BigDecimal("100"));

        // Identify priority areas
        List<GeoPoint> priorityAreas = identifyPriorityAreas(underservedAreas, allBranches);

        CoverageGapAnalysis analysis = new CoverageGapAnalysis(
                bounds,
                maxDistanceKm,
                totalArea,
                underservedArea,
                coveragePercentage,
                underservedAreas,
                priorityAreas,
                categorizeCoverage(coveragePercentage));

        // Cache result
        cachePort.put(cacheKey, analysis, CACHE_TTL_HOURS);

        return analysis;
    }

    private List<GeoPoint> findUnderservedAreas(
            GeographicBounds bounds, List<Branch> branches,
            BigDecimal maxDistance, BigDecimal gridSize) {

        List<GeoPoint> underservedAreas = new java.util.ArrayList<>();
        double maxDistanceDouble = maxDistance.doubleValue();
        double gridSizeDouble = gridSize.doubleValue() / 111.0; // Convert km to degrees

        // Grid-based analysis
        double lat = bounds.southWest().latitude();
        while (lat < bounds.northEast().latitude()) {
            double lon = bounds.southWest().longitude();
            while (lon < bounds.northEast().longitude()) {
                GeoPoint gridPoint = new GeoPoint(lat, lon);

                // Find nearest branch using domain service
                List<Branch> nearestBranches = distanceCalculator.findNearestBranches(
                        gridPoint, branches, maxDistanceDouble, 1);

                // If no branches found within max distance, this is underserved
                if (nearestBranches.isEmpty()) {
                    underservedAreas.add(gridPoint);
                }

                lon += gridSizeDouble;
            }
            lat += gridSizeDouble;
        }

        return underservedAreas;
    }

    private List<GeoPoint> identifyPriorityAreas(List<GeoPoint> underservedAreas, List<Branch> branches) {
        // Priority areas are those farthest from existing branches
        return underservedAreas.stream()
                .sorted((p1, p2) -> {
                    double dist1 = findNearestBranchDistance(p1, branches);
                    double dist2 = findNearestBranchDistance(p2, branches);
                    return Double.compare(dist2, dist1); // Descending order
                })
                .limit(10) // Top 10 priority areas
                .toList();
    }

    private double findNearestBranchDistance(GeoPoint point, List<Branch> branches) {
        return branches.stream()
                .mapToDouble(branch -> branch.getLocation().distanceTo(point).getKilometers())
                .min()
                .orElse(999.99);
    }

    private BigDecimal calculateCircleArea(BigDecimal radiusKm) {
        // Ï€ * rÂ²
        BigDecimal pi = new BigDecimal("3.14159265359");
        return pi.multiply(radiusKm.multiply(radiusKm));
    }

    private BigDecimal calculateRectangleArea(GeographicBounds bounds) {
        // Approximate area calculation for geographic bounds
        double latDiff = bounds.northEast().latitude() - bounds.southWest().latitude();
        double lonDiff = bounds.northEast().longitude() - bounds.southWest().longitude();

        // Convert degrees to kilometers (approximate)
        BigDecimal latKm = BigDecimal.valueOf(latDiff * 111.0); // 1 degree â‰ˆ 111 km
        BigDecimal lonKm = BigDecimal.valueOf(lonDiff * 111.0 *
                Math.cos(Math.toRadians(bounds.center().latitude())));

        return latKm.multiply(lonKm);
    }

    private String categorizeDensity(BigDecimal density) {
        if (density.compareTo(new BigDecimal("2.0")) >= 0) {
            return "HIGH_DENSITY";
        } else if (density.compareTo(new BigDecimal("0.5")) >= 0) {
            return "MEDIUM_DENSITY";
        } else if (density.compareTo(new BigDecimal("0.1")) >= 0) {
            return "LOW_DENSITY";
        } else {
            return "VERY_LOW_DENSITY";
        }
    }

    private String categorizeCoverage(BigDecimal coveragePercentage) {
        if (coveragePercentage.compareTo(new BigDecimal("90")) >= 0) {
            return "EXCELLENT_COVERAGE";
        } else if (coveragePercentage.compareTo(new BigDecimal("75")) >= 0) {
            return "GOOD_COVERAGE";
        } else if (coveragePercentage.compareTo(new BigDecimal("50")) >= 0) {
            return "MODERATE_COVERAGE";
        } else {
            return "POOR_COVERAGE";
        }
    }

    private void publishNetworkAnalysisEvent(String analysisType, GeoPoint location, int resultCount) {
        // Implementation placeholder - logs analysis metrics
        System.out.printf("Analysis: %s at location %s found %d results%n",
                analysisType, location, resultCount);
    } // Value objects for analysis results

    public record NetworkDensityAnalysis(
            GeoPoint center,
            Distance radius,
            BigDecimal areaKm2,
            int totalBranches,
            BigDecimal branchesPerKm2,
            Map<String, Long> branchTypeDistribution,
            BigDecimal clusteringCoefficient,
            String densityCategory) {
    }

    public record CoverageGapAnalysis(
            GeographicBounds bounds,
            BigDecimal maxAcceptableDistance,
            BigDecimal totalAreaKm2,
            BigDecimal underservedAreaKm2,
            BigDecimal coveragePercentage,
            List<GeoPoint> underservedAreas,
            List<GeoPoint> priorityExpansionAreas,
            String coverageCategory) {
    }

    public record GeographicBounds(
            GeoPoint southWest,
            GeoPoint northEast) {
        public GeoPoint center() {
            double centerLat = (southWest.latitude() + northEast.latitude()) / 2.0;
            double centerLon = (southWest.longitude() + northEast.longitude()) / 2.0;
            return new GeoPoint(centerLat, centerLon);
        }
    }
}

