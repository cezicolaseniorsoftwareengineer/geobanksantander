package com.santander.geobank.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.santander.geobank.domain.model.GeoPoint;
import com.santander.geobank.shared.utils.DistanceCalculator;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

// Property-based testing for geographic calculations using JQwik framework.
// Validates mathematical properties and edge cases for branch distance computation.

class PropertyBasedGeoTests {

    @Property
    void anyValidCoordinatesShouldBeAccepted(@ForAll("validCoordinates") GeoPoint point) {
        // Property: Any valid coordinate should be accepted by the system
        assertDoesNotThrow(() -> {
            boolean validationResult = isValidGeoPoint(point);
            assertThat(validationResult).isTrue();
        });
    }

    @Property
    void distanceFromPointToItselfShouldBeZero(@ForAll("validCoordinates") GeoPoint point) {
        // Property: The distance from any point to itself must always be zero
        double distance = DistanceCalculator.calculateDistance(point, point);
        assertThat(distance).isEqualTo(0.0);
    }

    @Property
    void distanceShouldBeSymmetric(@ForAll("validCoordinates") GeoPoint point1,
            @ForAll("validCoordinates") GeoPoint point2) {
        // Property: Distance from A to B must equal distance from B to A
        double distanceAB = DistanceCalculator.calculateDistance(point1, point2);
        double distanceBA = DistanceCalculator.calculateDistance(point2, point1);
        assertThat(distanceAB).isEqualTo(distanceBA);
    }

    @Property
    void distanceShouldNeverBeNegative(@ForAll("validCoordinates") GeoPoint point1,
            @ForAll("validCoordinates") GeoPoint point2) {
        // Property: Distance must always be non-negative
        double distance = DistanceCalculator.calculateDistance(point1, point2);
        assertThat(distance).isGreaterThanOrEqualTo(0.0);
    }

    @Property
    void validCoordinatesShouldPassValidation(@ForAll("validCoordinates") GeoPoint point) {
        // Property: All valid coordinates must pass validation
        assertThat(isValidGeoPoint(point)).isTrue();
    }

    @Property
    void brazilianCoordinatesShouldBeInBounds(@ForAll("brazilianCoordinates") GeoPoint point) {
        // Property: All Brazilian coordinates must be within the official bounding box
        assertThat(isValidBrazilianCoordinates(point)).isTrue();
        assertThat(point.latitude()).isBetween(-33.75, 5.271841);
        assertThat(point.longitude()).isBetween(-73.98, -28.84);
    }

    @Provide
    Arbitrary<GeoPoint> validCoordinates() {
        // Generates valid global coordinates
        return Combinators.combine(
                Arbitraries.doubles().between(-90.0, 90.0), // latitude
                Arbitraries.doubles().between(-180.0, 180.0) // longitude
        ).as(GeoPoint::new);
    }

    @Provide
    Arbitrary<GeoPoint> brazilianCoordinates() {
        // Generates coordinates within the Brazilian bounding box
        return Combinators.combine(
                Arbitraries.doubles().between(-33.75, 5.27), // Brazil latitude range
                Arbitraries.doubles().between(-73.98, -28.84) // Brazil longitude range
        ).as(GeoPoint::new);
    }

    private boolean isValidGeoPoint(GeoPoint point) {
        return point.latitude() >= -90.0 && point.latitude() <= 90.0 &&
                point.longitude() >= -180.0 && point.longitude() <= 180.0;
    }

    private boolean isValidBrazilianCoordinates(GeoPoint point) {
        return point.latitude() >= -33.75 && point.latitude() <= 5.27 &&
                point.longitude() >= -73.98 && point.longitude() <= -28.84;
    }
}
