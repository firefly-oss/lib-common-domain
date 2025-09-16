# Firefly Common Domain Library

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

A powerful Spring Boot library that enables domain-driven design (DDD) with reactive programming support, featuring an **enhanced CQRS framework with zero boilerplate**, smart caching, automatic validation, and comprehensive event handling capabilities for the **Core-Domain Layer** of the Firefly OpenCore Banking Platform.

## 📋 Table of Contents

- [🏗️ Architecture Overview](#️-architecture-overview)
- [🚀 Key Features](#-key-features)
- [💡 Core Concepts](#-core-concepts)
  - [CQRS (Command Query Responsibility Segregation)](#cqrs-command-query-responsibility-segregation)
  - [Saga Pattern](#saga-pattern)
  - [Domain Events](#domain-events)
  - [Reactive Programming](#reactive-programming)
- [📦 Quick Start](#-quick-start)
- [🎯 CQRS Framework Usage](#-cqrs-framework-usage)
- [🔐 Authorization System](#-authorization-system)
- [🌐 ServiceClient Framework](#-serviceclient-framework)
- [📡 Domain Events](#-domain-events)
- [🔄 CQRS + Saga Integration](#-cqrs--saga-integration)
- [🔧 Technology Stack](#-technology-stack)
- [📄 License](#-license)
- [🏢 About Firefly](#-about-firefly)

## 🏗️ Architecture Overview

This library serves as the foundational architecture framework for the **Core-Domain Layer** within Firefly's 4-tier microservices architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                    Channels Layer                           │
│              (User-facing interfaces)                       │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│               Application/Process Layer                     │
│            (Business processes & workflows)                 │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│ ★                    Core-Domain Layer                    ★ │
│        (Business logic & middleware - THIS LIBRARY)         │
│   • CQRS Framework    • Domain Events    • ServiceClients   │
│   • Event Processing  • Resilience     • Observability      │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                  Core-Infrastructure Layer                  │
│              (Database CRUD & data persistence)             │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Key Features

### 🎯 Enhanced CQRS Framework (Zero Boilerplate)
- **Single Best Way**: Only ONE way to create handlers - extend `CommandHandler<C,R>` or `QueryHandler<Q,R>` - no confusion!
- **Zero Boilerplate**: Automatic type detection from generics - no `getCommandType()` or `getResultType()` methods needed
- **Jakarta Validation Integration**: Automatic validation using @NotBlank, @NotNull, @Min, @Max annotations - no manual validation code
- **Built-in Features**: Automatic logging, **metrics by default**, error handling, correlation context, and performance monitoring
- **Smart Caching**: Intelligent query result caching with configurable TTL, automatic cache key generation, and Redis support
- **Builder Pattern Support**: Clean command/query creation using @Builder annotation with Lombok
- **Reactive Processing**: Built on Project Reactor for non-blocking, asynchronous operations
- **Focus on Business Logic**: Just implement `doHandle()` - everything else is handled automatically

### 🔐 Authorization System (Zero-Trust Banking)
- **lib-common-auth Integration**: Seamless integration with existing authentication infrastructure
- **Zero-Trust Architecture**: All operations denied by default with explicit authorization required
- **Four Authorization Patterns**: Standard + Custom, Custom Override, Custom Only, Both Must Pass
- **Configurable Security**: Environment-based configuration for different deployment scenarios
- **Banking-Grade Security**: Resource ownership validation, fraud detection, compliance checks
- **Performance Optimized**: Optional caching, async processing, and timeout controls

### 🌐 ServiceClient Framework (Redesigned)
- **Unified API**: Single interface for REST and gRPC clients
- **Fluent Request Builder**: Intuitive method chaining for all operations
- **Advanced Resilience**: Bulkhead isolation, rate limiting, adaptive timeouts
- **Health Monitoring**: Automatic service health detection and recovery
- **Interceptor Framework**: Request/response interceptors for cross-cutting concerns
- **Production Ready**: Banking-optimized with comprehensive monitoring

### 📡 Multi-Messaging Domain Events
- **Adapter Pattern**: Support for Kafka, RabbitMQ, AWS SQS, Kinesis, and in-process events
- **Auto-Detection**: Intelligent adapter selection based on available infrastructure
- **Event Sourcing Ready**: Built-in support for event-driven architectures
- **Correlation Tracking**: Distributed tracing across service boundaries
- **Event Envelope**: Standardized event structure with headers and metadata

### 🔄 lib-transactional-engine Integration
- **Manual Integration Pattern**: Flexible integration allowing developers to wire lib-transactional-engine with CQRS framework manually
- **StepEventPublisherBridge**: Bridge component that publishes saga step events through domain events infrastructure
- **Metadata Enrichment**: Enhanced step events with execution metrics and correlation data
- **Unified Event Handling**: Single event processing pipeline for both domain and step events
- **Developer Control**: No native integration - developers maintain full control over saga orchestration patterns

### 🔍 Enhanced Observability
- **Metrics by Default**: Auto-configured MeterRegistry with CQRS command/query metrics (no manual setup required)
- **Comprehensive Metrics**: JVM, HTTP client, thread pool, and application startup metrics
- **Health Indicators**: Thread pool, cache, and external service health monitoring
- **Distributed Tracing**: Correlation context propagation across all operations
- **Step Event Monitoring**: Detailed saga execution tracking and performance metrics

## 💡 Core Concepts

Understanding these fundamental patterns is essential for effectively using this library in banking and financial services applications.

### CQRS (Command Query Responsibility Segregation)

**CQRS** is an architectural pattern that separates read and write operations into different models, optimizing each for their specific use cases.

**Key Principles:**
- **Commands**: Represent intentions to change state (e.g., "Transfer Money", "Create Account")
- **Queries**: Retrieve data without side effects (e.g., "Get Account Balance", "List Transactions")
- **Separation**: Different models for reading and writing data
- **Scalability**: Independent scaling of read and write operations

**Benefits in Banking:**
- **Performance**: Optimized read models for complex reporting and analytics
- **Security**: Clear separation between data modification and retrieval operations
- **Auditability**: All state changes are explicit commands with full traceability
- **Scalability**: Read replicas can be optimized for specific query patterns

**Enhanced Example with Zero Boilerplate:**
```java
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import com.firefly.common.domain.cqrs.annotations.CommandHandlerComponent;

// Command with Jakarta validation - no boilerplate!
@Data
@Builder
public class TransferMoneyCommand implements Command<TransferResult> {
    @NotBlank(message = "Source account is required")
    private final String fromAccount;

    @NotBlank(message = "Destination account is required")
    private final String toAccount;

    @NotNull
    @Min(value = 1, message = "Amount must be greater than zero")
    @Max(value = 1000000, message = "Amount cannot exceed $1,000,000")
    private final BigDecimal amount;

    @NotBlank(message = "Description is required")
    private final String description;

    private final String correlationId;

    // No validate() method needed - Jakarta validation handles annotations automatically!
    // Framework automatically validates @NotBlank, @NotNull, @Min, @Max annotations
}

// Zero-boilerplate command handler - THE ONLY WAY to create handlers!
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class TransferMoneyHandler extends CommandHandler<TransferMoneyCommand, TransferResult> {

    private final ServiceClient accountServiceClient;
    private final DomainEventPublisher eventPublisher;

    @Override
    protected Mono<TransferResult> doHandle(TransferMoneyCommand command) {
        // Only business logic - validation, logging, metrics handled automatically!
        return executeTransfer(command)
            .flatMap(this::publishTransferEvent);
    }

    // No getCommandType() needed - automatically detected from generics!
    // Built-in logging, metrics, error handling, correlation context - all automatic!

    private Mono<TransferResult> executeTransfer(TransferMoneyCommand command) {
        return accountServiceClient.post("/transfers", TransferResult.class)
            .withBody(command)
            .execute();
    }

    private Mono<TransferResult> publishTransferEvent(TransferResult result) {
        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("banking.transfers")
            .type("transfer.completed")
            .key(result.getTransferId())
            .payload(result)
            .timestamp(Instant.now())
            .build();

        return eventPublisher.publish(event).thenReturn(result);
    }
}

// Simple command execution using builder pattern
TransferMoneyCommand command = TransferMoneyCommand.builder()
    .fromAccount("ACC-001")
    .toAccount("ACC-002")
    .amount(new BigDecimal("1000.00"))
    .description("Monthly transfer")
    .correlationId("CORR-12345")
    .build();

commandBus.send(command).subscribe();
```

### Saga Pattern

**Saga** is a distributed transaction pattern that manages data consistency across microservices without using distributed transactions.

**Key Principles:**
- **Local Transactions**: Each service manages its own data with local ACID transactions
- **Compensation**: Failed transactions trigger compensating actions to undo previous steps
- **Orchestration**: A central coordinator manages the sequence of operations
- **Event-Driven**: Services communicate through events rather than direct calls

**Benefits in Banking:**
- **Reliability**: Handles partial failures gracefully with automatic compensation
- **Consistency**: Maintains eventual consistency across distributed services
- **Scalability**: No distributed locks or two-phase commits
- **Observability**: Clear visibility into complex business process execution

**Example Banking Workflow:**
```
Money Transfer Saga:
1. Reserve funds from source account
2. Validate destination account
3. Execute transfer
4. Send notifications

If step 3 fails → Compensate: Release reservation from step 1
```

### Domain Events

**Domain Events** represent significant business occurrences that other parts of the system care about.

**Key Principles:**
- **Business Significance**: Events represent meaningful business state changes
- **Decoupling**: Publishers don't know about subscribers
- **Eventual Consistency**: Systems synchronize through event processing
- **Event Sourcing**: Events can be stored as the source of truth

**Benefits in Banking:**
- **Integration**: Loose coupling between banking services
- **Auditability**: Complete audit trail of all business events
- **Real-time Processing**: Immediate reaction to business events
- **Scalability**: Asynchronous processing of business logic

**Example:**
```java
// Domain event published when money transfer completes
MoneyTransferCompletedEvent event = MoneyTransferCompletedEvent.builder()
    .transferId("TXN-12345")
    .fromAccount("ACC-001")
    .toAccount("ACC-002")
    .amount(new BigDecimal("1000.00"))
    .completedAt(Instant.now())
    .build();
```

### Reactive Programming

**Reactive Programming** is a programming paradigm focused on asynchronous data streams and the propagation of change.

**Key Principles:**
- **Non-blocking**: Operations don't block threads waiting for I/O
- **Backpressure**: Handling of fast producers and slow consumers
- **Resilience**: Graceful handling of failures and recovery
- **Responsiveness**: Systems remain responsive under varying load

**Benefits in Banking:**
- **Performance**: Handle thousands of concurrent transactions efficiently
- **Resource Efficiency**: Better utilization of system resources
- **Scalability**: Handle varying loads without degrading performance
- **Resilience**: Graceful degradation under high load or failures

**Example:**
```java
// Reactive money transfer processing
public Mono<TransferResult> transferMoney(TransferRequest request) {
    return validateAccount(request.getFromAccount())
        .flatMap(account -> reserveFunds(account, request.getAmount()))
        .flatMap(reservation -> executeTransfer(reservation, request))
        .doOnSuccess(result -> publishTransferEvent(result))
        .doOnError(error -> handleTransferFailure(request, error));
}
```

## 🎯 **The Single Best Way**

This framework provides **ONE clear way** to create CQRS handlers - no confusion, no multiple approaches:

- **Commands**: Use `@CommandHandlerComponent` + extend `CommandHandler<Command, Result>` + implement `doHandle()`
- **Queries**: Use `@QueryHandlerComponent` + extend `QueryHandler<Query, Result>` + implement `doHandle()`
- **Zero Boilerplate**: Automatic type detection, logging, metrics, validation, caching
- **Annotations**: Configure timeout, retries, caching, metrics via annotations
- **Focus**: Only write business logic - everything else is handled automatically

## 📦 Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Enable Auto-Configuration

The library uses Spring Boot's standard auto-configuration mechanism. Simply add the dependency and the components will be automatically configured:

```java
@SpringBootApplication
public class BankingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingServiceApplication.class, args);
    }
}
```

The following components are automatically configured when the library is on the classpath:
- **CQRS Framework** (`CqrsAutoConfiguration`) - **includes auto-configured MeterRegistry for metrics**
- **Domain Events** (`DomainEventsAutoConfiguration`)
- **ServiceClient Framework** (`ServiceClientAutoConfiguration`)
- **StepEvents Bridge** (`StepBridgeConfiguration`)
- **JSON Logging** (`JsonLoggingAutoConfiguration`)
- **Observability Features** (`DomainEventsActuatorAutoConfiguration`)

### 3. Basic Configuration

```yaml
firefly:
  # CQRS Framework
  cqrs:
    enabled: true
    command:
      timeout: 30s
      metrics-enabled: true
    query:
      caching-enabled: true
      cache-ttl: 15m
      timeout: 15s
      cache:
        type: LOCAL  # LOCAL (default) or REDIS
        redis:
          enabled: false  # Enable Redis cache (disabled by default)

  # Domain Events (auto-detects available messaging infrastructure)
  events:
    enabled: true
    adapter: auto  # AUTO, KAFKA, RABBIT, SQS, KINESIS, APPLICATION_EVENT, NOOP

  # lib-transactional-engine Integration
  stepevents:
    enabled: true

  # ServiceClient Framework
  service-client:
    enabled: true

# Domain topic for step events (optional, defaults to "domain-events")
domain:
  topic: banking-domain-events
```

### 4. Cache Configuration (Local vs Redis)

The framework supports both local in-memory cache and distributed Redis cache for query results:

#### Local Cache (Default)
```yaml
firefly:
  cqrs:
    query:
      cache:
        type: LOCAL  # Default - no additional configuration needed
```

#### Redis Cache (Production)
```yaml
firefly:
  cqrs:
    query:
      cache:
        type: REDIS              # Switch to Redis cache
        redis:
          enabled: true          # Must be explicitly enabled
          host: localhost        # Redis server host
          port: 6379            # Redis server port
          database: 0           # Redis database index
          password: your-password # Optional password
          timeout: 2s           # Connection timeout
          key-prefix: "firefly:cqrs:" # Cache key prefix
          statistics: true      # Enable cache statistics (reserved for future use)
```

**Important Notes:**
- Redis cache is **disabled by default** - no Redis connections are attempted unless explicitly enabled
- When Redis is unavailable, the framework gracefully falls back to local cache with a warning
- Redis dependency is optional when using local cache only

### 5. Create Your First Command and Handler

```java
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

// Command with automatic Jakarta validation
@Data
@Builder
public class ProcessPaymentCommand implements Command<PaymentResult> {
    @NotBlank(message = "Customer ID is required")
    private final String customerId;

    @NotNull(message = "Payment amount is required")
    @Min(value = 1, message = "Payment amount must be greater than zero")
    @Max(value = 50000, message = "Payment amount cannot exceed $50,000")
    private final BigDecimal amount;

    @NotBlank(message = "Payment method is required")
    private final String paymentMethod;

    @NotBlank(message = "Description is required")
    private final String description;

    private final String correlationId;

    // No validate() method needed - Jakarta validation handles all annotations!
}

// THE ONLY WAY to create command handlers - extend CommandHandler!
@CommandHandlerComponent(timeout = 15000, metrics = true)
public class ProcessPaymentHandler extends CommandHandler<ProcessPaymentCommand, PaymentResult> {

    private final ServiceClient paymentClient;
    private final ServiceClient notificationClient;

    @Override
    protected Mono<PaymentResult> doHandle(ProcessPaymentCommand command) {
        // Only business logic - validation, logging, metrics handled automatically!
        return processPayment(command)
            .flatMap(paymentResult -> sendNotification(command, paymentResult));
    }

    // No getCommandType() needed - automatically detected from generics!
    // Built-in features: logging, metrics, error handling, correlation context

    private Mono<PaymentResult> processPayment(ProcessPaymentCommand command) {
        return paymentClient.post("/payments", PaymentResult.class)
            .withBody(command)
            .execute();
    }

    private Mono<PaymentResult> sendNotification(ProcessPaymentCommand command, PaymentResult result) {
        return notificationClient.post("/notifications", Void.class)
            .withBody(createNotificationRequest(command, result))
            .execute()
            .thenReturn(result); // Return the original payment result
    }

    private Object createNotificationRequest(ProcessPaymentCommand command, PaymentResult result) {
        // Create notification request object from command and result
        return new NotificationRequest(command.getCustomerId(), result.getTransactionId());
    }
}

// Usage example
ProcessPaymentCommand command = ProcessPaymentCommand.builder()
    .customerId("CUST-12345")
    .amount(new BigDecimal("250.00"))
    .paymentMethod("CREDIT_CARD")
    .description("Monthly subscription payment")
    .correlationId("CORR-PAY-001")
    .build();

commandBus.send(command)
    .doOnSuccess(result -> log.info("Payment processed: {}", result.getTransactionId()))
    .doOnError(error -> log.error("Payment failed: {}", error.getMessage()))
    .subscribe();
```

## 🎯 Enhanced CQRS Framework Usage

The enhanced CQRS framework eliminates boilerplate code through automatic Jakarta validation, smart caching, builder patterns, and clean handler interfaces.

### 1. Commands with Automatic Validation

Commands now use Jakarta validation annotations, eliminating manual validation code:

```java
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransferMoneyCommand implements Command<TransferResult> {
    @NotBlank(message = "Source account is required")
    private final String fromAccount;

    @NotBlank(message = "Destination account is required")
    private final String toAccount;

    @NotNull(message = "Transfer amount is required")
    @Min(value = 1, message = "Transfer amount must be greater than zero")
    @Max(value = 1000000, message = "Transfer amount cannot exceed $1,000,000")
    private final BigDecimal amount;

    @NotBlank(message = "Description is required")
    private final String description;

    private final String correlationId;

    // No validate() method needed - Jakarta validation handles annotations automatically!
    // The CommandBus will automatically validate @NotBlank, @NotNull, @Min, @Max before processing

    @Override
    public Mono<ValidationResult> validate() {
        // Only custom business validation here (Jakarta validation happens first)
        if (fromAccount != null && fromAccount.equals(toAccount)) {
            return Mono.just(ValidationResult.failure("accounts", "Cannot transfer to the same account"));
        }
        return Mono.just(ValidationResult.success());
    }
}
```

### 2. Zero-Boilerplate Command Handlers

THE ONLY WAY to create command handlers - extend CommandHandler for automatic features:

```java
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class TransferMoneyHandler extends CommandHandler<TransferMoneyCommand, TransferResult> {

    private final ServiceClient accountServiceClient;
    private final ServiceClient fraudServiceClient;
    private final DomainEventPublisher eventPublisher;

    @Override
    protected Mono<TransferResult> doHandle(TransferMoneyCommand command) {
        // Only business logic - validation, logging, metrics handled automatically!
        return performFraudCheck(command)
            .flatMap(fraudResult -> {
                if (!fraudResult.isApproved()) {
                    return Mono.error(new TransferBlockedException("Transfer blocked by fraud detection"));
                }
                return executeTransfer(command);
            })
            .flatMap(this::publishTransferEvent);
    }

    // No getCommandType() needed - automatically detected from generics!
    // Built-in features: logging, metrics, error handling, correlation context

    private Mono<FraudCheckResult> performFraudCheck(TransferMoneyCommand command) {
        return fraudServiceClient.post("/fraud-check", FraudCheckResult.class)
            .withBody(command)
            .execute();
    }

    private Mono<TransferResult> executeTransfer(TransferMoneyCommand command) {
        return accountServiceClient.post("/transfers", TransferResult.class)
            .withBody(command)
            .execute();
    }

    private Mono<TransferResult> publishTransferEvent(TransferResult result) {
        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("banking.transfers")
            .type("transfer.completed")
            .key(result.getTransferId())
            .payload(result)
            .timestamp(Instant.now())
            .build();

        return eventPublisher.publish(event).thenReturn(result);
    }
}
```

### 3. Clean Command Creation and Execution

Create and execute commands using the builder pattern:

```java
@Service
@RequiredArgsConstructor
public class TransferService {

    private final CommandBus commandBus;

    public Mono<TransferResult> transferMoney(TransferRequest request) {
        // Clean command creation using builder pattern
        TransferMoneyCommand command = TransferMoneyCommand.builder()
            .fromAccount(request.getFromAccount())
            .toAccount(request.getToAccount())
            .amount(request.getAmount())
            .description(request.getDescription())
            .correlationId(generateCorrelationId())
            .build();

        // Execute command - validation happens automatically
        return commandBus.send(command);
    }

    public Mono<TransferResult> transferMoneyWithContext(TransferRequest request) {
        // Alternative with correlation context
        String correlationId = CorrelationContext.current().getCorrelationId();

        TransferMoneyCommand command = TransferMoneyCommand.builder()
            .fromAccount(request.getFromAccount())
            .toAccount(request.getToAccount())
            .amount(request.getAmount())
            .description(request.getDescription())
            .correlationId(correlationId)
            .build();

        return commandBus.send(command)
            .doOnSuccess(result -> log.info("Transfer completed: {}", result.getTransferId()))
            .doOnError(error -> log.error("Transfer failed: {}", error.getMessage()));
    }

    private String generateCorrelationId() {
        return "TRANSFER-" + UUID.randomUUID().toString();
    }
}
```

### 4. Smart Queries with Automatic Caching

THE ONLY WAY to create query handlers - extend QueryHandler for automatic features:

```java
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetAccountBalanceQuery implements Query<AccountBalance> {
    @NotBlank(message = "Account number is required")
    private final String accountNumber;

    @NotBlank(message = "Currency is required")
    private final String currency;

    private final String correlationId;

    // No need to override getCacheKey() - automatic generation based on fields!
}

@QueryHandlerComponent(cacheable = true, cacheTtl = 300, metrics = true)
public class GetAccountBalanceHandler extends QueryHandler<GetAccountBalanceQuery, AccountBalance> {

    private final ServiceClient accountServiceClient;

    @Override
    protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query) {
        // Only business logic - validation, caching, metrics handled automatically!
        return accountServiceClient.get("/accounts/{accountNumber}/balance", AccountBalance.class)
            .withPathVariable("accountNumber", query.getAccountNumber())
            .withQueryParam("currency", query.getCurrency())
            .execute();
    }

    // No getQueryType() needed - automatically detected from generics!
    // Built-in features: caching, logging, metrics, error handling

    @Override
    public boolean supportsCaching() {
        return true; // Enable caching for this query
    }

    @Override
    public Long getCacheTtlSeconds() {
        return 300L; // Cache for 5 minutes
    }
}

// Simple query execution using builder pattern
GetAccountBalanceQuery query = GetAccountBalanceQuery.builder()
    .accountNumber("ACC-123")
    .currency("USD")
    .correlationId("QUERY-001")
    .build();

queryBus.query(query)
    .doOnSuccess(balance -> log.info("Account balance: {}", balance.getAmount()))
    .subscribe();
```

### 5. ExecutionContext for Additional Values

Pass additional context values that aren't part of the command/query itself:

```java
// Create execution context with additional values
ExecutionContext context = ExecutionContext.builder()
    .withUserId("user-123")
    .withTenantId("tenant-456")
    .withOrganizationId("org-789")
    .withFeatureFlag("premium-features", true)
    .withFeatureFlag("enhanced-view", false)
    .withProperty("priority", "HIGH")
    .withProperty("source", "mobile-app")
    .build();

// Send command with context
commandBus.send(transferCommand, context)
    .subscribe(result -> log.info("Transfer completed: {}", result));

// Query with context
queryBus.query(balanceQuery, context)
    .subscribe(balance -> log.info("Balance: {}", balance));
```

#### Context-Aware Handlers

For handlers that always need context, use `ContextAwareCommandHandler` or `ContextAwareQueryHandler`:

```java
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class CreateTenantAccountHandler extends ContextAwareCommandHandler<CreateAccountCommand, AccountResult> {

    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command, ExecutionContext context) {
        String tenantId = context.getTenantId();
        String userId = context.getUserId();
        boolean premiumFeatures = context.getFeatureFlag("premium-features", false);

        if (tenantId == null) {
            return Mono.error(new IllegalArgumentException("Tenant ID is required"));
        }

        return createAccountWithTenantContext(command, tenantId, userId, premiumFeatures);
    }
}
```

#### Flexible Handlers

Regular handlers can optionally use context by overriding the context-aware doHandle method:

```java
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class FlexibleAccountHandler extends CommandHandler<CreateAccountCommand, AccountResult> {

    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command) {
        // Standard implementation without context
        return createStandardAccount(command);
    }

    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command, ExecutionContext context) {
        // Enhanced implementation with context
        String tenantId = context.getTenantId();
        boolean premiumFeatures = context.getFeatureFlag("premium-features", false);

        return createAccountWithContext(command, tenantId, premiumFeatures);
    }
}
```

## 🔐 Authorization System

The Firefly Common Domain Library includes a comprehensive authorization system designed for zero-trust banking applications. It seamlessly integrates with lib-common-auth while providing flexible custom authorization capabilities.

### Key Features

- **🔗 Seamless Integration**: Automatic detection and integration with lib-common-auth
- **🛡️ Zero-Trust Architecture**: All operations denied by default with explicit authorization required
- **⚙️ Configurable Security**: Environment-based configuration for different deployment scenarios
- **🏦 Banking-Grade Security**: Resource ownership validation, fraud detection, compliance checks
- **🚀 Performance Optimized**: Optional caching, async processing, and timeout controls
- **📊 Comprehensive Monitoring**: Detailed metrics and logging for audit and compliance

### Quick Start

Enable authorization in your application:

```yaml
firefly:
  cqrs:
    authorization:
      enabled: true
      lib-common-auth:
        enabled: true
      custom:
        enabled: true
```

### Implementation Example

```java
@RequiresRole("CUSTOMER")
@RequiresScope("accounts.transfer")
@CustomAuthorization(description = "Transfer limits validation")
public class TransferMoneyCommand implements Command<TransferResult> {

    @Override
    public Mono<AuthorizationResult> authorize(ExecutionContext context) {
        // Custom business logic validation
        return validateTransferLimits(amount, context.getUserId());
    }
}
```

> 📖 **For complete documentation**, see [Authorization System Guide](docs/AUTHORIZATION.md) which covers:
> - All four authorization patterns with examples
> - Complete configuration reference
> - Integration patterns with lib-common-auth
> - Banking-specific use cases and best practices
> - Monitoring and troubleshooting

```yaml
      # Performance settings
      performance:
        cache-enabled: false
        cache-ttl-seconds: 300
        async-enabled: false
```

### Environment Variables

All configuration can be overridden using environment variables:

```bash
# Disable authorization completely
FIREFLY_CQRS_AUTHORIZATION_ENABLED=false

# Disable lib-common-auth integration
FIREFLY_CQRS_AUTHORIZATION_LIB_COMMON_AUTH_ENABLED=false

# Enable verbose logging
FIREFLY_CQRS_AUTHORIZATION_LOGGING_LOG_SUCCESSFUL=true
FIREFLY_CQRS_AUTHORIZATION_LOGGING_LEVEL=DEBUG
```

### Usage Examples

#### Basic Command Authorization

```java
@CommandHandlerComponent
public class TransferMoneyHandler extends CommandHandler<TransferMoneyCommand, TransferResult> {

    @Override
    protected Mono<TransferResult> doHandle(TransferMoneyCommand command) {
        // Business logic here - authorization is handled automatically
        return processTransfer(command);
    }
}

// Command with custom authorization
public class TransferMoneyCommand implements Command<TransferResult> {

    @Override
    public Mono<AuthorizationResult> authorize(ExecutionContext context) {
        String userId = context.getUserId();

        // Verify account ownership
        return accountService.verifyOwnership(sourceAccountId, userId)
            .flatMap(ownsSource -> {
                if (!ownsSource) {
                    return Mono.just(AuthorizationResult.failure("sourceAccount",
                        "Source account does not belong to user"));
                }

                // Check transfer limits
                return transferLimitService.checkLimit(userId, amount)
                    .map(withinLimit -> withinLimit ?
                        AuthorizationResult.success() :
                        AuthorizationResult.failure("amount", "Transfer exceeds daily limit"));
            });
    }
}
```

#### Advanced Authorization with Annotations

```java
// Override lib-common-auth decisions
@CustomAuthorization(overrideLibCommonAuth = true)
public class EmergencyTransferCommand implements Command<TransferResult> {

    @Override
    public Mono<AuthorizationResult> authorize(ExecutionContext context) {
        // Emergency transfers bypass normal limits but require special approval
        return emergencyApprovalService.checkApproval(context.getUserId())
            .map(approved -> approved ?
                AuthorizationResult.success() :
                AuthorizationResult.failure("approval", "Emergency transfer not approved"));
    }
}

// Require both lib-common-auth and custom authorization
@CustomAuthorization(requiresBothToPass = true)
public class HighValueTransferCommand implements Command<TransferResult> {

    @Override
    public Mono<AuthorizationResult> authorize(ExecutionContext context) {
        // Additional checks for high-value transfers
        return fraudDetectionService.checkTransaction(this, context)
            .map(safe -> safe ?
                AuthorizationResult.success() :
                AuthorizationResult.failure("fraud", "Transaction flagged as suspicious"));
    }
}
```

#### Profile-Based Configuration

```yaml
---
# Development profile - verbose logging, no caching
spring:
  config:
    activate:
      on-profile: development

firefly:
  cqrs:
    authorization:
      logging:
        log-successful: true
        level: "DEBUG"
      performance:
        cache-enabled: false

---
# Production profile - optimized for performance
spring:
  config:
    activate:
      on-profile: production

firefly:
  cqrs:
    authorization:
      lib-common-auth:
        fail-fast: true
      custom:
        allow-override: false
        timeout-ms: 3000
      logging:
        log-successful: false
        level: "WARN"
      performance:
        cache-enabled: true
        cache-ttl-seconds: 600

---
# Testing profile - authorization disabled
spring:
  config:
    activate:
      on-profile: test

firefly:
  cqrs:
    authorization:
      enabled: false
```



## 📚 Documentation

| Document | Description |
|----------|-------------|
| [API Reference](docs/API_REFERENCE.md) | Complete API reference with method signatures |
| [Architecture Guide](docs/ARCHITECTURE.md) | Architecture patterns and design principles |
| [Authorization System](docs/AUTHORIZATION.md) | Comprehensive authorization system guide with configuration, patterns, and examples |
| [Package Structure](docs/PACKAGE_STRUCTURE.md) | Package organization for library and microservices |
| [Examples](docs/EXAMPLES.md) | Real working examples and best practices |
| [Observability](docs/OBSERVABILITY.md) | Monitoring, metrics, and observability configuration |

## 🏦 Banking Domain Examples

### Money Transfer with ServiceClient Integration

```java
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class MoneyTransferHandler extends CommandHandler<TransferMoneyCommand, TransferResult> {

    private final ServiceClient accountServiceClient;
    private final ServiceClient fraudServiceClient;
    private final DomainEventPublisher eventPublisher;

    @Override
    protected Mono<TransferResult> doHandle(TransferMoneyCommand command) {
        // Only business logic - validation, logging, metrics handled automatically!
        return performFraudCheck(command)
            .flatMap(fraudResult -> {
                if (!fraudResult.isApproved()) {
                    return Mono.error(new FraudDetectedException("Transfer blocked"));
                }
                return executeTransfer(command);
            })
            .flatMap(this::publishTransferEvent);
    }

    // No getCommandType() needed - automatically detected from generics!
    // Built-in features: logging, metrics, error handling, correlation context

    private Mono<FraudCheckResult> performFraudCheck(TransferMoneyCommand command) {
        return fraudServiceClient.post("/fraud-check", FraudCheckResult.class)
            .withBody(command)
            .execute();
    }

    private Mono<TransferResult> executeTransfer(TransferMoneyCommand command) {
        return accountServiceClient.post("/transfers", TransferResult.class)
            .withBody(command)
            .execute();
    }

    private Mono<TransferResult> publishTransferEvent(TransferResult result) {
        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("banking.transfers")
            .type("transfer.completed")
            .key(result.getTransferId())
            .payload(result)
            .build();

        return eventPublisher.publish(event).thenReturn(result);
    }
}
```

### Account Balance Query with Caching

```java
@Component
public class GetAccountBalanceHandler implements QueryHandler<GetAccountBalanceQuery, AccountBalance> {

    private final ServiceClient accountServiceClient;

    @Override
    public Mono<AccountBalance> handle(GetAccountBalanceQuery query) {
        return accountServiceClient.get("/accounts/{id}/balance", AccountBalance.class)
            .withPathParam("id", query.getAccountNumber())
            .execute();
    }

    @Override
    public boolean supportsCaching() {
        return true;
    }

    @Override
    public Long getCacheTtlSeconds() {
        return 300L; // 5 minutes
    }
}
```

### Event-Driven Customer Onboarding

```java
@Component
public class CustomerOnboardingWorkflow {

    private final CommandBus commandBus;

    @org.springframework.context.event.EventListener
    public void handleCustomerCreated(DomainSpringEvent event) {
        if ("customer.created".equals(event.getEnvelope().getType())) {
            CustomerCreatedEvent customerEvent = (CustomerCreatedEvent) event.getEnvelope().getPayload();
            // Trigger KYC verification workflow
            commandBus.send(new StartKycVerificationCommand(customerEvent.getCustomerId()))
                .subscribe();
        }
    }
}
```

### CQRS + Saga Integration

This section demonstrates how to combine the CQRS framework with lib-transactional-engine for distributed transaction management in complex business workflows.

#### Foundational Concepts

**SAGA Pattern** is a distributed transaction pattern that manages data consistency across microservices without using distributed transactions. It coordinates a sequence of local transactions, where each transaction updates data within a single service. If any transaction fails, the saga executes compensating transactions to undo the impact of the preceding transactions.

**CQRS (Command Query Responsibility Segregation)** separates read and write operations into different models. Commands modify state and are processed asynchronously, while Queries retrieve data and can be optimized for specific read scenarios with caching and denormalization.

**Why Combine CQRS + Saga?** This integration provides:
- **Distributed Transaction Management**: Saga orchestrates complex workflows across multiple services
- **Clear Separation of Concerns**: Commands handle state changes, Queries handle data retrieval
- **Automatic Compensation**: Failed saga steps trigger compensating actions
- **Event-Driven Architecture**: Step completion events enable loose coupling between services
- **Scalability**: Asynchronous processing and reactive patterns support high throughput

**Integration Architecture** within lib-common-domain:
```
SagaEngine → @SagaStep Methods → CommandBus/QueryBus → Handlers → Business Logic
     ↓              ↓                    ↓               ↓            ↓
Step Coordination   Input/Output     CQRS Processing   Domain Logic   Data Layer
     ↓              ↓                    ↓               ↓            ↓
Compensation    Step Dependencies    Validation/Cache   Events      Persistence
```

#### Basic Integration Patterns

**Command within Saga Step:**

```java
// Command execution within saga step
@SagaStep(id = "create-account", compensate = "deleteAccount")
public Mono<AccountResult> createAccount(@Input CreateAccountRequest request) {
    CreateAccountCommand command = CreateAccountCommand.builder()
        .customerId(request.getCustomerId())
        .accountType(request.getAccountType())
        .initialDeposit(request.getInitialDeposit())
        .correlationId(request.getCorrelationId())
        .build();

    return commandBus.send(command);
}
```

**Query within Saga Step:**
```java
// Query execution within saga step
@SagaStep(id = "validate-customer", retry = 3)
public Mono<ValidationResult> validateCustomer(@Input CustomerRequest request) {
    ValidateCustomerQuery query = ValidateCustomerQuery.builder()
        .email(request.getEmail())
        .phoneNumber(request.getPhoneNumber())
        .correlationId(request.getCorrelationId())
        .build();

    return queryBus.query(query);
}
```

**Compensation Pattern:**
```java
// Compensation method for failed saga steps
public Mono<Void> deleteAccount(@FromStep("create-account") AccountResult account) {
    DeleteAccountCommand command = DeleteAccountCommand.builder()
        .accountId(account.getAccountId())
        .reason("SAGA_COMPENSATION")
        .build();

    return commandBus.send(command).then();
}
```

#### Complete Banking Workflow Example

This example demonstrates a money transfer saga that combines both commands and queries:

```java
@Component
@Saga(name = "money-transfer")
@EnableTransactionalEngine
public class MoneyTransferSaga {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    // Step 1: Validate source account using Query
    @SagaStep(id = "validate-source", retry = 2)
    public Mono<AccountValidation> validateSourceAccount(@Input TransferRequest request) {
        GetAccountQuery query = GetAccountQuery.builder()
            .accountId(request.getSourceAccountId())
            .correlationId(request.getCorrelationId())
            .build();

        return queryBus.query(query)
            .map(account -> AccountValidation.builder()
                .accountId(account.getAccountId())
                .balance(account.getBalance())
                .isValid(account.getBalance().compareTo(request.getAmount()) >= 0)
                .build());
    }

    // Step 2: Reserve funds using Command
    @SagaStep(id = "reserve-funds",
              dependsOn = "validate-source",
              compensate = "releaseReservation")
    public Mono<ReservationResult> reserveFunds(
            @Input TransferRequest request,
            @FromStep("validate-source") AccountValidation validation) {

        if (!validation.isValid()) {
            return Mono.error(new InsufficientFundsException());
        }

        ReserveFundsCommand command = ReserveFundsCommand.builder()
            .accountId(request.getSourceAccountId())
            .amount(request.getAmount())
            .correlationId(request.getCorrelationId())
            .build();

        return commandBus.send(command);
    }

    // Step 3: Validate destination account using Query
    @SagaStep(id = "validate-destination", dependsOn = "reserve-funds")
    public Mono<AccountValidation> validateDestinationAccount(@Input TransferRequest request) {
        GetAccountQuery query = GetAccountQuery.builder()
            .accountId(request.getDestinationAccountId())
            .correlationId(request.getCorrelationId())
            .build();

        return queryBus.query(query)
            .map(account -> AccountValidation.builder()
                .accountId(account.getAccountId())
                .isValid(account.getStatus().equals("ACTIVE"))
                .build());
    }

    // Step 4: Execute transfer using Command
    @SagaStep(id = "execute-transfer",
              dependsOn = "validate-destination",
              compensate = "reverseTransfer")
    public Mono<TransferResult> executeTransfer(
            @Input TransferRequest request,
            @FromStep("reserve-funds") ReservationResult reservation,
            @FromStep("validate-destination") AccountValidation destination) {

        if (!destination.isValid()) {
            return Mono.error(new InvalidDestinationAccountException());
        }

        ExecuteTransferCommand command = ExecuteTransferCommand.builder()
            .reservationId(reservation.getReservationId())
            .sourceAccountId(request.getSourceAccountId())
            .destinationAccountId(request.getDestinationAccountId())
            .amount(request.getAmount())
            .correlationId(request.getCorrelationId())
            .build();

        return commandBus.send(command);
    }

    // Compensation Methods
    public Mono<Void> releaseReservation(@FromStep("reserve-funds") ReservationResult reservation) {
        ReleaseReservationCommand command = ReleaseReservationCommand.builder()
            .reservationId(reservation.getReservationId())
            .build();
        return commandBus.send(command).then();
    }

    public Mono<Void> reverseTransfer(@FromStep("execute-transfer") TransferResult transfer) {
        ReverseTransferCommand command = ReverseTransferCommand.builder()
            .transferId(transfer.getTransferId())
            .build();
        return commandBus.send(command).then();
    }
}
```

#### Saga Orchestration Service

```java
@Service
public class TransferService {

    private final SagaEngine sagaEngine;
    private final CommandBus commandBus;
    private final QueryBus queryBus;

    /**
     * Executes money transfer using saga orchestration
     */
    public Mono<TransferResult> transferMoney(TransferRequest request) {
        // Create saga inputs for the first step
        StepInputs inputs = StepInputs.of("validate-source", request);

        // Execute the saga by name
        return sagaEngine.execute("money-transfer", inputs)
            .map(this::buildTransferResult);
    }

    /**
     * Alternative: Type-safe saga execution
     */
    public Mono<TransferResult> transferMoneyTypeSafe(TransferRequest request) {
        StepInputs inputs = StepInputs.of("validate-source", request);

        return sagaEngine.execute(MoneyTransferSaga.class, inputs)
            .map(this::buildTransferResult);
    }

    /**
     * Query transfer status using CQRS Query
     */
    public Mono<TransferStatus> getTransferStatus(String transferId) {
        GetTransferStatusQuery query = GetTransferStatusQuery.builder()
            .transferId(transferId)
            .build();

        return queryBus.query(query);
    }

    private TransferResult buildTransferResult(SagaResult sagaResult) {
        if (sagaResult.isSuccess()) {
            // Extract results from completed steps
            TransferResult transfer = sagaResult.resultOf("execute-transfer", TransferResult.class)
                .orElseThrow(() -> new IllegalStateException("Transfer execution step not found"));

            return TransferResult.builder()
                .transferId(transfer.getTransferId())
                .sourceAccountId(transfer.getSourceAccountId())
                .destinationAccountId(transfer.getDestinationAccountId())
                .amount(transfer.getAmount())
                .status("COMPLETED")
                .completedAt(Instant.now())
                .build();
        } else {
            // Handle saga failure with compensation
            List<String> failedSteps = sagaResult.failedSteps();
            List<String> compensatedSteps = sagaResult.compensatedSteps();

            return TransferResult.builder()
                .status("FAILED")
                .failedSteps(failedSteps)
                .compensatedSteps(compensatedSteps)
                .errorMessage("Transfer failed at steps: " + String.join(", ", failedSteps))
                .build();
        }
    }
}
```

#### Configuration

Enable CQRS and Saga integration in your application:

```yaml
firefly:
  cqrs:
    enabled: true
  events:
    enabled: true
  stepevents:
    enabled: true
```

**Required Dependencies:**
```xml
<!-- Only lib-common-domain needed - includes lib-transactional-engine as dependency -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

> **Note**: lib-common-domain automatically includes lib-transactional-engine as a transitive dependency.

**Auto-Configuration:** The integration is automatically configured when lib-common-domain is on the classpath. The following components are auto-wired:
- `CommandBus` and `QueryBus` for CQRS operations
- `SagaEngine` for saga orchestration
- `StepEventPublisherBridge` for step event publishing

#### Best Practices

**1. Command and Query Balance:**
- Use Commands for state-changing operations within saga steps
- Use Queries for validation and data retrieval
- Keep saga steps focused on single responsibilities

**2. Error Handling:**
- Implement proper compensation logic for each saga step
- Use meaningful error messages and correlation IDs
- Handle both business logic errors and technical failures

**3. Performance Optimization:**
- Enable query caching for frequently accessed data
- Use appropriate retry policies for transient failures
- Set reasonable timeouts for external service calls

**4. Testing Strategy:**
- Test individual command/query handlers independently
- Test complete saga flows with success and failure scenarios
- Use integration tests to verify compensation logic

This integration provides a powerful foundation for building complex, distributed banking workflows that maintain data consistency across multiple services while leveraging the benefits of both CQRS and Saga patterns.

## 🔧 Technology Stack

- **Java 21+** - Latest LTS with modern language features, virtual threads, and enhanced performance
- **Spring Boot 3.x** - Auto-configuration and dependency injection with native compilation support
- **Project Reactor** - Reactive programming foundation for non-blocking, asynchronous operations
- **Resilience4j** - Circuit breakers, retry mechanisms, and fault tolerance patterns
- **Micrometer** - Metrics collection and observability with Prometheus integration
- **lib-common-auth** - Integrated authentication and authorization framework (included dependency)
- **Spring Security** - Authentication, authorization, and security context propagation
- **Multiple Messaging** - Kafka, RabbitMQ, AWS SQS/Kinesis support with auto-detection
- **lib-transactional-engine** - Saga orchestration and distributed transaction management (manual integration)
- **Jackson** - JSON serialization/deserialization with reactive streaming support
- **Redis Cache** - Optional distributed caching support for CQRS queries (disabled by default, requires explicit enablement)
- **Local Cache** - High-performance in-memory caching using ConcurrentMapCacheManager (default)

## 📊 Metrics and Monitoring

The library provides comprehensive metrics for production monitoring:

### Authorization Metrics

- `firefly.authorization.attempts` - Total authorization attempts
- `firefly.authorization.successes` - Successful authorizations
- `firefly.authorization.failures` - Failed authorizations
- `firefly.authorization.duration` - Authorization timing
- `firefly.authorization.cache.hits` - Cache hit rate
- `firefly.authorization.active_requests` - Active authorization requests

### CQRS Metrics

- `firefly.cqrs.command.executions` - Command execution count
- `firefly.cqrs.query.executions` - Query execution count
- `firefly.cqrs.command.duration` - Command processing time
- `firefly.cqrs.query.duration` - Query processing time
- `firefly.cqrs.validation.failures` - Validation failure count

### Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🏢 About Firefly

Developed by **Firefly Software Solutions Inc** as part of the Firefly OpenCore Banking Platform.

- **Website**: [getfirefly.io](https://getfirefly.io)
- **GitHub**: [firefly-oss](https://github.com/firefly-oss)
- **License**: Apache 2.0

---

**Ready to build reactive, resilient banking microservices?** Start with our [Developer Guide](docs/DEVELOPER_GUIDE.md) for step-by-step tutorials and banking domain examples.
