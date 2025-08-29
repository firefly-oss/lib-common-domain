package com.firefly.common.domain.actuator.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * HTTP client metrics collector for observability.
 * Tracks HTTP client calls (RestTemplate, WebClient) including success/failure rates,
 * response times, and error types.
 */
@Component
public class HttpClientMetrics {

    private final MeterRegistry meterRegistry;
    
    // Cache for counters and timers to avoid recreation
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    public HttpClientMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Records a successful HTTP client request.
     */
    public void recordHttpRequest(String clientType, String method, String uri, int statusCode, Duration duration) {
        String statusClass = getStatusClass(statusCode);
        
        // Record request counter
        getOrCreateCounter("http_client_requests_total",
                "client_type", clientType,
                "method", method,
                "uri", sanitizeUri(uri),
                "status", String.valueOf(statusCode),
                "status_class", statusClass,
                "result", isSuccessStatus(statusCode) ? "success" : "error")
                .increment();
        
        // Record request duration
        getOrCreateTimer("http_client_request_duration",
                "client_type", clientType,
                "method", method,
                "uri", sanitizeUri(uri),
                "status_class", statusClass)
                .record(duration);
    }

    /**
     * Records a failed HTTP client request.
     */
    public void recordHttpRequestError(String clientType, String method, String uri, String errorType, Duration duration) {
        // Record error counter
        getOrCreateCounter("http_client_requests_total",
                "client_type", clientType,
                "method", method,
                "uri", sanitizeUri(uri),
                "status", "unknown",
                "status_class", "error",
                "result", "error",
                "error_type", errorType)
                .increment();
        
        // Record error duration if available
        if (duration != null) {
            getOrCreateTimer("http_client_request_duration",
                    "client_type", clientType,
                    "method", method,
                    "uri", sanitizeUri(uri),
                    "status_class", "error")
                    .record(duration);
        }
        
        // Record specific error counter
        getOrCreateCounter("http_client_errors_total",
                "client_type", clientType,
                "method", method,
                "uri", sanitizeUri(uri),
                "error_type", errorType)
                .increment();
    }

    /**
     * Records HTTP connection pool metrics.
     */
    public void recordConnectionPoolMetrics(String clientType, String poolName, 
                                          int activeConnections, int idleConnections, 
                                          int maxConnections, int pendingRequests) {
        
        meterRegistry.gauge("http_client_connections_active",
                io.micrometer.core.instrument.Tags.of(
                        "client_type", clientType,
                        "pool", poolName),
                activeConnections);
        
        meterRegistry.gauge("http_client_connections_idle",
                io.micrometer.core.instrument.Tags.of(
                        "client_type", clientType,
                        "pool", poolName),
                idleConnections);
        
        meterRegistry.gauge("http_client_connections_max",
                io.micrometer.core.instrument.Tags.of(
                        "client_type", clientType,
                        "pool", poolName),
                maxConnections);
        
        meterRegistry.gauge("http_client_requests_pending",
                io.micrometer.core.instrument.Tags.of(
                        "client_type", clientType,
                        "pool", poolName),
                pendingRequests);
    }

    /**
     * Records circuit breaker metrics for HTTP clients.
     */
    public void recordCircuitBreakerEvent(String clientType, String circuitBreakerName, String event) {
        getOrCreateCounter("http_client_circuit_breaker_events_total",
                "client_type", clientType,
                "circuit_breaker", circuitBreakerName,
                "event", event)
                .increment();
    }

    /**
     * Records retry metrics for HTTP clients.
     */
    public void recordRetryAttempt(String clientType, String method, String uri, int attemptNumber, boolean successful) {
        getOrCreateCounter("http_client_retry_attempts_total",
                "client_type", clientType,
                "method", method,
                "uri", sanitizeUri(uri),
                "attempt", String.valueOf(attemptNumber),
                "result", successful ? "success" : "failure")
                .increment();
    }

    /**
     * Records cache hit/miss for HTTP client responses.
     */
    public void recordCacheEvent(String clientType, String method, String uri, String cacheResult) {
        getOrCreateCounter("http_client_cache_events_total",
                "client_type", clientType,
                "method", method,
                "uri", sanitizeUri(uri),
                "result", cacheResult)
                .increment();
    }

    private Counter getOrCreateCounter(String name, String... tags) {
        String key = buildKey(name, tags);
        return counters.computeIfAbsent(key, k -> 
                Counter.builder(name)
                        .description("HTTP Client metric: " + name)
                        .tags(tags)
                        .register(meterRegistry)
        );
    }

    private Timer getOrCreateTimer(String name, String... tags) {
        String key = buildKey(name, tags);
        return timers.computeIfAbsent(key, k ->
                Timer.builder(name)
                        .description("HTTP Client metric: " + name)
                        .tags(tags)
                        .register(meterRegistry)
        );
    }

    private String buildKey(String name, String... tags) {
        StringBuilder sb = new StringBuilder(name);
        for (int i = 0; i < tags.length; i += 2) {
            if (i + 1 < tags.length) {
                sb.append(":").append(tags[i]).append("=").append(tags[i + 1]);
            }
        }
        return sb.toString();
    }

    private String getStatusClass(int statusCode) {
        if (statusCode >= 100 && statusCode < 200) return "1xx";
        if (statusCode >= 200 && statusCode < 300) return "2xx";
        if (statusCode >= 300 && statusCode < 400) return "3xx";
        if (statusCode >= 400 && statusCode < 500) return "4xx";
        if (statusCode >= 500 && statusCode < 600) return "5xx";
        return "unknown";
    }

    private boolean isSuccessStatus(int statusCode) {
        return statusCode >= 200 && statusCode < 400;
    }

    private String sanitizeUri(String uri) {
        if (uri == null) return "unknown";
        
        // Remove query parameters
        int queryIndex = uri.indexOf('?');
        if (queryIndex != -1) {
            uri = uri.substring(0, queryIndex);
        }
        
        // Replace path parameters with placeholders
        uri = uri.replaceAll("/\\d+(?:/|$)", "/{id}/")
                 .replaceAll("/[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}(?:/|$)", "/{uuid}/");
        
        // Remove trailing slash if present
        if (uri.endsWith("/") && uri.length() > 1) {
            uri = uri.substring(0, uri.length() - 1);
        }
        
        return uri;
    }
}