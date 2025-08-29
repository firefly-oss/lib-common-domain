# Firefly Common Domain Library

[![Maven](https://img.shields.io/badge/Maven-1.0.0--SNAPSHOT-blue.svg)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)]()

A Spring Boot library that provides domain event publishing and consumption capabilities with support for multiple messaging adapters including Kafka, RabbitMQ, AWS SQS, AWS Kinesis, and Spring Application Events.

## 🌟 Overview

The Firefly Common Domain Library enables event-driven architectures by providing:

- **Multi-Adapter Support**: Automatic detection and configuration for Kafka, RabbitMQ, Kinesis, SQS, and Application Events
- **Reactive Programming**: Built on Project Reactor with `Mono<Void>` return types
- **Auto-Configuration**: Zero-configuration setup with intelligent adapter selection
- **Annotation-Driven**: `@EventPublisher` annotation for declarative event publishing
- **Transactional Engine Integration**: Bridge pattern for step events from lib-transactional-engine-core
- **Health Monitoring**: Built-in health indicators for all messaging adapters
- **Retry Support**: Configurable retry mechanisms for resilient event publishing

## 📋 Table of Contents

- [Installation](#installation)
- [Configuration](#configuration)
- [Supported Adapters](#supported-adapters)
- [Usage](#usage)
- [Event Consumption](#event-consumption)
- [Transactional Engine Integration](#transactional-engine-integration)
- [Health Monitoring](#health-monitoring)
- [Configuration Reference](#configuration-reference)

## 📦 Installation

### Maven

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.firefly:lib-common-domain:1.0.0-SNAPSHOT'
```

## ⚙️ Configuration

### Basic Configuration

Add to your `application.yml`:

```yaml
firefly:
  events:
    enabled: true              # Default: true
    adapter: auto             # Options: auto, kafka, rabbit, kinesis, sqs, application_event, noop
```

### 🎯 Fully Encapsulated Configuration (Recommended)

**NEW**: The library now creates all messaging infrastructure automatically from Firefly properties. No Spring-specific configuration required!

#### Simple Kafka Setup
```yaml
firefly:
  events:
    adapter: kafka  # or 'auto' for auto-detection
    kafka:
      bootstrap-servers: localhost:9092
      retries: 3
      batch-size: 16384
      acks: all
```

#### Simple RabbitMQ Setup
```yaml
firefly:
  events:
    adapter: rabbit  # or 'auto' for auto-detection
    rabbit:
      host: localhost
      port: 5672
      username: guest
      password: guest
```

#### Simple AWS SQS Setup
```yaml
firefly:
  events:
    adapter: sqs  # or 'auto' for auto-detection
    sqs:
      region: us-east-1
      queue-url: https://sqs.us-east-1.amazonaws.com/123456789012/my-queue
```

#### Simple AWS Kinesis Setup
```yaml
firefly:
  events:
    adapter: kinesis  # or 'auto' for auto-detection
    kinesis:
      region: us-east-1
      stream-name: my-stream
```

### Adapter Auto-Detection

When `adapter: auto` is configured (default), the library automatically detects available messaging systems in this priority order:

1. **Kafka** - if `KafkaTemplate` bean exists OR `bootstrap-servers` is configured
2. **RabbitMQ** - if `RabbitTemplate` bean exists OR `host` is configured
3. **Kinesis** - if `KinesisAsyncClient` bean exists OR `region` is configured
4. **SQS** - if `SqsAsyncClient` bean exists OR `region` is configured  
5. **Application Events** - fallback (always available)

### 🔄 Backward Compatibility

The library maintains full backward compatibility. If you have existing Spring beans (KafkaTemplate, RabbitTemplate, etc.), the library will use them instead of creating new ones.

## 🔌 Supported Adapters

### Kafka

Requires Spring Kafka dependency and KafkaTemplate bean.

```yaml
firefly:
  events:
    adapter: kafka
    kafka:
      template-bean-name: kafkaTemplate    # Optional: custom bean name
      bootstrap-servers: localhost:9092
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      retries: 3
      batch-size: 16384
      linger-ms: 1
      buffer-memory: 33554432
      acks: all
```

### RabbitMQ

Requires Spring AMQP dependency and RabbitTemplate bean.

```yaml
firefly:
  events:
    adapter: rabbit
    rabbit:
      template-bean-name: rabbitTemplate   # Optional: custom bean name
      exchange: ${topic}                   # Default: uses event topic as exchange
      routing-key: ${type}                 # Default: uses event type as routing key
```

### AWS SQS

Requires AWS SDK v2 SqsAsyncClient bean.

```yaml
firefly:
  events:
    adapter: sqs
    sqs:
      client-bean-name: sqsAsyncClient     # Optional: custom bean name
      queue-url: https://sqs.us-east-1.amazonaws.com/123456789012/my-queue
      queue-name: my-queue                 # Alternative to queue-url
```

### AWS Kinesis

Requires AWS SDK v2 KinesisAsyncClient bean.

```yaml
firefly:
  events:
    adapter: kinesis
    kinesis:
      client-bean-name: kinesisAsyncClient # Optional: custom bean name
      stream-name: my-stream
      partition-key: ${key}                # Default: uses event key, supports ${topic}, ${type}, ${key}
```

### Application Events

Uses Spring's ApplicationEventPublisher for in-process events.

```yaml
firefly:
  events:
    adapter: application_event
```

### No-Op

Disables event publishing (useful for testing).

```yaml
firefly:
  events:
    adapter: noop
```

## 🚀 Usage

### Declarative Publishing with @EventPublisher

The `@EventPublisher` annotation enables automatic event publishing from method results:

```java
import com.firefly.common.domain.events.outbound.EventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OrderService {
    
    @EventPublisher(topic = "'orders'", type = "'order.created'", key = "#result.id")
    public Mono<Order> createOrder(CreateOrderRequest request) {
        Order order = processOrder(request);
        return Mono.just(order);
    }
    
    @EventPublisher(
        topic = "'payments'", 
        type = "'payment.processed'",
        key = "#orderId",
        payload = "{'orderId': #orderId, 'amount': #amount, 'status': 'COMPLETED'}"
    )
    public Mono<Void> processPayment(String orderId, BigDecimal amount) {
        // Payment processing logic
        return Mono.empty();
    }
}
```

### Programmatic Publishing

Direct use of DomainEventPublisher:

```java
import com.firefly.common.domain.events.outbound.DomainEventPublisher;
import com.firefly.common.domain.events.DomainEventEnvelope;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    
    private final DomainEventPublisher eventPublisher;
    
    public OrderService(DomainEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    public Mono<Void> publishOrderEvent(Order order) {
        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("orders")
            .type("order.created")
            .key(order.getId())
            .payload(order)
            .headers(Map.of("source", "order-service"))
            .build();
            
        return eventPublisher.publish(event);
    }
}
```

## 📥 Event Consumption

Event consumption is disabled by default and must be explicitly enabled:

```yaml
firefly:
  events:
    consumer:
      enabled: true
      type-header: event_type      # Header containing event type
      key-header: event_key        # Header containing event key
```

### Kafka Consumer

```yaml
firefly:
  events:
    consumer:
      enabled: true
      kafka:
        topics:
          - orders
          - payments
        group-id: my-service-group
        consumer-factory-bean-name: kafkaConsumerFactory  # Optional
```

### RabbitMQ Consumer

```yaml
firefly:
  events:
    consumer:
      enabled: true
      rabbit:
        queues:
          - order-events-queue
          - payment-events-queue
```

### SQS Consumer

```yaml
firefly:
  events:
    consumer:
      enabled: true
      sqs:
        queue-url: https://sqs.us-east-1.amazonaws.com/123456789012/my-queue
        queue-name: my-queue         # Alternative to queue-url
        wait-time-seconds: 10        # Long polling (0-20)
        max-messages: 10             # Max messages per poll (1-10)
        poll-delay-millis: 1000      # Delay between polls
```

### Kinesis Consumer

```yaml
firefly:
  events:
    consumer:
      enabled: true
      kinesis:
        stream-name: my-stream
        application-name: domain-events-consumer
        poll-delay-millis: 5000
```

### Event Listener

Create event listeners using the `@EventListener` annotation:

```java
import com.firefly.common.domain.events.inbound.EventListener;
import com.firefly.common.domain.events.DomainEventEnvelope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OrderEventListener {
    
    @EventListener(topic = "orders", type = "order.created")
    public Mono<Void> handleOrderCreated(DomainEventEnvelope event) {
        Order order = (Order) event.getPayload();
        // Process the order created event
        return Mono.empty();
    }
    
    @EventListener(topic = "orders") // Listen to all events from orders topic
    public Mono<Void> handleAllOrderEvents(DomainEventEnvelope event) {
        // Process any order event
        return Mono.empty();
    }
}
```

## 🔄 Transactional Engine Integration

The library integrates with `lib-transactional-engine-core` to bridge step events to domain events:

### Dependency

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-transactional-engine-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Configuration

Step events are automatically enabled when the transactional engine is on the classpath:

```yaml
firefly:
  stepevents:
    enabled: true              # Default: true when lib-transactional-engine-core is present
```

### Usage

Step events from the transactional engine are automatically converted to domain events with additional metadata:

- `step.attempts` - Number of execution attempts
- `step.latency_ms` - Execution latency in milliseconds
- `step.started_at` - Step start timestamp
- `step.completed_at` - Step completion timestamp  
- `step.result_type` - Result type of the step

The bridge uses your configured domain event adapter, so step events are published using the same messaging system (Kafka, RabbitMQ, etc.).

## 🏥 Health Monitoring

The library provides health indicators for all messaging adapters:

### Kafka Health Indicator
- Endpoint: `/actuator/health/domainEventsKafka`
- Checks: KafkaTemplate availability, producer factory status

### RabbitMQ Health Indicator  
- Endpoint: `/actuator/health/domainEventsRabbit`
- Checks: RabbitTemplate availability, connection status

### SQS Health Indicator
- Endpoint: `/actuator/health/domainEventsSqs`  
- Checks: SqsAsyncClient availability, queue accessibility

### Kinesis Health Indicator
- Endpoint: `/actuator/health/domainEventsKinesis`
- Checks: KinesisAsyncClient availability, stream status

### Application Events Health Indicator
- Endpoint: `/actuator/health/domainEventsApplicationEvent`
- Checks: ApplicationEventPublisher availability

### Configuration

Health indicators are automatically enabled. To disable:

```yaml
management:
  health:
    domainEventsKafka:
      enabled: false
    domainEventsRabbit:
      enabled: false
    domainEventsSqs:
      enabled: false
    domainEventsKinesis:
      enabled: false
    domainEventsApplicationEvent:
      enabled: false
```

## 📊 Metrics and Observability

The library provides additional observability components:

```yaml
firefly:
  observability:
    jvm:
      enabled: true              # JVM metrics
    http-client:
      enabled: true              # HTTP client metrics
      health-enabled: true       # HTTP client health checks
    startup:
      enabled: true              # Application startup metrics
    thread-pool:
      enabled: true              # Thread pool health monitoring
    cache:
      enabled: true              # Cache health monitoring
```

Available endpoints:
- `/actuator/health/threadPool` - Thread pool health
- `/actuator/health/httpClient` - HTTP client health  
- `/actuator/health/cache` - Cache health
- `/actuator/info/domainEvents` - Domain events configuration info

## 🔧 Configuration Reference

### Complete Configuration Example

```yaml
firefly:
  events:
    enabled: true
    adapter: auto
    
    kafka:
      template-bean-name: kafkaTemplate
      bootstrap-servers: localhost:9092
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      retries: 3
      batch-size: 16384
      linger-ms: 1
      buffer-memory: 33554432
      acks: all
      properties:
        max.request.size: 1048576
        
    rabbit:
      template-bean-name: rabbitTemplate
      exchange: ${topic}
      routing-key: ${type}
      
    sqs:
      client-bean-name: sqsAsyncClient
      queue-url: https://sqs.us-east-1.amazonaws.com/123456789012/my-queue
      queue-name: my-queue
      
    kinesis:
      client-bean-name: kinesisAsyncClient
      stream-name: my-stream
      partition-key: ${key}
      
    consumer:
      enabled: false
      type-header: event_type
      key-header: event_key
      
      kafka:
        topics:
          - orders
          - payments
        group-id: my-service-group
        consumer-factory-bean-name: kafkaConsumerFactory
        
      rabbit:
        queues:
          - order-events-queue
          - payment-events-queue
          
      sqs:
        queue-url: https://sqs.us-east-1.amazonaws.com/123456789012/consumer-queue
        queue-name: consumer-queue
        wait-time-seconds: 10
        max-messages: 10
        poll-delay-millis: 1000
        
      kinesis:
        stream-name: consumer-stream
        application-name: domain-events-consumer
        poll-delay-millis: 5000
        
  stepevents:
    enabled: true
    
  observability:
    jvm:
      enabled: true
    http-client:
      enabled: true
      health-enabled: true
    startup:
      enabled: true
    thread-pool:
      enabled: true
    cache:
      enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  health:
    show-details: always
```

### Property Validation

The library includes comprehensive validation for configuration properties:

- **SQS Queue URLs**: Must match valid SQS URL format
- **SQS Queue Names**: 1-80 characters, alphanumeric with hyphens/underscores, optional .fifo suffix
- **Kinesis Stream Names**: 1-128 characters, alphanumeric with hyphens/underscores/periods
- **Kinesis Partition Keys**: 1-256 characters
- **Consumer Settings**: Wait times, message limits, and poll delays within AWS limits
- **Kafka Settings**: Batch sizes, retry counts, and buffer memory within reasonable ranges

## 🛠️ Retry Configuration

The library supports retry mechanisms through Spring Retry. Configure a `RetryTemplate` bean named `domainEventsRetryTemplate`:

```java
@Bean
public RetryTemplate domainEventsRetryTemplate() {
    return RetryTemplate.builder()
        .maxAttempts(3)
        .fixedBackoff(1000)
        .retryOn(Exception.class)
        .build();
}
```

This retry template will be automatically used by all messaging adapters for resilient event publishing.

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.