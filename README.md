# lib-common-domain

**A comprehensive Spring Boot library for domain-driven design (DDD) with CQRS and SAGA orchestration support.**

## 🚀 Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>2.0.0-SNAPSHOT</version>
    <!-- lib-common-cqrs is included transitively -->
</dependency>

<!-- For event-driven architecture features -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-eda</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Auto-Configuration

The framework auto-configures when detected on the classpath:

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
        // ✅ CQRS features available via lib-common-cqrs
        // ✅ SAGA step event bridge configured
        // ✅ Distributed tracing is enabled
        // ✅ Event publishing available via lib-common-eda
    }
}
```

### Publishing Events with lib-common-eda

For event publishing, use the `lib-common-eda` library:

```java
@Service
public class AccountService {

    private final EventPublisher eventPublisher;
    private final AccountRepository accountRepository;

    public Mono<Account> createAccount(CreateAccountCommand command) {
        return accountRepository.save(buildAccount(command))
            .flatMap(this::publishAccountCreatedEvent);
    }

    private Mono<Account> publishAccountCreatedEvent(Account account) {
        AccountCreatedEvent event = new AccountCreatedEvent(
            account.getId(),
            account.getCustomerId(),
            account.getType(),
            account.getBalance()
        );

        Map<String, Object> headers = Map.of(
            "event_type", "account.created",
            "aggregate_id", account.getId()
        );

        return eventPublisher.publish(event, "account-events", headers)
            .thenReturn(account);
    }
}
```

### Service Client Support

For service-to-service communication, use the dedicated `lib-common-client` library:

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-client</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

See the [lib-common-client documentation](../lib-common-client/README.md) for comprehensive service client usage.

## 🎯 Features

### 🎯 CQRS Framework (via lib-common-cqrs)
- **Command & Query Separation**: Clear separation of write and read operations
- **Handler Auto-Discovery**: Automatic registration of command and query handlers
- **Validation**: Jakarta Bean Validation integration
- **Authorization**: Built-in authorization framework
- **Caching**: Intelligent query result caching
- **Metrics & Tracing**: Built-in observability

### 🔄 SAGA Orchestration Integration
- **lib-transactional-engine**: Native integration with saga orchestration
- **Step Event Bridge**: Automatic saga step event publishing via lib-common-eda
- **Metadata Enrichment**: Context propagation through saga steps
- **EDA Integration**: Leverages lib-common-eda for reliable event delivery

### 📡 Event-Driven Architecture (via lib-common-eda)
For event publishing and consumption, use the dedicated `lib-common-eda` library:
- **Multi-Platform Support**: Kafka, RabbitMQ, AWS SQS/Kinesis, Spring Events
- **Reactive Streams**: Non-blocking event publishing and consumption
- **Resilience Patterns**: Circuit breakers, retries, rate limiting
- **Health Monitoring**: Real-time messaging platform health checks
- **Metrics**: Comprehensive event publishing and consumption metrics

### 🌍 ExecutionContext (via lib-common-cqrs)
- **Context Propagation**: Pass additional data not part of domain objects
- **Multi-Tenancy**: Tenant isolation and context awareness
- **Feature Flags**: Dynamic feature enablement
- **User Context**: Authentication and authorization data
- **Distributed Tracing**: Correlation across service boundaries

### 🔍 Comprehensive Observability
- **Metrics Collection**: Micrometer integration with custom metrics
- **Health Indicators**: Spring Boot Actuator health checks
- **Distributed Tracing**: Zipkin/Jaeger integration
- **Structured Logging**: JSON logging with correlation context

## 📖 Event-Driven Architecture with lib-common-eda

For event publishing and consumption, this library integrates with `lib-common-eda`. See the [lib-common-eda documentation](../lib-common-eda/README.md) for:

- **Supported Messaging Platforms**: Kafka, RabbitMQ, AWS SQS, AWS Kinesis, Spring Application Events
- **Configuration Options**: Platform-specific configuration
- **Event Publishing**: Unified EventPublisher API
- **Event Consumption**: Event listeners and handlers
- **Resilience Patterns**: Circuit breakers, retries, rate limiting
- **Health Monitoring**: Platform health indicators

### Quick Example

```yaml
# application.yml
firefly:
  eda:
    enabled: true
    default-publisher-type: KAFKA  # or RABBIT, SQS, KINESIS, APPLICATION_EVENT
    kafka:
      bootstrap-servers: localhost:9092
```

```java
@Service
public class OrderService {

    private final EventPublisher eventPublisher;

    public Mono<Order> createOrder(CreateOrderCommand command) {
        return processOrder(command)
            .flatMap(order -> publishOrderCreatedEvent(order));
    }

    private Mono<Order> publishOrderCreatedEvent(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(order.getId(), order.getTotal());

        Map<String, Object> headers = Map.of(
            "event_type", "order.created",
            "aggregate_id", order.getId()
        );

        return eventPublisher.publish(event, "order-events", headers)
            .thenReturn(order);
    }
}
```

## 🏗️ Architecture Overview

### Domain-Driven Design Support

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                  lib-common-domain Architecture                                             │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                                             │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐                              │
│  │   CQRS Framework    │    │   SAGA Integration  │    │   Observability     │                              │
│  │  (lib-common-cqrs)  │    │                     │    │                     │                              │
│  │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │                              │
│  │ │ CommandBus      │ │    │ │ Step Event      │ │    │ │ Metrics         │ │                              │
│  │ │ QueryBus        │ │    │ │ Bridge          │ │    │ │ Collection      │ │                              │
│  │ └─────────────────┘ │    │ └─────────────────┘ │    │ └─────────────────┘ │                              │
│  │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │                              │
│  │ │ Handler         │ │    │ │ Metadata        │ │    │ │ Health          │ │                              │
│  │ │ Auto-Discovery  │ │    │ │ Enrichment      │ │    │ │ Indicators      │ │                              │
│  │ └─────────────────┘ │    │ └─────────────────┘ │    │ └─────────────────┘ │                              │
│  │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │                              │
│  │ │ Validation      │ │    │ │ EDA Integration │ │    │ │ Distributed     │ │                              │
│  │ │ Authorization   │ │    │ │ (lib-common-eda)│ │    │ │ Tracing         │ │                              │
│  │ └─────────────────┘ │    │ └─────────────────┘ │    │ └─────────────────┘ │                              │
│  │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │                              │
│  │ │ ExecutionContext│ │    │ │ Saga Events     │ │    │ │ JSON Logging    │ │                              │
│  │ │ Multi-Tenancy   │ │    │ │ Compensation    │ │    │ │ Correlation     │ │                              │
│  │ └─────────────────┘ │    │ └─────────────────┘ │    │ └─────────────────┘ │                              │
│  └─────────────────────┘    └─────────────────────┘    └─────────────────────┘                              │
│                                                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────────────────┐   │
│  │                          External Dependencies                                                        │   │
│  │                                                                                                       │   │
│  │  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐                      │   │
│  │  │  lib-common-eda     │    │ lib-transactional-  │    │  lib-common-client  │                      │   │
│  │  │                     │    │      engine         │    │                     │                      │   │
│  │  │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │                      │   │
│  │  │ │ EventPublisher  │ │    │ │ StepEventPubl.  │ │    │ │ ServiceClient   │ │                      │   │
│  │  │ │ Multi-Platform  │ │    │ │ Interface       │ │    │ │ REST & gRPC     │ │                      │   │
│  │  │ └─────────────────┘ │    │ └─────────────────┘ │    │ └─────────────────┘ │                      │   │
│  │  │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │                      │   │
│  │  │ │ Kafka/RabbitMQ  │ │    │ │ Saga            │ │    │ │ Circuit Breaker │ │                      │   │
│  │  │ │ SQS/Kinesis     │ │    │ │ Orchestration   │ │    │ │ Retry Logic     │ │                      │   │
│  │  │ └─────────────────┘ │    │ └─────────────────┘ │    │ └─────────────────┘ │                      │   │
│  │  └─────────────────────┘    └─────────────────────┘    └─────────────────────┘                      │   │
│  └─────────────────────────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                                             │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

## 📡 Event Publishing with lib-common-eda

For event publishing and consumption, use the dedicated `lib-common-eda` library:

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-eda</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

The `lib-common-eda` library provides:
- **Multi-Platform Support**: Kafka, RabbitMQ, AWS SQS, AWS Kinesis, Spring Application Events
- **Unified API**: Single EventPublisher interface for all platforms
- **Reactive Streams**: Non-blocking event publishing and consumption
- **Resilience Patterns**: Circuit breakers, retries, rate limiting
- **Health Monitoring**: Real-time messaging platform health checks
- **Metrics**: Comprehensive event publishing and consumption metrics

### Event Publishing Example

```java
@Service
public class OrderService {

    private final EventPublisher eventPublisher;

    public Mono<Order> processOrder(ProcessOrderCommand command) {
        return createOrder(command)
            .flatMap(this::validateOrder)
            .flatMap(this::publishOrderEvents)
            .flatMap(this::updateOrderStatus);
    }

    private Mono<Order> publishOrderEvents(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getCustomerId(),
            order.getTotal()
        );

        Map<String, Object> headers = Map.of(
            "event_type", "order.created",
            "aggregate_id", order.getId(),
            "source", "order-service"
        );

        return eventPublisher.publish(event, "order-events", headers)
            .thenReturn(order);
    }
}
```

### Event Consumption Example

```java
@Component
public class OrderEventHandler {

    private final NotificationService notificationService;

    @EventListener
    public Mono<Void> handleOrderCreated(Event<OrderCreatedEvent> event) {
        OrderCreatedEvent payload = event.getPayload();

        return notificationService.sendOrderConfirmation(
            payload.getCustomerId(),
            payload.getOrderId(),
            payload.getTotal()
        );
    }
}
```

See the [lib-common-eda documentation](../lib-common-eda/README.md) for complete usage examples, configuration options, and advanced features.

## 🔗 Service Client Framework

For service-to-service communication, use the dedicated `lib-common-client` library:

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-client</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

The `lib-common-client` library provides:
- **Unified API**: Single interface for REST and gRPC communication
- **Circuit Breakers**: Built-in resilience patterns
- **Reactive Programming**: Non-blocking operations with Spring WebFlux
- **Health Checks**: Built-in service health monitoring
- **Request/Response Interceptors**: Extensible request processing
- **Type Safety**: Strong typing with support for generic types

See the [lib-common-client documentation](../lib-common-client/README.md) for complete usage examples and configuration options.



## 🔄 SAGA Orchestration Integration

This library provides integration between `lib-transactional-engine` (SAGA orchestration) and `lib-common-eda` (event publishing).

### Dependencies

```xml
<!-- SAGA orchestration -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-transactional-engine</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Event publishing -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-eda</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Step Event Bridge Configuration

The `StepEventPublisherBridge` automatically bridges SAGA step events to the EDA infrastructure:

```yaml
firefly:
  stepevents:
    enabled: true  # Default: true
    topic: saga-step-events  # Default: step-events

  eda:
    enabled: true
    default-publisher-type: KAFKA  # or RABBIT, SQS, KINESIS, APPLICATION_EVENT
    kafka:
      bootstrap-servers: localhost:9092
```

### How It Works

The bridge automatically:
1. Receives step events from `lib-transactional-engine` via the `StepEventPublisher` interface
2. Enriches events with metadata (saga ID, transaction ID, step name, status)
3. Publishes events through `lib-common-eda`'s `EventPublisher` to your configured messaging platform

### SAGA Implementation Example

For SAGA implementation details, see the [lib-transactional-engine documentation](../lib-transactional-engine/README.md).

```java
@Service
public class MoneyTransferSaga {

    private final SagaOrchestrator orchestrator;

    public Mono<TransferResult> executeTransfer(TransferCommand command) {
        return orchestrator.execute(
            SagaDefinition.builder()
                .sagaId(command.getSagaId())
                .step("debit-source", () -> debitAccount(command))
                .step("credit-target", () -> creditAccount(command))
                .compensation("debit-source", () -> refundAccount(command))
                .build()
        );
        // Step events are automatically published via StepEventPublisherBridge
    }
}
```

### Step Event Structure

Step events published through the bridge include:

```json
{
  "sagaId": "saga-123",
  "transactionId": "txn-456",
  "stepName": "debit-source-account",
  "status": "COMPLETED",
  "timestamp": "2025-01-05T10:30:00Z",
  "payload": { /* step result data */ }
}
```

Headers automatically added:
- `saga_id`: SAGA identifier
- `transaction_id`: Transaction identifier
- `step_name`: Step name
- `step_status`: Step status (STARTED, COMPLETED, FAILED, COMPENSATED)


## ⚙️ Configuration

### Basic Configuration

```yaml
# application.yml
firefly:
  # CQRS Configuration (from lib-common-cqrs)
  cqrs:
    enabled: true
    command:
      validation:
        enabled: true
    query:
      cache:
        enabled: true

  # SAGA Step Events Configuration
  stepevents:
    enabled: true  # Default: true
    topic: saga-step-events  # Default: step-events

  # Event-Driven Architecture (from lib-common-eda)
  eda:
    enabled: true
    default-publisher-type: KAFKA  # KAFKA, RABBIT, SQS, KINESIS, APPLICATION_EVENT
    kafka:
      bootstrap-servers: localhost:9092
      producer:
        acks: all
        retries: 3
      consumer:
        group-id: ${spring.application.name}
        auto-offset-reset: earliest
```

### Environment Variables

All configuration properties can be overridden with environment variables:

```bash
# CQRS
FIREFLY_CQRS_ENABLED=true
FIREFLY_CQRS_COMMAND_VALIDATION_ENABLED=true
FIREFLY_CQRS_QUERY_CACHE_ENABLED=true

# SAGA Step Events
FIREFLY_STEPEVENTS_ENABLED=true
FIREFLY_STEPEVENTS_TOPIC=saga-step-events

# Event-Driven Architecture
FIREFLY_EDA_ENABLED=true
FIREFLY_EDA_DEFAULT_PUBLISHER_TYPE=KAFKA
```

### Event Publishing Configuration (lib-common-eda)

For detailed event publishing configuration, see the [lib-common-eda documentation](../lib-common-eda/README.md).

#### Kafka Configuration
```yaml
firefly:
  eda:
    default-publisher-type: KAFKA
    kafka:
      bootstrap-servers: localhost:9092
      producer:
        acks: all
        retries: 3
        key-serializer: org.apache.kafka.common.serialization.StringSerializer
        value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

#### RabbitMQ Configuration
```yaml
firefly:
  eda:
    default-publisher-type: RABBIT
    rabbit:
      host: localhost
      port: 5672
      username: guest
      password: guest
      exchange: domain.events.exchange
```

#### AWS Configuration
```yaml
firefly:
  eda:
    default-publisher-type: SQS
    sqs:
      region: ${AWS_REGION:us-east-1}
      queue-name: domain-events-${spring.profiles.active}
```

## 🔍 Observability & Monitoring

### Spring Boot Actuator Integration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      show-components: always
```

### Available Metrics

#### CQRS Metrics (from lib-common-cqrs)
- `firefly.cqrs.command.executed.total` - Total commands executed
- `firefly.cqrs.command.execution.duration` - Command execution time
- `firefly.cqrs.query.executed.total` - Total queries executed
- `firefly.cqrs.query.execution.duration` - Query execution time
- `firefly.cqrs.query.cache.hits` - Query cache hits
- `firefly.cqrs.query.cache.misses` - Query cache misses

#### Event Publishing Metrics (from lib-common-eda)
- `firefly.eda.events.published.total` - Total events published
- `firefly.eda.events.publishing.duration` - Event publishing time
- `firefly.eda.events.consumed.total` - Total events consumed
- `firefly.eda.events.processing.duration` - Event processing time
- `firefly.eda.events.errors.total` - Event processing errors

#### SAGA Step Event Metrics
- `firefly.stepevents.published.total` - Total step events published
- `firefly.stepevents.publishing.duration` - Step event publishing time

### Health Indicators

The library provides health indicators for SAGA step event bridge:

```json
{
  "status": "UP",
  "components": {
    "stepEventBridge": {
      "status": "UP",
      "details": {
        "topic": "saga-step-events",
        "publisher": "KAFKA",
        "enabled": true
      }
    }
  }
}
```

For EDA health indicators (Kafka, RabbitMQ, SQS, Kinesis), see the [lib-common-eda documentation](../lib-common-eda/README.md#observability).

## 🧪 Testing

### Test Configuration

```yaml
# application-test.yml
firefly:
  cqrs:
    enabled: true
    command:
      validation:
        enabled: true
    query:
      cache:
        enabled: false  # Disable cache for predictable testing

  stepevents:
    enabled: true
    topic: test-step-events

  eda:
    default-publisher-type: APPLICATION_EVENT  # Use in-memory events for testing
```

### Integration Testing with Testcontainers

```java
@SpringBootTest
@Testcontainers
class StepEventBridgeIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:latest")
    );

    @Autowired
    private StepEventPublisherBridge stepEventBridge;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("firefly.eda.default-publisher-type", () -> "KAFKA");
    }

    @Test
    void shouldPublishStepEvent() {
        // Given
        StepEventEnvelope stepEvent = StepEventEnvelope.builder()
            .sagaId("saga-123")
            .transactionId("txn-456")
            .stepName("debit-account")
            .status("COMPLETED")
            .payload(Map.of("accountId", "ACC-123", "amount", 100.00))
            .build();

        // When
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete();
    }
}
```

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class StepEventPublisherBridgeTest {

    @Mock
    private EventPublisher eventPublisher;

    private StepEventPublisherBridge bridge;

    @BeforeEach
    void setUp() {
        bridge = new StepEventPublisherBridge("step-events", eventPublisher);
    }

    @Test
    void shouldPublishStepEventWithMetadata() {
        // Given
        when(eventPublisher.publish(any(), anyString(), anyMap()))
            .thenReturn(Mono.empty());

        StepEventEnvelope stepEvent = StepEventEnvelope.builder()
            .sagaId("saga-123")
            .stepName("test-step")
            .build();

        // When
        StepVerifier.create(bridge.publish(stepEvent))
            .verifyComplete();

        // Then
        verify(eventPublisher).publish(
            eq(stepEvent),
            eq("step-events"),
            argThat(headers ->
                headers.containsKey("saga_id") &&
                headers.containsKey("step_name")
            )
        );
    }
}
```

### Testing with lib-common-eda

For event publishing and consumption testing, see the [lib-common-eda testing documentation](../lib-common-eda/README.md#testing).

## 🔄 Integration with lib-common-cqrs

For CQRS functionality, use `lib-common-cqrs` alongside this library:

```xml
<!-- For domain layer capabilities -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>

<!-- For CQRS capabilities -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-cqrs</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- For service client capabilities -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-client</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**What you get from lib-common-cqrs:**
- CommandBus and QueryBus: Central dispatching for commands and queries
- Handler Annotations: @CommandHandlerComponent and @QueryHandlerComponent
- ExecutionContext: Context propagation for tenant isolation and cross-cutting concerns
- Automatic Validation: Jakarta Bean Validation integration
- Query Caching: Intelligent caching with configurable TTL
- Authorization Framework: Integration with lib-common-auth
- Metrics and Tracing: Built-in observability for CQRS operations

**Combined Usage Example:**
```java
@Service
public class BankingOrchestrationService {
    
    private final CommandBus commandBus;           // From lib-common-cqrs
    private final QueryBus queryBus;              // From lib-common-cqrs  
    private final DomainEventPublisher eventPublisher; // From lib-common-domain
    private final ServiceClient paymentService;   // From lib-common-client
    
    public Mono<TransferResult> processTransfer(TransferRequest request) {
        // ExecutionContext is from lib-common-cqrs
        ExecutionContext context = ExecutionContext.builder()
            .userId(request.getUserId())
            .tenantId(request.getTenantId())
            .correlationId(request.getCorrelationId())
            .build();
        
        return validateTransfer(request, context)
            .flatMap(validation -> executeTransfer(request, context))
            .flatMap(this::publishTransferEvents)
            .flatMap(this::notifyExternalSystems);
    }
    
    private Mono<TransferResult> executeTransfer(TransferRequest request, ExecutionContext context) {
        // Use CQRS command
        TransferMoneyCommand command = new TransferMoneyCommand(
            request.getSourceAccountId(),
            request.getTargetAccountId(),
            request.getAmount()
        );
        
        return commandBus.send(command, context);
    }
    
    private Mono<TransferResult> publishTransferEvents(TransferResult result) {
        // Use domain events
        TransferCompletedEvent event = new TransferCompletedEvent(
            result.getTransferId(),
            result.getSourceAccountId(),
            result.getTargetAccountId(),
            result.getAmount()
        );
        
        return eventPublisher.publish(event)
            .thenReturn(result);
    }
    
    private Mono<TransferResult> notifyExternalSystems(TransferResult result) {
        // Use service client
        TransferNotification notification = new TransferNotification(
            result.getTransferId(),
            result.getAmount(),
            "COMPLETED"
        );
        
        return paymentService.post("/notifications", Void.class)
            .withBody(notification)
            .execute()
            .thenReturn(result);
    }
}
```

> **Note**: For ExecutionContext usage patterns, authorization, caching, and CQRS handler development, see the [lib-common-cqrs documentation](../lib-common-cqrs/README.md).

## 🤝 Contributing

1. Follow the existing code style and patterns
2. Write comprehensive tests for new features
3. Update documentation for any API changes
4. Use reactive programming patterns consistently
5. Ensure proper integration with messaging platforms

## 📜 License

Copyright 2025 Firefly Software Solutions Inc

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.