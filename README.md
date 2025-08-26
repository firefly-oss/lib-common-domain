# Firefly Common Domain Library

[![Maven](https://img.shields.io/badge/Maven-1.0.0--SNAPSHOT-blue.svg)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)]()

A powerful Spring Boot library that enables domain-driven design (DDD) with reactive programming support, featuring multi-messaging adapter architecture and comprehensive event handling capabilities.

## 🌟 Overview

The Firefly Common Domain Library simplifies the implementation of event-driven architectures in Spring Boot applications by providing:

- **Multi-Transport Event System**: Support for Kafka, RabbitMQ, AWS SQS, and Spring Application Events
- **Reactive Architecture**: Built on Project Reactor for non-blocking operations
- **Modular Design**: Choose only the messaging adapters you need
- **Auto-Configuration**: Zero-configuration setup with intelligent adapter detection
- **Declarative Programming**: Annotation-driven event publishing and consumption
- **Production-Ready**: Comprehensive monitoring, health checks, and metrics

## 📋 Table of Contents

- [Architecture](#-architecture)
- [Installation](#-installation)
- [Quick Start](#-quick-start)
- [Configuration](#-configuration)
- [Usage Examples](#-usage-examples)
- [Modules](#-modules)
- [Monitoring & Health](#-monitoring--health)
- [Testing](#-testing)
- [Contributing](#-contributing)
- [License](#-license)

## 🏗️ Architecture

The library follows a hexagonal architecture pattern with clear separation between:

- **Core Domain**: Event interfaces, envelopes, and business logic
- **Ports**: Abstract interfaces for publishing and consuming events
- **Adapters**: Concrete implementations for different messaging systems
- **Configuration**: Auto-configuration and property management

### Supported Messaging Systems

| Adapter | Transport | Use Case |
|---------|-----------|----------|
| Kafka | Apache Kafka | High-throughput, distributed streaming |
| RabbitMQ | AMQP | Complex routing, reliable messaging |
| AWS SQS | Cloud Queue | Serverless, managed queuing |
| Application Events | In-Memory | Testing, monolithic applications |

### Transactional Engine Integration

The library seamlessly integrates with **lib-transactional-engine-core** to provide step event publishing capabilities for transactional workflows and saga patterns.

#### StepEventPublisher Bridge Pattern

The integration follows a bridge pattern where:

- **Port**: `StepEventPublisher` interface from lib-transactional-engine-core
- **Bridge**: `StepEventPublisherBridge` that adapts StepEvents to DomainEvents
- **Adapters**: Reuses all existing messaging adapters (Kafka, RabbitMQ, SQS, Application Events)

This design allows transactional engine step events to leverage the same messaging infrastructure and configuration as domain events.

#### StepEvent vs DomainEvent Mapping

| StepEventEnvelope Field | DomainEventEnvelope Field | Purpose |
|-------------------------|---------------------------|---------|
| `topic` | `topic` | Target destination/topic |
| `type` | `type` | Event type identifier |
| `key` | `key` | Partitioning/routing key |
| `payload` | `payload` | Event data/content |
| `headers` | `headers` | Additional metadata |

#### Auto-Configuration

StepEventPublisher is auto-configured when:
1. `lib-transactional-engine-core` is on classpath
2. A `DomainEventPublisher` bean is available
3. StepEvents are enabled (default: `true`)

**Simplified Configuration:** Step Events always use Domain Events infrastructure - no separate configuration needed!

```yaml
firefly:
  stepevents:
    enabled: true     # Default: true - Only configuration needed!
    
  # Step Events automatically use whatever adapter is configured for Domain Events
  events:
    adapter: kafka    # StepEvents will use Kafka via bridge pattern
    kafka:
      bootstrap-servers: localhost:9092
      # All other Kafka settings apply to both Domain Events AND Step Events
```

## 📦 Installation

### Installation

The library is now provided as a single, consolidated module that includes all functionality:

```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**What's Included**: This single module includes ALL messaging dependencies (Kafka, RabbitMQ, SQS, Kinesis) and functionality. There are no separate adapter modules - everything you need is in this one dependency.

### Supported Messaging Adapters

The library provides comprehensive support for all major messaging systems:

**✅ Application Events (Default - Local Publishing)**
- Works out of the box with no additional configuration
- Events published within the same JVM using Spring's ApplicationEventPublisher
- Ideal for monolithic applications, testing, and development environments

**✅ Apache Kafka (Remote Publishing)**
- Full producer and consumer support with comprehensive configuration options
- Events published to Kafka topics using Spring Kafka with headers support
- Ideal for high-throughput event streaming, distributed applications, and event sourcing
- Supports all Kafka producer configurations (serializers, retries, batching, etc.)

**✅ RabbitMQ (Remote Publishing)**
- Full publisher and subscriber support with flexible routing
- Events published to RabbitMQ exchanges using Spring AMQP
- Supports custom exchange and routing key patterns with placeholders
- Ideal for reliable messaging, complex routing scenarios, and microservices

**✅ AWS SQS (Remote Publishing)**
- Complete integration with AWS SQS using SDK v2
- Events published to SQS queues with automatic URL resolution
- Supports both queue URL and queue name configuration
- Ideal for cloud-native applications, serverless architectures, and AWS environments

**✅ AWS Kinesis (Remote Publishing)**
- Complete integration with AWS Kinesis Data Streams using SDK v2
- Events published to Kinesis streams with automatic partitioning
- Supports configurable partition keys and stream names
- Ideal for real-time data streaming, event sourcing, and high-throughput event processing
- Perfect for analytics, monitoring, and building event-driven architectures with AWS

### AWS Kinesis Setup

If you want to use Kinesis for remote event publishing, the AWS SDK dependency is already included:

```yaml
firefly:
  events:
    adapter: kinesis
    kinesis:
      # Optional: Bean name override
      client-bean-name: myKinesisAsyncClient
      
      # Required: Kinesis stream name
      stream-name: domain-events-stream
      
      # Optional: Partition key pattern (supports placeholders)
      partition-key: "${key}"  # Default: uses event key, supports ${topic}, ${type}, ${key}
    
    # Optional: Consumer configuration for inbound events
    consumer:
      enabled: true
      kinesis:
        # Stream to consume from
        stream-name: domain-events-stream
        
        # Application name for Kinesis Client Library
        application-name: domain-events-consumer
        
        # Polling configuration
        poll-delay-millis: 5000      # Delay between polls (1000-300000ms)

# AWS SDK configuration
aws:
  region: us-east-1
  credentials:
    access-key: your-access-key
    secret-key: your-secret-key
```

### AWS SQS Setup

If you want to use SQS for remote event publishing, add the AWS SDK dependency:

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sqs</artifactId>
    <version>2.25.32</version>
</dependency>
```

And configure your SQS settings in `application.yml`:

```yaml
firefly:
  events:
    adapter: sqs
    sqs:
      # Optional: Bean name override
      client-bean-name: mySqsAsyncClient
      
      # Option 1: Direct queue URL (validated format)
      queue-url: https://sqs.us-east-1.amazonaws.com/123456789012/my-events-queue
      
      # Option 2: Queue name (auto-resolved via GetQueueUrl)
      # queue-name: my-events-queue  # Supports alphanumeric, hyphens, underscores, .fifo
    
    # Optional: Consumer configuration for inbound events
    consumer:
      enabled: true
      sqs:
        # Consumer-specific queue configuration
        queue-url: https://sqs.us-east-1.amazonaws.com/123456789012/my-consumer-queue
        # OR queue-name: my-consumer-queue
        
        # Polling configuration
        wait-time-seconds: 20        # Long polling (0-20 seconds)
        max-messages: 10             # Max messages per poll (1-10)
        poll-delay-millis: 1000      # Delay between polls (100-300000ms)

# AWS SDK configuration
aws:
  region: us-east-1
  credentials:
    access-key: your-access-key
    secret-key: your-secret-key
```

### Kafka Setup

If you want to use Kafka for remote event publishing, add the Spring Kafka dependency:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

And configure your Kafka settings in `application.yml`:

```yaml
firefly:
  events:
    adapter: kafka
    kafka:
      # Required: Kafka bootstrap servers
      bootstrap-servers: localhost:9092
      
      # Optional: Bean name override
      template-bean-name: myKafkaTemplate
      
      # Optional: Producer configuration
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      retries: 3
      batch-size: 16384
      linger-ms: 5
      buffer-memory: 33554432
      acks: "1"
      
      # Optional: Additional producer properties
      properties:
        compression.type: gzip
        max.in.flight.requests.per.connection: 1
    
    # Optional: Consumer configuration for inbound events
    consumer:
      enabled: true
      kafka:
        topics:
          - domain-events
          - user-events
        group-id: my-service-consumer
```

### RabbitMQ Setup

If you want to use RabbitMQ for remote event publishing, add the Spring AMQP dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

And configure your RabbitMQ settings in `application.yml`:

```yaml
firefly:
  events:
    adapter: rabbit
    rabbit:
      # Optional: Bean name override
      template-bean-name: myRabbitTemplate
      
      # Exchange configuration (supports placeholders)
      exchange: "domain-events"        # Default: ${topic}
      routing-key: "${type}"           # Default: ${type}, supports ${topic}, ${type}, ${key}
    
    # Optional: Consumer configuration for inbound events
    consumer:
      enabled: true
      rabbit:
        queues:
          - user-events-queue
          - order-events-queue

# Spring RabbitMQ connection configuration
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    connection-timeout: 60000
    publisher-confirms: true
    publisher-returns: true
```

### Transactional Engine Integration

For saga patterns and distributed transaction workflows:

```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-transactional-engine-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Important**: The transactional engine integration is already included in the core module. It automatically reuses your configured domain event adapter. However, due to the current implementation limitations, step events will use the same adapters as domain events:
- **Application Events** (default) - Step events published locally within the JVM
- **SQS** - Step events published to AWS SQS queues (if configured)

### Gradle Installation

```groovy
implementation 'com.catalis:lib-common-domain:1.0.0-SNAPSHOT'
```

## 🚀 Quick Start

### 1. Enable Auto-Configuration

The library automatically configures itself when added to your classpath. No additional configuration is required for basic usage.

### 2. Basic Configuration

Add to your `application.yml`:

```yaml
firefly:
  events:
    enabled: true
    adapter: auto  # Options: auto, application_event, sqs
    # Note: auto will select sqs if AWS SQS is configured, otherwise application_event
```

### 3. Publishing Events

#### Declarative Approach with @EventPublisher

```java
import com.catalis.common.domain.events.outbound.EventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OrderService {
    
    @EventPublisher(topic = "'orders'", type = "'order.created'", key = "#result.id")
    public Mono<Order> createOrder(CreateOrderRequest request) {
        // Your business logic
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

#### Programmatic Approach

```java
import com.catalis.common.domain.events.outbound.DomainEventPublisher;
import com.catalis.common.domain.events.DomainEventEnvelope;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    
    private final DomainEventPublisher eventPublisher;
    
    public OrderService(DomainEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    public Mono<Order> createOrder(CreateOrderRequest request) {
        Order order = new Order(request);
        
        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("orders")
            .type("order.created")
            .key(order.getId())
            .payload(order)
            .headers(Map.of("version", "1.0", "source", "order-service"))
            .build();
            
        return eventPublisher.publish(event)
            .thenReturn(order);
    }
}
```

### 4. Consuming Events

```java
import com.catalis.common.domain.events.inbound.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventHandler {
    
    @EventListener(topic = "orders", type = "order.created")
    public void handleOrderCreated(Order order) {
        log.info("New order created: {}", order.getId());
        // Process the event
        sendWelcomeEmail(order.getCustomerEmail());
        updateInventory(order.getItems());
    }
    
    @EventListener(topic = "payments", type = "payment.completed")
    public void handlePaymentCompleted(PaymentEvent event) {
        log.info("Payment completed for order: {}", event.getOrderId());
        // Update order status
        orderService.markAsPaid(event.getOrderId());
    }
}
```

### 5. Using StepEvents (Transactional Engine Integration)

For saga patterns and distributed transaction workflows:

#### Programmatic StepEvent Publishing

```java
import com.catalis.transactionalengine.events.StepEventPublisher;
import com.catalis.transactionalengine.events.StepEventEnvelope;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OrderSagaOrchestrator {
    
    private final StepEventPublisher stepEventPublisher;
    
    public OrderSagaOrchestrator(StepEventPublisher stepEventPublisher) {
        this.stepEventPublisher = stepEventPublisher;
    }
    
    public Mono<Void> executeOrderProcessingSaga(String orderId) {
        // Step 1: Reserve inventory
        StepEventEnvelope reserveInventory = new StepEventEnvelope();
        reserveInventory.topic = "inventory-saga";
        reserveInventory.type = "inventory.reserve";
        reserveInventory.key = orderId;
        reserveInventory.payload = Map.of("orderId", orderId, "step", "reserve");
        reserveInventory.headers = Map.of("sagaId", UUID.randomUUID().toString());
        
        return stepEventPublisher.publish(reserveInventory)
            .then(publishPaymentStep(orderId))
            .then(publishShippingStep(orderId));
    }
    
    private Mono<Void> publishPaymentStep(String orderId) {
        StepEventEnvelope processPayment = new StepEventEnvelope();
        processPayment.topic = "payment-saga";
        processPayment.type = "payment.process";
        processPayment.key = orderId;
        processPayment.payload = Map.of("orderId", orderId, "step", "payment");
        
        return stepEventPublisher.publish(processPayment);
    }
    
    private Mono<Void> publishShippingStep(String orderId) {
        StepEventEnvelope arrangeShipping = new StepEventEnvelope();
        arrangeShipping.topic = "shipping-saga";
        arrangeShipping.type = "shipping.arrange";
        arrangeShipping.key = orderId;
        arrangeShipping.payload = Map.of("orderId", orderId, "step", "shipping");
        
        return stepEventPublisher.publish(arrangeShipping);
    }
}
```

#### StepEvents Configuration

**Simplified Configuration:** Step Events always use Domain Events infrastructure - no separate configuration needed!

```yaml
firefly:
  stepevents:
    enabled: true  # Default: true - Only configuration needed!
    
  # Step Events automatically use whatever adapter is configured for Domain Events
  events:
    adapter: kafka  # StepEvents will use Kafka via bridge pattern
    kafka:
      bootstrap-servers: "localhost:9092"
      template-bean-name: kafkaTemplate
      # All other Kafka settings apply to both Domain Events AND Step Events
```

**Key Benefits:**
- **Bridge Pattern**: StepEvents automatically use your existing domain event messaging configuration
- **Unified Infrastructure**: No need for separate messaging setup for saga events
- **Consistent Monitoring**: StepEvents appear in the same health checks and metrics as domain events
- **Same Error Handling**: Retry policies and error handling work identically for both event types

## ⚙️ Configuration

### Adapter Selection

The library automatically detects available messaging systems in this priority order:

1. **Kafka** - if Kafka dependencies and `KafkaTemplate` are available
2. **RabbitMQ** - if RabbitMQ dependencies and `RabbitTemplate` are available
3. **AWS SQS** - if AWS SDK and `SqsAsyncClient` are available
4. **Application Events** - always available as fallback

### Messaging-Specific Configuration

#### Kafka Configuration

**Basic Configuration:**
```yaml
firefly:
  events:
    adapter: kafka  # or auto
    kafka:
      template-bean-name: myKafkaTemplate  # optional custom template
      use-messaging-if-available: true
    consumer:
      enabled: true
      kafka:
        topics: ["orders", "payments"]
        group-id: "my-service"
        consumer-factory-bean-name: myConsumerFactory  # optional
```

**Advanced Configuration with Bootstrap Servers and Producer Properties:**
```yaml
firefly:
  events:
    adapter: kafka
    kafka:
      # Connection settings
      bootstrap-servers: "localhost:9092,localhost:9093"
      
      # Serialization settings
      key-serializer: "org.apache.kafka.common.serialization.StringSerializer"
      value-serializer: "org.springframework.kafka.support.serializer.JsonSerializer"
      
      # Performance tuning
      retries: 3
      batch-size: 32768
      linger-ms: 100
      buffer-memory: 67108864
      acks: "all"
      
      # Additional Kafka properties
      properties:
        "ssl.truststore.location": "/path/to/truststore.jks"
        "ssl.truststore.password": "truststore-password"
        "security.protocol": "SSL"
        "compression.type": "snappy"
        "max.request.size": 1048576
      
      # Legacy settings (still supported)
      template-bean-name: myKafkaTemplate  # optional custom template
      use-messaging-if-available: true
    consumer:
      enabled: true
      kafka:
        topics: ["orders", "payments"]
        group-id: "my-service"
        consumer-factory-bean-name: myConsumerFactory  # optional
```

**Configuration Properties Reference:**
- `bootstrap-servers`: Comma-separated list of Kafka broker addresses
- `key-serializer`: Serializer class for message keys (default: StringSerializer)
- `value-serializer`: Serializer class for message values (default: StringSerializer)
- `retries`: Number of retry attempts for failed sends
- `batch-size`: Batch size for batching records
- `linger-ms`: Time to wait for additional records before sending
- `buffer-memory`: Total memory available for buffering
- `acks`: Acknowledgment mode ("none", "1", "all", or "-1")
- `properties`: Map of additional Kafka producer properties

#### RabbitMQ Configuration

```yaml
firefly:
  events:
    adapter: rabbit
    rabbit:
      template-bean-name: myRabbitTemplate  # optional
      exchange: "events.${topic}"  # SpEL expression
      routing-key: "${type}.${key}"  # SpEL expression
    consumer:
      enabled: true
      rabbit:
        queues: ["orders.queue", "payments.queue"]
```

#### AWS SQS Configuration

```yaml
firefly:
  events:
    adapter: sqs
    sqs:
      client-bean-name: mySqsClient  # optional
      queue-url: "https://sqs.region.amazonaws.com/account/queue"
      # OR use queue name (will be resolved to URL)
      queue-name: "my-events-queue"
    consumer:
      enabled: true
      sqs:
        queue-url: "https://sqs.region.amazonaws.com/account/consumer-queue"
        wait-time-seconds: 10
        max-messages: 10
        poll-delay-millis: 1000
```

### Advanced Configuration

```yaml
firefly:
  events:
    enabled: true
    adapter: auto
    consumer:
      enabled: true
      type-header: "event_type"  # Header name for event type
      key-header: "event_key"    # Header name for event key
  stepevents:  # Integration with transactional engine
    enabled: true  # Automatically uses Domain Events infrastructure
```

### Configuration Validation

The library provides comprehensive validation for all configuration properties to ensure correct setup and prevent runtime errors.

#### Validation Examples

**Valid SQS Configuration:**
```yaml
firefly:
  events:
    adapter: sqs
    sqs:
      # Valid queue URL format
      queue-url: https://sqs.us-east-1.amazonaws.com/123456789012/my-queue
      # OR valid queue name
      queue-name: my-events-queue
```

**Invalid Configuration Examples and Error Messages:**

```yaml
# ❌ Invalid SQS queue URL format
# queue-url: "invalid-url"
# Error: Queue URL must be a valid SQS URL format

# ❌ Invalid queue name characters  
# queue-name: "my@queue!"
# Error: Queue name can only contain alphanumeric characters, hyphens, underscores, and optional .fifo suffix

# ❌ Invalid Kafka configuration
# retries: -1              # Error: Retries must be 0 or greater
# batch-size: 0            # Error: Batch size must be 1 or greater  
# acks: "invalid"          # Error: Acks must be 'none', 'all', '1', or '-1'
```

#### Validation Constraints Reference

| Property | Validation Rule | Error Message |
|----------|----------------|---------------|
| `sqs.queue-url` | Must match SQS URL pattern | Queue URL must be a valid SQS URL format |
| `sqs.queue-name` | 1-80 chars, alphanumeric + hyphens/underscores + optional .fifo | Queue name validation message |
| `kafka.bootstrap-servers` | Non-empty string | Bootstrap servers cannot be empty |
| `kafka.retries` | ≥ 0 | Retries must be 0 or greater |
| `kafka.batch-size` | ≥ 1 | Batch size must be 1 or greater |
| `kafka.linger-ms` | ≥ 0 | Linger ms must be 0 or greater |
| `kafka.buffer-memory` | ≥ 1 | Buffer memory must be 1 or greater |
| `kafka.acks` | "none", "all", "1", or "-1" | Acks must be 'none', 'all', '1', or '-1' |

#### Best Practices

1. **Use Environment Variables**: Keep sensitive information like credentials in environment variables
2. **Validate Early**: Enable validation during development to catch configuration issues early
3. **Monitor Configuration**: Use Spring Boot Actuator to monitor configuration health
4. **Test Configuration**: Use different profiles for different environments

## 💡 Usage Examples

### E-commerce Order Processing

```java
@Service
@Transactional
public class EcommerceOrderService {
    
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    
    @EventPublisher(topic = "'orders'", type = "'order.placed'", key = "#result.id")
    public Mono<Order> placeOrder(PlaceOrderRequest request) {
        Order order = Order.create(request);
        return orderRepository.save(order)
            .doOnSuccess(savedOrder -> log.info("Order placed: {}", savedOrder.getId()));
    }
    
    @EventPublisher(
        topic = "'orders'", 
        type = "'order.status.changed'",
        key = "#orderId",
        payload = "{'orderId': #orderId, 'oldStatus': #oldStatus, 'newStatus': #newStatus}"
    )
    public Mono<Order> updateOrderStatus(String orderId, OrderStatus newStatus) {
        return orderRepository.findById(orderId)
            .map(order -> {
                OrderStatus oldStatus = order.getStatus();
                order.setStatus(newStatus);
                return order;
            })
            .flatMap(orderRepository::save);
    }
}

@Component
public class OrderEventHandlers {
    
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    
    @EventListener(topic = "orders", type = "order.placed")
    public void handleOrderPlaced(Order order) {
        // Reserve inventory
        inventoryService.reserveItems(order.getItems())
            .subscribe();
        
        // Send confirmation email
        notificationService.sendOrderConfirmation(order.getCustomerEmail(), order)
            .subscribe();
    }
    
    @EventListener(topic = "payments", type = "payment.successful")
    public void handlePaymentSuccessful(PaymentEvent event) {
        // Update order status to paid
        orderService.updateOrderStatus(event.getOrderId(), OrderStatus.PAID)
            .subscribe();
    }
    
    @EventListener(topic = "orders", type = "order.status.changed")
    public void handleOrderStatusChange(OrderStatusChangeEvent event) {
        if (event.getNewStatus() == OrderStatus.SHIPPED) {
            // Send tracking information
            notificationService.sendTrackingInfo(event.getOrderId())
                .subscribe();
        }
    }
}
```

### Microservices Communication Pattern

```java
// User Service
@Service
public class UserManagementService {
    
    @EventPublisher(topic = "'users'", type = "'user.registered'", key = "#result.id")
    public Mono<User> registerUser(UserRegistrationRequest request) {
        User user = User.create(request);
        return userRepository.save(user);
    }
    
    @EventPublisher(topic = "'users'", type = "'user.profile.updated'", key = "#userId")
    public Mono<User> updateProfile(String userId, UpdateProfileRequest request) {
        return userRepository.findById(userId)
            .map(user -> user.updateProfile(request))
            .flatMap(userRepository::save);
    }
}

// Notification Service
@Component
public class UserNotificationHandler {
    
    @EventListener(topic = "users", type = "user.registered")
    public void sendWelcomeEmail(User user) {
        emailService.sendWelcomeEmail(user.getEmail(), user.getName())
            .subscribe();
    }
    
    @EventListener(topic = "orders", type = "order.placed")
    public void notifyOrderPlaced(Order order) {
        smsService.sendOrderConfirmation(order.getCustomerPhone(), order.getId())
            .subscribe();
    }
}

// Analytics Service
@Component 
public class EventAnalyticsHandler {
    
    @EventListener(topic = "*", type = "*")  // Listen to all events
    public void trackEvent(String eventPayload, 
                          @Header("event_type") String eventType,
                          @Header("event_topic") String topic) {
        analyticsService.recordEvent(topic, eventType, eventPayload)
            .subscribe();
    }
}
```

## 📊 Architecture

The library uses a single-module architecture that includes all functionality:

### Single Module (`lib-common-domain`)
**Purpose**: Complete functionality for domain events with all messaging adapters

**Includes**:
- Domain event interfaces and envelopes
- `@EventPublisher` aspect and SpEL processing
- `@EventListener` event dispatcher
- Configuration properties and validation
- Actuator support (health checks, metrics)
- Distributed tracing utilities
- All messaging adapters: Application Events, Kafka, RabbitMQ, SQS
- Transactional engine integration
- Complete testing support for all messaging systems

**Dependencies**: Spring Boot WebFlux, AOP, Actuator, Micrometer, Spring Kafka, Spring AMQP, AWS SQS SDK

**Benefits of Single Module Approach**:
- **Honest Architecture**: No misleading separate modules that don't provide additional functionality
- **Simplified Dependency Management**: One dependency includes everything
- **Reduced Maintenance Overhead**: No need to maintain multiple module structures
- **Clear Expectations**: What you see is what you get - all messaging systems are available

## 📈 Monitoring & Health

### Health Checks

The library automatically provides health indicators for each messaging adapter:

```bash
GET /actuator/health
```

```json
{
  "status": "UP",
  "components": {
    "domainEventsKafka": {
      "status": "UP",
      "details": {
        "adapter": "kafka",
        "status": "Kafka template available"
      }
    },
    "domainEventsRabbit": {
      "status": "UP",
      "details": {
        "adapter": "rabbit",
        "status": "RabbitMQ connection healthy"
      }
    }
  }
}
```

### Metrics

Built-in metrics for monitoring event publishing and consumption:

- `domain_events_published_total` - Counter of published events (by adapter, topic, type)
- `domain_events_publish_duration` - Publishing duration timer
- `domain_events_consumed_total` - Counter of consumed events
- `domain_events_consume_duration` - Consumption duration timer

### Configuration Info

```bash
GET /actuator/info
```

Provides detailed configuration information for debugging and verification.

## 🧪 Testing

### Integration Testing with Testcontainers

```java
@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    
    @Autowired
    private OrderService orderService;
    
    @Test
    void shouldPublishEventWhenOrderCreated() {
        CreateOrderRequest request = new CreateOrderRequest("customer-123", items);
        
        StepVerifier.create(orderService.createOrder(request))
            .assertNext(order -> assertThat(order.getId()).isNotNull())
            .verifyComplete();
        
        // Verify event was published (using test consumer)
    }
}
```

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private DomainEventPublisher eventPublisher;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void shouldPublishOrderCreatedEvent() {
        when(eventPublisher.publish(any())).thenReturn(Mono.empty());
        
        CreateOrderRequest request = new CreateOrderRequest("customer-123", items);
        
        StepVerifier.create(orderService.createOrder(request))
            .expectNextMatches(order -> order.getCustomerId().equals("customer-123"))
            .verifyComplete();
        
        verify(eventPublisher).publish(argThat(envelope ->
            "orders".equals(envelope.getTopic()) &&
            "order.created".equals(envelope.getType())
        ));
    }
}
```

### Testing Configuration

```yaml
# application-test.yml
firefly:
  events:
    adapter: application_event  # Use in-memory events for testing
    consumer:
      enabled: true
```

## 🤝 Contributing

We welcome contributions! Please follow these guidelines:

### Development Setup

1. **Clone and build**:
   ```bash
   git clone <repository-url>
   cd lib-common-domain
   ./mvnw clean compile
   ```

2. **Run tests**:
   ```bash
   ./mvnw test
   ```

3. **Integration tests** (requires Docker):
   ```bash
   ./mvnw verify
   ```

### Guidelines

- **Code Quality**: Follow existing patterns and maintain test coverage
- **Documentation**: Update documentation for new features
- **Compatibility**: Maintain backward compatibility when possible
- **Testing**: Add comprehensive tests for new functionality

### Pull Request Process

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes with tests
4. Update documentation as needed
5. Submit a pull request with detailed description

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---

## 🚀 Getting Support

- **Documentation**: Comprehensive examples in this README
- **Issues**: Report bugs and request features via GitHub Issues
- **Discussions**: Join community discussions for questions and best practices

For enterprise support and consulting, contact the Firefly Team