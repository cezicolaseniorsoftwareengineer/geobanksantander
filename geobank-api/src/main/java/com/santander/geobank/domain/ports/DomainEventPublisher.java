package com.santander.geobank.domain.ports;

/**
 * DomainEventPublisher - Port for publishing domain events
 *
 * Defines the contract for event publishing in the domain layer.
 * Abstracts event infrastructure from domain logic.
 * */
public interface DomainEventPublisher {

    /**
     * Publish a domain event
     *
     * @param event domain event to publish
     */
    void publish(Object event);

    /**
     * Publish multiple domain events
     *
     * @param events list of domain events to publish
     */
    void publishAll(Object... events);
}

