package com.santander.geobank.infrastructure.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * No-Operation Cache Service for development environments without Redis.
 * Provides fallback caching implementation using in-memory storage only.
 *
 * This service activates when Redis is not available, ensuring the application
 * continues to function with reduced caching capabilities.
 */
@Service
@ConditionalOnMissingBean(RedisTemplate.class)
public class NoOpCacheService {

    private static final Logger logger = LoggerFactory.getLogger(NoOpCacheService.class);

    private final Map<String, Object> localCache = new ConcurrentHashMap<>();
    private final AtomicLong requests = new AtomicLong(0);

    public NoOpCacheService() {
        logger.warn("Redis cache not available - using local in-memory cache only");
    }

    /**
     * Store value in local memory cache (not distributed)
     */
    public void put(String key, Object value) {
        localCache.put(key, value);
        logger.debug("Cached locally: {}", key);
    }

    /**
     * Retrieve value from local memory cache
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        requests.incrementAndGet();
        Object value = localCache.get(key);
        if (value != null && type.isInstance(value)) {
            logger.debug("Cache hit (local): {}", key);
            return (T) value;
        }
        logger.debug("Cache miss (local): {}", key);
        return null;
    }

    /**
     * Remove key from local cache
     */
    public void evict(String key) {
        localCache.remove(key);
        logger.debug("Evicted from local cache: {}", key);
    }

    /**
     * Clear all local cache
     */
    public void clear() {
        localCache.clear();
        logger.info("Local cache cleared - {} requests processed", requests.get());
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getStats() {
        return Map.of(
                "type", "local-memory",
                "size", localCache.size(),
                "requests", requests.get(),
                "redis_available", false);
    }
}
