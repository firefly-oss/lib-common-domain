# Configuration Guide

This document provides a comprehensive guide to configuring the Firefly Common Domain Library for different environments and use cases.

## Table of Contents

- [Overview](#overview)
- [Auto-Configuration](#auto-configuration)
- [CQRS Configuration](#cqrs-configuration)
- [Domain Events Configuration](#domain-events-configuration)
- [ServiceClient Configuration](#serviceclient-configuration)
- [Observability Configuration](#observability-configuration)
- [Environment-Specific Configurations](#environment-specific-configurations)
- [Security Configuration](#security-configuration)

## Overview

The Firefly Common Domain Library uses Spring Boot's auto-configuration mechanism to provide sensible defaults while allowing extensive customization through application properties.

### Configuration Hierarchy

1. **Auto-Configuration Defaults**: Built-in sensible defaults
2. **Application Properties**: YAML/Properties file configuration
3. **Environment Variables**: Runtime environment overrides
4. **Programmatic Configuration**: Bean-based configuration

### Main Configuration Properties

```yaml
firefly:
  # CQRS Framework
  cqrs:
    enabled: true
    query:
      cache:
        enabled: true
        default-ttl: 300

  # Domain Events
  events:
    enabled: true
    adapter: auto

  # StepEvents Integration (lib-transactional-engine)
  stepevents:
    enabled: true

  # ServiceClient Framework
  service-client:
    enabled: true

# Domain topic configuration for step events
domain:
  topic: banking-domain-events
```

## Auto-Configuration

### Auto-Configuration

The framework uses Spring Boot's standard auto-configuration mechanism:

```java
@SpringBootApplication
public class BankingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingServiceApplication.class, args);
    }
}
```

Components are automatically configured through these auto-configuration classes:
- `CqrsAutoConfiguration` - CQRS framework components (CommandBus, QueryBus, CacheManager)
- `DomainEventsAutoConfiguration` - Domain events and messaging adapters (auto-detection)
- `ServiceClientAutoConfiguration` - ServiceClient framework (WebClient, Circuit Breakers, Retry)
- `StepBridgeConfiguration` - StepEvents integration with lib-transactional-engine
- `DomainEventsActuatorAutoConfiguration` - Observability features (metrics, health indicators)
- `JsonLoggingAutoConfiguration` - Structured JSON logging configuration

### Selective Component Enablement

You can disable specific components using configuration properties:

```yaml
firefly:
  cqrs:
    enabled: false  # Disable CQRS framework
  events:
    enabled: false  # Disable Domain Events
  stepevents:
    enabled: false  # Disable StepEvents bridge
  service-client:
    enabled: false  # Disable ServiceClient framework
```

### Manual Configuration Requirements

Most components are automatically configured, but some scenarios require manual configuration:

#### 1. Custom CacheManager for CQRS Queries

```java
@Configuration
public class CustomCacheConfiguration {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        // Custom cache manager implementation
        RedisCacheManager.Builder builder = RedisCacheManager
            .RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory())
            .cacheDefaults(cacheConfiguration());
        return builder.build();
    }
}
```

#### 2. Custom CorrelationContext Implementation

```java
@Configuration
public class CorrelationConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CorrelationContext correlationContext() {
        // Custom correlation context implementation
        return new CustomCorrelationContext();
    }
}
```

#### 3. External Messaging Infrastructure

For production deployments, you need to configure external messaging infrastructure:

```yaml
# Kafka Configuration
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

# RabbitMQ Configuration
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

# AWS Configuration
cloud:
  aws:
    region:
      static: us-east-1
    credentials:
      access-key: your-access-key
      secret-key: your-secret-key
```



## StepEvents Configuration

StepEvents integration provides seamless connectivity with lib-transactional-engine for saga orchestration.

### Basic Configuration

```yaml
firefly:
  stepevents:
    enabled: true  # Enable StepEvents bridge (default: true)

# Optional: Configure default topic for step events
domain:
  topic: banking-step-events  # Default: "domain-events"
```

### StepEvents Bridge

The `StepEventPublisherBridge` automatically converts StepEvents from lib-transactional-engine into DomainEvents:

```java
@Autowired
private StepEventPublisherBridge stepEventBridge;

public Mono<Void> publishSagaStep(String sagaName, String sagaId, Object payload) {
    StepEventEnvelope stepEvent = new StepEventEnvelope();
    stepEvent.setSagaName(sagaName);
    stepEvent.setSagaId(sagaId);
    stepEvent.setType("saga.step.completed");
    stepEvent.setPayload(payload);
    stepEvent.setResultType("SUCCESS");

    return stepEventBridge.publish(stepEvent);
}
```

### Metadata Enrichment

Step events are automatically enriched with execution metadata:
- `step.attempts` - Number of execution attempts
- `step.latency_ms` - Execution latency in milliseconds
- `step.started_at` - Step start timestamp
- `step.completed_at` - Step completion timestamp
- `step.result_type` - Execution result (SUCCESS, FAILURE, etc.)

## CQRS Configuration

### Basic CQRS Configuration

```yaml
firefly:
  cqrs:
    enabled: true

    # Command processing
    command:
      timeout: 30s              # Default: 30s
      metrics-enabled: true     # Default: true
      tracing-enabled: true     # Default: true

    # Query processing
    query:
      timeout: 15s              # Default: 15s
      caching-enabled: true     # Default: true
      cache-ttl: 15m            # Default: 15 minutes
      metrics-enabled: true     # Default: true
      tracing-enabled: true     # Default: true
```

### Complete CQRS Configuration

```yaml
firefly:
  cqrs:
    enabled: true

    # Command processing configuration
    command:
      timeout: 60s              # Command processing timeout
      metrics-enabled: true     # Enable command metrics collection
      tracing-enabled: true     # Enable command tracing

    # Query processing configuration
    query:
      timeout: 15s              # Query processing timeout
      caching-enabled: true     # Enable query result caching
      cache-ttl: 15m            # Cache time-to-live
      metrics-enabled: true     # Enable query metrics collection
      tracing-enabled: true     # Enable query tracing
```

## Domain Events Configuration

### Adapter Selection

```yaml
firefly:
  events:
    enabled: true
    adapter: auto  # AUTO, KAFKA, RABBIT, SQS, KINESIS, APPLICATION_EVENT, NOOP
```

### Kafka Configuration

```yaml
firefly:
  events:
    adapter: kafka
    kafka:
      # Connection
      bootstrap-servers: localhost:9092,localhost:9093
      template-bean-name: kafkaTemplate
      
      # Serialization
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      
      # Performance
      retries: 3
      batch-size: 16384
      linger-ms: 5
      buffer-memory: 33554432
      acks: "all"
      
      # Security
      properties:
        security.protocol: SSL
        ssl.truststore.location: /etc/ssl/kafka.truststore.jks
        ssl.truststore.password: ${KAFKA_TRUSTSTORE_PASSWORD}
        ssl.keystore.location: /etc/ssl/kafka.keystore.jks
        ssl.keystore.password: ${KAFKA_KEYSTORE_PASSWORD}
        
    # Consumer configuration
    consumer:
      enabled: true
      kafka:
        topics:
          - banking.accounts
          - banking.transactions
          - banking.customers
        group-id: banking-service
        auto-offset-reset: earliest
        enable-auto-commit: false
```

### RabbitMQ Configuration

```yaml
spring:
  rabbitmq:
    host: rabbitmq-cluster
    port: 5672
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
    virtual-host: /banking
    connection-timeout: 60000
    publisher-confirm-type: correlated
    publisher-returns: true
    
firefly:
  events:
    adapter: rabbit
    rabbit:
      exchange: "banking.events"
      routing-key: "${type}.${key}"  # Supports placeholders
      
    consumer:
      enabled: true
      rabbit:
        queues:
          - banking.accounts.queue
          - banking.transactions.queue
        dead-letter-exchange: banking.events.dlx
        retry-attempts: 3
```

### AWS SQS Configuration

```yaml
aws:
  region: us-east-1
  credentials:
    access-key: ${AWS_ACCESS_KEY_ID}
    secret-key: ${AWS_SECRET_ACCESS_KEY}
    
firefly:
  events:
    adapter: sqs
    sqs:
      queue-url: https://sqs.us-east-1.amazonaws.com/123456789012/banking-events
      # OR queue-name: banking-events
      
    consumer:
      enabled: true
      sqs:
        queue-url: https://sqs.us-east-1.amazonaws.com/123456789012/banking-events-consumer
        wait-time-seconds: 20
        max-messages: 10
        poll-delay-millis: 1000
        visibility-timeout-seconds: 30
```

### AWS Kinesis Configuration

```yaml
aws:
  region: us-east-1
  credentials:
    access-key: ${AWS_ACCESS_KEY_ID}
    secret-key: ${AWS_SECRET_ACCESS_KEY}
    
firefly:
  events:
    adapter: kinesis
    kinesis:
      stream-name: banking-events-stream
      partition-key: "${key}"
      
    consumer:
      enabled: true
      kinesis:
        stream-name: banking-events-stream
        application-name: banking-service
        initial-position: LATEST
        checkpoint-interval: 60s
        poll-delay-millis: 1000
```

## ServiceClient Configuration

### Global ServiceClient Configuration

```yaml
firefly:
  service-client:
    enabled: true
    
    # REST client defaults
    rest:
      max-connections: 100
      max-idle-time: 5m
      max-life-time: 30m
      pending-acquire-timeout: 10s
      response-timeout: 30s
      connect-timeout: 10s
      max-in-memory-size: 1048576  # 1MB
      
    # gRPC client defaults
    grpc:
      keep-alive-time: 5m
      keep-alive-timeout: 30s
      keep-alive-without-calls: true
      max-inbound-message-size: 4194304  # 4MB
      max-inbound-metadata-size: 8192    # 8KB
      
    # Circuit breaker defaults
    circuit-breaker:
      failure-rate-threshold: 50
      wait-duration-in-open-state: 30s
      sliding-window-size: 10
      minimum-number-of-calls: 5
      slow-call-rate-threshold: 100
      slow-call-duration-threshold: 60s
      
    # Retry defaults
    retry:
      max-attempts: 3
      wait-duration: 500ms
      exponential-backoff-multiplier: 2.0
      retry-exceptions:
        - java.net.ConnectException
        - java.net.SocketTimeoutException
        - java.util.concurrent.TimeoutException
```

### Service-Specific Configuration

```yaml
firefly:
  service-client:
    services:
      # Customer service configuration
      customer-service:
        base-url: http://customer-service:8080
        timeout: 15s
        circuit-breaker:
          failure-rate-threshold: 30
        retry:
          max-attempts: 5
          
      # Payment service configuration
      payment-service:
        base-url: https://payment-service:8443
        timeout: 45s
        authentication:
          type: bearer
          token-provider: paymentTokenProvider
        circuit-breaker:
          failure-rate-threshold: 20
          wait-duration-in-open-state: 60s
          
      # Core banking gRPC service
      core-banking-grpc:
        address: core-banking:9090
        use-plaintext: false
        tls-cert-path: /etc/ssl/core-banking.crt
        keep-alive-time: 2m
```

## Observability Configuration

### Comprehensive Observability

```yaml
# Spring Boot Actuator configuration for observability
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,configprops,env
  endpoint:
    health:
      show-details: always
      show-components: always
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
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
        http.client.requests: true
```

## Environment-Specific Configurations

### Development Environment

```yaml
# application-development.yml
spring:
  profiles:
    active: development
    
firefly:
  # Use in-process events for development
  events:
    adapter: application_event
    
  # Relaxed CQRS settings
  cqrs:
    query:
      cache:
        enabled: false  # Disable caching for development
      
  # Relaxed ServiceClient settings
  service-client:
    circuit-breaker:
      failure-rate-threshold: 80  # More lenient
    retry:
      max-attempts: 1  # Fail fast in development


# Enable debug logging
logging:
  level:
    com.firefly.common.domain: DEBUG
    org.springframework.web.reactive.function.client: DEBUG
```

### Testing Environment

```yaml
# application-test.yml
spring:
  profiles:
    active: test
    
firefly:
  # Use in-process events for testing
  events:
    adapter: application_event
    
  # Disable external dependencies
  service-client:
    enabled: false
    
  # Fast CQRS processing
  cqrs:
    command:
      timeout: 5s
    query:
      timeout: 2s
      cache:
        enabled: false


# Test-specific logging
logging:
  level:
    com.firefly.common.domain: WARN
    org.springframework: WARN
```

### Production Environment

```yaml
# application-production.yml
spring:
  profiles:
    active: production
    
firefly:
  # Production-grade messaging
  events:
    adapter: kafka
    kafka:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
      properties:
        security.protocol: SSL
        ssl.truststore.location: ${KAFKA_TRUSTSTORE_PATH}
        ssl.truststore.password: ${KAFKA_TRUSTSTORE_PASSWORD}
        
  # Production CQRS settings
  cqrs:
    command:
      timeout: 60s
    query:
      cache:
        enabled: true
        default-ttl: 600  # 10 minutes
        max-size: 10000
      
  # Production ServiceClient settings
  service-client:
    circuit-breaker:
      failure-rate-threshold: 30
      wait-duration-in-open-state: 60s
    retry:
      max-attempts: 5
      wait-duration: 1s


# Production logging
logging:
  level:
    com.firefly.common.domain: INFO
    org.springframework: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n"
```

### Cloud-Native Configuration (Kubernetes)

```yaml
# application-k8s.yml
spring:
  profiles:
    active: k8s
    
firefly:
  # Auto-detect messaging infrastructure
  events:
    adapter: auto
    
  # Service discovery integration
  service-client:
    services:
      customer-service:
        base-url: http://customer-service.banking.svc.cluster.local:8080
      payment-service:
        base-url: http://payment-service.banking.svc.cluster.local:8080
      core-banking-grpc:
        address: core-banking-grpc.banking.svc.cluster.local:9090


# Kubernetes health checks
management:
  endpoint:
    health:
      probes:
        enabled: true
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

## Security Configuration

### Authentication Configuration

```yaml
firefly:
  service-client:
    # Global authentication
    authentication:
      type: oauth2
      client-id: ${OAUTH2_CLIENT_ID}
      client-secret: ${OAUTH2_CLIENT_SECRET}
      token-uri: ${OAUTH2_TOKEN_URI}
      
    services:
      # Service-specific authentication
      secure-service:
        base-url: https://secure-service:8443
        authentication:
          type: bearer
          token: ${SECURE_SERVICE_TOKEN}
          
      # mTLS authentication
      banking-core:
        base-url: https://banking-core:8443
        authentication:
          type: mtls
          keystore-path: /etc/ssl/client.p12
          keystore-password: ${CLIENT_KEYSTORE_PASSWORD}
          truststore-path: /etc/ssl/truststore.p12
          truststore-password: ${TRUSTSTORE_PASSWORD}
```

### TLS Configuration

```yaml
# TLS for gRPC services
firefly:
  service-client:
    grpc:
      tls:
        enabled: true
        cert-chain-file: /etc/ssl/client-cert.pem
        private-key-file: /etc/ssl/client-key.pem
        trust-cert-collection-file: /etc/ssl/ca-cert.pem
        
# TLS for messaging
spring:
  kafka:
    ssl:
      trust-store-location: /etc/ssl/kafka.truststore.jks
      trust-store-password: ${KAFKA_TRUSTSTORE_PASSWORD}
      key-store-location: /etc/ssl/kafka.keystore.jks
      key-store-password: ${KAFKA_KEYSTORE_PASSWORD}
```

---

This configuration guide provides the foundation for deploying the Firefly Common Domain Library across different environments while maintaining security, performance, and observability requirements for banking applications.
