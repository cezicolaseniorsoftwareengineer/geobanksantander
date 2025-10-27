package com.santander.geobank.shared.utils;

import com.santander.geobank.domain.model.GeoPoint;

/**
 * Utility class for calculating distances between geographic points.
 *
 * Implements Haversine formula for accurate distance calculations
 * on Earth's surface considering its curvature.
 */
public final class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private DistanceCalculator() {
        // Utility class - prevent instantiation
    }

    /**
     * Calculate distance between two geographic points using Haversine formula.
     *
     * @param point1 first geographic point
     * @param point2 second geographic point
     * @return distance in kilometers
     */
    public static double calculateDistance(GeoPoint point1, GeoPoint point2) {
        if (point1 == null || point2 == null) {
            throw new IllegalArgumentException("Geographic points cannot be null");
        }

        double lat1Rad = Math.toRadians(point1.latitude());
        double lat2Rad = Math.toRadians(point2.latitude());
        double deltaLatRad = Math.toRadians(point2.latitude() - point1.latitude());
        double deltaLonRad = Math.toRadians(point2.longitude() - point1.longitude());

        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Check if a point is within specified radius of another point.
     *
     * @param center   center point
     * @param target   target point to check
     * @param radiusKm radius in kilometers
     * @return true if target is within radius of center
     */
    public static boolean isWithinRadius(GeoPoint center, GeoPoint target, double radiusKm) {
        if (radiusKm < 0) {
            throw new IllegalArgumentException("Radius cannot be negative");
        }

        return calculateDistance(center, target) <= radiusKm;
    }
}

