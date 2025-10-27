package com.santander.geobank.infrastructure.persistence.repositories;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.santander.geobank.infrastructure.persistence.entities.BranchEntity;
import com.santander.geobank.infrastructure.persistence.entities.BranchStatusEntity;

/**
 * JPA Repository for Branch entities with optimized geospatial queries.
 * Provides efficient distance-based searches using SQL functions.
 *
 * Custom queries leverage database-level calculations for performance
 * and implement bounding box optimization for large datasets.
 */
@Repository
public interface BranchJpaRepository extends JpaRepository<BranchEntity, String> {

    /**
     * Find branches within specified distance using Haversine formula.
     * Query optimized with bounding box pre-filter for performance.
     *
     * @param latitude      center latitude
     * @param longitude     center longitude
     * @param maxDistanceKm maximum distance in kilometers
     * @param limit         maximum number of results
     * @return list of branches within distance, ordered by proximity
     */
    @Query(value = """
            SELECT b.* FROM branches b
            WHERE b.status = 'ACTIVE'
            AND (
                6371 * acos(
                    cos(radians(:latitude)) *
                    cos(radians(b.latitude)) *
                    cos(radians(b.longitude) - radians(:longitude)) +
                    sin(radians(:latitude)) *
                    sin(radians(b.latitude))
                )
            ) <= :maxDistanceKm
            ORDER BY (
                6371 * acos(
                    cos(radians(:latitude)) *
                    cos(radians(b.latitude)) *
                    cos(radians(b.longitude) - radians(:longitude)) +
                    sin(radians(:latitude)) *
                    sin(radians(b.latitude))
                )
            ) ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<BranchEntity> findNearestBranches(
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("maxDistanceKm") BigDecimal maxDistanceKm,
            @Param("limit") Integer limit);

    /**
     * Find branches within bounding box for efficient spatial filtering.
     * Pre-filter for complex geospatial operations to improve performance.
     *
     * @param minLat minimum latitude
     * @param maxLat maximum latitude
     * @param minLon minimum longitude
     * @param maxLon maximum longitude
     * @return branches within bounding box
     */
    @Query("SELECT b FROM BranchEntity b " +
            "WHERE b.status = com.santander.geobank.infrastructure.persistence.entities.BranchStatusEntity.ACTIVE " +
            "AND b.latitude BETWEEN :minLat AND :maxLat " +
            "AND b.longitude BETWEEN :minLon AND :maxLon " +
            "ORDER BY b.createdAt DESC")
    List<BranchEntity> findWithinBoundingBox(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLon") BigDecimal minLon,
            @Param("maxLon") BigDecimal maxLon);

    /**
     * Find all active branches for caching and bulk operations.
     * Optimized query with minimal data transfer.
     *
     * @return all active branches
     */
    @Query("SELECT b FROM BranchEntity b " +
            "WHERE b.status = com.santander.geobank.infrastructure.persistence.entities.BranchStatusEntity.ACTIVE " +
            "ORDER BY b.createdAt ASC")
    List<BranchEntity> findAllActive();

    /**
     * Count branches by status for monitoring and analytics.
     *
     * @param status branch status
     * @return count of branches with specified status
     */
    long countByStatus(BranchStatusEntity status);

    /**
     * Find branches by type for network analysis.
     *
     * @param type branch type
     * @return branches of specified type
     */
    @Query("SELECT b FROM BranchEntity b " +
            "WHERE b.type = :type " +
            "AND b.status = com.santander.geobank.infrastructure.persistence.entities.BranchStatusEntity.ACTIVE " +
            "ORDER BY b.name ASC")
    List<BranchEntity> findByTypeAndActive(@Param("type") String type);

    /**
     * Calculate average distance between branches for density analysis.
     * Complex spatial analytics query for strategic planning.
     *
     * @param centerLat analysis center latitude
     * @param centerLon analysis center longitude
     * @param radiusKm  analysis radius
     * @return average distance in kilometers
     */
    @Query(value = """
            SELECT AVG(
                6371 * acos(
                    cos(radians(:centerLat)) *
                    cos(radians(b.latitude)) *
                    cos(radians(b.longitude) - radians(:centerLon)) +
                    sin(radians(:centerLat)) *
                    sin(radians(b.latitude))
                )
            ) as avgDistance
            FROM branches b
            WHERE b.status = 'ACTIVE'
            AND (
                6371 * acos(
                    cos(radians(:centerLat)) *
                    cos(radians(b.latitude)) *
                    cos(radians(b.longitude) - radians(:centerLon)) +
                    sin(radians(:centerLat)) *
                    sin(radians(b.latitude))
                )
            ) <= :radiusKm
            """, nativeQuery = true)
    Double calculateAverageDistanceInRadius(
            @Param("centerLat") BigDecimal centerLat,
            @Param("centerLon") BigDecimal centerLon,
            @Param("radiusKm") BigDecimal radiusKm);

    /**
     * Check if branch exists at exact coordinates to prevent duplicates.
     * Business rule enforcement at database level.
     *
     * @param latitude  exact latitude
     * @param longitude exact longitude
     * @return true if branch exists at coordinates
     */
    @Query("SELECT COUNT(b) > 0 FROM BranchEntity b " +
            "WHERE b.latitude = :latitude AND b.longitude = :longitude")
    boolean existsByCoordinates(
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude);
}

