package com.santander.geobank.domain.ports;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.santander.geobank.domain.model.Branch;
import com.santander.geobank.domain.model.BranchId;
import com.santander.geobank.domain.model.BranchType;
import com.santander.geobank.domain.model.GeoPoint;

/**
 * BranchRepository - Port for branch persistence operations
 *
 * Defines the contract for branch data access following repository pattern.
 * Abstracts persistence technology from domain logic.
 * */
public interface BranchRepository {

    /**
     * Save a branch (create or update)
     *
     * @param branch branch to save
     * @return saved branch with updated timestamps
     */
    Branch save(Branch branch);

    /**
     * Find branch by ID
     *
     * @param id branch identifier
     * @return optional branch if found
     */
    Optional<Branch> findById(BranchId id);

    /**
     * Find all active branches
     *
     * @return list of active branches
     */
    List<Branch> findAllActive();

    /**
     * Find all branches (regardless of status)
     *
     * @return list of all branches
     */
    List<Branch> findAll();

    /**
     * Find branches by type
     *
     * @param types set of branch types to search for
     * @return list of branches matching the types
     */
    List<Branch> findByTypes(Set<BranchType> types);

    /**
     * Find branches within radius of a location
     *
     * @param location center point for search
     * @param radiusKm search radius in kilometers
     * @return list of branches within radius
     */
    List<Branch> findWithinRadius(GeoPoint location, double radiusKm);

    /**
     * Find branches within radius by type
     *
     * @param location center point for search
     * @param radiusKm search radius in kilometers
     * @param types    set of branch types to filter by
     * @return list of branches within radius and matching types
     */
    List<Branch> findWithinRadiusByTypes(GeoPoint location, double radiusKm, Set<BranchType> types);

    /**
     * Find nearest branches to a location
     *
     * @param location   center point for search
     * @param maxResults maximum number of results to return
     * @return list of nearest branches ordered by distance
     */
    List<Branch> findNearestBranches(GeoPoint location, int maxResults);

    /**
     * Find nearest branches of specific types
     *
     * @param location   center point for search
     * @param types      set of branch types to filter by
     * @param maxResults maximum number of results to return
     * @return list of nearest branches of specified types
     */
    List<Branch> findNearestBranchesByTypes(GeoPoint location, Set<BranchType> types, int maxResults);

    /**
     * Check if branch exists by ID
     *
     * @param id branch identifier
     * @return true if branch exists
     */
    boolean existsById(BranchId id);

    /**
     * Delete branch by ID
     *
     * @param id branch identifier
     * @return true if branch was deleted
     */
    boolean deleteById(BranchId id);

    /**
     * Count total number of branches
     *
     * @return total branch count
     */
    long count();

    /**
     * Count active branches
     *
     * @return active branch count
     */
    long countActive();

    /**
     * Count branches by type
     *
     * @param type branch type
     * @return count of branches of specified type
     */
    long countByType(BranchType type);

    /**
     * Find branches by name (case-insensitive partial match)
     *
     * @param namePattern name pattern to search for
     * @return list of branches matching name pattern
     */
    List<Branch> findByNameContaining(String namePattern);

    /**
     * Find branches by address (case-insensitive partial match)
     *
     * @param addressPattern address pattern to search for
     * @return list of branches matching address pattern
     */
    List<Branch> findByAddressContaining(String addressPattern);

    /**
     * Find branches in a geographical bounding box
     *
     * @param northEast northeast corner of bounding box
     * @param southWest southwest corner of bounding box
     * @return list of branches within bounding box
     */
    List<Branch> findInBoundingBox(GeoPoint northEast, GeoPoint southWest);

    /**
     * Batch save multiple branches
     *
     * @param branches list of branches to save
     * @return list of saved branches
     */
    List<Branch> saveAll(List<Branch> branches);

    /**
     * Find branches that support a specific service
     *
     * @param serviceType service type identifier
     * @param location    optional location for proximity filtering
     * @param radiusKm    optional radius for proximity filtering
     * @return list of branches supporting the service
     */
    List<Branch> findBySupportedService(String serviceType, Optional<GeoPoint> location, Optional<Double> radiusKm);
}

