package com.santander.geobank.domain.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BranchType - Value Object representing different types of banking branches
 *
 * Enumeration of branch types with specific characteristics and capabilities.
 * Supports business rules for service availability and operational hours.
 * */
public enum BranchType {

    /**
     * Traditional full-service branch with all banking capabilities
     */
    TRADITIONAL("Traditional", "Full-service branch with all banking operations", true, true, true),

    /**
     * Digital-only branch focused on technology and self-service
     */
    DIGITAL("Digital", "Technology-focused branch with minimal staff", true, false, true),

    /**
     * Premium branch for high-net-worth customers
     */
    PREMIUM("Premium", "Exclusive branch for premium banking services", true, true, false),

    /**
     * Express branch for quick transactions only
     */
    EXPRESS("Express", "Limited services for quick transactions", false, false, true),

    /**
     * ATM-only location
     */
    ATM_ONLY("ATM Only", "Automated services only", false, false, false);

    private final String displayName;
    private final String description;
    private final boolean hasFullBankingServices;
    private final boolean hasPersonalBanker;
    private final boolean has24HourAccess;

    BranchType(String displayName, String description,
            boolean hasFullBankingServices, boolean hasPersonalBanker, boolean has24HourAccess) {
        this.displayName = displayName;
        this.description = description;
        this.hasFullBankingServices = hasFullBankingServices;
        this.hasPersonalBanker = hasPersonalBanker;
        this.has24HourAccess = has24HourAccess;
    }

    /**
     * Get display name for UI
     *
     * @return human-readable display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get description of branch type
     *
     * @return detailed description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if branch offers full banking services
     *
     * @return true if full services available
     */
    public boolean hasFullBankingServices() {
        return hasFullBankingServices;
    }

    /**
     * Check if branch has personal banker services
     *
     * @return true if personal banker available
     */
    public boolean hasPersonalBanker() {
        return hasPersonalBanker;
    }

    /**
     * Check if branch has 24-hour access
     *
     * @return true if 24-hour access available
     */
    public boolean has24HourAccess() {
        return has24HourAccess;
    }

    /**
     * Get branch types that support specific service
     *
     * @param requiresFullServices   true if full banking services needed
     * @param requiresPersonalBanker true if personal banker needed
     * @param requires24HourAccess   true if 24-hour access needed
     * @return set of compatible branch types
     */
    public static Set<BranchType> getCompatibleTypes(boolean requiresFullServices,
            boolean requiresPersonalBanker,
            boolean requires24HourAccess) {
        return Arrays.stream(values())
                .filter(type -> (!requiresFullServices || type.hasFullBankingServices))
                .filter(type -> (!requiresPersonalBanker || type.hasPersonalBanker))
                .filter(type -> (!requires24HourAccess || type.has24HourAccess))
                .collect(Collectors.toSet());
    }

    /**
     * Get branch types suitable for specific customer segment
     *
     * @param customerSegment customer segment identifier
     * @return set of suitable branch types
     */
    public static Set<BranchType> getTypesForCustomerSegment(String customerSegment) {
        return switch (customerSegment.toLowerCase()) {
            case "premium", "private", "wealth" -> Set.of(PREMIUM, TRADITIONAL);
            case "digital", "tech", "millennial" -> Set.of(DIGITAL, EXPRESS);
            case "business", "corporate" -> Set.of(TRADITIONAL, PREMIUM);
            case "retail", "individual" -> Set.of(TRADITIONAL, DIGITAL, EXPRESS);
            default -> Set.of(TRADITIONAL, DIGITAL);
        };
    }

    /**
     * Check if this branch type can serve specific customer needs
     *
     * @param serviceType type of service requested
     * @return true if branch type supports the service
     */
    public boolean supportsService(String serviceType) {
        return switch (serviceType.toLowerCase()) {
            case "account_opening", "loan_application", "investment_consultation" ->
                hasFullBankingServices && hasPersonalBanker;
            case "cash_withdrawal", "balance_inquiry", "transfer" -> true;
            case "safe_deposit", "currency_exchange" -> hasFullBankingServices;
            case "after_hours_banking" -> has24HourAccess;
            default -> hasFullBankingServices;
        };
    }

    /**
     * Get priority score for branch type (higher = more comprehensive)
     *
     * @return priority score from 1-5
     */
    public int getPriorityScore() {
        return switch (this) {
            case PREMIUM -> 5;
            case TRADITIONAL -> 4;
            case DIGITAL -> 3;
            case EXPRESS -> 2;
            case ATM_ONLY -> 1;
        };
    }

    /**
     * Parse BranchType from string with fallback
     *
     * @param value string representation
     * @return BranchType instance
     * @throws IllegalArgumentException if invalid value
     */
    public static BranchType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("BranchType value cannot be null or empty");
        }

        String normalized = value.trim().toUpperCase().replace(" ", "_");

        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try matching by display name
            return Arrays.stream(values())
                    .filter(type -> type.displayName.equalsIgnoreCase(value.trim()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("Unknown BranchType: %s. Valid types: %s",
                                    value, Arrays.toString(values()))));
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}

