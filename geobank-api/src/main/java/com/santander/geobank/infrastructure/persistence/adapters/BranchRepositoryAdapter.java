package com.santander.geobank.infrastructure.persistence.adapters;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.santander.geobank.domain.model.Branch;
import com.santander.geobank.domain.model.BranchId;
import com.santander.geobank.domain.model.BranchType;
import com.santander.geobank.domain.model.GeoPoint;
import com.santander.geobank.domain.ports.BranchRepository;
import com.santander.geobank.infrastructure.persistence.entities.BranchStatusEntity;
import com.santander.geobank.infrastructure.persistence.mappers.BranchMapper;
import com.santander.geobank.infrastructure.persistence.repositories.BranchJpaRepository;

/**
 * JPA Adapter implementing BranchRepository port.
 * Translates between domain model and persistence layer using BranchMapper.
 *
 * Implements all repository operations with geospatial query optimization
 * and proper exception handling for production environment.
 */
@Component
@Transactional
public class BranchRepositoryAdapter implements BranchRepository {

    private final BranchJpaRepository jpaRepository;

    public BranchRepositoryAdapter(BranchJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Branch save(Branch branch) {
        if (branch == null) {
            throw new IllegalArgumentException("Branch cannot be null");
        }

        var entity = BranchMapper.toEntity(branch);
        var savedEntity = jpaRepository.save(entity);
        return BranchMapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Branch> findById(BranchId id) {
        if (id == null) {
            return Optional.empty();
        }

        return jpaRepository.findById(id.value())
                .map(BranchMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> findAllActive() {
        var activeEntities = jpaRepository.findAllActive();
        return BranchMapper.toDomainList(activeEntities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> findAll() {
        var allEntities = jpaRepository.findAll();
        return BranchMapper.toDomainList(allEntities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> findByTypes(Set<BranchType> types) {
        if (types == null || types.isEmpty()) {
            return List.of();
        }

        // Simplified implementation using individual queries per type
        // TODO: Add findByTypeIn method to repository for better performance
        List<Branch> results = new java.util.ArrayList<>();
        for (BranchType type : types) {
            var entityType = BranchMapper.mapToEntityTypes(Set.of(type)).iterator().next();
            var entities = jpaRepository.findByTypeAndActive(entityType.name());
            results.addAll(BranchMapper.toDomainList(entities));
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> findWithinRadius(GeoPoint location, double radiusKm) {
        if (location == null || radiusKm <= 0) {
            return List.of();
        }

        var entities = jpaRepository.findNearestBranches(
                java.math.BigDecimal.valueOf(location.latitude()),
                java.math.BigDecimal.valueOf(location.longitude()),
                java.math.BigDecimal.valueOf(radiusKm),
                1000 // Large limit to get all within radius
        );
        return BranchMapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> findWithinRadiusByTypes(GeoPoint location, double radiusKm, Set<BranchType> types) {
        if (location == null || radiusKm <= 0 || types == null || types.isEmpty()) {
            return List.of();
        }

        // Get all branches within radius and filter by types in memory
        // TODO: Add type filtering to spatial query for better performance
        var allWithinRadius = findWithinRadius(location, radiusKm);
        return allWithinRadius.stream()
                .filter(branch -> types.contains(branch.getType()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> findNearestBranches(GeoPoint location, int maxResults) {
        if (location == null || maxResults <= 0) {
            return List.of();
        }

        var entities = jpaRepository.findNearestBranches(
                java.math.BigDecimal.valueOf(location.latitude()),
                java.math.BigDecimal.valueOf(location.longitude()),
                java.math.BigDecimal.valueOf(50.0), // 50km radius as reasonable default
                maxResults);
        return BranchMapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> findNearestBranchesByTypes(GeoPoint location, Set<BranchType> types, int maxResults) {
        if (location == null || types == null || types.isEmpty() || maxResults <= 0) {
            return List.of();
        }

        // Get nearest branches and filter by types in memory
        // TODO: Add type filtering to spatial query for better performance
        var nearest = findNearestBranches(location, maxResults * 2); // Get more to filter
        return nearest.stream()
                .filter(branch -> types.contains(branch.getType()))
                .limit(maxResults)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(BranchId id) {
        if (id == null) {
            return false;
        }

        return jpaRepository.existsById(id.value());
    }

    @Override
    @Transactional
    public boolean deleteById(BranchId id) {
        if (id == null) {
            return false;
        }

        if (jpaRepository.existsById(id.value())) {
            jpaRepository.deleteById(id.value());
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return jpaRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActive() {
        return jpaRepository.countByStatus(BranchStatusEntity.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByType(BranchType type) {
        if (type == null) {
            return 0;
        }

        // Simplified implementation using filtered count
        // TODO: Add countByType method to repository for better performance
        var allActive = jpaRepository.findAllActive();
        var entityType = BranchMapper.mapToEntityTypes(Set.of(type)).iterator().next();
        return allActive.stream()
                .filter(entity -> entity.getType() == entityType)
                .count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> findByNameContaining(String namePattern) {
        if (namePattern == null || namePattern.trim().isEmpty()) {
            return List.of();
        }

        // Simplified implementation using memory filtering
        // TODO: Add findByNameContainingIgnoreCase method to repository
        var allActive = jpaRepository.findAllActive();
        var filtered = allActive.stream()
                .filter(entity -> entity.getName() != null &&
                        entity.getName().toLowerCase().contains(namePattern.trim().toLowerCase()))
                .collect(java.util.stream.Collectors.toList());
        return BranchMapper.toDomainList(filtered);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> findByAddressContaining(String addressPattern) {
        if (addressPattern == null || addressPattern.trim().isEmpty()) {
            return List.of();
        }

        // Simplified implementation using memory filtering
        // TODO: Add findByAddressContainingIgnoreCase method to repository
        var allActive = jpaRepository.findAllActive();
        var filtered = allActive.stream()
                .filter(entity -> entity.getAddress() != null &&
                        entity.getAddress().toLowerCase().contains(addressPattern.trim().toLowerCase()))
                .collect(java.util.stream.Collectors.toList());
        return BranchMapper.toDomainList(filtered);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> findInBoundingBox(GeoPoint northEast, GeoPoint southWest) {
        if (northEast == null || southWest == null) {
            return List.of();
        }

        var entities = jpaRepository.findWithinBoundingBox(
                java.math.BigDecimal.valueOf(southWest.latitude()), // minLat
                java.math.BigDecimal.valueOf(northEast.latitude()), // maxLat
                java.math.BigDecimal.valueOf(southWest.longitude()), // minLon
                java.math.BigDecimal.valueOf(northEast.longitude()) // maxLon
        );
        return BranchMapper.toDomainList(entities);
    }

    @Override
    @Transactional
    public List<Branch> saveAll(List<Branch> branches) {
        if (branches == null || branches.isEmpty()) {
            return List.of();
        }

        var entities = BranchMapper.toEntityList(branches);
        var savedEntities = jpaRepository.saveAll(entities);
        return BranchMapper.toDomainList(savedEntities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> findBySupportedService(String serviceType, Optional<GeoPoint> location,
            Optional<Double> radiusKm) {

        // For now, return all branches as service mapping is not implemented
        // TODO: Implement service-based filtering when branch services are added
        if (location.isPresent() && radiusKm.isPresent()) {
            return findWithinRadius(location.get(), radiusKm.get());
        }

        return findAllActive();
    }

    /**
     * Health check method for repository connectivity.
     *
     * @return true if repository is accessible
     */
    public boolean isHealthy() {
        try {
            jpaRepository.count();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

