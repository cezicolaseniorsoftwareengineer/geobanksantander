package com.santander.geobank.infrastructure.persistence.mappers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.santander.geobank.domain.model.Branch;
import com.santander.geobank.domain.model.BranchId;
import com.santander.geobank.domain.model.BranchStatus;
import com.santander.geobank.domain.model.BranchType;
import com.santander.geobank.domain.model.GeoPoint;
import com.santander.geobank.infrastructure.persistence.entities.BranchEntity;
import com.santander.geobank.infrastructure.persistence.entities.BranchStatusEntity;
import com.santander.geobank.infrastructure.persistence.entities.BranchTypeEntity;

/**
 * Mapper between Branch domain model and BranchEntity JPA entity.
 *
 * Handles bidirectional conversion based on actual domain/entity structures.
 * Simplified mapping focusing on core geographical and business attributes.
 */
public class BranchMapper {

    /**
     * Convert JPA entity to domain model.
     *
     * @param entity JPA entity
     * @return domain model
     */
    public static Branch toDomain(BranchEntity entity) {
        if (entity == null) {
            return null;
        }

        GeoPoint location = new GeoPoint(
                entity.getLatitude().doubleValue(),
                entity.getLongitude().doubleValue());

        BranchType domainType = mapToDomainType(entity.getType());
        BranchStatus domainStatus = mapToDomainStatus(entity.getStatus());

        // Use existing constructor with available fields from entity
        Branch branch = new Branch(
                new BranchId(entity.getId()),
                entity.getName(),
                location,
                domainType,
                entity.getAddress(),
                null, // contactPhone not in entity
                domainStatus,
                entity.getCreatedAt(),
                entity.getUpdatedAt());

        return branch;
    }

    /**
     * Convert domain model to JPA entity.
     * Uses public constructor with required fields.
     *
     * @param branch domain model
     * @return JPA entity
     */
    public static BranchEntity toEntity(Branch branch) {
        if (branch == null) {
            return null;
        }

        BranchEntity entity = new BranchEntity(
                branch.getId().value(),
                branch.getName(),
                BigDecimal.valueOf(branch.getLocation().latitude()),
                BigDecimal.valueOf(branch.getLocation().longitude()),
                mapToEntityType(branch.getType()),
                mapToEntityStatus(branch.getStatus()),
                branch.getAddress(),
                null // operatingHours not in domain
        );

        // Set additional fields if needed
        entity.setCreatedAt(branch.getCreatedAt());
        entity.setUpdatedAt(branch.getUpdatedAt());

        return entity;
    }

    /**
     * Convert list of entities to domain models.
     *
     * @param entities list of JPA entities
     * @return list of domain models
     */
    public static List<Branch> toDomainList(List<BranchEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(BranchMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of domain models to entities.
     *
     * @param branches list of domain models
     * @return list of JPA entities
     */
    public static List<BranchEntity> toEntityList(List<Branch> branches) {
        if (branches == null) {
            return List.of();
        }

        return branches.stream()
                .map(BranchMapper::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * Map entity type to domain type with fallback mapping.
     */
    private static BranchType mapToDomainType(BranchTypeEntity entityType) {
        if (entityType == null) {
            return BranchType.TRADITIONAL;
        }

        return switch (entityType) {
            case AGENCY -> BranchType.TRADITIONAL;
            case ATM -> BranchType.ATM_ONLY;
            case SELF_SERVICE -> BranchType.EXPRESS;
            case DIGITAL_POINT -> BranchType.DIGITAL;
        };
    }

    /**
     * Map domain type to entity type with fallback mapping.
     */
    private static BranchTypeEntity mapToEntityType(BranchType domainType) {
        if (domainType == null) {
            return BranchTypeEntity.AGENCY;
        }

        return switch (domainType) {
            case TRADITIONAL -> BranchTypeEntity.AGENCY;
            case PREMIUM -> BranchTypeEntity.AGENCY; // Premium maps to agency
            case ATM_ONLY -> BranchTypeEntity.ATM;
            case EXPRESS -> BranchTypeEntity.SELF_SERVICE;
            case DIGITAL -> BranchTypeEntity.DIGITAL_POINT;
        };
    }

    /**
     * Map entity status to domain status.
     * Maps simplified entity status to rich domain status.
     */
    private static BranchStatus mapToDomainStatus(BranchStatusEntity entityStatus) {
        if (entityStatus == null) {
            return BranchStatus.ACTIVE;
        }

        return switch (entityStatus) {
            case ACTIVE -> BranchStatus.ACTIVE;
            case INACTIVE -> BranchStatus.TEMPORARILY_CLOSED;
            case MAINTENANCE -> BranchStatus.UNDER_MAINTENANCE;
        };
    }

    /**
     * Map domain status to entity status.
     * Maps rich domain status to simplified entity status.
     */
    private static BranchStatusEntity mapToEntityStatus(BranchStatus domainStatus) {
        if (domainStatus == null) {
            return BranchStatusEntity.ACTIVE;
        }

        return switch (domainStatus) {
            case ACTIVE -> BranchStatusEntity.ACTIVE;
            case TEMPORARILY_CLOSED, PERMANENTLY_CLOSED, PLANNED -> BranchStatusEntity.INACTIVE;
            case UNDER_MAINTENANCE -> BranchStatusEntity.MAINTENANCE;
        };
    }

    /**
     * Map set of domain types to entity types for queries.
     */
    public static Set<BranchTypeEntity> mapToEntityTypes(Set<BranchType> domainTypes) {
        if (domainTypes == null) {
            return Set.of();
        }

        return domainTypes.stream()
                .map(BranchMapper::mapToEntityType)
                .collect(Collectors.toSet());
    }
}

