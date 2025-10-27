package com.santander.geobank.infrastructure.monitoring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;

/**
 * Observability configuration for distributed tracing and metrics.
 * Implements OpenTelemetry standards for banking-grade monitoring.
 *
 * Observability Pillars:
 * - Metrics: RED (Rate, Errors, Duration) and USE (Utilization, Saturation,
 * Errors)
 * - Traces: Distributed request tracing across microservices
 * - Logs: Structured logging with correlation IDs
 *
 * Integration:
 * - Micrometer: Unified metrics facade
 * - OpenTelemetry: CNCF standard for observability
 * - Prometheus: Metrics storage and alerting
 * - Grafana: Visualization and dashboards
 * - Zipkin: Distributed tracing backend
 *
 * Compliance:
 * - PCI DSS 10.6: Review logs and security events
 * - SOX: Audit trail for financial transactions
 * - SLA monitoring: P50, P95, P99 latency tracking
 *
 * @author Platform Engineering Team
 * @since 1.0.0
 */
@Configuration
@EnableAspectJAutoProxy
public class ObservabilityConfiguration {

    /**
     * Enable @Observed annotation support for automatic instrumentation.
     * Provides AOP-based tracing without code pollution.
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}
