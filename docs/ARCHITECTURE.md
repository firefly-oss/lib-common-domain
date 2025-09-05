# Architecture Guide

This document provides a comprehensive overview of the Firefly Common Domain Library architecture, design patterns, and how it enables domain-driven design in the core-domain layer of banking microservices.

## Table of Contents

- [Overview](#overview)
- [4-Tier Architecture](#4-tier-architecture)
- [Core Design Patterns](#core-design-patterns)
- [Component Architecture](#component-architecture)
- [Integration Patterns](#integration-patterns)
- [Banking Domain Modeling](#banking-domain-modeling)

## Overview

The Firefly Common Domain Library implements a sophisticated architecture that combines **Domain-Driven Design (DDD)**, **Command Query Responsibility Segregation (CQRS)**, **Event-Driven Architecture**, and **Reactive Programming** to create a robust foundation for banking microservices.

### Key Architectural Principles

1. **Reactive-First**: Built on Project Reactor for non-blocking, asynchronous operations
2. **Domain-Centric**: Business logic and domain models are the primary focus
3. **Event-Driven**: Loose coupling through domain events and messaging
4. **Resilient**: Circuit breakers, retries, and graceful degradation
5. **Observable**: Comprehensive metrics, tracing, and health monitoring

## 4-Tier Architecture

The library serves as the foundation for the **Core-Domain Layer** in Firefly's 4-tier microservices architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                    CHANNELS LAYER                           │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐            │
│  │   Web UI    │ │ Mobile App  │ │  API Gateway│            │
│  │             │ │             │ │             │            │
│  └─────────────┘ └─────────────┘ └─────────────┘            │
└─────────────────────────────────────────────────────────────┘
                              │
                    HTTP/REST/GraphQL
                              │
┌─────────────────────────────────────────────────────────────┐
│              APPLICATION/PROCESS LAYER                      │
│      ┌─────────────┐ ┌─────────────┐ ┌─────────────┐        │
│      │  Workflow   │ │  Process    │ │ Integration │        │
│      │  Services   │ │ Orchestrator│ │   Services  │        │
│      └─────────────┘ └─────────────┘ └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
                              │
                    Commands/Queries/Events
                              │
┌─────────────────────────────────────────────────────────────┐
│ ★                    CORE-DOMAIN LAYER                    ★ │
│                        (THIS LIBRARY)                       │
│  ┌─────────────┐   ┌───────────────┐   ┌───────────────┐    │
│  │    CQRS     │   │     Domain    │   │    Service    │    │
│  │  Framework  │   │     Events    │   │    Clients    │    │
│  │             │   │               │   │               │    │
│  │ ┌─────────┐ │   │ ┌───────────┐ │   │ ┌───────────┐ │    │
│  │ │Commands │ │   │ │ Publishers│ │   │ │   REST    │ │    │
│  │ │Queries  │ │   │ │ Consumers │ │   │ │   gRPC    │ │    │
│  │ │Handlers │ │   │ │ Adapters  │ │   │ │    SDK    │ │    │
│  │ │         │ │   │ │ Filtering │ │   │ │ Resilience│ │    │
│  │ └─────────┘ │   │ └───────────┘ │   │ └───────────┘ │    │
│  └─────────────┘   └───────────────┘   └───────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │
                    Database Operations/External APIs
                              │
┌─────────────────────────────────────────────────────────────┐
│                 CORE-INFRASTRUCTURE LAYER                   │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐    │
│  │  Database   │     │   Cache     │     │  External   │    │
│  │   CRUD      │     │  Services   │     │    APIs     │    │
│  │  Services   │     │             │     │             │    │
│  └─────────────┘     └─────────────┘     └─────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

#### Channels Layer
- User interfaces (Web, Mobile, APIs)
- Authentication and authorization
- Request/response transformation
- Rate limiting and throttling

#### Application/Process Layer
- Business process orchestration
- Workflow management
- Cross-cutting concerns
- Integration with external systems

#### Core-Domain Layer (This Library)
- **Business logic and domain models**
- **CQRS command and query handling**
- **Domain event publishing and consumption**
- **StepEvents integration with lib-transactional-engine**
- **Saga orchestration and step event processing**
- **Service-to-service communication**
- **Resilience and observability patterns**

#### Core-Infrastructure Layer
- Data persistence (CRUD operations)
- Caching strategies
- External API integrations
- Infrastructure services

## Core Design Patterns

### 1. Command Query Responsibility Segregation (CQRS)

CQRS separates read and write operations, allowing for optimized data models and scalability.

```java
// Command Side - Write Operations
@Component
public class CreateAccountHandler implements CommandHandler<CreateAccountCommand, AccountResult> {
    
    @Override
    public Mono<AccountResult> handle(CreateAccountCommand command) {
        return command.validate()
            .flatMap(this::createAccount)
            .flatMap(this::publishAccountCreatedEvent);
    }
}

// Query Side - Read Operations  
@Component
public class GetAccountHandler implements QueryHandler<GetAccountQuery, Account> {
    
    @Cacheable("accounts")
    @Override
    public Mono<Account> handle(GetAccountQuery query) {
        return accountRepository.findById(query.getAccountId());
    }
}
```

### 2. Event-Driven Architecture

Domain events enable loose coupling and eventual consistency across microservices.

```java
// Event Publishing
@EventPublisher(topic = "banking.accounts", type = "account.created")
public Mono<Account> createAccount(CreateAccountCommand command) {
    // Business logic
    return accountService.create(command);
}

// Event Consumption
@EventListener
public void handleAccountCreated(DomainSpringEvent event) {
    if ("account.created".equals(event.getEnvelope().getType())) {
        // Trigger downstream processes
        commandBus.send(new SetupAccountServicesCommand(accountId));
    }
}
```

### 3. Event-Driven Workflows

The library supports event-driven workflows for complex business processes. Commands can publish domain events that trigger subsequent processing steps.

```java
@Component
public class MoneyTransferCommandHandler implements CommandHandler<TransferMoneyCommand, TransferResult> {

    private final DomainEventPublisher eventPublisher;

    @Override
    public Mono<TransferResult> handle(TransferMoneyCommand command) {
        // Process the transfer and publish events
        return processTransfer(command)
            .flatMap(result -> publishTransferEvents(result))
            .map(result -> new TransferResult(result.isSuccess()));
    }
}
```

### 4. Service Client Pattern

Unified abstraction for service-to-service communication with resilience patterns.

```java
// REST Client
@Component
public class NotificationService {
    
    private final RestServiceClient notificationClient;
    
    public Mono<Void> sendTransferNotification(TransferEvent event) {
        return notificationClient.post("/notifications/transfer", event, Void.class);
    }
}

// SDK Client
@Component
public class PaymentService {
    
    private final SdkServiceClient<StripeSDK> stripeClient;
    
    public Mono<PaymentResult> processPayment(PaymentRequest request) {
        return stripeClient.execute(sdk -> 
            sdk.charges().create(request.toChargeParams()));
    }
}
```

## Component Architecture

### CQRS Framework Components

```
┌─────────────────────────────────────────────────────────────┐
│                       CQRS Framework                        │
│                                                             │
│             ┌─────────────┐    ┌─────────────┐              │
│             │ CommandBus  │    │  QueryBus   │              │
│             │             │    │             │              │
│             │ • Routing   │    │ • Routing   │              │
│             │ • Validation│    │ • Caching   │              │
│             │ • Tracing   │    │ • Tracing   │              │
│             └─────────────┘    └─────────────┘              │
│                    │                   │                    │
│             ┌─────────────┐    ┌─────────────┐              │
│             │   Command   │    │    Query    │              │
│             │  Handlers   │    │  Handlers   │              │
│             └─────────────┘    └─────────────┘              │
└─────────────────────────────────────────────────────────────┘
```

### Domain Events Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Domain Events                             │
│                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐      │
│  │   Event     │    │  Messaging  │    │   Event     │      │
│  │ Publishers  │    │  Adapters   │    │ Consumers   │      │
│  │             │    │             │    │             │      │
│  │ • Annotation│    │ • Kafka     │    │ • Listeners │      │
│  │ • Programm. │    │ • RabbitMQ  │    │ • Filtering │      │
│  │ • Async     │    │ • SQS       │    │ • Routing   │      │
│  │ • Retry     │    │ • Kinesis   │    │ • Error Hdl │      │
│  └─────────────┘    │ • AppEvents │    └─────────────┘      │
│                     └─────────────┘                         │
└─────────────────────────────────────────────────────────────┘
```

### StepEvents Integration Architecture

```
┌─────────────────────────────────────────────────────────────┐
│              StepEvents Bridge Pattern                      │
│                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌────────────────┐   │
│  │lib-transact │    │StepEvent    │    │     Domain     │   │
│  │ional-engine │───▶│Publisher    │───▶│     Events     │   │
│  │             │    │   Bridge    │    │ Infrastructure │   │
│  │ • Saga Mgmt │    │             │    │                │   │
│  │ • Step Exec │    │ • Transform │    │    • Kafka     │   │
│  │ • Retry     │    │ • Enrich    │    │    • RabbitMQ  │   │
│  │ • Rollback  │    │ • Metadata  │    │    • SQS       │   │
│  └─────────────┘    └─────────────┘    │    • Kinesis   │   │
│                                        │    • AppEvents │   │
│                                        └────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### ServiceClient Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  ServiceClient Framework                    │
│                                                             │
│     ┌─────────────┐    ┌─────────────┐    ┌─────────────┐   │
│     │    REST     │    │    gRPC     │    │     SDK     │   │
│     │   Client    │    │   Client    │    │   Client    │   │
│     │             │    │             │    │             │   │
│     │ • WebClient │    │ • Stubs     │    │ • Factories │   │
│     │ • Reactive  │    │ • Streaming │    │ • Lifecycle │   │
│     │ • Auth      │    │ • Metadata  │    │ • Wrapping  │   │
│     └─────────────┘    └─────────────┘    └─────────────┘   │
│            │                   │                   │        │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                     Resilience Patterns                 ││
│  │  • Circuit Breakers  • Retries  • Timeouts  • Bulkhead  ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

## Integration Patterns

### 1. Command-Driven Integration

Commands trigger operations across service boundaries:

```java
@Component
public class LoanApplicationHandler implements CommandHandler<SubmitLoanApplicationCommand, LoanResult> {
    
    private final SdkServiceClient<CreditBureauSDK> creditBureauClient;
    private final RestServiceClient riskAssessmentClient;
    
    @Override
    public Mono<LoanResult> handle(SubmitLoanApplicationCommand command) {
        return performCreditCheck(command)
            .flatMap(this::assessRisk)
            .flatMap(this::makeDecision)
            .flatMap(this::publishDecisionEvent);
    }
}
```

### 2. Event-Driven Integration

Events enable reactive, asynchronous integration:

```java
@EventListener
public void handleCustomerOnboarded(DomainSpringEvent event) {
    if ("customer.onboarded".equals(event.getEnvelope().getType())) {
        // Trigger account setup
        commandBus.send(new CreateDefaultAccountsCommand(customerId));
        
        // Start KYC process
        commandBus.send(new InitiateKycCommand(customerId));
        
        // Send welcome notification
        commandBus.send(new SendWelcomeNotificationCommand(customerId));
    }
}
```

### 3. Query-Based Integration

Queries retrieve data from multiple sources:

```java
@Component
public class CustomerProfileHandler implements QueryHandler<GetCustomerProfileQuery, CustomerProfile> {
    
    private final RestServiceClient customerServiceClient;
    private final SdkServiceClient<AccountSDK> accountClient;
    private final RestServiceClient transactionServiceClient;
    
    @Override
    public Mono<CustomerProfile> handle(GetCustomerProfileQuery query) {
        return Mono.zip(
            getCustomerDetails(query.getCustomerId()),
            getAccountSummary(query.getCustomerId()),
            getRecentTransactions(query.getCustomerId())
        ).map(this::buildCustomerProfile);
    }
}
```

## Banking Domain Modeling

### Domain Entities and Value Objects

```java
// Aggregate Root
@Entity
public class Account {
    private AccountId id;
    private CustomerId customerId;
    private AccountNumber accountNumber;
    private Money balance;
    private AccountStatus status;
    private List<Transaction> transactions;
    
    // Domain methods
    public void debit(Money amount) {
        if (balance.isLessThan(amount)) {
            throw new InsufficientFundsException();
        }
        this.balance = balance.subtract(amount);
        addTransaction(Transaction.debit(amount));
    }
}

// Value Object
@Value
public class Money {
    BigDecimal amount;
    Currency currency;
    
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }
}
```

### Banking-Specific Patterns

#### 1. Account Aggregates
- Account as aggregate root
- Transactions as entities within aggregate
- Balance as calculated value

#### 2. Money Transfer Process
- Multi-step event-driven process
- Eventual consistency across accounts
- Audit trail for regulatory compliance

#### 3. Regulatory Event Sourcing
- Immutable event log for compliance
- Replay capability for audits
- Point-in-time account reconstruction

---

This architecture enables building robust, scalable, and maintainable banking microservices that can handle the complexity and regulatory requirements of financial services while maintaining high performance and reliability.
