package com.santander.geobank.infrastructure.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.santander.geobank.domain.events.DomainEvent;
import com.santander.geobank.domain.ports.DomainEventPublisher;

/**
 * No-Operation Event Publisher for development environments without Kafka.
 * Provides fallback implementation when EventPublisher is not available.
 *
 * Logs events instead of publishing to maintain audit trail visibility
 * during development and testing phases.
 */
@Service
@ConditionalOnMissingBean(KafkaTemplate.class)
public class NoOpEventPublisher implements DomainEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NoOpEventPublisher.class);

    @Override
    public void publish(Object event) {
        if (event instanceof DomainEvent) {
            DomainEvent domainEvent = (DomainEvent) event;
            logger.warn("Event publishing disabled (Kafka not available): {} - {}",
                    domainEvent.getEventType(), domainEvent.getAggregateId());
            logger.debug("Event details: {}", event.toString());
        } else {
            logger.warn("Event publishing disabled (Kafka not available): {}",
                    event.getClass().getSimpleName());
        }
    }

    @Override
    public void publishAll(Object... events) {
        logger.warn("Batch event publishing disabled (Kafka not available): {} events", events.length);
        for (Object event : events) {
            publish(event);
        }
    }
}
