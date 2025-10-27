package com.santander.geobank.application.usecases;

import java.util.Objects;
import java.util.UUID;

import com.santander.geobank.domain.events.BranchRegisteredEvent;
import com.santander.geobank.domain.model.Branch;
import com.santander.geobank.domain.model.BranchId;
import com.santander.geobank.domain.model.BranchStatus;
import com.santander.geobank.domain.model.BranchType;
import com.santander.geobank.domain.model.GeoPoint;
import com.santander.geobank.domain.ports.BranchRepository;
import com.santander.geobank.domain.ports.CachePort;
import com.santander.geobank.domain.ports.DomainEventPublisher;

/**
 * RegisterBranchUseCase - Use Case for registering new banking branches
 *
 * Handles the complete process of branch registration including validation,
 * persistence, cache invalidation, and event publishing.
 * */
public class RegisterBranchUseCase {

    private final BranchRepository branchRepository;
    private final DomainEventPublisher eventPublisher;
    private final CachePort cachePort;

    public RegisterBranchUseCase(BranchRepository branchRepository,
            DomainEventPublisher eventPublisher,
            CachePort cachePort) {
        this.branchRepository = Objects.requireNonNull(branchRepository, "Branch repository cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "Event publisher cannot be null");
        this.cachePort = Objects.requireNonNull(cachePort, "Cache port cannot be null");
    }

    /**
     * Execute branch registration
     *
     * @param command registration command with branch details
     * @return registered branch with generated ID
     * @throws BranchRegistrationException if registration fails
     */
    public RegisterBranchResponse execute(RegisterBranchCommand command) {
        Objects.requireNonNull(command, "Registration command cannot be null");

        try {
            // Create correlation ID for tracing
            String correlationId = UUID.randomUUID().toString();

            // Validate command
            validateCommand(command);

            // Check for duplicate locations (business rule)
            validateLocationUniqueness(command.location(), correlationId);

            // Create branch entity
            Branch branch = createBranchFromCommand(command);

            // Save to repository
            Branch savedBranch = branchRepository.save(branch);

            // Invalidate relevant caches
            invalidateCache(savedBranch.getLocation());

            // Publish domain event
            BranchRegisteredEvent event = BranchRegisteredEvent.from(savedBranch, correlationId);
            eventPublisher.publish(event);

            return new RegisterBranchResponse(
                    savedBranch.getId().toString(),
                    savedBranch.getName(),
                    savedBranch.getLocation().latitude(),
                    savedBranch.getLocation().longitude(),
                    savedBranch.getType().name(),
                    savedBranch.getAddress(),
                    savedBranch.getContactPhone().orElse(null),
                    savedBranch.getCreatedAt(),
                    correlationId);

        } catch (Exception e) {
            throw new BranchRegistrationException("Failed to register branch: " + e.getMessage(), e);
        }
    }

    /**
     * Validate registration command
     */
    private void validateCommand(RegisterBranchCommand command) {
        if (command.name() == null || command.name().trim().isEmpty()) {
            throw new BranchRegistrationException("Branch name is required");
        }

        if (command.location() == null) {
            throw new BranchRegistrationException("Branch location is required");
        }

        if (command.type() == null) {
            throw new BranchRegistrationException("Branch type is required");
        }

        if (command.address() == null || command.address().trim().isEmpty()) {
            throw new BranchRegistrationException("Branch address is required");
        }

        // Validate coordinate ranges (additional to domain validation)
        if (command.location().latitude() < -90.0 || command.location().latitude() > 90.0) {
            throw new BranchRegistrationException("Invalid latitude: must be between -90 and 90");
        }

        if (command.location().longitude() < -180.0 || command.location().longitude() > 180.0) {
            throw new BranchRegistrationException("Invalid longitude: must be between -180 and 180");
        }
    }

    /**
     * Validate that location is not too close to existing branches
     */
    private void validateLocationUniqueness(GeoPoint location, String correlationId) {
        double minimumDistanceKm = 0.1; // 100 meters minimum distance

        var nearbyBranches = branchRepository.findWithinRadius(location, minimumDistanceKm);

        if (!nearbyBranches.isEmpty()) {
            // Log validation failure for audit
            System.err.printf("Location validation failed [%s]: Branch too close to existing ones%n", correlationId);
            throw new BranchRegistrationException(
                    String.format("Another branch exists within %.1f km of this location", minimumDistanceKm));
        }
    }

    /**
     * Create branch entity from command
     */
    private Branch createBranchFromCommand(RegisterBranchCommand command) {
        BranchId branchId = command.id() != null ? BranchId.fromBranchCode(command.id()) : BranchId.generate();

        return new Branch(
                branchId,
                command.name().trim(),
                command.location(),
                command.type(),
                command.address().trim(),
                command.contactPhone() != null ? command.contactPhone().trim() : null,
                BranchStatus.ACTIVE,
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now());
    }

    /**
     * Invalidate relevant cache entries
     */
    private void invalidateCache(GeoPoint location) {
        try {
            // Invalidate general branch caches
            cachePort.evictByPattern("branches:*");
            cachePort.evictByPattern("nearest:*");

            // Invalidate location-specific caches in a radius around new branch
            // Location-based invalidation for lat: %f, lon: %f
            String locationKey = String.format("loc:%.4f:%.4f", location.latitude(), location.longitude());
            cachePort.evictByPattern(locationKey + ":*");

            for (int i = 1; i <= 50; i++) {
                cachePort.evict(String.format("distance:%d:*", i));
            }

        } catch (Exception e) {
            // Log cache invalidation failure but don't fail the registration
            System.err.println("Cache invalidation failed: " + e.getMessage());
        }
    }

    /**
     * Registration command record
     */
    public record RegisterBranchCommand(
            String id, // Optional: custom branch ID
            String name,
            GeoPoint location,
            BranchType type,
            String address,
            String contactPhone // Optional
    ) {
        public RegisterBranchCommand {
            // Defensive validation in constructor
            Objects.requireNonNull(name, "Branch name cannot be null");
            Objects.requireNonNull(location, "Branch location cannot be null");
            Objects.requireNonNull(type, "Branch type cannot be null");
            Objects.requireNonNull(address, "Branch address cannot be null");
        }

        /**
         * Create command with minimal required fields
         */
        public static RegisterBranchCommand create(String name, double latitude, double longitude,
                BranchType type, String address) {
            return new RegisterBranchCommand(
                    null, name, new GeoPoint(latitude, longitude), type, address, null);
        }

        /**
         * Create command with custom ID
         */
        public static RegisterBranchCommand createWithId(String id, String name,
                double latitude, double longitude,
                BranchType type, String address) {
            return new RegisterBranchCommand(
                    id, name, new GeoPoint(latitude, longitude), type, address, null);
        }
    }

    /**
     * Registration response record
     */
    public record RegisterBranchResponse(
            String id,
            String name,
            double latitude,
            double longitude,
            String type,
            String address,
            String contactPhone,
            java.time.LocalDateTime createdAt,
            String correlationId) {
    }

    /**
     * Branch registration exception
     */
    public static class BranchRegistrationException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public BranchRegistrationException(String message) {
            super(message);
        }

        public BranchRegistrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

