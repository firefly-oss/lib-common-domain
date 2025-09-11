# Enhanced Observability Features

This document describes the comprehensive observability features provided by the Firefly Common Domain library, including metrics collection, health indicators, and monitoring capabilities.

## Overview

The library provides enhanced observability through:

- **JVM Metrics** - Memory, garbage collection, and thread monitoring
- **Thread Pool Health** - Executor service and thread pool monitoring  
- **HTTP Client Metrics** - Request/response tracking and health checks
- **Application Startup Metrics** - Startup time and phase tracking
- **Cache Health Indicators** - Cache manager and cache instance monitoring
- **Domain Events Metrics** - Event publishing and consumption tracking

## Configuration

Observability features are automatically enabled when the library is on the classpath. The features are controlled through Spring Boot Actuator configuration:

```yaml
# Spring Boot Actuator configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,cqrs
  endpoint:
    health:
      show-details: always
      show-components: always
    cqrs:
      enabled: true
  health:
    # Domain Events health indicators (automatically configured)
    domainEventsApplicationEvent:
      enabled: true
    domainEventsKafka:
      enabled: true
    domainEventsRabbit:
      enabled: true
    domainEventsSqs:
      enabled: true
    domainEventsKinesis:
      enabled: true
    # CQRS health indicator (automatically configured)
    cqrs:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
```

### Domain Events Configuration

The observability features are tied to the domain events configuration:

```yaml
firefly:
  events:
    enabled: true  # Enables domain events and related health indicators
    adapter: auto  # AUTO, KAFKA, RABBIT, SQS, KINESIS, APPLICATION_EVENT, NOOP
```

## JVM Metrics

Provides comprehensive JVM monitoring including:

### Memory Metrics
- `jvm.memory.heap.used` - Used heap memory in bytes
- `jvm.memory.heap.committed` - Committed heap memory in bytes  
- `jvm.memory.heap.max` - Maximum heap memory in bytes
- `jvm.memory.heap.init` - Initial heap memory in bytes
- `jvm.memory.non_heap.used` - Used non-heap memory in bytes
- `jvm.memory.non_heap.committed` - Committed non-heap memory in bytes
- `jvm.memory.non_heap.max` - Maximum non-heap memory in bytes
- `jvm.memory.non_heap.init` - Initial non-heap memory in bytes

### Garbage Collection Metrics
- `jvm.gc.collection.count{gc=<gc_name>}` - Number of GC collections
- `jvm.gc.collection.time{gc=<gc_name>}` - Time spent in GC collections (ms)

### Thread Metrics
- `jvm.threads.live` - Current number of live threads
- `jvm.threads.daemon` - Current number of daemon threads
- `jvm.threads.peak` - Peak number of live threads
- `jvm.threads.started` - Total threads started since JVM start
- `jvm.threads.deadlocked` - Number of deadlocked threads
- `jvm.threads.deadlocked.monitor` - Number of monitor deadlocked threads

## Thread Pool Health

Monitors thread pools and executor services with detailed health information:

### Monitored Components
- Common ForkJoinPool
- All ExecutorService beans in the application context
- ThreadPoolExecutor instances
- ForkJoinPool instances

### Health Status Levels
- **UP** - Normal operation
- **DEGRADED** - High usage (>80% pool/queue utilization)
- **DOWN** - Critical usage (>95% pool/queue utilization)

### Available Information
- Pool sizes (core, maximum, current)
- Active thread counts
- Queue sizes and remaining capacity
- Task counts (total, completed)
- Thread usage percentages

## HTTP Client Metrics

Tracks HTTP client performance and provides external service health monitoring:

### Request Metrics
- `http_client_requests_total` - Total HTTP requests with labels:
  - `client_type` - RestTemplate, WebClient, etc.
  - `method` - HTTP method
  - `uri` - Sanitized URI (path parameters replaced)
  - `status` - HTTP status code
  - `status_class` - 1xx, 2xx, 3xx, 4xx, 5xx
  - `result` - success, error
  - `error_type` - Error classification

- `http_client_request_duration` - Request duration timer
- `http_client_errors_total` - Error counter by type
- `http_client_circuit_breaker_events_total` - Circuit breaker events
- `http_client_retry_attempts_total` - Retry attempt tracking
- `http_client_cache_events_total` - Cache hit/miss events

### Connection Pool Metrics
- `http_client_connections_active` - Active connections
- `http_client_connections_idle` - Idle connections
- `http_client_connections_max` - Maximum connections
- `http_client_requests_pending` - Pending requests

### Health Checks
- Configurable external service health checks
- Response time monitoring
- Automatic service status determination
- Cached health results (configurable timeout)

## Application Startup Metrics

Comprehensive startup time tracking:

### Timing Metrics
- `application.startup.duration.jvm_to_application_start` - JVM start to Spring boot
- `application.startup.duration.application_start_to_context_refresh` - Context loading time
- `application.startup.duration.context_refresh_to_started` - Bean initialization time
- `application.startup.duration.started_to_ready` - Post-processing time
- `application.startup.duration.total` - Total startup time

### Timestamp Metrics
- `application.startup.timestamp.*` - Timestamps for each startup phase
- `application.startup.phases.total` - Phase completion counters

### Custom Milestones
```java
@Autowired
private ApplicationStartupMetrics startupMetrics;

// Record custom startup milestone
startupMetrics.recordStartupMilestone("database_connected", "Database connection established");

// Record resource initialization
startupMetrics.recordResourceInitialization("database", "primary_db", 1500);

// Record startup issues
startupMetrics.recordStartupIssue("cache", "redis_connection_slow", "warning");
```

## Cache Health Indicators

Monitors Spring Cache abstraction and cache managers:

### Monitored Cache Types
- All CacheManager beans
- Individual Cache instances
- Performance testing of cache operations

### Health Checks
- Put/Get/Evict operation testing
- Performance threshold monitoring
- Cache availability verification

### Performance Thresholds
- **UP** - Normal operation (put < 1000ms, get < 500ms, total < 2000ms)
- **DEGRADED** - Slow operations (exceeding normal thresholds)
- **DOWN** - Failed operations or incorrect values

### Available Information
- Cache manager types and names
- Individual cache status
- Operation durations (put, get, evict)
- Error details and types

## Usage Examples

### Programmatic Metrics Recording

```java
@Component
public class MyService {
    
    @Autowired
    private HttpClientMetrics httpClientMetrics;
    
    @Autowired
    private ApplicationStartupMetrics startupMetrics;
    
    public void makeHttpRequest() {
        long start = System.currentTimeMillis();
        try {
            // Make HTTP request
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            Duration duration = Duration.ofMillis(System.currentTimeMillis() - start);
            
            httpClientMetrics.recordHttpRequest(
                "RestTemplate", 
                "GET", 
                url, 
                response.getStatusCodeValue(), 
                duration
            );
        } catch (Exception e) {
            Duration duration = Duration.ofMillis(System.currentTimeMillis() - start);
            httpClientMetrics.recordHttpRequestError(
                "RestTemplate", 
                "GET", 
                url, 
                e.getClass().getSimpleName(), 
                duration
            );
        }
    }
    
    @PostConstruct
    public void init() {
        startupMetrics.recordStartupMilestone("service_initialized", "MyService initialization complete");
    }
}
```

### Health Check Endpoints

Access health information through Spring Boot Actuator:

```bash
# Overall health
GET /actuator/health

# Specific health indicators
GET /actuator/health/threadPool
GET /actuator/health/httpClient
GET /actuator/health/cache
GET /actuator/health/cqrs
```

### Metrics Endpoints

Access metrics through Spring Boot Actuator:

```bash
# All metrics
GET /actuator/metrics

# Specific metric families
GET /actuator/metrics/jvm.memory.heap.used
GET /actuator/metrics/http_client_requests_total
GET /actuator/metrics/application.startup.duration.total

# CQRS-specific metrics
GET /actuator/metrics/firefly.cqrs.command.processed
GET /actuator/metrics/firefly.cqrs.command.failed
GET /actuator/metrics/firefly.cqrs.command.processing.time
GET /actuator/metrics/firefly.cqrs.query.processed
GET /actuator/metrics/firefly.cqrs.query.processing.time
```

### CQRS Metrics Endpoint

Access comprehensive CQRS framework metrics:

```bash
# Complete CQRS metrics overview
GET /actuator/cqrs

# Command processing metrics
GET /actuator/cqrs/commands

# Query processing metrics
GET /actuator/cqrs/queries

# Handler registry information
GET /actuator/cqrs/handlers

# CQRS framework health status
GET /actuator/cqrs/health
```

Example CQRS metrics response:

```json
{
  "framework": {
    "version": "2025-08",
    "uptime": "PT2H30M15S",
    "startup_time": "2025-09-11T09:15:00Z",
    "metrics_enabled": true,
    "command_metrics_enabled": true
  },
  "commands": {
    "total_processed": 1250,
    "total_failed": 15,
    "total_validation_failed": 5,
    "success_rate": 98.8,
    "failure_rate": 1.2,
    "avg_processing_time_ms": 45.2,
    "max_processing_time_ms": 1250.0,
    "by_type": {
      "CreateAccountCommand": {
        "processed": 450,
        "failed": 2,
        "avg_processing_time_ms": 35.8
      },
      "TransferFundsCommand": {
        "processed": 800,
        "failed": 13,
        "avg_processing_time_ms": 52.1
      }
    }
  },
  "queries": {
    "total_processed": 3420,
    "avg_processing_time_ms": 12.8,
    "max_processing_time_ms": 450.0,
    "cache": {
      "hits": 2890,
      "misses": 530,
      "hit_rate": 84.5
    }
  },
  "handlers": {
    "command_handlers": {
      "count": 12,
      "registered_types": [
        "CreateAccountCommand",
        "TransferFundsCommand",
        "CloseAccountCommand"
      ]
    },
    "query_handlers": {
      "count": 8,
      "registered_types": [
        "GetAccountBalanceQuery",
        "GetTransactionHistoryQuery"
      ]
    }
  },
  "health": {
    "status": "HEALTHY",
    "components": {
      "command_bus": "UP",
      "query_bus": "UP",
      "command_handler_registry": "UP",
      "meter_registry": "UP",
      "command_metrics_service": "UP"
    }
  }
}
```

## Integration with Monitoring Systems

The metrics are compatible with popular monitoring systems:

### Prometheus
All metrics are automatically exposed in Prometheus format when `micrometer-registry-prometheus` is on the classpath.

### Grafana Dashboards
Create dashboards using the provided metrics for:
- JVM performance monitoring
- HTTP client performance tracking  
- Application startup analysis
- Cache performance monitoring
- Thread pool utilization tracking

### Alerting Rules
Example Prometheus alerting rules:

```yaml
groups:
  - name: firefly-observability
    rules:
      - alert: HighJVMMemoryUsage
        expr: jvm_memory_heap_used / jvm_memory_heap_max > 0.8
        labels:
          severity: warning
          
      - alert: ThreadPoolHighUtilization  
        expr: threadpool_utilization > 0.8
        labels:
          severity: warning
          
      - alert: HTTPClientHighErrorRate
        expr: rate(http_client_requests_total{result="error"}[5m]) > 0.1
        labels:
          severity: critical
```

## Best Practices

1. **Resource Monitoring** - Monitor JVM metrics to detect memory leaks and performance issues
2. **Thread Pool Sizing** - Use thread pool metrics to optimize executor configurations
3. **HTTP Client Tuning** - Monitor connection pools and response times for external services
4. **Startup Optimization** - Track startup metrics to identify slow initialization phases
5. **Cache Performance** - Monitor cache hit rates and operation performance
6. **Alerting** - Set up alerts for critical thresholds and error rates
7. **Dashboard Creation** - Create comprehensive dashboards combining all metrics

## Troubleshooting

### Common Issues

1. **Metrics Not Appearing**
   - Verify MeterRegistry bean is available
   - Check property configuration
   - Ensure actuator dependencies are present

2. **Health Checks Failing**  
   - Verify health indicator configuration
   - Check external service connectivity
   - Review timeout settings

3. **High Memory Usage**
   - Monitor metric cardinality
   - Consider metric retention policies
   - Review cache timeout settings

### Debug Configuration

Enable debug logging for observability components:

```yaml
logging:
  level:
    com.firefly.common.domain.actuator: DEBUG
    org.springframework.boot.actuate: DEBUG
```