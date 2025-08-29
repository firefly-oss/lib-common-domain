package com.firefly.common.domain.actuator.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Health indicator for cache systems monitoring.
 * Monitors various cache managers and cache instances for health and performance.
 */
@Component
public class CacheHealthIndicator implements HealthIndicator {

    private final ApplicationContext applicationContext;
    private final ConcurrentMap<String, CacheHealthInfo> cacheHealthCache = new ConcurrentHashMap<>();
    
    // Health check configuration
    private static final String TEST_KEY = "__health_check_key__";
    private static final String TEST_VALUE = "__health_check_value__";
    private static final long CACHE_INFO_TIMEOUT = 30000; // 30 seconds

    public CacheHealthIndicator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            Health.Builder healthBuilder = Health.up();
            boolean anyManagerDown = false;
            boolean anyManagerDegraded = false;

            // Get all cache managers from the application context
            Map<String, CacheManager> cacheManagers = applicationContext.getBeansOfType(CacheManager.class);
            
            if (cacheManagers.isEmpty()) {
                return Health.up()
                        .withDetail("message", "No cache managers found")
                        .build();
            }

            details.put("totalCacheManagers", cacheManagers.size());

            for (Map.Entry<String, CacheManager> entry : cacheManagers.entrySet()) {
                String managerName = entry.getKey();
                CacheManager cacheManager = entry.getValue();
                
                Map<String, Object> managerDetails = checkCacheManagerHealth(cacheManager);
                details.put("cacheManager_" + managerName, managerDetails);
                
                String status = (String) managerDetails.get("status");
                if ("DOWN".equals(status)) {
                    anyManagerDown = true;
                } else if ("DEGRADED".equals(status)) {
                    anyManagerDegraded = true;
                }
            }

            if (anyManagerDown) {
                healthBuilder.down();
            } else if (anyManagerDegraded) {
                healthBuilder.status("DEGRADED");
            }

            return healthBuilder.withDetails(details).build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", "Failed to check cache health: " + e.getMessage())
                    .build();
        }
    }

    private Map<String, Object> checkCacheManagerHealth(CacheManager cacheManager) {
        Map<String, Object> managerDetails = new HashMap<>();
        
        try {
            managerDetails.put("type", cacheManager.getClass().getSimpleName());
            
            // Get all cache names
            java.util.Collection<String> cacheNames = cacheManager.getCacheNames();
            managerDetails.put("totalCaches", cacheNames.size());
            managerDetails.put("cacheNames", cacheNames);
            
            if (cacheNames.isEmpty()) {
                managerDetails.put("status", "UP");
                managerDetails.put("message", "Cache manager is up but no caches configured");
                return managerDetails;
            }
            
            Map<String, Object> cacheDetails = new HashMap<>();
            boolean anyCacheDown = false;
            boolean anyCacheDegraded = false;
            
            for (String cacheName : cacheNames) {
                try {
                    Cache cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        CacheHealthInfo healthInfo = checkCacheHealth(cache);
                        cacheDetails.put(cacheName, healthInfo.toMap());
                        
                        if (healthInfo.getStatus() == CacheStatus.DOWN) {
                            anyCacheDown = true;
                        } else if (healthInfo.getStatus() == CacheStatus.DEGRADED) {
                            anyCacheDegraded = true;
                        }
                    } else {
                        cacheDetails.put(cacheName, Map.of("status", "DOWN", "message", "Cache is null"));
                        anyCacheDown = true;
                    }
                } catch (Exception e) {
                    cacheDetails.put(cacheName, Map.of(
                            "status", "DOWN", 
                            "message", "Error checking cache: " + e.getMessage(),
                            "error", e.getClass().getSimpleName()
                    ));
                    anyCacheDown = true;
                }
            }
            
            managerDetails.put("caches", cacheDetails);
            
            // Determine overall manager status
            if (anyCacheDown) {
                managerDetails.put("status", "DOWN");
            } else if (anyCacheDegraded) {
                managerDetails.put("status", "DEGRADED");
            } else {
                managerDetails.put("status", "UP");
            }
            
        } catch (Exception e) {
            managerDetails.put("status", "DOWN");
            managerDetails.put("message", "Error checking cache manager: " + e.getMessage());
            managerDetails.put("error", e.getClass().getSimpleName());
        }
        
        return managerDetails;
    }

    private CacheHealthInfo checkCacheHealth(Cache cache) {
        String cacheName = cache.getName();
        String cacheKey = cacheName + "_health";
        
        // Check if we have recent cached health info
        CacheHealthInfo cachedInfo = cacheHealthCache.get(cacheKey);
        if (cachedInfo != null && cachedInfo.isValid()) {
            return cachedInfo;
        }
        
        CacheHealthInfo healthInfo = performCacheHealthCheck(cache);
        cacheHealthCache.put(cacheKey, healthInfo);
        return healthInfo;
    }

    private CacheHealthInfo performCacheHealthCheck(Cache cache) {
        String cacheName = cache.getName();
        long startTime = System.currentTimeMillis();
        
        try {
            // Test basic cache operations
            long putStartTime = System.currentTimeMillis();
            cache.put(TEST_KEY, TEST_VALUE);
            long putDuration = System.currentTimeMillis() - putStartTime;
            
            long getStartTime = System.currentTimeMillis();
            Cache.ValueWrapper valueWrapper = cache.get(TEST_KEY);
            long getDuration = System.currentTimeMillis() - getStartTime;
            
            // Verify the value
            boolean valueCorrect = valueWrapper != null && TEST_VALUE.equals(valueWrapper.get());
            
            // Clean up test data
            cache.evict(TEST_KEY);
            long evictDuration = System.currentTimeMillis() - System.currentTimeMillis();
            
            long totalDuration = System.currentTimeMillis() - startTime;
            
            // Determine status based on performance
            CacheStatus status = determineStatus(putDuration, getDuration, valueCorrect, totalDuration);
            
            return new CacheHealthInfo(
                    cacheName,
                    status,
                    totalDuration,
                    putDuration,
                    getDuration,
                    evictDuration,
                    valueCorrect,
                    "Cache operations completed successfully",
                    null,
                    System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            
            return new CacheHealthInfo(
                    cacheName,
                    CacheStatus.DOWN,
                    totalDuration,
                    0,
                    0,
                    0,
                    false,
                    "Cache operation failed: " + e.getMessage(),
                    e.getClass().getSimpleName(),
                    System.currentTimeMillis()
            );
        }
    }

    private CacheStatus determineStatus(long putDuration, long getDuration, boolean valueCorrect, long totalDuration) {
        if (!valueCorrect) {
            return CacheStatus.DOWN;
        }
        
        // Consider cache degraded if operations are slow
        if (putDuration > 1000 || getDuration > 500 || totalDuration > 2000) {
            return CacheStatus.DEGRADED;
        }
        
        return CacheStatus.UP;
    }

    // Supporting classes and enums

    public enum CacheStatus {
        UP, DEGRADED, DOWN
    }

    public static class CacheHealthInfo {
        private final String cacheName;
        private final CacheStatus status;
        private final long totalDuration;
        private final long putDuration;
        private final long getDuration;
        private final long evictDuration;
        private final boolean valueCorrect;
        private final String message;
        private final String errorType;
        private final long timestamp;

        public CacheHealthInfo(String cacheName, CacheStatus status, long totalDuration,
                             long putDuration, long getDuration, long evictDuration,
                             boolean valueCorrect, String message, String errorType, long timestamp) {
            this.cacheName = cacheName;
            this.status = status;
            this.totalDuration = totalDuration;
            this.putDuration = putDuration;
            this.getDuration = getDuration;
            this.evictDuration = evictDuration;
            this.valueCorrect = valueCorrect;
            this.message = message;
            this.errorType = errorType;
            this.timestamp = timestamp;
        }

        public boolean isValid() {
            return System.currentTimeMillis() - timestamp < CACHE_INFO_TIMEOUT;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("status", status.name());
            map.put("totalDuration", totalDuration + "ms");
            map.put("putDuration", putDuration + "ms");
            map.put("getDuration", getDuration + "ms");
            map.put("evictDuration", evictDuration + "ms");
            map.put("valueCorrect", valueCorrect);
            map.put("message", message);
            if (errorType != null) {
                map.put("errorType", errorType);
            }
            map.put("lastChecked", new java.util.Date(timestamp));
            return map;
        }

        // Getters
        public CacheStatus getStatus() { return status; }
        public String getMessage() { return message; }
    }
}