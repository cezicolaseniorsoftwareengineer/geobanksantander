package com.santander.geobank.api.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.santander.geobank.domain.ports.CachePort;

/**
 * Simple cache service following Kent Beck's principles.
 *
 * Responsibilities:
 * 1. Cache distance queries for 5 minutes
 * 2. Auto-renew cache every 15 minutes
 * 3. Provide basic metrics
 *
 * Simplicity over premature optimization.
 * * @since 1.0.0
 */
@Service
public class SimpleCacheService {

    private static final Logger logger = LoggerFactory.getLogger(SimpleCacheService.class);
    private static final int QUERY_CACHE_TTL_SECONDS = 300; // 5 minutes

    private final CachePort cachePort;
    private final CacheMetrics metrics;

    public SimpleCacheService(CachePort cachePort) {
        this.cachePort = cachePort;
        this.metrics = new CacheMetrics();
    }

    /**
     * Cache distance query result.
     * Simple: just cache it.
     */
    public void cacheDistanceQuery(String key, Object value) {
        cachePort.put(key, value, QUERY_CACHE_TTL_SECONDS);
        metrics.recordCacheMiss(); // New entry = previous miss
    }

    /**
     * Get cached result.
     * Simple: get it and track metrics.
     */
    public <T> Optional<T> getCachedResult(String key, Class<T> type) {
        var result = cachePort.get(key, type);

        if (result.isPresent()) {
            metrics.recordCacheHit();
        } else {
            metrics.recordCacheMiss();
        }

        return result;
    }

    /**
     * Invalidate distance cache.
     * Simple: clear distance patterns.
     */
    public void invalidateDistanceCache() {
        cachePort.evictByPattern("distance:*");
        logger.info("Distance cache invalidated");
    }

    /**
     * Auto-renewal every 15 minutes.
     * Simple: just clear old stuff.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void autoRenewalCache() {
        logger.info("Starting cache renewal");

        cachePort.evictByPattern("distance:*");
        metrics.recordAutoRenewal();

        logger.info("Cache renewal completed");
    }

    /**
     * Get simple metrics.
     */
    public CacheMetrics getCacheMetrics() {
        return metrics;
    }

    /**
     * Simple metrics tracking.
     * No premature optimization.
     */
    public static class CacheMetrics {
        private long hits = 0;
        private long misses = 0;
        private long renewals = 0;

        public synchronized void recordCacheHit() {
            hits++;
        }

        public synchronized void recordCacheMiss() {
            misses++;
        }

        public synchronized void recordAutoRenewal() {
            renewals++;
        }

        public synchronized double getHitRatio() {
            long total = hits + misses;
            return total > 0 ? (double) hits / total : 0.0;
        }

        public synchronized String getSummary() {
            return String.format("Hits: %d, Misses: %d, Hit Ratio: %.1f%%, Renewals: %d",
                    hits, misses, getHitRatio() * 100, renewals);
        }

        // Simple getters
        public synchronized long getHits() {
            return hits;
        }

        public synchronized long getMisses() {
            return misses;
        }

        public synchronized long getRenewals() {
            return renewals;
        }
    }
}

