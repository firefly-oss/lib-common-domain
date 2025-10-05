# lib-common-domain

**A comprehensive Spring Boot library for domain-driven design (DDD) with reactive programming support, featuring multi-messaging event publishing, service client framework, resilience patterns, and saga integration.**

## 🚀 Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>2.0.0-SNAPSHOT</version>
    <!-- lib-common-cqrs is included transitively -->
</dependency>
```

### Auto-Configuration

The framework auto-configures when detected on the classpath:

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
        // ✅ Domain event publishers are automatically available
        // ✅ Service clients are auto-configured  
        // ✅ Circuit breakers are ready to use
        // ✅ Distributed tracing is enabled
        // ✅ CQRS features available via lib-common-cqrs
    }
}
```

### Your First Domain Event Publisher

```java
@Service
public class AccountService {
    
    private final DomainEventPublisher eventPublisher;
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
        
        return eventPublisher.publish(event)
            .thenReturn(account);
    }
}
```

### Your First Service Client

```java
@Service 
public class CustomerServiceClient {
    
    private final ServiceClient serviceClient;
    
    public CustomerServiceClient() {
        this.serviceClient = ServiceClient.rest("customer-service")
            .baseUrl("http://customer-service:8080")
            .timeout(Duration.ofSeconds(30))
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
    
    public Mono<Customer> getCustomer(String customerId) {
        return serviceClient.get("/customers/{id}", Customer.class)
            .withPathParam("id", customerId)
            .withCircuitBreaker("customer-service")
            .withRetry(3)
            .execute();
    }
    
    public Mono<Customer> createCustomer(CustomerCreateRequest request) {
        return serviceClient.post("/customers", Customer.class)
            .withBody(request)
            .execute();
    }
}
```

## 🎯 Features

### 📡 Multi-Messaging Domain Events
- **Unified API**: Single interface for multiple messaging platforms
- **Reactive Streams**: Non-blocking event publishing and consumption
- **Auto-Configuration**: Automatic adapter selection and configuration
- **Flexible Routing**: Topic-based routing with filtering capabilities
- **Reliable Delivery**: Built-in error handling and retry mechanisms

### 🔗 Service Client Framework
- **REST & gRPC**: Unified interface for different protocols
- **Circuit Breakers**: Automatic failure detection and recovery
- **Retry Logic**: Configurable retry with exponential backoff
- **Load Balancing**: Multiple instance support with health checks
- **Request Tracing**: Automatic correlation ID propagation

### 🛡️ Resilience Patterns
- **Circuit Breaker Manager**: Advanced circuit breaker with multiple states
- **Sliding Window**: Time-based and count-based failure detection
- **Bulkhead Isolation**: Resource isolation between services
- **Timeout Management**: Configurable timeouts per operation
- **Health Monitoring**: Real-time service health tracking

### 🔄 Saga Integration
- **lib-transactional-engine**: Native integration with saga orchestration
- **Step Event Bridge**: Automatic saga step event publishing
- **Metadata Enrichment**: Context propagation through saga steps
- **Error Handling**: Built-in compensation and rollback support

### 🌍 ExecutionContext
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
- **Performance Monitoring**: JVM, HTTP client, and thread pool metrics

## 📖 Supported Messaging Platforms

### ✅ Kafka
```yaml
firefly:
  events:
    adapter: KAFKA
    kafka:
      bootstrap-servers: localhost:9092
      producer:
        key-serializer: org.apache.kafka.common.serialization.StringSerializer
        value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      consumer:
        group-id: my-app
        key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
        value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
```

### ✅ RabbitMQ
```yaml
firefly:
  events:
    adapter: RABBIT
    rabbit:
      host: localhost
      port: 5672
      username: guest
      password: guest
      exchange: domain.events
      routing-key-prefix: banking
```

### ✅ AWS SQS
```yaml
firefly:
  events:
    adapter: SQS
    sqs:
      region: us-east-1
      queue-name: domain-events-queue
      access-key: ${AWS_ACCESS_KEY_ID}
      secret-key: ${AWS_SECRET_ACCESS_KEY}
```

### ✅ AWS Kinesis
```yaml
firefly:
  events:
    adapter: KINESIS
    kinesis:
      region: us-east-1
      stream-name: domain-events-stream
      shard-count: 3
      access-key: ${AWS_ACCESS_KEY_ID}
      secret-key: ${AWS_SECRET_ACCESS_KEY}
```

### ✅ Spring ApplicationEvents
```yaml
firefly:
  events:
    adapter: APPLICATION_EVENT
    # No additional configuration needed - works out of the box
```

## 🏗️ Architecture Overview

### Domain-Driven Design Support

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                  lib-common-domain Architecture                                             │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                                             │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐   │
│  │   Domain Events     │    │   Service Clients   │    │   Resilience        │    │   Saga Integration  │   │
│  │                     │    │                     │    │   Patterns          │    │                     │   │
│  │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │   │
│  │ │ Multi-Messaging │ │    │ │ REST Clients    │ │    │ │ Circuit Breaker │ │    │ │ Step Event      │ │   │
│  │ │ Event Publisher │ │    │ │ gRPC Clients    │ │    │ │ Manager         │ │    │ │ Bridge          │ │   │
│  │ └─────────────────┘ │    │ └─────────────────┘ │    │ └─────────────────┘ │    │ └─────────────────┘ │   │
│  │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │   │
│  │ │ Event Listeners │ │    │ │ Request Builder │ │    │ │ Sliding Window  │ │    │ │ Metadata        │ │   │
│  │ │ & Filters       │ │    │ │ Response Mapper │ │    │ │ State Machine   │ │    │ │ Enrichment      │ │   │
│  │ └─────────────────┘ │    │ └─────────────────┘ │    │ └─────────────────┘ │    │ └─────────────────┘ │   │
│  │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │   │
│  │ │ Kafka/RabbitMQ  │ │    │ │ Health Checks   │ │    │ │ Retry Logic     │ │    │ │ Saga Events     │ │   │
│  │ │ SQS/Kinesis     │ │    │ │ Load Balancing  │ │    │ │ Timeout Mgmt    │ │    │ │ Compensation    │ │   │
│  │ │ ApplicationEvent│ │    │ │ Metrics         │ │    │ │ Bulkhead Isol.  │ │    │ │ Rollback        │ │   │
│  │ └─────────────────┘ │    │ └─────────────────┘ │    │ └─────────────────┘ │    │ └─────────────────┘ │   │
│  └─────────────────────┘    └─────────────────────┘    └─────────────────────┘    └─────────────────────┘   │
│                                                                                                             │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐                              │
│  │ ExecutionContext    │    │ Observability       │    │ CQRS Integration    │                              │
│  │                     │    │                     │    │                     │                              │
│  │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │                              │
│  │ │ Context         │ │    │ │ Metrics         │ │    │ │ lib-common-cqrs │ │                              │
│  │ │ Propagation     │ │    │ │ Collection      │ │    │ │ Integration     │ │                              │
│  │ └─────────────────┘ │    │ └─────────────────┘ │    │ └─────────────────┘ │                              │
│  │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │                              │
│  │ │ Tenant          │ │    │ │ Health          │ │    │ │ CommandBus      │ │                              │
│  │ │ Awareness       │ │    │ │ Indicators      │ │    │ │ QueryBus        │ │                              │
│  │ └─────────────────┘ │    │ └─────────────────┘ │    │ │ ExecutionCtx    │ │                              │
│  │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ └─────────────────┘ │                              │
│  │ │ Feature Flags   │ │    │ │ Distributed     │ │    │                     │                              │
│  │ │ User Context    │ │    │ │ Tracing         │ │    │                     │                              │
│  │ └─────────────────┘ │    │ └─────────────────┘ │    │                     │                              │
│  └─────────────────────┘    └─────────────────────┘    └─────────────────────┘                              │
│                                                                                                             │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

## 📡 Domain Events

### Event Publishing

Publish domain events to multiple messaging platforms with a unified API:

```java
// Simple event publishing
@Service
public class OrderService {
    
    private final DomainEventPublisher eventPublisher;
    
    public Mono<Order> processOrder(ProcessOrderCommand command) {
        return createOrder(command)
            .flatMap(this::validateOrder)
            .flatMap(this::publishOrderEvents)
            .flatMap(this::updateOrderStatus);
    }
    
    private Mono<Order> publishOrderEvents(Order order) {
        // Multiple events can be published
        List<DomainEvent> events = List.of(
            new OrderCreatedEvent(order.getId(), order.getCustomerId(), order.getTotal()),
            new InventoryReservedEvent(order.getId(), order.getItems()),
            new PaymentRequestedEvent(order.getId(), order.getTotal(), order.getCustomerId())
        );
        
        return Flux.fromIterable(events)
            .flatMap(eventPublisher::publish)
            .then(Mono.just(order));
    }
}

// Event with metadata
public class PaymentProcessedEvent implements DomainEvent {
    
    private final String paymentId;
    private final String orderId;
    private final BigDecimal amount;
    private final PaymentMethod paymentMethod;
    private final Instant processedAt;
    
    @Override
    public String getEventType() {
        return "payment.processed";
    }
    
    @Override
    public String getAggregateId() {
        return paymentId;
    }
    
    @Override
    public Map<String, Object> getMetadata() {
        return Map.of(
            "orderId", orderId,
            "paymentMethod", paymentMethod.name(),
            "processedAt", processedAt.toString(),
            "source", "payment-service"
        );
    }
}
```

### Event Consumption

Listen to domain events with flexible filtering:

```java
@EventListener
@Component
public class OrderEventHandler {
    
    private final NotificationService notificationService;
    private final InventoryService inventoryService;
    
    @EventHandler(eventType = "order.created")
    public Mono<Void> handleOrderCreated(OrderCreatedEvent event) {
        return notificationService.sendOrderConfirmation(
            event.getCustomerId(),
            event.getOrderId(),
            event.getTotal()
        );
    }
    
    @EventHandler(eventType = "payment.failed")
    public Mono<Void> handlePaymentFailed(PaymentFailedEvent event) {
        return inventoryService.releaseReservation(event.getOrderId())
            .then(notificationService.sendPaymentFailureNotification(
                event.getCustomerId(), event.getReason()));
    }
    
    // Filter events by metadata
    @EventHandler(eventType = "order.*", filter = "metadata.source == 'mobile-app'")
    public Mono<Void> handleMobileOrderEvents(DomainEvent event) {
        return mobileAnalyticsService.trackOrderEvent(event);
    }
}
```

### Event Filtering

Configure sophisticated event filtering:

```java
@Configuration
public class EventFilterConfiguration {
    
    @Bean
    public EventFilter orderEventFilter() {
        return CompositeEventFilter.builder()
            .add(new TypeFilter("order.*"))
            .add(new HeaderFilter("source", "order-service"))
            .add(new TopicFilter("banking.orders"))
            .build();
    }
    
    @Bean
    public EventFilter paymentEventFilter() {
        return CompositeEventFilter.builder()
            .add(new TypeFilter("payment.*"))
            .add(event -> {
                // Custom filtering logic
                BigDecimal amount = (BigDecimal) event.getMetadata().get("amount");
                return amount.compareTo(new BigDecimal("1000.00")) > 0;
            })
            .build();
    }
}
```

## 🔗 Service Client Framework

### REST Client Configuration

Create type-safe, resilient REST clients:

```java
@Configuration
public class ServiceClientConfiguration {
    
    @Bean
    public ServiceClient customerServiceClient() {
        return ServiceClient.rest("customer-service")
            .baseUrl("http://customer-service:8080")
            .timeout(Duration.ofSeconds(30))
            .maxConnections(100)
            .defaultHeader("Authorization", "Bearer ${jwt.token}")
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
    
    @Bean
    public ServiceClient paymentServiceClient() {
        return ServiceClient.rest("payment-service")
            .baseUrl("https://payment-service.banking.com")
            .timeout(Duration.ofSeconds(45))
            .maxConnections(50)
            .build();
    }
}
```

### Service Client Usage

```java
@Service
public class AccountService {
    
    private final ServiceClient customerService;
    private final ServiceClient paymentService;
    
    public Mono<AccountSummary> getAccountSummary(String accountId) {
        return getAccount(accountId)
            .flatMap(account -> enhanceWithCustomerInfo(account))
            .flatMap(account -> enhanceWithPaymentInfo(account))
            .map(this::toAccountSummary);
    }
    
    private Mono<Account> getAccount(String accountId) {
        return accountRepository.findById(accountId)
            .switchIfEmpty(Mono.error(new AccountNotFoundException(accountId)));
    }
    
    private Mono<Account> enhanceWithCustomerInfo(Account account) {
        return customerService.get("/customers/{id}", Customer.class)
            .withPathParam("id", account.getCustomerId())
            .withTimeout(Duration.ofSeconds(10))
            .withCircuitBreaker("customer-service")
            .withRetry(3)
            .execute()
            .map(customer -> account.withCustomerInfo(customer))
            .onErrorReturn(account); // Graceful degradation
    }
    
    private Mono<Account> enhanceWithPaymentInfo(Account account) {
        return paymentService.get("/payment-methods", PaymentMethodList.class)
            .withQueryParam("customerId", account.getCustomerId())
            .withHeader("X-Account-ID", account.getId())
            .execute()
            .map(paymentMethods -> account.withPaymentMethods(paymentMethods.getItems()))
            .onErrorReturn(account);
    }
}
```

### gRPC Client Support

```java
@Service
public class RiskAssessmentServiceClient {
    
    private final ServiceClient grpcClient;
    
    public RiskAssessmentServiceClient() {
        this.grpcClient = ServiceClient.grpc("risk-service", RiskServiceGrpc.RiskServiceBlockingStub.class)
            .address("risk-service:9090")
            .timeout(Duration.ofSeconds(15))
            .usePlaintext() // For internal services
            .build();
    }
    
    public Mono<RiskScore> assessRisk(String customerId, BigDecimal amount) {
        RiskAssessmentRequest request = RiskAssessmentRequest.newBuilder()
            .setCustomerId(customerId)
            .setAmount(amount.toString())
            .setTransactionType("TRANSFER")
            .build();
            
        return grpcClient.post("/assess-risk", RiskScore.class)
            .withBody(request)
            .execute();
    }
}
```

## 🛡️ Resilience Patterns

### Circuit Breaker Configuration

```java
@Configuration
public class ResilienceConfiguration {
    
    @Bean
    public CircuitBreaker customerServiceCircuitBreaker() {
        return CircuitBreaker.ofDefaults("customer-service")
            .toBuilder()
            .failureRateThreshold(50.0f)                    // 50% failure rate
            .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30 seconds
            .slidingWindowSize(20)                          // 20 calls window
            .minimumNumberOfCalls(5)                        // Min 5 calls before evaluation
            .build();
    }
    
    @Bean
    public Retry paymentServiceRetry() {
        return Retry.ofDefaults("payment-service")
            .toBuilder()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(2))
            .retryOnException(throwable -> 
                throwable instanceof ConnectException || 
                throwable instanceof SocketTimeoutException)
            .build();
    }
}
```

### Advanced Circuit Breaker Manager

```java
@Service
public class AdvancedRiskService {
    
    private final AdvancedResilienceManager resilienceManager;
    
    public Mono<RiskAssessment> assessTransactionRisk(TransactionRequest request) {
        return resilienceManager.executeWithResilience(
            "risk-assessment",
            () -> performRiskAssessment(request),
            ResilienceConfig.builder()
                .circuitBreaker(CircuitBreakerConfig.builder()
                    .failureRateThreshold(60.0f)
                    .waitDurationInOpenState(Duration.ofMinutes(2))
                    .slidingWindowType(SlidingWindowType.TIME_BASED)
                    .slidingWindowSize(60)
                    .build())
                .retry(RetryConfig.builder()
                    .maxAttempts(3)
                    .waitDuration(Duration.ofSeconds(1))
                    .exponentialBackoff(true)
                    .build())
                .timeout(Duration.ofSeconds(10))
                .bulkhead(BulkheadConfig.builder()
                    .maxConcurrentCalls(50)
                    .maxWaitDuration(Duration.ofMillis(500))
                    .build())
                .build()
        );
    }
}
```

## 🔄 Saga Integration

### Step Event Bridge Configuration

```yaml
firefly:
  step-events:
    enabled: true
    publisher-type: KAFKA  # KAFKA, RABBIT, SQS, KINESIS, APPLICATION_EVENT
    topic-name: saga-steps
    include-metadata: true
    correlation-enabled: true
```

### Saga Step Implementation

```java
@Service
public class MoneyTransferSaga {
    
    private final StepEventBridge stepEventBridge;
    
    @SagaStep("debit-source-account")
    public Mono<StepResult> debitSourceAccount(TransferCommand command) {
        return accountService.debitAccount(command.getSourceAccountId(), command.getAmount())
            .flatMap(result -> publishStepEvent("debit-source-account", result, command))
            .map(this::toStepResult)
            .onErrorResume(error -> publishStepFailure("debit-source-account", error, command));
    }
    
    @SagaStep("credit-target-account")  
    public Mono<StepResult> creditTargetAccount(TransferCommand command) {
        return accountService.creditAccount(command.getTargetAccountId(), command.getAmount())
            .flatMap(result -> publishStepEvent("credit-target-account", result, command))
            .map(this::toStepResult)
            .onErrorResume(error -> publishStepFailure("credit-target-account", error, command));
    }
    
    @CompensationStep("debit-source-account")
    public Mono<Void> compensateDebitSourceAccount(TransferCommand command) {
        return accountService.creditAccount(command.getSourceAccountId(), command.getAmount())
            .flatMap(result -> publishStepEvent("compensate-debit-source", result, command))
            .then();
    }
    
    private Mono<Object> publishStepEvent(String stepName, Object result, TransferCommand command) {
        StepEvent event = StepEvent.builder()
            .stepName(stepName)
            .sagaId(command.getSagaId())
            .transactionId(command.getTransactionId())
            .status(StepStatus.COMPLETED)
            .result(result)
            .metadata(Map.of(
                "sourceAccountId", command.getSourceAccountId(),
                "targetAccountId", command.getTargetAccountId(),
                "amount", command.getAmount().toString()
            ))
            .build();
            
        return stepEventBridge.publishStepEvent(event)
            .thenReturn(result);
    }
}
```


## ⚙️ Configuration

### Basic Configuration

```yaml
# application.yml
firefly:
  # Domain Events Configuration
  events:
    enabled: true
    adapter: AUTO  # AUTO, KAFKA, RABBIT, SQS, KINESIS, APPLICATION_EVENT, NOOP
    default-topic: domain.events
    correlation-enabled: true
    metadata-enabled: true
    
  # Service Clients Configuration  
  service-clients:
    default-timeout: 30s
    default-max-connections: 100
    circuit-breaker:
      enabled: true
      failure-rate-threshold: 50.0
      wait-duration: 30s
      sliding-window-size: 20
    retry:
      enabled: true
      max-attempts: 3
      wait-duration: 1s
      exponential-backoff: true
      
  # Resilience Configuration
  resilience:
    circuit-breaker:
      enabled: true
      default-config:
        failure-rate-threshold: 50.0
        wait-duration-in-open-state: 60s
        sliding-window-size: 100
        minimum-number-of-calls: 10
    retry:
      enabled: true
      default-config:
        max-attempts: 3
        wait-duration: 500ms
        
  # Saga Integration
  step-events:
    enabled: true
    publisher-type: AUTO
    topic-name: saga.steps
    include-metadata: true
    
  # Tracing Configuration
  tracing:
    enabled: true
    correlation-header: X-Correlation-ID
    trace-id-header: X-Trace-ID
    span-id-header: X-Span-ID
```

### Environment Variables

All configuration properties can be overridden with environment variables:

```bash
# Domain Events
FIREFLY_EVENTS_ENABLED=true
FIREFLY_EVENTS_ADAPTER=KAFKA
FIREFLY_EVENTS_DEFAULT_TOPIC=domain.events

# Service Clients
FIREFLY_SERVICE_CLIENTS_DEFAULT_TIMEOUT=30s
FIREFLY_SERVICE_CLIENTS_CIRCUIT_BREAKER_ENABLED=true
FIREFLY_SERVICE_CLIENTS_RETRY_ENABLED=true

# Resilience
FIREFLY_RESILIENCE_CIRCUIT_BREAKER_ENABLED=true
FIREFLY_RESILIENCE_RETRY_ENABLED=true

# Saga Integration  
FIREFLY_STEP_EVENTS_ENABLED=true
FIREFLY_STEP_EVENTS_PUBLISHER_TYPE=KAFKA

# Tracing
FIREFLY_TRACING_ENABLED=true
FIREFLY_TRACING_CORRELATION_HEADER=X-Correlation-ID
```

### Messaging Platform Specific Configuration

#### Kafka Configuration
```yaml
firefly:
  events:
    adapter: KAFKA
    kafka:
      bootstrap-servers: localhost:9092
      producer:
        acks: all
        retries: 3
        key-serializer: org.apache.kafka.common.serialization.StringSerializer
        value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      consumer:
        group-id: ${spring.application.name}
        key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
        value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
        auto-offset-reset: earliest
```

#### RabbitMQ Configuration
```yaml
firefly:
  events:
    adapter: RABBIT
    rabbit:
      host: localhost
      port: 5672
      username: guest
      password: guest
      virtual-host: /
      exchange: domain.events.exchange
      queue-name-prefix: ${spring.application.name}
      routing-key-prefix: banking
      durable: true
```

#### AWS Configuration
```yaml
firefly:
  events:
    adapter: SQS
    sqs:
      region: ${AWS_REGION:us-east-1}
      queue-name: domain-events-${spring.profiles.active}
      visibility-timeout: 300
      message-retention: 1209600  # 14 days
      dead-letter-queue:
        enabled: true
        max-receive-count: 3
        
# For Kinesis
firefly:
  events:
    adapter: KINESIS
    kinesis:
      region: ${AWS_REGION:us-east-1}
      stream-name: domain-events-${spring.profiles.active}
      shard-count: 3
      retention-period: 168  # 7 days
```

## 🔍 Observability & Monitoring

### Spring Boot Actuator Integration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,domain-events,service-clients
  endpoint:
    health:
      show-details: always
      show-components: always
  health:
    # Domain Events health indicators
    domainEventsKafka:
      enabled: true
    domainEventsRabbit:
      enabled: true
    domainEventsSqs:
      enabled: true
    domainEventsKinesis:
      enabled: true
    # Service Client health indicators
    serviceClients:
      enabled: true
    # Custom health indicators
    threadPool:
      enabled: true
    httpClient:
      enabled: true
```

### Available Metrics

#### Domain Events Metrics
- `firefly.events.published.total` - Total events published
- `firefly.events.publishing.duration` - Event publishing time
- `firefly.events.consumed.total` - Total events consumed
- `firefly.events.processing.duration` - Event processing time
- `firefly.events.errors.total` - Event processing errors

#### Service Client Metrics
- `firefly.service.client.requests.total` - Total service requests
- `firefly.service.client.request.duration` - Request duration
- `firefly.service.client.errors.total` - Service client errors
- `firefly.service.client.circuit.breaker.state` - Circuit breaker states
- `firefly.service.client.retry.attempts.total` - Retry attempts

#### Resilience Metrics
- `resilience4j.circuitbreaker.calls` - Circuit breaker calls
- `resilience4j.circuitbreaker.state` - Circuit breaker state
- `resilience4j.retry.calls` - Retry calls
- `resilience4j.bulkhead.available.concurrent.calls` - Available concurrent calls

### Health Indicators

The library provides comprehensive health indicators:

```json
{
  "status": "UP",
  "components": {
    "domainEventsKafka": {
      "status": "UP",
      "details": {
        "brokers": "localhost:9092",
        "topics": ["domain.events", "saga.steps"],
        "producer": "CONNECTED",
        "consumer": "CONNECTED"
      }
    },
    "serviceClients": {
      "status": "UP", 
      "details": {
        "customer-service": {
          "status": "UP",
          "baseUrl": "http://customer-service:8080",
          "circuitBreaker": "CLOSED",
          "lastHealthCheck": "2025-01-05T10:30:00Z"
        },
        "payment-service": {
          "status": "DEGRADED",
          "baseUrl": "https://payment-service.banking.com", 
          "circuitBreaker": "HALF_OPEN",
          "lastHealthCheck": "2025-01-05T10:29:45Z",
          "details": "Response time above threshold"
        }
      }
    },
    "threadPool": {
      "status": "UP",
      "details": {
        "commonForkJoinPool": {
          "parallelism": 8,
          "poolSize": 8,
          "activeThreadCount": 2,
          "queuedSubmissionCount": 0
        }
      }
    }
  }
}
```

## 🧪 Testing

### Test Configuration

```yaml
# application-test.yml
firefly:
  events:
    adapter: APPLICATION_EVENT  # Use in-memory events for testing
  service-clients:
    circuit-breaker:
      enabled: false  # Disable circuit breakers for predictable testing
    retry:
      enabled: false  # Disable retries for faster testing
  resilience:
    circuit-breaker:
      enabled: false
    retry:
      enabled: false
  tracing:
    enabled: false  # Disable tracing overhead in tests
```

### Integration Testing

```java
@SpringBootTest
@Testcontainers
class DomainEventsIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
    
    @Autowired
    private DomainEventPublisher eventPublisher;
    
    @Autowired
    private TestEventListener testEventListener;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("firefly.events.adapter", () -> "KAFKA");
    }
    
    @Test
    void shouldPublishAndConsumeEvents() {
        // Given
        AccountCreatedEvent event = new AccountCreatedEvent("ACC-123", "CUST-456", "SAVINGS", new BigDecimal("1000"));
        
        // When
        StepVerifier.create(eventPublisher.publish(event))
            .verifyComplete();
            
        // Then
        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> testEventListener.getReceivedEvents().size() == 1);
            
        AccountCreatedEvent receivedEvent = (AccountCreatedEvent) testEventListener.getReceivedEvents().get(0);
        assertThat(receivedEvent.getAccountId()).isEqualTo("ACC-123");
        assertThat(receivedEvent.getCustomerId()).isEqualTo("CUST-456");
    }
}

@Component
@EventListener
class TestEventListener {
    private final List<DomainEvent> receivedEvents = new CopyOnWriteArrayList<>();
    
    @EventHandler(eventType = "*")
    public Mono<Void> handleEvent(DomainEvent event) {
        receivedEvents.add(event);
        return Mono.empty();
    }
    
    public List<DomainEvent> getReceivedEvents() {
        return receivedEvents;
    }
    
    public void clear() {
        receivedEvents.clear();
    }
}
```

### Service Client Testing

```java
@SpringBootTest
class ServiceClientTest {

    @Autowired
    private ServiceClient customerServiceClient;
    
    private MockWebServer mockWebServer;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        // Configure client to use mock server
        ReflectionTestUtils.setField(customerServiceClient, "baseUrl", 
            mockWebServer.url("/").toString());
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void shouldHandleSuccessfulResponse() {
        // Given
        Customer expectedCustomer = new Customer("CUST-123", "John Doe", "john@example.com");
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(expectedCustomer)));
            
        // When
        Mono<Customer> result = customerServiceClient.get("/customers/{id}", Customer.class)
            .withPathParam("id", "CUST-123")
            .execute();
            
        // Then
        StepVerifier.create(result)
            .assertNext(customer -> {
                assertThat(customer.getId()).isEqualTo("CUST-123");
                assertThat(customer.getName()).isEqualTo("John Doe");
                assertThat(customer.getEmail()).isEqualTo("john@example.com");
            })
            .verifyComplete();
    }
    
    @Test
    void shouldHandleServiceUnavailable() {
        // Given
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        
        // When & Then
        StepVerifier.create(
            customerServiceClient.get("/customers/{id}", Customer.class)
                .withPathParam("id", "CUST-123")
                .execute()
        )
        .expectError(ServiceUnavailableException.class)
        .verify();
    }
}
```

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
    private final ServiceClient paymentService;   // From lib-common-domain
    
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