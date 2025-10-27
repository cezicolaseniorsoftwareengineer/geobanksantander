package com.santander.geobank.domain.ports;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * CachePort - Port for caching operations
 *
 * Defines the contract for cache operations in the domain layer.
 * Abstracts cache technology from domain logic.
 * */
public interface CachePort {

    /**
     * Get value from cache
     *
     * @param key       cache key
     * @param valueType expected value type
     * @return optional cached value
     */
    <T> Optional<T> get(String key, Class<T> valueType);

    /**
     * Put value in cache with default TTL
     *
     * @param key   cache key
     * @param value value to cache
     */
    void put(String key, Object value);

    /**
     * Put value in cache with specific TTL
     *
     * @param key        cache key
     * @param value      value to cache
     * @param ttlSeconds time to live in seconds
     */
    void put(String key, Object value, long ttlSeconds);

    /**
     * Remove value from cache
     *
     * @param key cache key
     */
    void evict(String key);

    /**
     * Remove all values with key pattern
     *
     * @param keyPattern key pattern (supports wildcards)
     */
    void evictByPattern(String keyPattern);

    /**
     * Clear all cache entries
     */
    void clear();

    /**
     * Check if key exists in cache
     *
     * @param key cache key
     * @return true if key exists
     */
    boolean exists(String key);

    /**
     * Get value from cache or compute if missing
     *
     * @param key       cache key
     * @param valueType expected value type
     * @param supplier  function to compute value if missing
     * @return cached or computed value
     */
    <T> T getOrCompute(String key, Class<T> valueType, java.util.function.Supplier<T> supplier);

    /**
     * Get value from cache or compute asynchronously if missing
     *
     * @param key       cache key
     * @param valueType expected value type
     * @param supplier  function to compute value if missing
     * @return future of cached or computed value
     */
    <T> CompletableFuture<T> getOrComputeAsync(String key, Class<T> valueType,
            java.util.function.Supplier<CompletableFuture<T>> supplier);

    /**
     * Get cache statistics
     *
     * @return cache statistics
     */
    CacheStatistics getStatistics();

    /**
     * Cache statistics record
     */
    record CacheStatistics(
            long hitCount,
            long missCount,
            long evictionCount,
            double hitRate,
            long size) {
    }
}

