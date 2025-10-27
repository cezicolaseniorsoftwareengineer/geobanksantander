package com.santander.geobank.domain.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.santander.geobank.domain.model.Branch;
import com.santander.geobank.domain.model.BranchId;
import com.santander.geobank.domain.model.BranchStatus;
import com.santander.geobank.domain.model.BranchType;
import com.santander.geobank.domain.model.GeoPoint;

/**
 * Unit tests for BranchBusinessRules domain service.
 * Tests pure business logic without infrastructure dependencies.
 *
 * Testing Strategy:
 * - Unit tests: fast, isolated, no external dependencies
 * - Parameterized tests: multiple scenarios with data-driven approach
 * - Business rule validation: all domain invariants verified
 * - Edge cases: boundary conditions and error scenarios
 *
 * @author Quality Assurance Team
 * @since 1.0.0
 */
@DisplayName("Branch Business Rules Tests")
class BranchBusinessRulesTest {

    private BranchBusinessRules businessRules;
    private List<Branch> existingBranches;

    @BeforeEach
    void setUp() {
        businessRules = new BranchBusinessRules();
        existingBranches = new ArrayList<>();
    }

    @Test
    @DisplayName("Should validate new branch registration successfully")
    void shouldValidateNewBranchRegistration() {
        // Given
        Branch newBranch = createBranch(-23.550520, -46.633308, BranchType.TRADITIONAL);
        Branch existing = createBranch(-23.560000, -46.640000, BranchType.TRADITIONAL);
        existingBranches.add(existing);

        // When
        BranchBusinessRules.ValidationResult result = businessRules.validateBranchRegistration(newBranch,
                existingBranches);

        // Then
        assertTrue(result.isValid());
        assertNull(result.getMessage());
    }

    @Test
    @DisplayName("Should reject branch too close to existing branch")
    void shouldRejectBranchTooClose() {
        // Given: branches less than 500m apart
        Branch newBranch = createBranch(-23.550520, -46.633308, BranchType.TRADITIONAL);
        Branch existing = createBranch(-23.550900, -46.633500, BranchType.TRADITIONAL);
        existingBranches.add(existing);

        // When
        BranchBusinessRules.ValidationResult result = businessRules.validateBranchRegistration(newBranch,
                existingBranches);

        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().contains("too close"));
    }

    @Test
    @DisplayName("Should reject traditional branch in saturated area")
    void shouldRejectTraditionalBranchInSaturatedArea() {
        // Given: Create exactly 10 branches in a compact circle around test location
        GeoPoint centerLocation = new GeoPoint(-23.550520, -46.633308);

        // Create 10 branches in circle pattern - all within 3km radius but >0.5km apart
        for (int i = 0; i < 10; i++) {
            double angle = (i * 36.0) * Math.PI / 180.0; // 36 degrees apart (360/10)
            double radius = 0.02; // ~2.2 km from center
            double lat = centerLocation.latitude() + radius * Math.cos(angle);
            double lon = centerLocation.longitude() + radius * Math.sin(angle);
            Branch existing = createBranch(lat, lon, BranchType.ATM_ONLY);
            existingBranches.add(existing);
        }

        // When: Try to register TRADITIONAL branch at the center (surrounded by 10
        // branches)
        Branch newBranch = createBranch(centerLocation.latitude(), centerLocation.longitude(), BranchType.TRADITIONAL);
        BranchBusinessRules.ValidationResult result = businessRules.validateBranchRegistration(newBranch,
                existingBranches);

        // Then
        assertFalse(result.isValid(), "Traditional branch should be rejected in saturated area");
        assertTrue(result.getMessage().contains("saturated"),
                "Expected saturation message but got: " + result.getMessage());
    }

    @Test
    @DisplayName("Should allow ATM in saturated area")
    void shouldAllowATMInSaturatedArea() {
        // Given: saturated area but ATM type
        Branch newBranch = createBranch(-23.550520, -46.633308, BranchType.ATM_ONLY);

        // Create 10 branches at safe distance (>0.5km ~= 0.0045 degrees) but within 5km
        // radius
        for (int i = 1; i <= 10; i++) {
            double lat = -23.550520 + (i * 0.01); // ~1.11 km apart
            double lon = -46.633308 + (i * 0.01);
            Branch existing = createBranch(lat, lon, BranchType.TRADITIONAL);
            existingBranches.add(existing);
        }

        // When
        BranchBusinessRules.ValidationResult result = businessRules.validateBranchRegistration(newBranch,
                existingBranches);

        // Then
        assertTrue(result.isValid(), "ATM should be allowed in saturated area. Error: " + result.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "50.0, 0.3, 0, ATM_ONLY",
            "150.0, 0.5, 3, EXPRESS",
            "500.0, 0.8, 2, PREMIUM",
            "1000.0, 0.4, 0, TRADITIONAL"
    })
    @DisplayName("Should recommend branch type based on area characteristics")
    void shouldRecommendBranchType(double density, double commercialIndex,
            int nearbyCount, String expectedType) {
        // When
        BranchType recommended = businessRules.recommendBranchType(
                density, commercialIndex, nearbyCount);

        // Then
        assertEquals(BranchType.valueOf(expectedType), recommended);
    }

    @Test
    @DisplayName("Should prevent permanent closure from active status")
    void shouldPreventDirectPermanentClosure() {
        // When
        BranchBusinessRules.ValidationResult result = businessRules.validateStatusTransition(
                BranchStatus.ACTIVE, BranchStatus.PERMANENTLY_CLOSED);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("temporary closure"));
    }

    @Test
    @DisplayName("Should allow temporary closure from active status")
    void shouldAllowTemporaryClosure() {
        // When
        BranchBusinessRules.ValidationResult result = businessRules.validateStatusTransition(
                BranchStatus.ACTIVE, BranchStatus.TEMPORARILY_CLOSED);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should prevent status change of permanently closed branch")
    void shouldPreventReopeningPermanentlyClosed() {
        // When
        BranchBusinessRules.ValidationResult result = businessRules.validateStatusTransition(
                BranchStatus.PERMANENTLY_CLOSED, BranchStatus.ACTIVE);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("permanently closed"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "TRADITIONAL", "PREMIUM", "DIGITAL", "EXPRESS" })
    @DisplayName("Should determine operational hours by branch type")
    void shouldDetermineOperationalHours(String branchTypeStr) {
        // Given
        BranchType branchType = BranchType.valueOf(branchTypeStr);
        Branch branch = createBranch(-23.550520, -46.633308, branchType);
        LocalDateTime withinHours = LocalDateTime.now().withHour(12).withMinute(0);

        // When
        boolean shouldClose = businessRules.shouldTemporarilyClose(branch, withinHours);

        // Then
        assertFalse(shouldClose, "Branch should be open at noon");
    }

    @Test
    @DisplayName("ATM should operate 24/7")
    void atmShouldOperate24Hours() {
        // Given
        Branch atm = createBranch(-23.550520, -46.633308, BranchType.ATM_ONLY);
        LocalDateTime midnight = LocalDateTime.now().withHour(0).withMinute(0);

        // When
        boolean shouldClose = businessRules.shouldTemporarilyClose(atm, midnight);

        // Then
        assertFalse(shouldClose, "ATM should operate 24/7");
    }

    @Test
    @DisplayName("Should calculate business priority with performance metrics")
    void shouldCalculateBusinessPriority() {
        // Given
        Branch branch = createBranch(-23.550520, -46.633308, BranchType.PREMIUM);
        double footfall = 1000.0; // 1000 customers
        double volume = 5_000_000.0; // R$ 5 million

        // When
        int priority = businessRules.calculateBusinessPriority(branch, footfall, volume);

        // Then
        assertTrue(priority > 0);
        assertEquals(5 + 10 + 5, priority); // base(5) + footfall(10) + volume(5)
    }

    @Test
    @DisplayName("Inactive branch should have zero priority")
    void inactiveBranchShouldHaveZeroPriority() {
        // Given
        Branch branch = createBranch(-23.550520, -46.633308, BranchType.PREMIUM);
        branch.temporarilyClose();

        // When
        int priority = businessRules.calculateBusinessPriority(branch, 1000.0, 5_000_000.0);

        // Then
        assertEquals(0, priority);
    }

    @Test
    @DisplayName("Should validate regulatory compliance - valid branch")
    void shouldValidateRegulatoryCompliance() {
        // Given
        Branch branch = Branch.create(
                "Test Branch",
                new GeoPoint(-23.550520, -46.633308),
                BranchType.TRADITIONAL,
                "Av. Paulista, 1000");
        branch.updateInfo("Test Branch", "Av. Paulista, 1000", "+55 11 98765-4321");

        // When
        BranchBusinessRules.ValidationResult result = businessRules.validateRegulatoryCompliance(branch);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should reject branch without contact phone")
    void shouldRejectBranchWithoutContact() {
        // Given
        Branch branch = createBranch(-23.550520, -46.633308, BranchType.TRADITIONAL);

        // When
        BranchBusinessRules.ValidationResult result = businessRules.validateRegulatoryCompliance(branch);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("contact phone"));
    }

    @Test
    @DisplayName("Should reject branch with invalid coordinates")
    void shouldRejectInvalidCoordinates() {
        // Given: valid branch for testing regulatory compliance
        Branch branch = createBranch(-23.550520, -46.633308, BranchType.TRADITIONAL);
        branch.updateInfo("Test", "Address", "+55 11 98765-4321");

        // When: validateRegulatoryCompliance checks coordinate ranges
        BranchBusinessRules.ValidationResult result = businessRules.validateRegulatoryCompliance(branch);

        // Then: valid coordinates should pass
        assertTrue(result.isValid());

        // Test that GeoPoint itself validates coordinates at construction time
        assertThrows(IllegalArgumentException.class, () -> {
            new GeoPoint(91.0, -46.633308); // Invalid latitude
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new GeoPoint(-23.550520, 181.0); // Invalid longitude
        });
    }

    // Helper methods

    private Branch createBranch(double lat, double lon, BranchType type) {
        return Branch.createWithId(
                BranchId.generate(),
                "Test Branch",
                new GeoPoint(lat, lon),
                type,
                "Test Address");
    }
}
