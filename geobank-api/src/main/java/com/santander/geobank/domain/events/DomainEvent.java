package com.santander.geobank.domain.events;

import java.time.Instant;

/**
 * Base interface for all domain events in the banking system.
 * Implements event sourcing pattern for audit trail and state reconstruction.
 *
 * Contract:
 * - All events are immutable (use records or final fields)
 * - Events represent facts that happened in the past (past tense naming)
 * - Events contain all data needed to reconstruct state
 * - Events are versioned for schema evolution
 *
 * Compliance:
 * - PCI DSS Requirement 10: Immutable audit log
 * - SOX: Financial transaction traceability
 * - GDPR Article 15: Right to explanation
 *
 * @author Banking Engineering Team
 * @since 1.0.0
 */
public interface DomainEvent {

    /**
     * Get unique event identifier for idempotency.
     */
    String getEventId();

    /**
     * Get event type identifier for routing and processing.
     */
    String getEventType();

    /**
     * Get aggregate identifier this event belongs to.
     */
    String getAggregateId();

    /**
     * Get aggregate type (Branch, Account, Transaction, etc.).
     */
    String getAggregateType();

    /**
     * Get event schema version for evolution support.
     */
    String getVersion();

    /**
     * Get timestamp when event occurred.
     */
    Instant getTimestamp();

    /**
     * Get correlation ID for distributed tracing.
     */
    String getCorrelationId();

    /**
     * Get user ID who triggered this event.
     */
    String getUserId();

    /**
     * Set correlation ID (called by infrastructure).
     */
    void setCorrelationId(String correlationId);

    /**
     * Set user ID (called by infrastructure).
     */
    void setUserId(String userId);

    /**
     * Set timestamp (called by infrastructure).
     */
    void setTimestamp(Instant timestamp);

    /**
     * Check if this event requires synchronous processing.
     */
    default boolean isCritical() {
        return false;
    }

    /**
     * Check if this event should be persisted to event store.
     */
    default boolean isPersistent() {
        return true;
    }
}
