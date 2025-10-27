package com.santander.geobank.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Distance - Value Object representing geographical distance
 *
 * Immutable representation of distance with multiple unit conversions.
 * Provides precise calculations with configurable decimal precision.
 * */
public record Distance(
        double kilometers) implements Comparable<Distance> {

    private static final int PRECISION_SCALE = 2;

    /**
     * Constructor with validation for non-negative distances
     */
    public Distance {
        if (kilometers < 0) {
            throw new IllegalArgumentException(
                    String.format("Distance cannot be negative: %f", kilometers));
        }
    }

    /**
     * Get distance in meters
     *
     * @return distance in meters
     */
    public double getMeters() {
        return kilometers * 1000.0;
    }

    /**
     * Get distance in kilometers with precision
     *
     * @return distance in kilometers rounded to 2 decimal places
     */
    public double getKilometers() {
        return BigDecimal.valueOf(kilometers)
                .setScale(PRECISION_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Get distance in miles
     *
     * @return distance in miles
     */
    public double getMiles() {
        return BigDecimal.valueOf(kilometers * 0.621371)
                .setScale(PRECISION_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Create Distance from meters
     *
     * @param meters distance in meters
     * @return Distance instance
     */
    public static Distance fromMeters(double meters) {
        return new Distance(meters / 1000.0);
    }

    /**
     * Create Distance from miles
     *
     * @param miles distance in miles
     * @return Distance instance
     */
    public static Distance fromMiles(double miles) {
        return new Distance(miles * 1.609344);
    }

    /**
     * Zero distance constant
     */
    public static Distance ZERO = new Distance(0.0);

    /**
     * Check if distance is within given threshold
     *
     * @param threshold maximum distance
     * @return true if within threshold
     */
    public boolean isWithin(Distance threshold) {
        Objects.requireNonNull(threshold, "Threshold cannot be null");
        return this.kilometers <= threshold.kilometers;
    }

    /**
     * Add two distances
     *
     * @param other distance to add
     * @return new Distance with sum
     */
    public Distance add(Distance other) {
        Objects.requireNonNull(other, "Distance to add cannot be null");
        return new Distance(this.kilometers + other.kilometers);
    }

    /**
     * Subtract distance
     *
     * @param other distance to subtract
     * @return new Distance with difference
     */
    public Distance subtract(Distance other) {
        Objects.requireNonNull(other, "Distance to subtract cannot be null");
        double result = this.kilometers - other.kilometers;
        return new Distance(Math.max(0, result)); // Ensure non-negative
    }

    @Override
    public int compareTo(Distance other) {
        return Double.compare(this.kilometers, other.kilometers);
    }

    @Override
    public String toString() {
        return String.format("%.2f km", getKilometers());
    }

    /**
     * Format distance for display with unit
     *
     * @param unit unit to display (km, m, mi)
     * @return formatted string
     */
    public String format(String unit) {
        return switch (unit.toLowerCase()) {
            case "m", "meters" -> String.format("%.0f m", getMeters());
            case "mi", "miles" -> String.format("%.2f mi", getMiles());
            default -> toString(); // Default to kilometers
        };
    }
}




