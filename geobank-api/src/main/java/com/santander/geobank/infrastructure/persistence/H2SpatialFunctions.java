package com.santander.geobank.infrastructure.persistence;

/**
 * H2 Spatial Functions for GeoBank.
 *
 * Custom spatial functions to extend H2 database with geospatial capabilities.
 * Implements Haversine formula for distance calculations.
 *
 * Note: For production use, consider PostgreSQL with PostGIS extension
 * for full spatial database capabilities.
 */
public class H2SpatialFunctions {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculate distance between two points using Haversine formula.
     *
     * @param lat1 latitude of first point in decimal degrees
     * @param lon1 longitude of first point in decimal degrees
     * @param lat2 latitude of second point in decimal degrees
     * @param lon2 longitude of second point in decimal degrees
     * @return distance in kilometers
     */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == lat2 && lon1 == lon2) {
            return 0.0;
        }

        // Convert degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Haversine formula
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calculate distance between two points in meters.
     *
     * @param lat1 latitude of first point in decimal degrees
     * @param lon1 longitude of first point in decimal degrees
     * @param lat2 latitude of second point in decimal degrees
     * @param lon2 longitude of second point in decimal degrees
     * @return distance in meters
     */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        return distanceKm(lat1, lon1, lat2, lon2) * 1000.0;
    }

    /**
     * Check if a point is within a circular area (bounding circle).
     *
     * @param pointLat  latitude of the point to check
     * @param pointLon  longitude of the point to check
     * @param centerLat latitude of circle center
     * @param centerLon longitude of circle center
     * @param radiusKm  radius in kilometers
     * @return true if point is within the circle
     */
    public static boolean withinRadius(double pointLat, double pointLon,
            double centerLat, double centerLon,
            double radiusKm) {
        double distance = distanceKm(pointLat, pointLon, centerLat, centerLon);
        return distance <= radiusKm;
    }

    /**
     * Calculate bearing (direction) from one point to another.
     *
     * @param lat1 latitude of starting point in decimal degrees
     * @param lon1 longitude of starting point in decimal degrees
     * @param lat2 latitude of ending point in decimal degrees
     * @param lon2 longitude of ending point in decimal degrees
     * @return bearing in degrees (0-360, where 0/360 is North)
     */
    public static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLonRad = Math.toRadians(lon2 - lon1);

        double y = Math.sin(deltaLonRad) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad);

        double bearingRad = Math.atan2(y, x);
        double bearingDeg = Math.toDegrees(bearingRad);

        // Normalize to 0-360 degrees
        return (bearingDeg + 360.0) % 360.0;
    }

    /**
     * Create a simple bounding box around a center point.
     * Returns array: [minLat, minLon, maxLat, maxLon]
     *
     * @param centerLat center latitude
     * @param centerLon center longitude
     * @param radiusKm  radius in kilometers
     * @return bounding box coordinates as array
     */
    public static double[] boundingBox(double centerLat, double centerLon, double radiusKm) {
        // Approximate degrees per kilometer (varies by latitude)
        double latDegreePerKm = 1.0 / 111.0;
        double lonDegreePerKm = 1.0 / (111.0 * Math.cos(Math.toRadians(centerLat)));

        double latDelta = radiusKm * latDegreePerKm;
        double lonDelta = radiusKm * lonDegreePerKm;

        return new double[] {
                centerLat - latDelta, // minLat
                centerLon - lonDelta, // minLon
                centerLat + latDelta, // maxLat
                centerLon + lonDelta // maxLon
        };
    }

    /**
     * Check if coordinates are valid (within valid ranges).
     *
     * @param latitude  latitude to validate
     * @param longitude longitude to validate
     * @return true if coordinates are valid
     */
    public static boolean isValidCoordinate(double latitude, double longitude) {
        return latitude >= -90.0 && latitude <= 90.0 &&
                longitude >= -180.0 && longitude <= 180.0;
    }
}

