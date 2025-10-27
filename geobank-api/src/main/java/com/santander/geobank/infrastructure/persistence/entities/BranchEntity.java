package com.santander.geobank.infrastructure.persistence.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * JPA Entity for Branch persistence with geospatial optimization.
 * Maps domain Branch aggregate to relational database structure.
 *
 * Includes spatial indexing for efficient distance queries
 * and audit fields for compliance tracking.
 */
@Entity
@Table(name = "branches", indexes = {
        @Index(name = "idx_branch_location", columnList = "latitude, longitude"),
        @Index(name = "idx_branch_type", columnList = "type"),
        @Index(name = "idx_branch_status", columnList = "status"),
        @Index(name = "idx_branch_created", columnList = "created_at")
})
public class BranchEntity {

    @Id
    @Column(name = "id", length = 20)
    @Size(min = 3, max = 20)
    private String id;

    @NotNull
    @Column(name = "name", length = 100, nullable = false)
    @Size(min = 2, max = 100)
    private String name;

    @NotNull
    @Column(name = "latitude", precision = 10, scale = 8, nullable = false)
    private BigDecimal latitude;

    @NotNull
    @Column(name = "longitude", precision = 11, scale = 8, nullable = false)
    private BigDecimal longitude;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private BranchTypeEntity type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private BranchStatusEntity status;

    @Column(name = "address", length = 500)
    @Size(max = 500)
    private String address;

    @Column(name = "operating_hours", length = 50)
    @Size(max = 50)
    private String operatingHours;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "version")
    private Long version;

    // Default constructor for JPA
    protected BranchEntity() {
    }

    public BranchEntity(String id, String name, BigDecimal latitude, BigDecimal longitude,
            BranchTypeEntity type, BranchStatusEntity status,
            String address, String operatingHours) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.status = status;
        this.address = address;
        this.operatingHours = operatingHours;
        this.createdAt = LocalDateTime.now();
        this.version = 0L;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BranchTypeEntity getType() {
        return type;
    }

    public void setType(BranchTypeEntity type) {
        this.type = type;
    }

    public BranchStatusEntity getStatus() {
        return status;
    }

    public void setStatus(BranchStatusEntity status) {
        this.status = status;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getOperatingHours() {
        return operatingHours;
    }

    public void setOperatingHours(String operatingHours) {
        this.operatingHours = operatingHours;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Calculate distance to given coordinates using Haversine formula.
     * Optimized for database-level distance calculations.
     *
     * @param targetLat target latitude
     * @param targetLon target longitude
     * @return distance in kilometers
     */
    public double calculateDistanceKm(BigDecimal targetLat, BigDecimal targetLon) {
        double earthRadiusKm = 6371.0;

        double dLat = Math.toRadians(targetLat.subtract(this.latitude).doubleValue());
        double dLon = Math.toRadians(targetLon.subtract(this.longitude).doubleValue());

        double lat1Rad = Math.toRadians(this.latitude.doubleValue());
        double lat2Rad = Math.toRadians(targetLat.doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) *
                        Math.cos(lat1Rad) * Math.cos(lat2Rad);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BranchEntity that))
            return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return String.format("BranchEntity{id='%s', name='%s', location=[%s,%s], type=%s, status=%s}",
                id, name, latitude, longitude, type, status);
    }
}

