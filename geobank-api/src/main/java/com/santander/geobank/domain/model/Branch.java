package com.santander.geobank.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Branch - Domain Entity representing a banking branch
 *
 * Core aggregate root for branch operations with geographical capabilities.
 * Encapsulates business rules for branch validation and distance calculations.
 * */
public class Branch {

    private final BranchId id;
    private final GeoPoint location;
    private final BranchType type;
    private String name;
    private String address;
    private String contactPhone;
    private BranchStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Constructor for creating new branch
     */
    public Branch(BranchId id, String name, GeoPoint location, BranchType type, String address) {
        this.id = Objects.requireNonNull(id, "Branch ID cannot be null");
        this.location = Objects.requireNonNull(location, "Branch location cannot be null");
        this.type = Objects.requireNonNull(type, "Branch type cannot be null");
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = BranchStatus.ACTIVE;

        setName(name);
        setAddress(address);
    }

    /**
     * Constructor for loading existing branch
     */
    public Branch(BranchId id, String name, GeoPoint location, BranchType type,
            String address, String contactPhone, BranchStatus status,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "Branch ID cannot be null");
        this.location = Objects.requireNonNull(location, "Branch location cannot be null");
        this.type = Objects.requireNonNull(type, "Branch type cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp cannot be null");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
        this.status = status != null ? status : BranchStatus.ACTIVE;

        setName(name);
        setAddress(address);
        setContactPhone(contactPhone);
    }

    /**
     * Factory method for creating new branch
     */
    public static Branch create(String name, GeoPoint location, BranchType type, String address) {
        return new Branch(BranchId.generate(), name, location, type, address);
    }

    /**
     * Factory method for creating branch with specific ID
     */
    public static Branch createWithId(BranchId id, String name, GeoPoint location,
            BranchType type, String address) {
        return new Branch(id, name, location, type, address);
    }

    /**
     * Calculate distance to user location
     *
     * @param userLocation user's geographical position
     * @return Distance to this branch
     */
    public Distance distanceTo(GeoPoint userLocation) {
        Objects.requireNonNull(userLocation, "User location cannot be null");
        return location.distanceTo(userLocation);
    }

    /**
     * Check if branch is within radius of given location
     *
     * @param location reference location
     * @param radiusKm radius in kilometers
     * @return true if within radius
     */
    public boolean isWithinRadius(GeoPoint location, double radiusKm) {
        return this.location.isWithinRadius(location, radiusKm);
    }

    /**
     * Check if branch supports specific service
     *
     * @param serviceType type of service required
     * @return true if branch supports the service
     */
    public boolean supportsService(String serviceType) {
        if (!isOperational()) {
            return false;
        }
        return type.supportsService(serviceType);
    }

    /**
     * Check if branch is currently operational
     *
     * @return true if branch is active and operational
     */
    public boolean isOperational() {
        return status == BranchStatus.ACTIVE;
    }

    /**
     * Update branch information
     *
     * @param name         new branch name
     * @param address      new address
     * @param contactPhone new contact phone
     */
    public void updateInfo(String name, String address, String contactPhone) {
        setName(name);
        setAddress(address);
        setContactPhone(contactPhone);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Activate branch
     */
    public void activate() {
        if (this.status == BranchStatus.PERMANENTLY_CLOSED) {
            throw new IllegalStateException("Cannot activate permanently closed branch");
        }
        this.status = BranchStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Temporarily close branch
     */
    public void temporarilyClose() {
        this.status = BranchStatus.TEMPORARILY_CLOSED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Permanently close branch
     */
    public void permanentlyClose() {
        this.status = BranchStatus.PERMANENTLY_CLOSED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Put branch under maintenance
     */
    public void putUnderMaintenance() {
        this.status = BranchStatus.UNDER_MAINTENANCE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Get priority score for branch ranking
     *
     * @return priority score based on type and status
     */
    public int getPriorityScore() {
        if (!isOperational()) {
            return 0;
        }
        return type.getPriorityScore();
    }

    // Validation methods
    private void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Branch name cannot be null or empty");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException("Branch name cannot exceed 100 characters");
        }
        this.name = name.trim();
    }

    private void setAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Branch address cannot be null or empty");
        }
        if (address.trim().length() > 255) {
            throw new IllegalArgumentException("Branch address cannot exceed 255 characters");
        }
        this.address = address.trim();
    }

    private void setContactPhone(String contactPhone) {
        if (contactPhone != null && !contactPhone.trim().isEmpty()) {
            String trimmed = contactPhone.trim();
            if (trimmed.length() > 20) {
                throw new IllegalArgumentException("Contact phone cannot exceed 20 characters");
            }
            this.contactPhone = trimmed;
        }
    }

    // Getters
    public BranchId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public BranchType getType() {
        return type;
    }

    public String getAddress() {
        return address;
    }

    public Optional<String> getContactPhone() {
        return Optional.ofNullable(contactPhone);
    }

    public BranchStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Branch branch = (Branch) obj;
        return Objects.equals(id, branch.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Branch[id=%s, name='%s', type=%s, location=%s, status=%s]",
                id.getShortDisplay(), name, type, location, status);
    }
}

