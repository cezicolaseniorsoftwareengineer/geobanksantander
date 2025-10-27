package com.santander.geobank.infrastructure.monitoring;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Custom metrics provider for banking-specific operations.
 * Implements RED (Rate, Errors, Duration) metrics pattern.
 *
 * Metric Categories:
 * - Business metrics: Branch operations, proximity searches
 * - Technical metrics: Cache hits, database queries
 * - Security metrics: Authentication failures, rate limits
 * - Performance metrics: P50, P95, P99 latencies
 *
 * Naming Convention:
 * - geobank.<category>.<metric>.<unit>
 * - Example: geobank.branch.proximity.search.duration
 *
 * Tags Strategy:
 * - environment: production, staging, development
 * - service: geobank-api
 * - operation: find_nearest_branches, create_branch
 * - status: success, error, timeout
 *
 * @author Platform Engineering Team
 * @since 1.0.0
 */
@Component
public class MetricsService {

    private final MeterRegistry registry;
    private final ConcurrentMap<String, Timer> timers;
    private final ConcurrentMap<String, Counter> counters;

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
        this.timers = new ConcurrentHashMap<>();
        this.counters = new ConcurrentHashMap<>();

        initializeMetrics();
    }

    /**
     * Initialize core banking metrics.
     */
    private void initializeMetrics() {
        // Branch operation metrics
        createTimer("geobank.branch.proximity.search", "Proximity search duration");
        createTimer("geobank.branch.create", "Branch creation duration");
        createTimer("geobank.branch.update", "Branch update duration");
        createTimer("geobank.branch.delete", "Branch deletion duration");

        // Cache metrics
        createCounter("geobank.cache.hit", "Cache hit count");
        createCounter("geobank.cache.miss", "Cache miss count");
        createCounter("geobank.cache.eviction", "Cache eviction count");

        // Database metrics
        createTimer("geobank.db.query", "Database query duration");
        createCounter("geobank.db.connection.acquired", "DB connections acquired");
        createCounter("geobank.db.connection.timeout", "DB connection timeouts");

        // Security metrics
        createCounter("geobank.auth.success", "Successful authentications");
        createCounter("geobank.auth.failure", "Failed authentications");
        createCounter("geobank.ratelimit.exceeded", "Rate limit exceeded");

        // Business metrics
        createCounter("geobank.branch.registered", "Branches registered");
        createCounter("geobank.search.performed", "Proximity searches performed");
    }

    /**
     * Record proximity search operation.
     */
    public void recordProximitySearch(Duration duration, boolean success, int resultsCount) {
        Timer timer = getTimer("geobank.branch.proximity.search");
        timer.record(duration);

        incrementCounter("geobank.search.performed");

        registry.gauge("geobank.search.results.count", resultsCount);

        if (success) {
            incrementCounterWithTags("geobank.operation.status", "status", "success");
        } else {
            incrementCounterWithTags("geobank.operation.status", "status", "error");
        }
    }

    /**
     * Record cache operation.
     */
    public void recordCacheOperation(String operation, boolean hit) {
        if (hit) {
            incrementCounter("geobank.cache.hit");
        } else {
            incrementCounter("geobank.cache.miss");
        }

        incrementCounterWithTags("geobank.cache.operation", "type", operation);
    }

    /**
     * Record database query.
     */
    public void recordDatabaseQuery(String queryType, Duration duration, boolean success) {
        Timer timer = getTimer("geobank.db.query");
        timer.record(duration);

        if (!success) {
            incrementCounterWithTags("geobank.db.error", "type", queryType);
        }
    }

    /**
     * Record authentication attempt.
     */
    public void recordAuthenticationAttempt(boolean success, String reason) {
        if (success) {
            incrementCounter("geobank.auth.success");
        } else {
            incrementCounter("geobank.auth.failure");
            incrementCounterWithTags("geobank.auth.failure.reason", "reason", reason);
        }
    }

    /**
     * Record branch registration.
     */
    public void recordBranchRegistration(String branchType, boolean success) {
        if (success) {
            incrementCounter("geobank.branch.registered");
            incrementCounterWithTags("geobank.branch.type", "type", branchType);
        }
    }

    /**
     * Record rate limiting event.
     */
    public void recordRateLimitExceeded(String endpoint) {
        incrementCounter("geobank.ratelimit.exceeded");
        incrementCounterWithTags("geobank.ratelimit.endpoint", "endpoint", endpoint);
    }

    /**
     * Get or create timer.
     */
    private Timer getTimer(String name) {
        return timers.computeIfAbsent(name, key -> Timer.builder(key)
                .description("Timer for " + key)
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry));
    }

    /**
     * Get or create counter.
     */
    private Counter getCounter(String name) {
        return counters.computeIfAbsent(name, key -> Counter.builder(key)
                .description("Counter for " + key)
                .register(registry));
    }

    /**
     * Create timer with description.
     */
    private void createTimer(String name, String description) {
        timers.put(name,
                Timer.builder(name)
                        .description(description)
                        .publishPercentiles(0.5, 0.95, 0.99)
                        .publishPercentileHistogram()
                        .register(registry));
    }

    /**
     * Create counter with description.
     */
    private void createCounter(String name, String description) {
        counters.put(name,
                Counter.builder(name)
                        .description(description)
                        .register(registry));
    }

    /**
     * Increment counter.
     */
    private void incrementCounter(String name) {
        Counter counter = getCounter(name);
        counter.increment();
    }

    /**
     * Increment counter with tags.
     */
    private void incrementCounterWithTags(String name, String tagKey, String tagValue) {
        Counter.builder(name)
                .tag(tagKey, tagValue)
                .register(registry)
                .increment();
    }

    /**
     * Get current cache hit ratio.
     */
    public double getCacheHitRatio() {
        double hits = getCounter("geobank.cache.hit").count();
        double misses = getCounter("geobank.cache.miss").count();

        if (hits + misses == 0) {
            return 0.0;
        }

        return hits / (hits + misses);
    }

    /**
     * Get authentication success rate.
     */
    public double getAuthenticationSuccessRate() {
        double success = getCounter("geobank.auth.success").count();
        double failure = getCounter("geobank.auth.failure").count();

        if (success + failure == 0) {
            return 0.0;
        }

        return success / (success + failure);
    }
}
