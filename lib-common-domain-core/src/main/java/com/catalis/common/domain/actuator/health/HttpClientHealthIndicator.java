package com.catalis.common.domain.actuator.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Health indicator for HTTP client connectivity to external services.
 * Monitors the health and connectivity of configured external services.
 */
@Component
@ConfigurationProperties(prefix = "firefly.health.http-client")
public class HttpClientHealthIndicator implements HealthIndicator {

    private final WebClient webClient;
    private final ConcurrentMap<String, ServiceHealthInfo> serviceHealthCache = new ConcurrentHashMap<>();
    
    // Configuration properties
    private List<ServiceEndpoint> endpoints = List.of();
    private Duration timeout = Duration.ofSeconds(5);
    private Duration cacheTimeout = Duration.ofMinutes(1);
    private boolean enabled = true;

    public HttpClientHealthIndicator() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024))
                .build();
    }

    @Override
    public Health health() {
        if (!enabled) {
            return Health.up().withDetail("message", "HTTP client health checks disabled").build();
        }

        try {
            Map<String, Object> details = new HashMap<>();
            Health.Builder healthBuilder = Health.up();
            boolean anyServiceDown = false;
            boolean anyServiceDegraded = false;

            for (ServiceEndpoint endpoint : endpoints) {
                ServiceHealthInfo healthInfo = checkServiceHealth(endpoint);
                details.put("service_" + endpoint.getName(), healthInfo.toMap());
                
                if (healthInfo.getStatus() == ServiceStatus.DOWN) {
                    anyServiceDown = true;
                } else if (healthInfo.getStatus() == ServiceStatus.DEGRADED) {
                    anyServiceDegraded = true;
                }
            }

            if (anyServiceDown) {
                healthBuilder.down();
            } else if (anyServiceDegraded) {
                healthBuilder.status("DEGRADED");
            }

            details.put("totalServices", endpoints.size());
            details.put("timeout", timeout.toString());
            
            return healthBuilder.withDetails(details).build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", "Failed to check HTTP client health: " + e.getMessage())
                    .build();
        }
    }

    private ServiceHealthInfo checkServiceHealth(ServiceEndpoint endpoint) {
        String cacheKey = endpoint.getName();
        ServiceHealthInfo cachedInfo = serviceHealthCache.get(cacheKey);
        
        // Return cached result if still valid
        if (cachedInfo != null && cachedInfo.isValid(cacheTimeout)) {
            return cachedInfo;
        }

        ServiceHealthInfo healthInfo = performHealthCheck(endpoint);
        serviceHealthCache.put(cacheKey, healthInfo);
        return healthInfo;
    }

    private ServiceHealthInfo performHealthCheck(ServiceEndpoint endpoint) {
        try {
            long startTime = System.currentTimeMillis();
            
            String response = webClient.get()
                    .uri(endpoint.getHealthCheckUrl())
                    .headers(headers -> {
                        if (endpoint.getHeaders() != null) {
                            endpoint.getHeaders().forEach(headers::add);
                        }
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeout)
                    .block();

            long responseTime = System.currentTimeMillis() - startTime;
            
            ServiceStatus status = determineStatus(responseTime, endpoint.getMaxResponseTime());
            
            return new ServiceHealthInfo(
                    endpoint.getName(),
                    status,
                    responseTime,
                    "Service responded successfully",
                    response != null ? response.length() : 0,
                    null,
                    System.currentTimeMillis()
            );

        } catch (WebClientResponseException e) {
            long responseTime = System.currentTimeMillis() - System.currentTimeMillis();
            
            return new ServiceHealthInfo(
                    endpoint.getName(),
                    ServiceStatus.DOWN,
                    responseTime,
                    "HTTP error: " + e.getStatusCode() + " - " + e.getMessage(),
                    0,
                    e.getClass().getSimpleName(),
                    System.currentTimeMillis()
            );

        } catch (Exception e) {
            return new ServiceHealthInfo(
                    endpoint.getName(),
                    ServiceStatus.DOWN,
                    0,
                    "Connection error: " + e.getMessage(),
                    0,
                    e.getClass().getSimpleName(),
                    System.currentTimeMillis()
            );
        }
    }

    private ServiceStatus determineStatus(long responseTime, long maxResponseTime) {
        if (responseTime <= maxResponseTime) {
            return ServiceStatus.UP;
        } else if (responseTime <= maxResponseTime * 2) {
            return ServiceStatus.DEGRADED;
        } else {
            return ServiceStatus.DOWN;
        }
    }

    // Configuration classes and enums

    public static class ServiceEndpoint {
        private String name;
        private String healthCheckUrl;
        private Map<String, String> headers;
        private long maxResponseTime = 1000; // 1 second default

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getHealthCheckUrl() { return healthCheckUrl; }
        public void setHealthCheckUrl(String healthCheckUrl) { this.healthCheckUrl = healthCheckUrl; }
        
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        
        public long getMaxResponseTime() { return maxResponseTime; }
        public void setMaxResponseTime(long maxResponseTime) { this.maxResponseTime = maxResponseTime; }
    }

    public enum ServiceStatus {
        UP, DEGRADED, DOWN
    }

    public static class ServiceHealthInfo {
        private final String serviceName;
        private final ServiceStatus status;
        private final long responseTime;
        private final String message;
        private final int responseSize;
        private final String errorType;
        private final long timestamp;

        public ServiceHealthInfo(String serviceName, ServiceStatus status, long responseTime, 
                               String message, int responseSize, String errorType, long timestamp) {
            this.serviceName = serviceName;
            this.status = status;
            this.responseTime = responseTime;
            this.message = message;
            this.responseSize = responseSize;
            this.errorType = errorType;
            this.timestamp = timestamp;
        }

        public boolean isValid(Duration cacheTimeout) {
            return System.currentTimeMillis() - timestamp < cacheTimeout.toMillis();
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("status", status.name());
            map.put("responseTime", responseTime + "ms");
            map.put("message", message);
            map.put("responseSize", responseSize);
            if (errorType != null) {
                map.put("errorType", errorType);
            }
            map.put("lastChecked", new java.util.Date(timestamp));
            return map;
        }

        // Getters
        public ServiceStatus getStatus() { return status; }
        public long getResponseTime() { return responseTime; }
        public String getMessage() { return message; }
    }

    // Configuration property setters
    public void setEndpoints(List<ServiceEndpoint> endpoints) {
        this.endpoints = endpoints != null ? endpoints : List.of();
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public void setCacheTimeout(Duration cacheTimeout) {
        this.cacheTimeout = cacheTimeout;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // Getters for configuration
    public List<ServiceEndpoint> getEndpoints() { return endpoints; }
    public Duration getTimeout() { return timeout; }
    public Duration getCacheTimeout() { return cacheTimeout; }
    public boolean isEnabled() { return enabled; }
}