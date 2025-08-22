# Firefly Common Domain Library

[![Maven](https://img.shields.io/badge/Maven-1.0.0--SNAPSHOT-blue.svg)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)]()

A comprehensive Spring Boot library that provides domain-driven design (DDD) capabilities with reactive programming support, featuring multi-adapter event publishing and consumption, step events integration, and structured JSON logging.

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Installation](#-installation)
- [Quick Start](#-quick-start)
- [Configuration](#-configuration)
- [Usage](#-usage)
  - [Publishing Domain Events](#publishing-domain-events)
  - [Consuming Domain Events](#consuming-domain-events)
  - [Step Events Integration](#step-events-integration)
- [Messaging Adapters](#-messaging-adapters)
- [Configuration Reference](#-configuration-reference)
- [Advanced Usage](#-advanced-usage)
- [Examples](#-examples)
- [Contributing](#-contributing)
- [License](#-license)

## 🌟 Overview

The Firefly Common Domain Library is designed to simplify domain-driven development in Spring Boot applications by providing:

- **Multi-transport Event Publishing**: Support for Kafka, RabbitMQ, SQS, and Spring Application Events
- **Reactive Programming**: Built with Project Reactor for non-blocking operations
- **Hexagonal Architecture**: Clean separation between ports and adapters
- **Auto-configuration**: Zero-configuration setup with sensible defaults
- **Aspect-Oriented Programming**: Declarative event publishing with annotations
- **Type-safe Configuration**: Comprehensive configuration properties with validation

## ✨ Features

### Domain Events
- **Multiple Messaging Adapters**: Kafka, RabbitMQ, SQS, Application Events
- **Auto-detection**: Automatically selects the best available adapter
- **Declarative Publishing**: `@EmitEvent` annotation with SpEL support
- **Programmatic Publishing**: Direct API for complex scenarios
- **Event Consumption**: Annotation-driven event handlers
- **Reactive Support**: Full Project Reactor integration

### Step Events Integration
- **Transactional Engine Support**: Integrates with `lib-transactional-engine`
- **Bridge Pattern**: Delegates to domain event publishers when available
- **Same Adapter Support**: Uses the same messaging infrastructure

### Additional Features
- **JSON Logging**: Structured logging with Logback integration
- **Spring Boot Auto-configuration**: Seamless integration with Spring Boot
- **Comprehensive Testing**: Extensive test coverage with examples
- **Flexible Configuration**: YAML/Properties-based configuration

## 🚀 Installation

### Maven

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

Add the dependency to your `build.gradle`:

```groovy
implementation 'com.catalis:lib-common-domain:1.0.0-SNAPSHOT'
```

### Additional Dependencies

Depending on your chosen messaging adapter, include the appropriate dependencies:

**For Kafka:**
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

**For RabbitMQ:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

**For Amazon SQS:**
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sqs</artifactId>
    <version>2.25.32</version>
</dependency>
```

## 🏁 Quick Start

### 1. Enable Auto-configuration

The library uses Spring Boot auto-configuration. Simply add it to your classpath and it will be automatically configured.

### 2. Basic Configuration

Add to your `application.yml`:

```yaml
firefly:
  events:
    enabled: true
    adapter: auto  # or kafka, rabbit, sqs, application_event, noop
```

### 3. Publishing Events

**Declarative approach** using `@EmitEvent`:

```java
@Service
public class OrderService {
    
    @EmitEvent(topic = "'orders'", type = "'order.created'", key = "#result.id")
    public Mono<Order> createOrder(CreateOrderRequest request) {
        // Your business logic here
        return Mono.just(new Order(UUID.randomUUID().toString(), request.getAmount()));
    }
}
```

**Programmatic approach**:

```java
@Service
public class OrderService {
    
    private final DomainEventPublisher eventPublisher;
    
    public OrderService(DomainEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    public Mono<Order> createOrder(CreateOrderRequest request) {
        Order order = new Order(UUID.randomUUID().toString(), request.getAmount());
        
        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("orders")
            .type("order.created")
            .key(order.getId())
            .payload(order)
            .build();
            
        return eventPublisher.publish(event)
            .thenReturn(order);
    }
}
```

### 4. Consuming Events

```java
@Component
public class OrderEventHandler {
    
    @OnDomainEvent(topic = "orders", type = "order.created")
    public void handleOrderCreated(Order order) {
        log.info("Order created: {}", order.getId());
        // Handle the event
    }
    
    @OnDomainEvent(topic = "orders", type = "order.cancelled")
    public void handleOrderCancelled(String orderId) {
        log.info("Order cancelled: {}", orderId);
        // Handle the event
    }
}
```

## ⚙️ Configuration

### Basic Configuration

```yaml
firefly:
  events:
    enabled: true
    adapter: auto  # auto, kafka, rabbit, sqs, application_event, noop
```

### Kafka Configuration

```yaml
firefly:
  events:
    adapter: kafka
    kafka:
      template-bean-name: myKafkaTemplate  # optional
      use-messaging-if-available: true
```

### RabbitMQ Configuration

```yaml
firefly:
  events:
    adapter: rabbit
    rabbit:
      template-bean-name: myRabbitTemplate  # optional
      exchange: "${topic}"  # SpEL expression
      routing-key: "${type}"  # SpEL expression
```

### SQS Configuration

```yaml
firefly:
  events:
    adapter: sqs
    sqs:
      client-bean-name: mySqsClient  # optional
      queue-url: "https://sqs.region.amazonaws.com/account/queue"
      queue-name: "my-queue"  # alternative to queue-url
```

### Consumer Configuration

```yaml
firefly:
  events:
    consumer:
      enabled: true
      type-header: "event_type"
      key-header: "event_key"
      kafka:
        topics:
          - "orders"
          - "payments"
        consumer-factory-bean-name: myConsumerFactory  # optional
        group-id: "my-service"  # optional
      rabbit:
        queues:
          - "orders.queue"
          - "payments.queue"
      sqs:
        queue-url: "https://sqs.region.amazonaws.com/account/consumer-queue"
        queue-name: "consumer-queue"  # alternative to queue-url
        wait-time-seconds: 10
        max-messages: 10
        poll-delay-millis: 1000
```

## 📝 Usage

### Publishing Domain Events

#### Using @EmitEvent Annotation

The `@EmitEvent` annotation provides a declarative way to publish events:

```java
@Service
public class PaymentService {
    
    // Basic usage - publishes method result as payload
    @EmitEvent(topic = "'payments'", type = "'payment.processed'")
    public Mono<PaymentResult> processPayment(PaymentRequest request) {
        // Process payment
        return Mono.just(new PaymentResult(request.getId(), "SUCCESS"));
    }
    
    // Custom key and payload using SpEL
    @EmitEvent(
        topic = "'payments'", 
        type = "'payment.failed'",
        key = "#request.userId",
        payload = "{'error': #result.errorCode, 'amount': #request.amount}"
    )
    public PaymentResult processFailedPayment(PaymentRequest request) {
        return new PaymentResult(request.getId(), "FAILED");
    }
    
    // Using method parameters in expressions
    @EmitEvent(
        topic = "'orders'", 
        type = "'order.updated'",
        key = "#orderId",
        payload = "{'orderId': #orderId, 'status': #status}"
    )
    public void updateOrderStatus(String orderId, String status) {
        // Update logic
    }
}
```

#### Programmatic Publishing

For complex scenarios, use the `DomainEventPublisher` directly:

```java
@Service
public class ComplexEventService {
    
    private final DomainEventPublisher eventPublisher;
    
    public ComplexEventService(DomainEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    public Mono<Void> publishComplexEvent(BusinessEntity entity) {
        Map<String, Object> headers = Map.of(
            "version", "1.0",
            "source", "order-service",
            "correlation-id", UUID.randomUUID().toString()
        );
        
        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("business-events")
            .type("entity.state.changed")
            .key(entity.getId())
            .payload(entity)
            .headers(headers)
            .build();
            
        return eventPublisher.publish(event);
    }
}
```

### Consuming Domain Events

#### Basic Event Handling

```java
@Component
public class OrderEventHandler {
    
    private static final Logger log = LoggerFactory.getLogger(OrderEventHandler.class);
    
    @OnDomainEvent(topic = "orders", type = "order.created")
    public void handleOrderCreated(Order order) {
        log.info("Processing new order: {}", order.getId());
        // Handle order creation
    }
    
    @OnDomainEvent(topic = "orders", type = "order.updated")
    public void handleOrderUpdated(OrderUpdateEvent event) {
        log.info("Order {} updated: {}", event.getOrderId(), event.getChanges());
        // Handle order update
    }
}
```

#### Advanced Event Handling with Type Conversion

The library automatically converts JSON payloads to your target types:

```java
@Component
public class PaymentEventHandler {
    
    // Automatic deserialization from JSON string to PaymentEvent
    @OnDomainEvent(topic = "payments", type = "payment.completed")
    public void handlePaymentCompleted(PaymentEvent event) {
        // event is automatically deserialized from JSON
        processPayment(event);
    }
    
    // Raw string handling
    @OnDomainEvent(topic = "payments", type = "payment.raw")
    public void handleRawPayment(String jsonPayload) {
        // Handle raw JSON string
        ObjectMapper mapper = new ObjectMapper();
        // Custom processing
    }
}
```

### Step Events Integration

The library integrates with the Firefly Transactional Engine:

```yaml
firefly:
  stepevents:
    enabled: true
    adapter: auto  # Uses same adapters as domain events
```

The step events will automatically use the same messaging infrastructure as domain events when available.

## 🔌 Messaging Adapters

### Auto-Detection Priority

When using `adapter: auto`, the library detects available messaging systems in this order:

1. **Kafka** - if `KafkaTemplate` bean is available
2. **RabbitMQ** - if `RabbitTemplate` bean is available  
3. **SQS** - if `SqsAsyncClient` bean is available
4. **Application Events** - fallback option

### Kafka Adapter

**Features:**
- Uses Spring Kafka's `KafkaTemplate`
- Supports both messaging and direct template usage
- Topic-based routing

**Configuration:**
```yaml
firefly:
  events:
    adapter: kafka
    kafka:
      template-bean-name: customKafkaTemplate
      use-messaging-if-available: true
```

### RabbitMQ Adapter

**Features:**
- Uses Spring AMQP's `RabbitTemplate`
- Configurable exchange and routing key patterns
- Supports SpEL expressions for dynamic routing

**Configuration:**
```yaml
firefly:
  events:
    adapter: rabbit
    rabbit:
      template-bean-name: customRabbitTemplate
      exchange: "events.${topic}"
      routing-key: "${type}.${key}"
```

### SQS Adapter

**Features:**
- Uses AWS SDK v2 `SqsAsyncClient`
- Supports both queue URL and queue name
- Configurable SQS client

**Configuration:**
```yaml
firefly:
  events:
    adapter: sqs
    sqs:
      client-bean-name: customSqsClient
      queue-url: "https://sqs.us-east-1.amazonaws.com/123456789/events"
      # OR
      queue-name: "events-queue"
```

### Application Events Adapter

**Features:**
- Uses Spring's `ApplicationEventPublisher`
- No external dependencies required
- Useful for testing and monolithic applications

**Configuration:**
```yaml
firefly:
  events:
    adapter: application_event
```

### No-Op Adapter

**Features:**
- Discards all events (useful for testing)
- No processing overhead

**Configuration:**
```yaml
firefly:
  events:
    adapter: noop
```

## 📖 Configuration Reference

### Domain Events Properties

| Property | Default | Description |
|----------|---------|-------------|
| `firefly.events.enabled` | `true` | Enable/disable domain events |
| `firefly.events.adapter` | `auto` | Messaging adapter selection |
| `firefly.events.kafka.template-bean-name` | - | Custom KafkaTemplate bean name |
| `firefly.events.kafka.use-messaging-if-available` | `true` | Use Spring Messaging abstractions |
| `firefly.events.rabbit.template-bean-name` | - | Custom RabbitTemplate bean name |
| `firefly.events.rabbit.exchange` | `${topic}` | Exchange pattern (SpEL) |
| `firefly.events.rabbit.routing-key` | `${type}` | Routing key pattern (SpEL) |
| `firefly.events.sqs.client-bean-name` | - | Custom SqsAsyncClient bean name |
| `firefly.events.sqs.queue-url` | - | SQS queue URL |
| `firefly.events.sqs.queue-name` | - | SQS queue name |

### Consumer Properties

| Property | Default | Description |
|----------|---------|-------------|
| `firefly.events.consumer.enabled` | `false` | Enable event consumption |
| `firefly.events.consumer.type-header` | `event_type` | Header name for event type |
| `firefly.events.consumer.key-header` | `event_key` | Header name for event key |
| `firefly.events.consumer.kafka.topics` | `[]` | Kafka topics to consume |
| `firefly.events.consumer.kafka.consumer-factory-bean-name` | - | Custom ConsumerFactory bean |
| `firefly.events.consumer.kafka.group-id` | - | Kafka consumer group ID |
| `firefly.events.consumer.rabbit.queues` | `[]` | RabbitMQ queues to consume |
| `firefly.events.consumer.sqs.queue-url` | - | SQS consumer queue URL |
| `firefly.events.consumer.sqs.queue-name` | - | SQS consumer queue name |
| `firefly.events.consumer.sqs.wait-time-seconds` | `10` | SQS long polling wait time |
| `firefly.events.consumer.sqs.max-messages` | `10` | SQS max messages per poll |
| `firefly.events.consumer.sqs.poll-delay-millis` | `1000` | Delay between polls |

### Step Events Properties

| Property | Default | Description |
|----------|---------|-------------|
| `firefly.stepevents.enabled` | `true` | Enable/disable step events |
| `firefly.stepevents.adapter` | `auto` | Messaging adapter (same as domain events) |

## 🚀 Advanced Usage

### Custom Event Publishers

Implement your own event publisher:

```java
@Component
public class CustomDomainEventPublisher implements DomainEventPublisher {
    
    @Override
    public Mono<Void> publish(DomainEventEnvelope envelope) {
        // Custom publishing logic
        return Mono.fromRunnable(() -> {
            log.info("Publishing event: {} to {}", envelope.type, envelope.topic);
            // Your custom implementation
        });
    }
}
```

### Custom Event Handlers

Create sophisticated event handlers:

```java
@Component
public class AuditEventHandler {
    
    private final AuditService auditService;
    
    public AuditEventHandler(AuditService auditService) {
        this.auditService = auditService;
    }
    
    @OnDomainEvent(topic = "orders", type = "order.created")
    @OnDomainEvent(topic = "orders", type = "order.updated")
    @OnDomainEvent(topic = "orders", type = "order.cancelled")
    public void auditOrderEvent(String eventPayload, 
                               @Header("event_type") String eventType,
                               @Header("correlation_id") String correlationId) {
        auditService.recordEvent(eventType, eventPayload, correlationId);
    }
}
```

### Conditional Configuration

Use Spring profiles for environment-specific configuration:

```yaml
# application-dev.yml
firefly:
  events:
    adapter: application_event
```

```yaml  
# application-prod.yml
firefly:
  events:
    adapter: kafka
    kafka:
      template-bean-name: prodKafkaTemplate
```

### Testing

#### Integration Testing

```java
@SpringBootTest
@TestPropertySource(properties = {
    "firefly.events.adapter=application_event"
})
class EventIntegrationTest {
    
    @Autowired
    private OrderService orderService;
    
    @EventListener
    void handleEvent(DomainSpringEvent event) {
        // Verify events in tests
    }
    
    @Test
    void shouldPublishEventWhenOrderCreated() {
        // Test your event publishing
    }
}
```

#### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private DomainEventPublisher eventPublisher;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void shouldPublishEventWhenOrderCreated() {
        when(eventPublisher.publish(any())).thenReturn(Mono.empty());
        
        StepVerifier.create(orderService.createOrder(request))
            .expectNext(order)
            .verifyComplete();
            
        verify(eventPublisher).publish(argThat(envelope ->
            "orders".equals(envelope.topic) &&
            "order.created".equals(envelope.type)
        ));
    }
}
```

## 💡 Examples

### E-commerce Order Processing

```java
@Service
@Transactional
public class OrderProcessingService {
    
    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    
    @EmitEvent(topic = "'orders'", type = "'order.created'", key = "#result.id")
    public Mono<Order> createOrder(CreateOrderRequest request) {
        Order order = new Order(request);
        return orderRepository.save(order);
    }
    
    @EmitEvent(topic = "'orders'", type = "'order.payment.requested'", 
               payload = "{'orderId': #orderId, 'amount': #order.totalAmount}")
    public Mono<Order> requestPayment(String orderId) {
        return orderRepository.findById(orderId)
            .map(order -> {
                order.setStatus(OrderStatus.PAYMENT_REQUESTED);
                return order;
            });
    }
}

@Component
public class OrderEventHandlers {
    
    @OnDomainEvent(topic = "payments", type = "payment.completed")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        // Update order status, send confirmation email, etc.
    }
    
    @OnDomainEvent(topic = "inventory", type = "items.reserved")
    public void handleItemsReserved(ItemReservationEvent event) {
        // Proceed with order fulfillment
    }
}
```

### Microservices Communication

```java
// User Service
@Service
public class UserService {
    
    @EmitEvent(topic = "'users'", type = "'user.created'", key = "#result.id")
    public Mono<User> createUser(CreateUserRequest request) {
        return userRepository.save(new User(request));
    }
    
    @EmitEvent(topic = "'users'", type = "'user.profile.updated'", key = "#userId")
    public Mono<User> updateProfile(String userId, UpdateProfileRequest request) {
        return userRepository.findById(userId)
            .map(user -> user.updateProfile(request));
    }
}

// Notification Service
@Component
public class UserNotificationHandler {
    
    private final NotificationService notificationService;
    
    @OnDomainEvent(topic = "users", type = "user.created")
    public void sendWelcomeEmail(User user) {
        notificationService.sendWelcomeEmail(user.getEmail(), user.getName());
    }
    
    @OnDomainEvent(topic = "users", type = "user.profile.updated")  
    public void notifyProfileUpdate(UserProfileUpdatedEvent event) {
        notificationService.sendProfileUpdateNotification(event.getUserId());
    }
}

// Analytics Service  
@Component
public class UserAnalyticsHandler {
    
    @OnDomainEvent(topic = "users", type = "user.created")
    @OnDomainEvent(topic = "users", type = "user.profile.updated")
    public void trackUserEvent(String eventPayload, 
                              @Header("event_type") String eventType) {
        analyticsService.track(eventType, eventPayload);
    }
}
```

## 🤝 Contributing

We welcome contributions to the Firefly Common Domain Library! Please follow these guidelines:

### Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/firefly-oss/lib-common-domain.git
   cd lib-common-domain
   ```

2. **Build the project**
   ```bash
   ./mvnw clean compile
   ```

3. **Run tests**
   ```bash
   ./mvnw test
   ```

### Contribution Guidelines

- **Code Style**: Follow the existing code style and formatting
- **Tests**: Add comprehensive tests for new features
- **Documentation**: Update documentation for new features or changes
- **Commits**: Use clear, descriptive commit messages
- **Pull Requests**: Create detailed pull requests with clear descriptions

### Reporting Issues

When reporting issues, please include:
- Library version
- Spring Boot version
- Messaging system and version (if applicable)
- Complete stack trace
- Minimal reproduction case

### Feature Requests

For feature requests, please:
- Describe the use case clearly
- Explain why the feature would be beneficial
- Provide examples of how it would be used

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For support and questions:
- **Issues**: [GitHub Issues](https://github.com/firefly-oss/lib-common-domain/issues)
- **Documentation**: This README and inline code documentation
- **Examples**: Check the `src/test` directory for comprehensive examples

---