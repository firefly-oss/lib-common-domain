# Domain Events Documentation

This document provides comprehensive documentation for the Domain Events system in the Firefly Common Domain Library, featuring a multi-messaging adapter architecture that supports various messaging infrastructures.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Event Envelope](#event-envelope)
- [Messaging Adapters](#messaging-adapters)
- [Event Publishing](#event-publishing)
- [Event Consumption](#event-consumption)
- [Configuration](#configuration)
- [Banking Examples](#banking-examples)

## Overview

The Domain Events system provides a flexible, adapter-based architecture for publishing and consuming domain events across different messaging infrastructures. It supports both synchronous (in-process) and asynchronous (external messaging) event handling.

### Key Features

- **Multi-Messaging Support**: Kafka, RabbitMQ, AWS SQS, Kinesis, and in-process events
- **Auto-Detection**: Intelligent adapter selection based on available infrastructure
- **Event Sourcing Ready**: Built-in support for event-driven architectures
- **Correlation Tracking**: Distributed tracing across service boundaries
- **Resilience Patterns**: Retry mechanisms and error handling
- **Annotation-Driven**: Simple event publishing with `@EventPublisher`

### Supported Adapters

| Adapter | Type | Use Case |
|---------|------|----------|
| **APPLICATION_EVENT** | In-process | Development, testing, single-service scenarios |
| **KAFKA** | External | High-throughput, distributed systems |
| **RABBIT** | External | Enterprise messaging, complex routing |
| **SQS** | External | AWS cloud-native applications |
| **KINESIS** | External | Real-time streaming, analytics |
| **AUTO** | Adaptive | Automatic selection based on available infrastructure |

## Architecture

### Event Flow Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Event Publishers                         │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐            │
│  │ Annotation  │ │Programmatic │ │   AOP       │            │
│  │@EventPublish│ │   Publish   │ │ Interceptor │            │
│  └─────────────┘ └─────────────┘ └─────────────┘            │
└─────────────────────────────────────────────────────────────┘
                              │
                    DomainEventEnvelope
                              │
┌─────────────────────────────────────────────────────────────┐
│                 Messaging Adapters                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐            │
│  │    Kafka    │ │  RabbitMQ   │ │     SQS     │            │
│  │   Adapter   │ │   Adapter   │ │   Adapter   │            │
│  └─────────────┘ └─────────────┘ └─────────────┘            │
│  ┌─────────────┐ ┌─────────────┐                            │
│  │   Kinesis   │ │Application  │                            │
│  │   Adapter   │ │Event Adapter│                            │
│  └─────────────┘ └─────────────┘                            │
└─────────────────────────────────────────────────────────────┘
                              │
                    External Infrastructure
                              │
┌─────────────────────────────────────────────────────────────┐
│                       Event Consumers                       │
│       ┌─────────────┐ ┌─────────────┐ ┌─────────────┐       │
│       │   Kafka     │ │  RabbitMQ   │ │     SQS     │       │
│       │ Subscriber  │ │ Subscriber  │ │ Subscriber  │       │
│       └─────────────┘ └─────────────┘ └─────────────┘       │
│                              │                              │
│                       DomainSpringEvent                     │
│                              │                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                    Event Listeners                  │    │
│  │            @EventListener, @DomainEventHandler      │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Adapter Selection Logic

The system uses intelligent adapter selection:

1. **Explicit Configuration**: Use specified adapter
2. **Auto-Detection**: Priority order: Kafka → RabbitMQ → Kinesis → SQS → ApplicationEvent
3. **Fallback**: ApplicationEvent for development/testing

## Event Envelope

All domain events are wrapped in a standardized envelope for consistent handling across adapters.

### DomainEventEnvelope Structure

````java
@Builder
@Data
public final class DomainEventEnvelope {
    
    private String topic;
    private String type;
    private String key;
    private Object payload;
    private Instant timestamp;
    private Map<String, Object> headers;
    private Map<String, Object> metadata;
}
````
</augment_code_snippet>

### Event Envelope Example

```java
// Creating a domain event envelope
DomainEventEnvelope event = DomainEventEnvelope.builder()
    .topic("banking.accounts")
    .type("account.created")
    .key("ACC-123456")
    .payload(AccountCreatedEvent.builder()
        .accountId("ACC-123456")
        .customerId("CUST-789")
        .accountType("CHECKING")
        .initialBalance(BigDecimal.valueOf(1000.00))
        .currency("USD")
        .createdAt(Instant.now())
        .build())
    .timestamp(Instant.now())
    .headers(Map.of(
        "source", "account-service",
        "version", "1.0"
    ))
    .metadata(Map.of(
        "correlationId", correlationContext.getCorrelationId(),
        "userId", correlationContext.getUserId()
    ))
    .build();
```

## Messaging Adapters

### Kafka Adapter

High-throughput messaging for distributed systems.

```java
// Kafka configuration
firefly:
  events:
    adapter: kafka
    kafka:
      bootstrap-servers: localhost:9092,localhost:9093
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      retries: 3
      acks: "all"
      properties:
        compression.type: gzip
        security.protocol: PLAINTEXT
```

### RabbitMQ Adapter

Enterprise messaging with complex routing capabilities.

```java
// RabbitMQ configuration
firefly:
  events:
    adapter: rabbit
    rabbit:
      exchange: "domain-events"
      routing-key: "${type}"  # Supports placeholders: ${topic}, ${type}, ${key}
      
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    publisher-confirm-type: correlated
```

### AWS SQS Adapter

Cloud-native messaging for AWS environments.

```java
// SQS configuration
firefly:
  events:
    adapter: sqs
    sqs:
      queue-url: https://sqs.us-east-1.amazonaws.com/123456789012/domain-events-queue
      # OR queue-name: domain-events-queue
      
aws:
  region: us-east-1
  credentials:
    access-key: ${AWS_ACCESS_KEY_ID}
    secret-key: ${AWS_SECRET_ACCESS_KEY}
```

### AWS Kinesis Adapter

Real-time streaming for analytics and event sourcing.

```yaml
# Kinesis configuration
firefly:
  events:
    adapter: kinesis
    kinesis:
      stream-name: domain-events-stream
      partition-key: "${key}"  # Supports placeholders

aws:
  region: us-east-1
  credentials:
    access-key: ${AWS_ACCESS_KEY_ID}
    secret-key: ${AWS_SECRET_ACCESS_KEY}
```

### Application Event Adapter

In-process events for development and single-service scenarios.

```yaml
# Application Event configuration (default for development)
firefly:
  events:
    adapter: application_event
```

## Event Publishing

### Annotation-Based Publishing

The simplest way to publish events using the `@EventPublisher` annotation:

````java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventPublisher {
    String topic();
    String type() default "";
    String key() default "";
    String payload() default "#result";  // SpEL expression
}
````
</augment_code_snippet>

```java
@Service
public class AccountService {
    
    // Publish event after successful method execution
    @EventPublisher(
        topic = "banking.accounts",
        type = "account.created",
        key = "#result.accountId",
        payload = "#result"
    )
    public Account createAccount(CreateAccountRequest request) {
        // Business logic to create account
        return accountRepository.save(newAccount);
    }
    
    // Publish event with custom payload
    @EventPublisher(
        topic = "banking.transactions",
        type = "transaction.completed",
        key = "#request.transactionId",
        payload = "T(com.example.TransactionEvent).from(#result, #request)"
    )
    public TransactionResult processTransaction(TransactionRequest request) {
        // Business logic
        return transactionProcessor.process(request);
    }
}
```

### Programmatic Publishing

Direct publishing using `DomainEventPublisher`:

```java
@Service
public class CustomerService {
    
    private final DomainEventPublisher eventPublisher;
    
    public Mono<Customer> onboardCustomer(OnboardingRequest request) {
        return createCustomer(request)
            .flatMap(this::publishCustomerCreatedEvent)
            .flatMap(this::initiateKycProcess);
    }
    
    private Mono<Customer> publishCustomerCreatedEvent(Customer customer) {
        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("banking.customers")
            .type("customer.created")
            .key(customer.getId())
            .payload(CustomerCreatedEvent.from(customer))
            .timestamp(Instant.now())
            .headers(Map.of("source", "customer-service"))
            .build();
            
        return eventPublisher.publish(event)
            .thenReturn(customer);
    }
}
```

### Batch Publishing

Publishing multiple events efficiently:

```java
@Service
public class BulkTransferService {
    
    private final DomainEventPublisher eventPublisher;
    
    public Mono<BulkTransferResult> processBulkTransfer(BulkTransferRequest request) {
        return processTransfers(request.getTransfers())
            .collectList()
            .flatMap(this::publishBulkTransferEvents);
    }
    
    private Mono<BulkTransferResult> publishBulkTransferEvents(List<TransferResult> transfers) {
        List<DomainEventEnvelope> events = transfers.stream()
            .map(transfer -> DomainEventEnvelope.builder()
                .topic("banking.transfers")
                .type("transfer.completed")
                .key(transfer.getTransferId())
                .payload(transfer)
                .timestamp(Instant.now())
                .build())
            .collect(Collectors.toList());
            
        return Flux.fromIterable(events)
            .flatMap(eventPublisher::publish)
            .then(Mono.just(BulkTransferResult.from(transfers)));
    }
}
```

## Event Consumption

### Spring Event Listeners

Consuming events using Spring's `@EventListener`:

```java
@Component
@Slf4j
public class AccountEventHandler {
    
    private final NotificationService notificationService;
    private final AuditService auditService;
    
    @EventListener
    public void handleAccountCreated(DomainSpringEvent event) {
        if ("account.created".equals(event.getEnvelope().getType())) {
            AccountCreatedEvent accountEvent = (AccountCreatedEvent) event.getEnvelope().getPayload();
            
            log.info("Account created: {}", accountEvent.getAccountId());
            
            // Send welcome notification
            notificationService.sendAccountWelcome(accountEvent.getCustomerId(), accountEvent.getAccountId())
                .subscribe();
                
            // Record audit event
            auditService.recordAccountCreation(accountEvent)
                .subscribe();
        }
    }
    
    @EventListener
    @Async
    public void handleTransactionCompleted(DomainSpringEvent event) {
        if ("transaction.completed".equals(event.getEnvelope().getType())) {
            TransactionEvent transactionEvent = (TransactionEvent) event.getEnvelope().getPayload();
            
            // Async processing for non-critical operations
            updateAccountBalance(transactionEvent)
                .then(updateTransactionHistory(transactionEvent))
                .then(checkForFraudPatterns(transactionEvent))
                .subscribe();
        }
    }
}
```

### Conditional Event Handling

```java
@Component
public class ConditionalEventHandler {
    
    // Handle only high-value transactions
    @EventListener(condition = "#event.envelope.type == 'transaction.completed' && " +
                              "#event.envelope.payload.amount.compareTo(T(java.math.BigDecimal).valueOf(10000)) > 0")
    public void handleHighValueTransaction(DomainSpringEvent event) {
        TransactionEvent transaction = (TransactionEvent) event.getEnvelope().getPayload();
        
        // Special handling for high-value transactions
        complianceService.reviewHighValueTransaction(transaction)
            .subscribe();
    }
    
    // Handle events from specific sources
    @EventListener(condition = "#event.envelope.headers['source'] == 'mobile-app'")
    public void handleMobileAppEvents(DomainSpringEvent event) {
        // Mobile-specific event handling
        mobileAnalyticsService.recordEvent(event.getEnvelope())
            .subscribe();
    }
}
```

### Event Filtering

```java
@Component
public class FilteredEventHandler {
    
    private final EventFilter eventFilter;
    
    @EventListener
    public void handleFilteredEvents(DomainSpringEvent event) {
        // Apply custom filtering logic
        if (eventFilter.shouldProcess(event.getEnvelope())) {
            processEvent(event.getEnvelope());
        }
    }
    
    private void processEvent(DomainEventEnvelope envelope) {
        switch (envelope.getType()) {
            case "customer.onboarded" -> handleCustomerOnboarded(envelope);
            case "account.created" -> handleAccountCreated(envelope);
            case "transaction.completed" -> handleTransactionCompleted(envelope);
            default -> log.debug("Unhandled event type: {}", envelope.getType());
        }
    }
}
```

## Configuration

### Complete Configuration Example

```yaml
firefly:
  events:
    enabled: true
    adapter: auto  # AUTO, KAFKA, RABBIT, SQS, KINESIS, APPLICATION_EVENT, NOOP
    
    # Kafka configuration
    kafka:
      bootstrap-servers: localhost:9092
      template-bean-name: kafkaTemplate
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      retries: 3
      batch-size: 16384
      linger-ms: 5
      buffer-memory: 33554432
      acks: "all"
      properties:
        compression.type: gzip
        security.protocol: PLAINTEXT
        
    # RabbitMQ configuration
    rabbit:
      template-bean-name: rabbitTemplate
      exchange: "domain-events"
      routing-key: "${type}"
      
    # SQS configuration
    sqs:
      client-bean-name: sqsAsyncClient
      queue-url: https://sqs.us-east-1.amazonaws.com/123456789012/events-queue
      
    # Kinesis configuration
    kinesis:
      client-bean-name: kinesisAsyncClient
      stream-name: domain-events-stream
      partition-key: "${key}"
      
    # Consumer configuration
    consumer:
      enabled: true
      type-header: "event-type"
      key-header: "event-key"
      
      kafka:
        topics:
          - domain-events
          - banking-events
        group-id: banking-service
        
      rabbit:
        queues:
          - banking-events-queue
          - notification-events-queue
          
      sqs:
        queue-url: https://sqs.us-east-1.amazonaws.com/123456789012/consumer-queue
        wait-time-seconds: 20
        max-messages: 10
        poll-delay-millis: 1000
        
      kinesis:
        stream-name: domain-events-stream
        application-name: banking-service
        poll-delay-millis: 1000
```

### Environment-Specific Configuration

```yaml
# Development - In-process events
spring:
  profiles:
    active: development
    
firefly:
  events:
    adapter: application_event

---
# Production - Kafka
spring:
  profiles:
    active: production
    
firefly:
  events:
    adapter: kafka
    kafka:
      bootstrap-servers: kafka-cluster:9092
      properties:
        security.protocol: SSL
        ssl.truststore.location: /etc/ssl/kafka.truststore.jks
        
---
# AWS Production - SQS/Kinesis
spring:
  profiles:
    active: aws-production
    
firefly:
  events:
    adapter: auto  # Will auto-detect SQS/Kinesis
    sqs:
      queue-url: ${AWS_SQS_QUEUE_URL}
    kinesis:
      stream-name: ${AWS_KINESIS_STREAM_NAME}
```

## Banking Examples

### Customer Lifecycle Events

```java
// Customer onboarding workflow
@Service
public class CustomerOnboardingWorkflow {
    
    @EventListener
    public void handleCustomerRegistered(DomainSpringEvent event) {
        if ("customer.registered".equals(event.getEnvelope().getType())) {
            CustomerRegisteredEvent customer = (CustomerRegisteredEvent) event.getEnvelope().getPayload();
            
            // Trigger KYC verification
            commandBus.send(new StartKycVerificationCommand(customer.getCustomerId()))
                .subscribe();
        }
    }
    
    @EventListener
    public void handleKycCompleted(DomainSpringEvent event) {
        if ("kyc.completed".equals(event.getEnvelope().getType())) {
            KycCompletedEvent kyc = (KycCompletedEvent) event.getEnvelope().getPayload();
            
            if (kyc.isApproved()) {
                // Create default accounts
                commandBus.send(new CreateDefaultAccountsCommand(kyc.getCustomerId()))
                    .subscribe();
            }
        }
    }
    
    @EventListener
    public void handleAccountsCreated(DomainSpringEvent event) {
        if ("accounts.created".equals(event.getEnvelope().getType())) {
            AccountsCreatedEvent accounts = (AccountsCreatedEvent) event.getEnvelope().getPayload();
            
            // Send welcome package
            commandBus.send(new SendWelcomePackageCommand(accounts.getCustomerId()))
                .subscribe();
        }
    }
}
```

### Transaction Processing Events

```java
// Real-time transaction monitoring
@Component
public class TransactionMonitor {
    
    @EventListener
    public void handleTransactionStarted(DomainSpringEvent event) {
        if ("transaction.started".equals(event.getEnvelope().getType())) {
            TransactionStartedEvent transaction = (TransactionStartedEvent) event.getEnvelope().getPayload();
            
            // Real-time fraud detection
            fraudDetectionService.analyzeTransaction(transaction)
                .filter(result -> result.isSuspicious())
                .flatMap(result -> commandBus.send(new FreezeTransactionCommand(transaction.getTransactionId())))
                .subscribe();
        }
    }
    
    @EventListener
    public void handleTransactionCompleted(DomainSpringEvent event) {
        if ("transaction.completed".equals(event.getEnvelope().getType())) {
            TransactionCompletedEvent transaction = (TransactionCompletedEvent) event.getEnvelope().getPayload();
            
            // Update customer spending patterns
            analyticsService.updateSpendingPattern(transaction)
                .subscribe();
                
            // Check for promotional offers
            promotionService.checkEligibility(transaction)
                .subscribe();
        }
    }
}
```

### Regulatory Compliance Events

```java
// Compliance and audit event handling
@Component
public class ComplianceEventHandler {
    
    @EventListener
    public void handleLargeTransaction(DomainSpringEvent event) {
        if ("transaction.completed".equals(event.getEnvelope().getType())) {
            TransactionCompletedEvent transaction = (TransactionCompletedEvent) event.getEnvelope().getPayload();
            
            // Report large transactions (>$10,000) to regulatory authorities
            if (transaction.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0) {
                complianceService.reportLargeTransaction(transaction)
                    .subscribe();
            }
        }
    }
    
    @EventListener
    public void handleSuspiciousActivity(DomainSpringEvent event) {
        if ("fraud.detected".equals(event.getEnvelope().getType())) {
            FraudDetectedEvent fraud = (FraudDetectedEvent) event.getEnvelope().getPayload();
            
            // File Suspicious Activity Report (SAR)
            complianceService.fileSuspiciousActivityReport(fraud)
                .subscribe();
        }
    }
}
```

---

The Domain Events system provides a robust, scalable foundation for event-driven banking applications, enabling loose coupling, eventual consistency, and comprehensive audit trails required in financial services.
