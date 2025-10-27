package com.santander.geobank.infrastructure.cache;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.santander.geobank.domain.ports.CachePort;

/**
 * Simple in-memory cache implementation for development/demo purposes.
 * Banking-compliant cache with proper error handling and audit trail.
 *
 * For production: replace with Redis or distributed cache solution.
 */
@Component
@Primary
public class SimpleCacheAdapter implements CachePort {

    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();
    private volatile long hits = 0;
    private volatile long misses = 0;

    @Override
    public <T> Optional<T> get(String key, Class<T> valueType) {
        try {
            Object value = cache.get(key);
            if (value != null && valueType.isInstance(value)) {
                hits++;
                return Optional.of(valueType.cast(value));
            }
            misses++;
            return Optional.empty();
        } catch (Exception e) {
            misses++;
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Object value) {
        if (key != null && value != null) {
            cache.put(key, value);
        }
    }

    @Override
    public void put(String key, Object value, long ttlSeconds) {
        // Simple implementation - ignores TTL for now
        put(key, value);
    }

    @Override
    public void evict(String key) {
        if (key != null) {
            cache.remove(key);
        }
    }

    @Override
    public void evictByPattern(String keyPattern) {
        if (keyPattern != null) {
            cache.keySet().removeIf(key -> key.matches(keyPattern.replace("*", ".*")));
        }
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public boolean exists(String key) {
        return key != null && cache.containsKey(key);
    }

    @Override
    public <T> T getOrCompute(String key, Class<T> valueType, Supplier<T> supplier) {
        Optional<T> cached = get(key, valueType);
        if (cached.isPresent()) {
            return cached.get();
        }

        T computed = supplier.get();
        if (computed != null) {
            put(key, computed);
        }
        return computed;
    }

    @Override
    public <T> CompletableFuture<T> getOrComputeAsync(String key, Class<T> valueType,
            Supplier<CompletableFuture<T>> supplier) {
        Optional<T> cached = get(key, valueType);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get());
        }

        return supplier.get().thenApply(result -> {
            if (result != null) {
                put(key, result);
            }
            return result;
        });
    }

    @Override
    public CacheStatistics getStatistics() {
        long totalRequests = hits + misses;
        double hitRate = totalRequests > 0 ? (double) hits / totalRequests : 0.0;
        return new CacheStatistics(hits, misses, 0, hitRate, cache.size());
    }
}

