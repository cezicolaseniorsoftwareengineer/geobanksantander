package com.santander.geobank.domain.model;

import java.util.Objects;

/**
 * GeoPoint - Value Object representing geographical coordinates
 *
 * Immutable representation of latitude and longitude with validation.
 * Used for branch locations and user positions in distance calculations.
 * */
public record GeoPoint(
        double latitude,
        double longitude) {

    /**
     * Constructor with validation for geographical boundaries
     */
    public GeoPoint {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException(
                    String.format("Invalid latitude: %f. Must be between -90.0 and 90.0", latitude));
        }

        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException(
                    String.format("Invalid longitude: %f. Must be between -180.0 and 180.0", longitude));
        }
    }

    /**
     * Calculate distance to another GeoPoint using Haversine formula
     *
     * @param other target GeoPoint
     * @return Distance object with calculated value
     */
    public Distance distanceTo(GeoPoint other) {
        Objects.requireNonNull(other, "Target GeoPoint cannot be null");

        double earthRadiusKm = 6371.0;

        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);

        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) *
                        Math.cos(lat1Rad) * Math.cos(lat2Rad);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distanceKm = earthRadiusKm * c;

        return new Distance(distanceKm);
    }

    /**
     * Factory method for creating GeoPoint from string coordinates
     *
     * @param latitude  latitude as string
     * @param longitude longitude as string
     * @return validated GeoPoint instance
     */
    public static GeoPoint of(String latitude, String longitude) {
        try {
            return new GeoPoint(
                    Double.parseDouble(latitude),
                    Double.parseDouble(longitude));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid coordinate format: lat=%s, lon=%s", latitude, longitude),
                    e);
        }
    }

    /**
     * Check if this point is within given radius of another point
     *
     * @param other    target point
     * @param radiusKm radius in kilometers
     * @return true if within radius
     */
    public boolean isWithinRadius(GeoPoint other, double radiusKm) {
        return distanceTo(other).getKilometers() <= radiusKm;
    }

    @Override
    public String toString() {
        return String.format("GeoPoint[lat=%.6f, lon=%.6f]", latitude, longitude);
    }
}

