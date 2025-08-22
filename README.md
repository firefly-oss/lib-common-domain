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

## 📦 Installation

### Basic Installation

Choose one of the following approaches based on your needs:

#### Option 1: All-in-One (Recommended for Getting Started)

```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-all</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### Option 2: Modular Installation (Recommended for Production)

Core module (required):
```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Add specific adapters as needed:

**For Kafka:**
```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-kafka</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

**For RabbitMQ:**
```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-rabbit</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

**For AWS SQS:**
```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-sqs</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sqs</artifactId>
    <version>2.25.32</version>
</dependency>
```

### Gradle Installation

```groovy
implementation 'com.catalis:lib-common-domain-all:1.0.0-SNAPSHOT'
// or modular approach
implementation 'com.catalis:lib-common-domain-core:1.0.0-SNAPSHOT'
implementation 'com.catalis:lib-common-domain-kafka:1.0.0-SNAPSHOT'
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
    adapter: auto  # Automatically detects available messaging systems
```

### 3. Publishing Events

#### Declarative Approach with @EmitEvent

```java
import com.catalis.common.domain.events.outbound.EmitEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OrderService {
    
    @EmitEvent(topic = "'orders'", type = "'order.created'", key = "#result.id")
    public Mono<Order> createOrder(CreateOrderRequest request) {
        // Your business logic
        Order order = processOrder(request);
        return Mono.just(order);
    }
    
    @EmitEvent(
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
import com.catalis.common.domain.events.inbound.OnDomainEvent;
import org.springframework.stereotype.Component;

@Component
public class OrderEventHandler {
    
    @OnDomainEvent(topic = "orders", type = "order.created")
    public void handleOrderCreated(Order order) {
        log.info("New order created: {}", order.getId());
        // Process the event
        sendWelcomeEmail(order.getCustomerEmail());
        updateInventory(order.getItems());
    }
    
    @OnDomainEvent(topic = "payments", type = "payment.completed")
    public void handlePaymentCompleted(PaymentEvent event) {
        log.info("Payment completed for order: {}", event.getOrderId());
        // Update order status
        orderService.markAsPaid(event.getOrderId());
    }
}
```

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
    enabled: true
    adapter: auto  # Uses same adapter as domain events
```

## 💡 Usage Examples

### E-commerce Order Processing

```java
@Service
@Transactional
public class EcommerceOrderService {
    
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    
    @EmitEvent(topic = "'orders'", type = "'order.placed'", key = "#result.id")
    public Mono<Order> placeOrder(PlaceOrderRequest request) {
        Order order = Order.create(request);
        return orderRepository.save(order)
            .doOnSuccess(savedOrder -> log.info("Order placed: {}", savedOrder.getId()));
    }
    
    @EmitEvent(
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
    
    @OnDomainEvent(topic = "orders", type = "order.placed")
    public void handleOrderPlaced(Order order) {
        // Reserve inventory
        inventoryService.reserveItems(order.getItems())
            .subscribe();
        
        // Send confirmation email
        notificationService.sendOrderConfirmation(order.getCustomerEmail(), order)
            .subscribe();
    }
    
    @OnDomainEvent(topic = "payments", type = "payment.successful")
    public void handlePaymentSuccessful(PaymentEvent event) {
        // Update order status to paid
        orderService.updateOrderStatus(event.getOrderId(), OrderStatus.PAID)
            .subscribe();
    }
    
    @OnDomainEvent(topic = "orders", type = "order.status.changed")
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
    
    @EmitEvent(topic = "'users'", type = "'user.registered'", key = "#result.id")
    public Mono<User> registerUser(UserRegistrationRequest request) {
        User user = User.create(request);
        return userRepository.save(user);
    }
    
    @EmitEvent(topic = "'users'", type = "'user.profile.updated'", key = "#userId")
    public Mono<User> updateProfile(String userId, UpdateProfileRequest request) {
        return userRepository.findById(userId)
            .map(user -> user.updateProfile(request))
            .flatMap(userRepository::save);
    }
}

// Notification Service
@Component
public class UserNotificationHandler {
    
    @OnDomainEvent(topic = "users", type = "user.registered")
    public void sendWelcomeEmail(User user) {
        emailService.sendWelcomeEmail(user.getEmail(), user.getName())
            .subscribe();
    }
    
    @OnDomainEvent(topic = "orders", type = "order.placed")
    public void notifyOrderPlaced(Order order) {
        smsService.sendOrderConfirmation(order.getCustomerPhone(), order.getId())
            .subscribe();
    }
}

// Analytics Service
@Component 
public class EventAnalyticsHandler {
    
    @OnDomainEvent(topic = "*", type = "*")  // Listen to all events
    public void trackEvent(String eventPayload, 
                          @Header("event_type") String eventType,
                          @Header("event_topic") String topic) {
        analyticsService.recordEvent(topic, eventType, eventPayload)
            .subscribe();
    }
}
```

## 📊 Modules

The library is designed with a modular architecture to minimize dependencies:

### Core Module (`lib-common-domain-core`)
**Purpose**: Essential functionality and Application Events adapter

**Includes**:
- Domain event interfaces and envelopes
- `@EmitEvent` aspect and SpEL processing
- `@OnDomainEvent` event dispatcher
- Configuration properties and validation
- Actuator support (health checks, metrics)
- Distributed tracing utilities
- Spring Application Events adapter

**Dependencies**: Spring Boot WebFlux, AOP, Actuator, Micrometer

### Messaging Adapters

#### Kafka Module (`lib-common-domain-kafka`)
- Kafka-specific publishing and consumption
- Spring Kafka integration
- Testcontainers support for integration testing

#### RabbitMQ Module (`lib-common-domain-rabbit`)
- RabbitMQ publishing with configurable exchanges/routing
- Spring AMQP integration
- Testcontainers support for integration testing

#### SQS Module (`lib-common-domain-sqs`)
- AWS SQS publishing and consumption
- AWS SDK v2 integration
- LocalStack support for testing

### All-in-One Module (`lib-common-domain-all`)
**Purpose**: Convenience module including all adapters

**Use When**: You need multiple messaging systems or want to avoid dependency management

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

For enterprise support and consulting, contact the Catalis team.