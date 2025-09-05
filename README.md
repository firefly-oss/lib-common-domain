# Firefly Common Domain Library

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

A powerful Spring Boot library that enables domain-driven design (DDD) with reactive programming support, featuring multi-messaging adapter architecture and comprehensive event handling capabilities for the **Core-Domain Layer** of the Firefly OpenCore Banking Platform.

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

### 🎯 CQRS Framework
- **Command/Query Separation**: Clean separation of read and write operations
- **Reactive Processing**: Built on Project Reactor for non-blocking operations
- **Event-Driven Workflows**: Support for complex business workflows through domain events
- **Automatic Handler Discovery**: Zero-configuration handler registration
- **Query Caching**: Built-in caching support with configurable TTL

### 🌐 ServiceClient Framework (Redesigned)
- **Unified API**: Single interface for REST, gRPC, and SDK clients
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

**Example:**
```java
// Command - Changes state
TransferMoneyCommand command = new TransferMoneyCommand(
    "ACC-001",           // fromAccount
    "ACC-002",           // toAccount  
    new BigDecimal("1000.00"), // amount
    "Monthly transfer",  // description
    "CORR-12345"        // correlationId
);

// Query - Reads data  
GetAccountBalanceQuery query = new GetAccountBalanceQuery("ACC-001");
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
- **CQRS Framework** (`CqrsAutoConfiguration`)
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

### 4. Create Your First Command Handler

```java
@Component
public class ProcessPaymentHandler implements CommandHandler<ProcessPaymentCommand, PaymentResult> {
    
    private final ServiceClient paymentClient;
    private final ServiceClient notificationClient;
    
    @Override
    public Mono<PaymentResult> handle(ProcessPaymentCommand command) {
        return command.validate()
            .flatMap(this::processPayment)
            .flatMap(this::sendNotification);
    }
    
    @Override
    public Class<ProcessPaymentCommand> getCommandType() {
        return ProcessPaymentCommand.class;
    }
}
```

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [Architecture Guide](docs/ARCHITECTURE.md) | Detailed architecture patterns and design decisions |
| [CQRS Framework](docs/CQRS.md) | Command/Query patterns, handlers, and lib-transactional-engine integration |
| [ServiceClient Framework](docs/NEW_SERVICE_CLIENT_GUIDE.md) | Redesigned unified API for REST, gRPC, and SDK clients |
| [Domain Events](docs/DOMAIN_EVENTS.md) | Multi-messaging adapter architecture and event patterns |
| [Configuration Guide](docs/CONFIGURATION.md) | Complete configuration reference with examples |
| [API Reference](docs/API_REFERENCE.md) | Detailed API documentation with method signatures |
| [Developer Guide](docs/DEVELOPER_GUIDE.md) | Tutorials, best practices, and banking domain examples |
| [Observability Features](OBSERVABILITY.md) | Enhanced metrics and health monitoring |

## 🏦 Banking Domain Examples

### Money Transfer with ServiceClient Integration

```java
@Component
public class MoneyTransferHandler implements CommandHandler<TransferMoneyCommand, TransferResult> {

    private final ServiceClient accountServiceClient;
    private final ServiceClient fraudServiceClient;

    @Override
    public Mono<TransferResult> handle(TransferMoneyCommand command) {
        return performFraudCheck(command)
            .flatMap(this::executeTransfer)
            .flatMap(this::sendNotification);
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
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-domain</artifactId>
</dependency>
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-transactional-engine</artifactId>
</dependency>
```

**Auto-Configuration:** The integration is automatically configured when both libraries are on the classpath. The following components are auto-wired:
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
- **Spring Security** - Authentication, authorization, and security context propagation
- **Multiple Messaging** - Kafka, RabbitMQ, AWS SQS/Kinesis support with auto-detection
- **lib-transactional-engine** - Saga orchestration and distributed transaction management (manual integration)
- **Jackson** - JSON serialization/deserialization with reactive streaming support
- **Caffeine** - High-performance caching for query results and metadata

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🏢 About Firefly

Developed by **Firefly Software Solutions Inc** as part of the Firefly OpenCore Banking Platform.

- **Website**: [getfirefly.io](https://getfirefly.io)
- **GitHub**: [firefly-oss](https://github.com/firefly-oss)
- **License**: Apache 2.0

---

**Ready to build reactive, resilient banking microservices?** Start with our [Developer Guide](docs/DEVELOPER_GUIDE.md) for step-by-step tutorials and banking domain examples.
