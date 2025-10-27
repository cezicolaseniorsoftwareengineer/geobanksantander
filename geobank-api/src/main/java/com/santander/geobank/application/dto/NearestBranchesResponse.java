package com.santander.geobank.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for nearest branches search response containing ordered results
 * with distance calculations and metadata.
 */
public record NearestBranchesResponse(
        BigDecimal searchLatitude,
        BigDecimal searchLongitude,
        Integer totalFound,
        BigDecimal maxDistanceKm,
        List<BranchDistanceInfo> branches,
        String message) {

    /**
     * Individual branch with calculated distance information.
     */
    public record BranchDistanceInfo(
            String id,
            String name,
            BigDecimal latitude,
            BigDecimal longitude,
            String type,
            String status,
            String address,
            String operatingHours,
            BigDecimal distanceKm,
            String distanceCategory) {

        /**
         * Creates distance info with calculated proximity category.
         *
         * @param id             branch ID
         * @param name           branch name
         * @param latitude       branch latitude
         * @param longitude      branch longitude
         * @param type           branch type
         * @param status         branch status
         * @param address        branch address
         * @param operatingHours operating hours
         * @param distanceKm     calculated distance
         * @return branch distance info with category
         */
        public static BranchDistanceInfo create(
                String id, String name, BigDecimal latitude, BigDecimal longitude,
                String type, String status, String address, String operatingHours,
                BigDecimal distanceKm) {
            return new BranchDistanceInfo(
                    id, name, latitude, longitude, type, status,
                    address, operatingHours, distanceKm,
                    categorizeDistance(distanceKm));
        }

        private static String categorizeDistance(BigDecimal distance) {
            if (distance.compareTo(new BigDecimal("0.5")) <= 0) {
                return "VERY_CLOSE";
            } else if (distance.compareTo(new BigDecimal("2.0")) <= 0) {
                return "CLOSE";
            } else if (distance.compareTo(new BigDecimal("5.0")) <= 0) {
                return "NEARBY";
            } else {
                return "DISTANT";
            }
        }
    }

    /**
     * Creates successful search response.
     *
     * @param searchLatitude  search center latitude
     * @param searchLongitude search center longitude
     * @param maxDistanceKm   search radius
     * @param branches        found branches with distances
     * @return success response
     */
    public static NearestBranchesResponse success(
            BigDecimal searchLatitude, BigDecimal searchLongitude,
            BigDecimal maxDistanceKm, List<BranchDistanceInfo> branches) {
        return new NearestBranchesResponse(
                searchLatitude, searchLongitude, branches.size(),
                maxDistanceKm, branches,
                String.format("Found %d branches within %.1f km", branches.size(), maxDistanceKm));
    }
}

