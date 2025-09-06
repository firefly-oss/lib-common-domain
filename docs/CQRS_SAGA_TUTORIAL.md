# CQRS and Sagas Tutorial

A comprehensive step-by-step guide to building distributed banking applications using CQRS and Saga patterns with the Firefly Common Domain Library.

## Table of Contents

1. [Introduction](#introduction)
2. [Use Case Definition](#use-case-definition)
3. [Use Case Architecture](#use-case-architecture)
4. [Prerequisites and Setup](#prerequisites-and-setup)
5. [Step-by-Step Implementation Guide](#step-by-step-implementation-guide)
   - [Step 1: Setting up CQRS Components](#step-1-setting-up-cqrs-components)
   - [Step 2: Creating Commands and Queries](#step-2-creating-commands-and-queries)
   - [Step 3: Implementing Command and Query Handlers](#step-3-implementing-command-and-query-handlers)
   - [Step 4: Building the Saga Orchestrator](#step-4-building-the-saga-orchestrator)
   - [Step 5: Integrating Domain Events](#step-5-integrating-domain-events)
   - [Step 6: Adding Configuration](#step-6-adding-configuration)
   - [Step 7: Testing the Complete Solution](#step-7-testing-the-complete-solution)
6. [Best Practices](#best-practices)
7. [Troubleshooting](#troubleshooting)
8. [Advanced Topics](#advanced-topics)

## Introduction

This tutorial demonstrates how to build a robust banking application using the Command Query Responsibility Segregation (CQRS) pattern combined with Saga orchestration for managing distributed transactions. You'll learn how to create a complete customer onboarding system that handles complex business workflows with automatic compensation and error handling.

### What You'll Learn

- How to implement CQRS commands and queries
- How to create Saga orchestrators for distributed transactions  
- How to integrate Domain Events for cross-service communication
- How to handle compensation and error scenarios
- How to test CQRS + Saga implementations

### Technologies Used

- **Firefly Common Domain Library**: CQRS and Domain Events framework
- **lib-transactional-engine**: Saga orchestration engine
- **Spring Boot**: Application framework
- **Project Reactor**: Reactive programming
- **Apache Kafka/RabbitMQ**: Event streaming (optional)

## Use Case Definition

We'll build a **Customer Onboarding System** for a digital bank that demonstrates the power of CQRS and Saga patterns. This system handles the complete customer registration workflow with multiple validation steps, external service integrations, and automatic compensation.

### Business Requirements

**Primary Flow: Customer Registration**
1. **Customer Data Validation** - Verify email uniqueness and phone format
2. **Profile Creation** - Create customer profile in the system
3. **KYC Verification** - Perform Know Your Customer verification
4. **Account Creation** - Create initial bank account
5. **Welcome Notification** - Send welcome email to customer

**Error Handling Requirements**
- If any step fails, automatically compensate completed steps
- Retry transient failures with exponential backoff
- Maintain data consistency across all services
- Provide detailed error reporting and audit trails

### Why CQRS + Saga?

This use case perfectly demonstrates why we need both patterns:

**CQRS Benefits:**
- **Separation of Concerns**: Commands handle state changes, queries handle data retrieval
- **Scalability**: Read and write operations can be optimized independently
- **Validation**: Commands include built-in validation logic
- **Tracing**: Built-in correlation context for distributed tracing

**Saga Benefits:**
- **Distributed Transactions**: Manage consistency across multiple microservices
- **Automatic Compensation**: Roll back completed steps when failures occur
- **Resilience**: Handle partial failures gracefully with retry logic
- **Observability**: Track complex workflow execution and performance

## Use Case Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Customer Onboarding Saga                     │
│                                                                 │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────┐  │
│  │   Validate  │ │   Create    │ │     KYC     │ │  Create   │  │
│  │  Customer   │→│   Profile   │→│Verification │→│  Account  │  │
│  │             │ │             │ │             │ │           │  │
│  └─────────────┘ └─────────────┘ └─────────────┘ └───────────┘  │
│                                         │                       │
│                                         ▼                       │
│                                  ┌─────────────┐                │
│                                  │   Welcome   │                │
│                                  │ Notification│                │
│                                  └─────────────┘                │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                            CQRS Layer                           │
│                                                                 │
│  Commands (Write)              │         Queries (Read)         │
│  ┌─────────────────────────────┼─────────────────────────────┐  │
│  │ • ValidateCustomerQuery     │  • GetCustomerProfileQuery  │  │
│  │ • CreateCustomerProfile     │  • GetAccountBalanceQuery   │  │
│  │ • StartKycVerification      │  • GetKycStatusQuery        │  │
│  │ • CreateAccountCommand      │  • GetTransactionHistory    │  │
│  │ • SendNotificationCommand   │                             │  │
│  └─────────────────────────────┼─────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      Domain Events Layer                        │
│                                                                 │
│  Event Publishers              │        Event Consumers         │
│  ┌─────────────────────────────┼─────────────────────────────┐  │
│  │ • customer.validated        │  • Audit Service            │  │
│  │ • profile.created           │  • Analytics Service        │  │
│  │ • kyc.completed             │  • Notification Service     │  │
│  │ • account.opened            │  • Compliance Service       │  │
│  │ • saga.step.completed       │                             │  │
│  └─────────────────────────────┼─────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Component Interactions

1. **SagaEngine** orchestrates the entire workflow
2. **SagaSteps** execute CQRS commands and queries
3. **CommandBus** routes commands to appropriate handlers
4. **QueryBus** routes queries to appropriate handlers
5. **DomainEventPublisher** publishes events for each step completion
6. **StepEventPublisherBridge** converts saga events to domain events
7. **Compensation Methods** handle rollback scenarios

### Data Flow

```
Client Request → SagaEngine → SagaStep → CQRS Bus → Handler → Domain Events
                     ↓              ↓         ↓        ↓            ↓
              Step Results ← Command/Query ← Business ← Event ← External
                     ↓              Response   Logic   Publishing Services
              Next Step or 
              Compensation
```

## Prerequisites and Setup

### Dependencies

Add these dependencies to your `pom.xml`:

```xml
<dependencies>
    <!-- Firefly Common Domain Library -->
    <dependency>
        <groupId>com.firefly</groupId>
        <artifactId>lib-common-domain</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
    <!-- Optional: Kafka for Domain Events -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
</dependencies>
```

### Configuration

Create `application.yml`:

```yaml
firefly:
  # Enable CQRS framework
  cqrs:
    enabled: true
    command:
      timeout: 30s
      metrics-enabled: true
    query:
      timeout: 15s
      caching-enabled: true
      cache-ttl: 15m
  
  # Enable Domain Events
  events:
    enabled: true
    adapter: auto  # Will auto-detect Kafka, RabbitMQ, or use in-process events
  
  # Enable StepEvents bridge for Saga integration
  stepevents:
    enabled: true

# Optional: Kafka configuration for Domain Events
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

# Domain topic for saga step events
domain:
  topic: banking-domain-events

logging:
  level:
    com.firefly: DEBUG
```

### Enable Components

Create your main application class:

```java
@SpringBootApplication
@EnableTransactionalEngine  // Enable Saga support
public class BankingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingApplication.class, args);
    }
}
```

## Step-by-Step Implementation Guide

### Step 1: Setting up CQRS Components

#### Create Data Transfer Objects

First, let's create the data structures for our customer onboarding process:

```java
// Customer Registration Request
@Data
@Builder
public class CustomerRegistrationRequest {
    private String customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String documentType;
    private String documentNumber;
    private BigDecimal initialDeposit;
    private String correlationId;
}

// Customer Validation Result
@Data
@Builder
public class CustomerValidationResult {
    private String customerId;
    private boolean isValid;
    private List<String> validationErrors;
}

// Customer Profile Result  
@Data
@Builder
public class CustomerProfileResult {
    private String customerId;
    private String profileId;
    private String email;
    private String status;
}

// KYC Result
@Data
@Builder
public class KycResult {
    private String kycId;
    private String customerId;
    private boolean isApproved;
    private String status;
    private String rejectionReason;
}

// Account Creation Result
@Data
@Builder
public class AccountCreationResult {
    private String accountId;
    private String accountNumber;
    private String customerId;
    private BigDecimal initialBalance;
    private String currency;
    private String status;
}

// Notification Result
@Data
@Builder  
public class NotificationResult {
    private String notificationId;
    private String customerId;
    private String type;
    private String status;
    private Instant sentAt;
}
```

### Step 2: Creating Commands and Queries

#### Define CQRS Commands

Commands represent write operations that change system state:

```java
// Validate Customer Query (read operation to check existing data)
@Data
@Builder
public class ValidateCustomerQuery implements Query<CustomerValidation> {
    private final String email;
    private final String phoneNumber;
    private final String correlationId;
    
    @Override
    public String getCorrelationId() {
        return correlationId;
    }
    
    @Override
    public Class<CustomerValidation> getResultType() {
        return CustomerValidation.class;
    }
}

// Create Customer Profile Command
@Data
@Builder
public class CreateCustomerProfileCommand implements Command<CustomerProfileResult> {
    private final String customerId;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phoneNumber;
    private final String correlationId;
    
    @Override
    public Mono<ValidationResult> validate() {
        ValidationResult.Builder builder = ValidationResult.builder();
        
        if (customerId == null || customerId.trim().isEmpty()) {
            builder.addError("customerId", "Customer ID is required");
        }
        
        if (firstName == null || firstName.trim().isEmpty()) {
            builder.addError("firstName", "First name is required");
        }
        
        if (lastName == null || lastName.trim().isEmpty()) {
            builder.addError("lastName", "Last name is required");
        }
        
        if (email == null || !isValidEmail(email)) {
            builder.addError("email", "Valid email is required");
        }
        
        if (phoneNumber == null || !isValidPhone(phoneNumber)) {
            builder.addError("phoneNumber", "Valid phone number is required");
        }
        
        return Mono.just(builder.build());
    }
    
    @Override
    public String getCorrelationId() {
        return correlationId;
    }
    
    @Override
    public Class<CustomerProfileResult> getResultType() {
        return CustomerProfileResult.class;
    }
    
    // Helper validation methods
    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }
    
    private boolean isValidPhone(String phone) {
        return phone.matches("^\\+?[1-9]\\d{1,14}$");
    }
}

// Start KYC Verification Command
@Data
@Builder
public class StartKycVerificationCommand implements Command<KycResult> {
    private final String customerId;
    private final String profileId;
    private final String documentType;
    private final String documentNumber;
    private final String correlationId;
    
    @Override
    public Mono<ValidationResult> validate() {
        ValidationResult.Builder builder = ValidationResult.builder();
        
        if (customerId == null || customerId.trim().isEmpty()) {
            builder.addError("customerId", "Customer ID is required");
        }
        
        if (profileId == null || profileId.trim().isEmpty()) {
            builder.addError("profileId", "Profile ID is required");
        }
        
        if (documentType == null || !isValidDocumentType(documentType)) {
            builder.addError("documentType", "Valid document type is required");
        }
        
        if (documentNumber == null || documentNumber.trim().isEmpty()) {
            builder.addError("documentNumber", "Document number is required");
        }
        
        return Mono.just(builder.build());
    }
    
    @Override
    public String getCorrelationId() {
        return correlationId;
    }
    
    @Override
    public Class<KycResult> getResultType() {
        return KycResult.class;
    }
    
    private boolean isValidDocumentType(String type) {
        return List.of("PASSPORT", "DRIVERS_LICENSE", "NATIONAL_ID").contains(type);
    }
}

// Create Account Command
@Data
@Builder
public class CreateAccountCommand implements Command<AccountCreationResult> {
    private final String customerId;
    private final String accountType;
    private final BigDecimal initialDeposit;
    private final String currency;
    private final String correlationId;
    
    @Override
    public Mono<ValidationResult> validate() {
        ValidationResult.Builder builder = ValidationResult.builder();
        
        if (customerId == null || customerId.trim().isEmpty()) {
            builder.addError("customerId", "Customer ID is required");
        }
        
        if (accountType == null || !isValidAccountType(accountType)) {
            builder.addError("accountType", "Valid account type is required");
        }
        
        if (initialDeposit != null && initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            builder.addError("initialDeposit", "Initial deposit cannot be negative");
        }
        
        if (currency == null || currency.trim().isEmpty()) {
            builder.addError("currency", "Currency is required");
        }
        
        return Mono.just(builder.build());
    }
    
    @Override
    public String getCorrelationId() {
        return correlationId;
    }
    
    @Override
    public Class<AccountCreationResult> getResultType() {
        return AccountCreationResult.class;
    }
    
    private boolean isValidAccountType(String type) {
        return List.of("CHECKING", "SAVINGS", "BUSINESS").contains(type);
    }
}
```

#### Define CQRS Queries

Queries represent read operations for data retrieval:

```java
// Customer Profile Query
@Data
@Builder
public class GetCustomerProfileQuery implements Query<CustomerProfile> {
    private final String customerId;
    private final String correlationId;
    
    @Override
    public String getCorrelationId() {
        return correlationId;
    }
    
    @Override
    public Class<CustomerProfile> getResultType() {
        return CustomerProfile.class;
    }
    
    @Override
    public String getCacheKey() {
        return "customer-profile:" + customerId;
    }
}

// Customer Profile Data
@Data
@Builder
public class CustomerProfile {
    private String customerId;
    private String profileId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String status;
    private Instant createdAt;
}

// Customer Validation Data
@Data
@Builder
public class CustomerValidation {
    private boolean isValid;
    private List<String> validationErrors;
    
    public static CustomerValidation valid() {
        return CustomerValidation.builder()
            .isValid(true)
            .validationErrors(Collections.emptyList())
            .build();
    }
    
    public static CustomerValidation invalid(List<String> errors) {
        return CustomerValidation.builder()
            .isValid(false)
            .validationErrors(errors)
            .build();
    }
}
```

### Step 3: Implementing Command and Query Handlers

#### Command Handlers

Command handlers contain the business logic for processing commands:

```java
// Customer Profile Command Handler
@Component
@Slf4j
public class CreateCustomerProfileHandler implements CommandHandler<CreateCustomerProfileCommand, CustomerProfileResult> {
    
    private final ServiceClient customerServiceClient;
    private final DomainEventPublisher eventPublisher;
    
    public CreateCustomerProfileHandler(ServiceClient customerServiceClient, 
                                      DomainEventPublisher eventPublisher) {
        this.customerServiceClient = customerServiceClient;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public Mono<CustomerProfileResult> handle(CreateCustomerProfileCommand command) {
        log.info("Creating customer profile for customer: {}", command.getCustomerId());
        
        return command.validate()
            .flatMap(validation -> {
                if (!validation.isValid()) {
                    return Mono.error(new ValidationException(validation));
                }
                return createProfile(command);
            })
            .flatMap(this::publishProfileCreatedEvent)
            .doOnSuccess(result -> log.info("Customer profile created: {}", result.getProfileId()))
            .doOnError(error -> log.error("Failed to create customer profile", error));
    }
    
    private Mono<CustomerProfileResult> createProfile(CreateCustomerProfileCommand command) {
        // Use ServiceClient to interact with core-infra layer
        CreateProfileRequest request = CreateProfileRequest.builder()
            .customerId(command.getCustomerId())
            .firstName(command.getFirstName())
            .lastName(command.getLastName())
            .email(command.getEmail())
            .build();
        
        return customerServiceClient.post("/profiles", CustomerProfileResult.class)
            .withBody(request)
            .execute();
    }
    
    private Mono<CustomerProfileResult> publishProfileCreatedEvent(CustomerProfileResult result) {
        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("banking.customers")
            .type("customer.profile.created")
            .key(result.getCustomerId())
            .payload(result)
            .timestamp(Instant.now())
            .build();
            
        return eventPublisher.publish(event)
            .thenReturn(result)
            .doOnSuccess(r -> log.debug("Published profile created event for customer: {}", r.getCustomerId()));
    }
    
    @Override
    public Class<CreateCustomerProfileCommand> getCommandType() {
        return CreateCustomerProfileCommand.class;
    }
}

// KYC Verification Command Handler
@Component
@Slf4j
public class StartKycVerificationHandler implements CommandHandler<StartKycVerificationCommand, KycResult> {
    
    private final ServiceClient kycServiceClient;
    private final DomainEventPublisher eventPublisher;
    
    public StartKycVerificationHandler(ServiceClient kycServiceClient,
                                     DomainEventPublisher eventPublisher) {
        this.kycServiceClient = kycServiceClient;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public Mono<KycResult> handle(StartKycVerificationCommand command) {
        log.info("Starting KYC verification for customer: {}", command.getCustomerId());
        
        return command.validate()
            .flatMap(validation -> {
                if (!validation.isValid()) {
                    return Mono.error(new ValidationException(validation));
                }
                return performKycVerification(command);
            })
            .flatMap(this::publishKycCompletedEvent)
            .doOnSuccess(result -> log.info("KYC verification completed: {} - {}", 
                result.getKycId(), result.isApproved() ? "APPROVED" : "REJECTED"))
            .doOnError(error -> log.error("Failed to complete KYC verification", error));
    }
    
    private Mono<KycResult> performKycVerification(StartKycVerificationCommand command) {
        // Use ServiceClient to interact with KYC verification service via core-infra layer
        KycVerificationRequest request = KycVerificationRequest.builder()
            .customerId(command.getCustomerId())
            .documentType(command.getDocumentType())
            .documentNumber(command.getDocumentNumber())
            .build();
        
        return kycServiceClient.post("/kyc/verify", KycResult.class)
            .withBody(request)
            .execute();
    }
    
    private Mono<KycResult> publishKycCompletedEvent(KycResult result) {
        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("banking.kyc")
            .type("kyc.verification.completed")
            .key(result.getCustomerId())
            .payload(result)
            .timestamp(Instant.now())
            .build();
            
        return eventPublisher.publish(event)
            .thenReturn(result)
            .doOnSuccess(r -> log.debug("Published KYC completed event for customer: {}", r.getCustomerId()));
    }
    
    @Override
    public Class<StartKycVerificationCommand> getCommandType() {
        return StartKycVerificationCommand.class;
    }
}

// Account Creation Command Handler
@Component
@Slf4j
public class CreateAccountHandler implements CommandHandler<CreateAccountCommand, AccountCreationResult> {
    
    private final ServiceClient accountServiceClient;
    private final DomainEventPublisher eventPublisher;
    
    public CreateAccountHandler(ServiceClient accountServiceClient,
                               DomainEventPublisher eventPublisher) {
        this.accountServiceClient = accountServiceClient;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public Mono<AccountCreationResult> handle(CreateAccountCommand command) {
        log.info("Creating account for customer: {}", command.getCustomerId());
        
        return command.validate()
            .flatMap(validation -> {
                if (!validation.isValid()) {
                    return Mono.error(new ValidationException(validation));
                }
                return createAccount(command);
            })
            .flatMap(this::publishAccountCreatedEvent)
            .doOnSuccess(result -> log.info("Account created: {}", result.getAccountNumber()))
            .doOnError(error -> log.error("Failed to create account", error));
    }
    
    private Mono<AccountCreationResult> createAccount(CreateAccountCommand command) {
        // Use ServiceClient to interact with account service via core-infra layer
        CreateAccountRequest request = CreateAccountRequest.builder()
            .customerId(command.getCustomerId())
            .accountType(command.getAccountType())
            .initialDeposit(command.getInitialDeposit() != null ? command.getInitialDeposit() : BigDecimal.ZERO)
            .currency(command.getCurrency())
            .build();
        
        return accountServiceClient.post("/accounts", AccountCreationResult.class)
            .withBody(request)
            .execute();
    }
    
    private Mono<AccountCreationResult> publishAccountCreatedEvent(AccountCreationResult result) {
        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("banking.accounts")
            .type("account.created")
            .key(result.getCustomerId())
            .payload(result)
            .timestamp(Instant.now())
            .build();
            
        return eventPublisher.publish(event)
            .thenReturn(result)
            .doOnSuccess(r -> log.debug("Published account created event for customer: {}", r.getCustomerId()));
    }
    
    @Override
    public Class<CreateAccountCommand> getCommandType() {
        return CreateAccountCommand.class;
    }
}
```

#### Query Handlers

Query handlers process read operations with caching support:

```java
// Customer Validation Query Handler
@Component
@Slf4j
public class ValidateCustomerHandler implements QueryHandler<ValidateCustomerQuery, CustomerValidation> {
    
    private final ServiceClient customerValidationServiceClient;
    
    public ValidateCustomerHandler(ServiceClient customerValidationServiceClient) {
        this.customerValidationServiceClient = customerValidationServiceClient;
    }
    
    @Override
    @Cacheable(value = "customer-validations", key = "#query.cacheKey")
    public Mono<CustomerValidation> handle(ValidateCustomerQuery query) {
        log.debug("Validating customer email: {}, phone: {}", query.getEmail(), query.getPhoneNumber());
        
        return performValidation(query)
            .doOnSuccess(result -> log.debug("Customer validation completed: valid={}", result.isValid()));
    }
    
    private Mono<CustomerValidation> performValidation(ValidateCustomerQuery query) {
        // Use ServiceClient to interact with customer validation service via core-infra layer
        CustomerValidationRequest request = CustomerValidationRequest.builder()
            .email(query.getEmail())
            .phoneNumber(query.getPhoneNumber())
            .build();
        
        return customerValidationServiceClient.post("/validation/customer", CustomerValidation.class)
            .withBody(request)
            .execute();
    }
    
    @Override
    public Class<ValidateCustomerQuery> getQueryType() {
        return ValidateCustomerQuery.class;
    }
}

// Customer Profile Query Handler
@Component
@Slf4j
public class GetCustomerProfileHandler implements QueryHandler<GetCustomerProfileQuery, CustomerProfile> {
    
    @Override
    @Cacheable(value = "customer-profiles", key = "#query.cacheKey")
    public Mono<CustomerProfile> handle(GetCustomerProfileQuery query) {
        log.debug("Retrieving customer profile for: {}", query.getCustomerId());
        
        return fetchCustomerProfile(query.getCustomerId())
            .doOnSuccess(profile -> log.debug("Retrieved profile for customer: {}", profile.getCustomerId()));
    }
    
    private Mono<CustomerProfile> fetchCustomerProfile(String customerId) {
        // Simulate database lookup - in real implementation, this would query database
        return Mono.just(CustomerProfile.builder()
            .customerId(customerId)
            .profileId("PROF-" + UUID.randomUUID().toString())
            .firstName("John")  // Mock data
            .lastName("Doe")    // Mock data
            .email("john.doe@example.com")  // Mock data
            .phoneNumber("+1-555-123-4567") // Mock data
            .status("ACTIVE")
            .createdAt(Instant.now())
            .build())
            .delayElement(Duration.ofMillis(30)); // Simulate query time
    }
    
    @Override
    public Class<GetCustomerProfileQuery> getQueryType() {
        return GetCustomerProfileQuery.class;
    }
}
```

### Step 4: Building the Saga Orchestrator

Now we'll create the main Saga orchestrator that coordinates the entire customer onboarding workflow:

```java
/**
 * Customer Registration Saga demonstrating CQRS + lib-transactional-engine integration.
 * This saga orchestrates a complete customer onboarding process using CQRS commands and queries
 * within saga steps, with automatic compensation on failures.
 */
@Component
@Saga(name = "customer-registration")
@EnableTransactionalEngine
@Slf4j
public class CustomerRegistrationSaga {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    public CustomerRegistrationSaga(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    /**
     * Step 1: Validate customer data using CQRS Query
     * This step validates email uniqueness and phone number format
     */
    @SagaStep(id = "validate-customer", retry = 3, backoffMs = 1000)
    public Mono<CustomerValidationResult> validateCustomer(@Input CustomerRegistrationRequest request) {
        log.info("Step 1: Validating customer data for {}", request.getCustomerId());
        
        ValidateCustomerQuery query = ValidateCustomerQuery.builder()
            .email(request.getEmail())
            .phoneNumber(request.getPhoneNumber())
            .correlationId(request.getCorrelationId())
            .build();
            
        return queryBus.query(query)
            .map(validation -> CustomerValidationResult.builder()
                .customerId(request.getCustomerId())
                .isValid(validation.isValid())
                .validationErrors(validation.getValidationErrors())
                .build())
            .doOnSuccess(result -> log.info("Customer validation completed: valid={}", result.isValid()))
            .doOnError(error -> log.error("Customer validation failed", error));
    }

    /**
     * Step 2: Create customer profile using CQRS Command
     * This step creates the customer profile in the system
     * Compensation: deleteProfile
     */
    @SagaStep(id = "create-profile",
              dependsOn = "validate-customer",
              compensate = "deleteProfile",
              timeoutMs = 30000)
    public Mono<CustomerProfileResult> createProfile(
            @FromStep("validate-customer") CustomerValidationResult validation,
            @Input CustomerRegistrationRequest request) {

        log.info("Step 2: Creating customer profile for {}", validation.getCustomerId());

        if (!validation.isValid()) {
            return Mono.error(new CustomerValidationException(validation.getValidationErrors()));
        }

        CreateCustomerProfileCommand command = CreateCustomerProfileCommand.builder()
            .customerId(validation.getCustomerId())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .phoneNumber(request.getPhoneNumber())
            .correlationId(request.getCorrelationId())
            .build();

        return commandBus.send(command)
            .doOnSuccess(result -> log.info("Customer profile created: {}", result.getProfileId()))
            .doOnError(error -> log.error("Failed to create customer profile", error));
    }

    /**
     * Step 3: Perform KYC verification using external service
     * This step initiates KYC verification process
     * Compensation: cancelKyc
     */
    @SagaStep(id = "kyc-verification",
              dependsOn = "create-profile",
              compensate = "cancelKyc",
              retry = 2,
              timeoutMs = 60000)
    public Mono<KycResult> performKycVerification(
            @FromStep("create-profile") CustomerProfileResult profile,
            @Input CustomerRegistrationRequest request) {

        log.info("Step 3: Starting KYC verification for {}", profile.getCustomerId());

        StartKycVerificationCommand command = StartKycVerificationCommand.builder()
            .customerId(profile.getCustomerId())
            .profileId(profile.getProfileId())
            .documentType(request.getDocumentType())
            .documentNumber(request.getDocumentNumber())
            .correlationId(request.getCorrelationId())
            .build();

        return commandBus.send(command)
            .doOnSuccess(result -> log.info("KYC verification completed: {} - {}", 
                result.getKycId(), result.isApproved() ? "APPROVED" : "REJECTED"))
            .doOnError(error -> log.error("KYC verification failed", error));
    }

    /**
     * Step 4: Create initial account using CQRS Command
     * This step creates the customer's initial bank account
     * Compensation: closeAccount
     */
    @SagaStep(id = "create-account",
              dependsOn = "kyc-verification",
              compensate = "closeAccount")
    public Mono<AccountCreationResult> createInitialAccount(
            @FromStep("create-profile") CustomerProfileResult profile,
            @FromStep("kyc-verification") KycResult kyc,
            @Input CustomerRegistrationRequest request) {

        log.info("Step 4: Creating account for customer {}", profile.getCustomerId());

        if (!kyc.isApproved()) {
            return Mono.error(new KycRejectionException("KYC verification failed: " + kyc.getRejectionReason()));
        }

        CreateAccountCommand command = CreateAccountCommand.builder()
            .customerId(profile.getCustomerId())
            .accountType("CHECKING")
            .initialDeposit(request.getInitialDeposit())
            .currency("USD")
            .correlationId(request.getCorrelationId())
            .build();

        return commandBus.send(command)
            .doOnSuccess(result -> log.info("Account created: {}", result.getAccountNumber()))
            .doOnError(error -> log.error("Failed to create account", error));
    }

    /**
     * Step 5: Send welcome notification
     * This step sends a welcome email to the customer
     * No compensation needed as this is a notification
     */
    @SagaStep(id = "send-welcome", dependsOn = "create-account")
    public Mono<NotificationResult> sendWelcomeNotification(
            @FromStep("create-profile") CustomerProfileResult profile,
            @FromStep("create-account") AccountCreationResult account) {
        
        log.info("Step 5: Sending welcome notification to customer {}", profile.getCustomerId());
        
        // For this tutorial, we'll simulate sending a notification
        return Mono.just(NotificationResult.builder()
            .notificationId("NOTIF-" + UUID.randomUUID().toString())
            .customerId(profile.getCustomerId())
            .type("WELCOME_EMAIL")
            .status("SENT")
            .sentAt(Instant.now())
            .build())
            .delayElement(Duration.ofMillis(100)) // Simulate processing time
            .doOnSuccess(result -> log.info("Welcome notification sent: {}", result.getNotificationId()));
    }

    // ==================== COMPENSATION METHODS ====================

    /**
     * Compensation for create-profile step
     * Deletes the customer profile if subsequent steps fail
     */
    public Mono<Void> deleteProfile(@FromStep("create-profile") CustomerProfileResult profile) {
        log.warn("COMPENSATING: Deleting customer profile {}", profile.getProfileId());
        
        // In a real implementation, this would call a DeleteCustomerProfileCommand
        // For this tutorial, we'll simulate the compensation
        return Mono.delay(Duration.ofMillis(100))
            .then(Mono.fromRunnable(() -> 
                log.info("Profile {} deleted successfully", profile.getProfileId())))
            .then();
    }

    /**
     * Compensation for kyc-verification step
     * Cancels the KYC verification process
     */
    public Mono<Void> cancelKyc(@FromStep("kyc-verification") KycResult kyc) {
        log.warn("COMPENSATING: Canceling KYC verification {}", kyc.getKycId());
        
        // In a real implementation, this would call a CancelKycVerificationCommand
        // For this tutorial, we'll simulate the compensation
        return Mono.delay(Duration.ofMillis(200))
            .then(Mono.fromRunnable(() -> 
                log.info("KYC verification {} canceled successfully", kyc.getKycId())))
            .then();
    }

    /**
     * Compensation for create-account step
     * Closes the created account
     */
    public Mono<Void> closeAccount(@FromStep("create-account") AccountCreationResult account) {
        log.warn("COMPENSATING: Closing account {}", account.getAccountNumber());
        
        // In a real implementation, this would call a CloseAccountCommand
        // For this tutorial, we'll simulate the compensation
        return Mono.delay(Duration.ofMillis(150))
            .then(Mono.fromRunnable(() -> 
                log.info("Account {} closed successfully", account.getAccountNumber())))
            .then();
    }

    // ==================== EXCEPTION CLASSES ====================

    /**
     * Exception thrown when customer validation fails
     */
    public static class CustomerValidationException extends RuntimeException {
        private final List<String> validationErrors;
        
        public CustomerValidationException(List<String> errors) {
            super("Customer validation failed: " + String.join(", ", errors));
            this.validationErrors = errors;
        }
        
        public List<String> getValidationErrors() {
            return validationErrors;
        }
    }

    /**
     * Exception thrown when KYC verification is rejected
     */
    public static class KycRejectionException extends RuntimeException {
        public KycRejectionException(String message) {
            super(message);
        }
    }
}
```

#### Key Saga Features Explained

**1. Saga Step Configuration:**
- `@SagaStep(id = "step-name")` - Unique identifier for the step
- `dependsOn` - Specifies step dependencies
- `compensate` - Method to call if subsequent steps fail
- `retry` - Number of retry attempts for transient failures
- `timeoutMs` - Maximum execution time for the step
- `backoffMs` - Delay between retry attempts

**2. Step Input Parameters:**
- `@Input` - Receives the original saga input (CustomerRegistrationRequest)
- `@FromStep("step-id")` - Receives output from a previous step
- Multiple `@FromStep` parameters can be used to access results from different steps

**3. Compensation Logic:**
- Compensation methods are called automatically when a step fails
- They receive the output from the step being compensated
- Compensation happens in reverse order (last successful step first)
- No compensation is needed for the last step (welcome notification)

**4. Error Handling:**
- Custom exceptions control saga flow (CustomerValidationException, KycRejectionException)
- Transient errors are automatically retried based on configuration
- Permanent failures trigger compensation of completed steps

### Step 5: Integrating Domain Events

Domain Events provide loose coupling and enable other services to react to saga steps. The Firefly framework automatically publishes step events through the StepEventPublisherBridge.

#### Configure Step Event Publishing

```java
@Configuration
public class DomainEventsConfiguration {
    
    /**
     * Configure the StepEventPublisherBridge to convert saga step events into domain events
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.stepevents", name = "enabled", havingValue = "true", matchIfMissing = true)
    public StepEventPublisherBridge stepEventPublisherBridge(DomainEventPublisher domainEventPublisher) {
        return new StepEventPublisherBridge(domainEventPublisher);
    }
}
```

#### Create Event Listeners

Other services can listen to saga step events:

```java
// Audit Service Event Listener
@Component
@Slf4j
public class CustomerOnboardingAuditListener {
    
    @EventListener
    public void handleCustomerProfileCreated(DomainSpringEvent event) {
        if ("customer.profile.created".equals(event.getType())) {
            CustomerProfileResult profile = (CustomerProfileResult) event.getPayload();
            log.info("AUDIT: Customer profile created - ID: {}, Email: {}", 
                profile.getCustomerId(), profile.getEmail());
            
            // Save audit record
            saveAuditRecord("PROFILE_CREATED", profile.getCustomerId(), event);
        }
    }
    
    @EventListener
    public void handleAccountCreated(DomainSpringEvent event) {
        if ("account.created".equals(event.getType())) {
            AccountCreationResult account = (AccountCreationResult) event.getPayload();
            log.info("AUDIT: Account created - Number: {}, Customer: {}", 
                account.getAccountNumber(), account.getCustomerId());
                
            // Save audit record
            saveAuditRecord("ACCOUNT_CREATED", account.getCustomerId(), event);
        }
    }
    
    @EventListener 
    public void handleSagaStepCompleted(DomainSpringEvent event) {
        if ("saga.step.completed".equals(event.getType())) {
            log.info("AUDIT: Saga step completed - Type: {}, Key: {}", 
                event.getType(), event.getKey());
                
            // Track saga progress
            trackSagaProgress(event);
        }
    }
    
    private void saveAuditRecord(String action, String customerId, DomainSpringEvent event) {
        // Implementation for audit logging
        log.debug("Saving audit record: {} for customer {}", action, customerId);
    }
    
    private void trackSagaProgress(DomainSpringEvent event) {
        // Implementation for saga progress tracking
        log.debug("Tracking saga progress for event: {}", event.getKey());
    }
}

// Analytics Service Event Listener  
@Component
@Slf4j
public class CustomerOnboardingAnalyticsListener {
    
    @EventListener
    public void handleKycCompleted(DomainSpringEvent event) {
        if ("kyc.verification.completed".equals(event.getType())) {
            KycResult kyc = (KycResult) event.getPayload();
            
            // Update analytics metrics
            updateKycMetrics(kyc.isApproved());
            
            if (!kyc.isApproved()) {
                log.warn("ANALYTICS: KYC rejected for customer {}: {}", 
                    kyc.getCustomerId(), kyc.getRejectionReason());
            }
        }
    }
    
    private void updateKycMetrics(boolean approved) {
        // Implementation for updating KYC metrics
        log.debug("Updating KYC metrics: approved={}", approved);
    }
}
```

### Step 6: Adding Configuration

#### Complete Application Configuration

Update your `application.yml` with complete configuration:

```yaml
# Application Configuration
spring:
  application:
    name: banking-app
  
  # Kafka Configuration for Domain Events
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3
      batch-size: 16384
      linger-ms: 5
      buffer-memory: 33554432
    consumer:
      group-id: banking-app
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
  
  # Cache Configuration
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterAccess=15m

# Firefly Configuration
firefly:
  # CQRS Configuration
  cqrs:
    enabled: true
    command:
      timeout: 30s
      metrics-enabled: true
      tracing-enabled: true
    query:
      timeout: 15s
      caching-enabled: true
      cache-ttl: 15m
      metrics-enabled: true
      tracing-enabled: true
  
  # Domain Events Configuration
  events:
    enabled: true
    adapter: kafka  # Use Kafka for production, 'auto' for development
    kafka:
      bootstrap-servers: localhost:9092
      use-messaging-if-available: true
    # Event consumption (optional - for listening to events from other services)
    consumer:
      enabled: false  # Enable if you need to consume events from other services
      type-header: event_type
      key-header: event_key
  
  # StepEvents Configuration (Saga Integration)
  stepevents:
    enabled: true

# Saga Engine Configuration
saga:
  engine:
    # Maximum number of concurrent saga executions
    max-concurrent-executions: 100
    # Default timeout for saga execution
    default-timeout: 300s
    # Enable saga metrics
    metrics-enabled: true

# Domain topic for saga events
domain:
  topic: banking-domain-events


# Logging Configuration
logging:
  level:
    com.firefly: DEBUG
    com.firefly.transactionalengine: INFO
    com.example.banking: DEBUG
    org.apache.kafka: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{correlationId:-}] %logger{36} - %msg%n"

# Management and Monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

#### Environment-Specific Configurations

Create environment-specific configurations:

**application-dev.yml** (Development):
```yaml
firefly:
  events:
    adapter: application_event  # Use in-process events for development
  
logging:
  level:
    com.firefly: DEBUG
    com.example.banking: DEBUG

# Use embedded H2 database for development
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
```

**application-prod.yml** (Production):
```yaml
firefly:
  events:
    adapter: kafka
    kafka:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:kafka-cluster:9092}
      retries: 5

logging:
  level:
    com.firefly: INFO
    com.example.banking: INFO
    org.apache.kafka: WARN

# Production database configuration
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
```

### Step 7: Testing the Complete Solution

#### Integration Test

Create a comprehensive integration test that verifies the entire saga flow:

```java
@SpringBootTest
@TestPropertySource(properties = {
    "firefly.cqrs.enabled=true",
    "firefly.events.enabled=true", 
    "firefly.stepevents.enabled=true",
    "firefly.events.adapter=application_event",
    "logging.level.com.firefly=DEBUG"
})
@Slf4j
class CustomerOnboardingSagaIntegrationTest {

    @Autowired
    private SagaEngine sagaEngine;

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private QueryBus queryBus;

    @Test
    @DisplayName("Should successfully complete customer onboarding saga")
    void shouldCompleteCustomerOnboardingSagaSuccessfully() {
        // Given: A valid customer registration request
        CustomerRegistrationRequest request = createValidCustomerRequest();
        
        // When: Execute the customer registration saga
        StepInputs inputs = StepInputs.of("validate-customer", request);
        Mono<SagaResult> sagaExecution = sagaEngine.execute("customer-registration", inputs);

        // Then: Saga should complete successfully with all steps
        StepVerifier.create(sagaExecution)
            .assertNext(result -> {
                log.info("Saga completed: success={}, steps={}", result.isSuccess(), result.steps().size());
                
                // Verify saga success
                assertThat(result.isSuccess()).isTrue();
                assertThat(result.failedSteps()).isEmpty();
                assertThat(result.steps()).hasSize(5);
                
                // Verify all expected steps were executed
                assertThat(result.steps().keySet()).containsExactlyInAnyOrder(
                    "validate-customer", "create-profile", "kyc-verification", 
                    "create-account", "send-welcome"
                );

                // Verify step results
                verifyStepResults(result, request);
            })
            .expectComplete()
            .verify(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("Should compensate when KYC verification fails")
    void shouldCompensateWhenKycFails() {
        // Given: A customer request that will fail KYC
        CustomerRegistrationRequest request = createKycFailureRequest();
        
        // When: Execute the customer registration saga
        StepInputs inputs = StepInputs.of("validate-customer", request);
        Mono<SagaResult> sagaExecution = sagaEngine.execute("customer-registration", inputs);

        // Then: Saga should fail and compensate completed steps
        StepVerifier.create(sagaExecution)
            .assertNext(result -> {
                log.info("Saga failed as expected: success={}, failedSteps={}, compensatedSteps={}", 
                    result.isSuccess(), result.failedSteps(), result.compensatedSteps());
                
                // Verify saga failure
                assertThat(result.isSuccess()).isFalse();
                assertThat(result.failedSteps()).contains("kyc-verification");
                
                // Verify compensation occurred
                assertThat(result.compensatedSteps()).contains("create-profile");
                
                // Verify partial execution
                assertThat(result.steps().keySet()).contains(
                    "validate-customer", "create-profile", "kyc-verification"
                );
                assertThat(result.steps().keySet()).doesNotContain(
                    "create-account", "send-welcome"
                );
            })
            .expectComplete()
            .verify(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("Should execute individual CQRS operations")
    void shouldExecuteIndividualCqrsOperations() {
        // Test individual command execution
        String customerId = "CUST-INDIVIDUAL-" + UUID.randomUUID();
        
        CreateCustomerProfileCommand command = CreateCustomerProfileCommand.builder()
            .customerId(customerId)
            .firstName("Alice")
            .lastName("Smith") 
            .email("alice.smith@example.com")
            .phoneNumber("+1-555-111-2222")
            .correlationId("CORR-" + UUID.randomUUID())
            .build();

        // Execute command
        StepVerifier.create(commandBus.send(command))
            .assertNext(result -> {
                assertThat(result.getCustomerId()).isEqualTo(customerId);
                assertThat(result.getProfileId()).isNotNull();
                assertThat(result.getStatus()).isEqualTo("CREATED");
            })
            .expectComplete()
            .verify(Duration.ofSeconds(10));

        // Test individual query execution
        GetCustomerProfileQuery query = GetCustomerProfileQuery.builder()
            .customerId(customerId)
            .build();

        StepVerifier.create(queryBus.query(query))
            .assertNext(profile -> {
                assertThat(profile.getCustomerId()).isEqualTo(customerId);
                assertThat(profile.getFirstName()).isEqualTo("John"); // Mock data from handler
                assertThat(profile.getEmail()).isEqualTo("john.doe@example.com");
            })
            .expectComplete()
            .verify(Duration.ofSeconds(5));
    }

    private CustomerRegistrationRequest createValidCustomerRequest() {
        return CustomerRegistrationRequest.builder()
            .customerId("CUST-" + UUID.randomUUID())
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .phoneNumber("+1-555-123-4567")
            .documentType("PASSPORT")
            .documentNumber("P123456789")
            .initialDeposit(new BigDecimal("1000.00"))
            .correlationId("CORR-" + UUID.randomUUID())
            .build();
    }

    private CustomerRegistrationRequest createKycFailureRequest() {
        return CustomerRegistrationRequest.builder()
            .customerId("CUST-FAIL-" + UUID.randomUUID())
            .firstName("Jane")
            .lastName("Doe")
            .email("jane.doe@example.com")
            .phoneNumber("+1-555-987-6543")
            .documentType("INVALID_DOCUMENT")  // This will cause KYC to fail
            .documentNumber("INVALID123")
            .initialDeposit(new BigDecimal("500.00"))
            .correlationId("CORR-FAIL-" + UUID.randomUUID())
            .build();
    }

    private void verifyStepResults(SagaResult result, CustomerRegistrationRequest request) {
        // Verify customer validation
        CustomerValidationResult validation = result.resultOf("validate-customer", CustomerValidationResult.class)
            .orElseThrow(() -> new AssertionError("Validation step result not found"));
        assertThat(validation.isValid()).isTrue();

        // Verify profile creation
        CustomerProfileResult profile = result.resultOf("create-profile", CustomerProfileResult.class)
            .orElseThrow(() -> new AssertionError("Profile creation step result not found"));
        assertThat(profile.getCustomerId()).isEqualTo(request.getCustomerId());
        assertThat(profile.getStatus()).isEqualTo("CREATED");

        // Verify KYC verification
        KycResult kyc = result.resultOf("kyc-verification", KycResult.class)
            .orElseThrow(() -> new AssertionError("KYC step result not found"));
        assertThat(kyc.isApproved()).isTrue();

        // Verify account creation
        AccountCreationResult account = result.resultOf("create-account", AccountCreationResult.class)
            .orElseThrow(() -> new AssertionError("Account creation step result not found"));
        assertThat(account.getAccountNumber()).isNotNull();
        assertThat(account.getInitialBalance()).isEqualTo(request.getInitialDeposit());

        // Verify notification
        NotificationResult notification = result.resultOf("send-welcome", NotificationResult.class)
            .orElseThrow(() -> new AssertionError("Welcome notification step result not found"));
        assertThat(notification.getStatus()).isEqualTo("SENT");
    }
}
```

#### Unit Test Examples

Test individual components:

```java
// Test Command Handler
@ExtendWith(MockitoExtension.class)
class CreateCustomerProfileHandlerTest {

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private CreateCustomerProfileHandler handler;

    @Test
    void shouldCreateCustomerProfileSuccessfully() {
        // Given
        CreateCustomerProfileCommand command = CreateCustomerProfileCommand.builder()
            .customerId("CUST-123")
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .phoneNumber("+1-555-123-4567")
            .correlationId("CORR-123")
            .build();

        when(eventPublisher.publish(any(DomainEventEnvelope.class)))
            .thenReturn(Mono.empty());

        // When
        Mono<CustomerProfileResult> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .assertNext(profile -> {
                assertThat(profile.getCustomerId()).isEqualTo("CUST-123");
                assertThat(profile.getProfileId()).isNotNull();
                assertThat(profile.getStatus()).isEqualTo("CREATED");
            })
            .expectComplete()
            .verify();

        verify(eventPublisher).publish(argThat(event -> 
            event.getType().equals("customer.profile.created")));
    }
}
```

## Best Practices

### 1. Command and Query Design

**Commands:**
- Keep commands immutable and focused on a single business operation
- Include all necessary data to complete the operation
- Implement comprehensive validation logic
- Use meaningful names that express business intent
- Include correlation IDs for tracing

**Queries:**
- Design for specific read scenarios, not generic data access
- Implement proper caching strategies with appropriate TTL
- Include pagination for large result sets
- Use projection patterns to return only needed data
- Consider eventual consistency in distributed scenarios

### 2. Saga Design Patterns

**Step Design:**
- Each step should be idempotent (safe to retry)
- Keep steps focused on a single business operation
- Design compensation logic for every step that changes state
- Use meaningful step IDs that reflect business operations
- Include appropriate timeouts and retry configurations

**Error Handling:**
- Distinguish between transient and permanent failures
- Use custom exceptions to control saga flow
- Log detailed information for troubleshooting
- Implement circuit breaker patterns for external service calls
- Design compensations to be idempotent

**State Management:**
- Pass only necessary data between steps
- Use immutable data structures for step results
- Avoid storing large payloads in saga state
- Consider data privacy in saga step results

### 3. Domain Events Best Practices

**Event Design:**
- Events should represent business facts, not technical operations
- Use past tense names (e.g., "CustomerCreated", not "CreateCustomer")
- Include all necessary context for event consumers
- Version events for backward compatibility
- Keep event payloads focused and lightweight

**Event Publishing:**
- Publish events after successful state changes
- Use reliable messaging patterns (at-least-once delivery)
- Include metadata for routing and filtering
- Consider event ordering requirements
- Implement proper error handling for event publishing

### 4. Performance Considerations

**CQRS Performance:**
- Use query-specific read models optimized for access patterns
- Implement proper caching strategies at multiple levels
- Consider async query processing for non-critical operations
- Monitor and optimize command processing times
- Use connection pooling for database operations

**Saga Performance:**
- Minimize the number of saga steps
- Use parallel execution where possible
- Implement proper timeout configurations
- Monitor saga execution times and success rates
- Consider saga complexity vs. maintainability trade-offs

### 5. Monitoring and Observability

**Metrics to Track:**
- Command/Query execution times and success rates
- Saga step execution times and failure rates
- Compensation execution frequency and success
- Event publishing and consumption metrics
- Cache hit/miss ratios

**Logging Strategy:**
- Include correlation IDs in all log messages
- Log saga step boundaries and results
- Log compensation executions with clear indicators
- Include business context in error messages
- Use structured logging for better searchability

## Troubleshooting

### Common Issues and Solutions

#### 1. Saga Step Timeouts

**Problem:** Steps timeout frequently causing unnecessary compensations.

**Solutions:**
- Increase timeout values for long-running operations
- Break down complex steps into smaller, faster operations
- Implement proper retry logic for transient failures
- Use async processing for operations that don't need immediate results

```yaml
# Increase step timeout
@SagaStep(id = "external-api-call", timeoutMs = 60000, retry = 3)
```

#### 2. Compensation Failures

**Problem:** Compensation steps are failing, leaving the system in an inconsistent state.

**Solutions:**
- Make compensation operations idempotent
- Implement dead letter queues for failed compensations
- Add manual intervention capabilities for stuck compensations
- Log detailed compensation failure information

```java
public Mono<Void> compensateAccountCreation(@FromStep("create-account") AccountResult account) {
    return accountService.deleteAccount(account.getAccountId())
        .onErrorResume(error -> {
            log.error("Compensation failed for account {}: {}", account.getAccountId(), error.getMessage());
            // Send to dead letter queue or alert administrators
            return sendToDeadLetterQueue("account-compensation-failed", account, error);
        });
}
```

#### 3. Event Publishing Failures

**Problem:** Domain events fail to publish, breaking downstream integrations.

**Solutions:**
- Implement retry logic with exponential backoff
- Use transactional outbox pattern for guaranteed delivery
- Monitor event publishing failures and set up alerts
- Implement circuit breakers for external messaging systems

#### 4. Command Validation Issues

**Problem:** Commands pass validation but fail during processing.

**Solutions:**
- Implement both syntactic and semantic validation
- Use async validation for external service dependencies
- Provide detailed validation error messages
- Consider validation caching for expensive checks

#### 5. Query Performance Problems

**Problem:** Queries are slow and impacting user experience.

**Solutions:**
- Implement proper database indexes
- Use query-specific read models
- Add caching at multiple levels
- Consider async query processing
- Monitor slow queries and optimize accordingly

### Debugging Tips

#### 1. Enable Debug Logging

```yaml
logging:
  level:
    com.firefly: DEBUG
    com.firefly.transactionalengine: DEBUG
    com.example.banking: DEBUG
```

#### 2. Use Correlation IDs

Ensure all operations include correlation IDs for end-to-end tracing:

```java
@Override
public String getCorrelationId() {
    return MDC.get("correlationId") != null ? MDC.get("correlationId") : UUID.randomUUID().toString();
}
```

#### 3. Monitor Saga State

Implement saga state monitoring and visualization:

```java
@EventListener
public void handleSagaStepCompleted(DomainSpringEvent event) {
    if ("saga.step.completed".equals(event.getType())) {
        sagaMonitor.updateStepStatus(event.getKey(), event.getStepId(), "COMPLETED");
    }
}
```

## Advanced Topics

### 1. Saga Choreography vs Orchestration

The tutorial demonstrates **orchestration** where a central saga coordinates all steps. For comparison, **choreography** uses events to coordinate between services without a central coordinator.

**When to use Orchestration (Current approach):**
- Complex business workflows with clear step dependencies
- Need for centralized monitoring and control
- Clear compensation requirements
- Strong consistency requirements

**When to use Choreography:**
- Loosely coupled services
- Simple workflows
- High autonomy requirements
- Event-driven architectures

### 2. Saga State Persistence

For production deployments, consider persisting saga state:

```java
@Configuration
public class SagaConfiguration {
    
    @Bean
    public SagaStateRepository sagaStateRepository() {
        return new DatabaseSagaStateRepository(); // Custom implementation
    }
}
```

### 3. Cross-Service Saga Coordination

For sagas spanning multiple microservices:

```java
@SagaStep(id = "external-service-call")
public Mono<ExternalResult> callExternalService(@Input RequestData request) {
    return serviceClient.post("/external-api", ExternalResult.class)
        .withBody(request)
        .withTimeout(Duration.ofSeconds(30))
        .execute();
}
```

### 4. Saga Testing Strategies

**Integration Testing:**
- Test complete saga flows with real dependencies
- Test compensation scenarios
- Test timeout and retry behaviors

**Contract Testing:**
- Define contracts for saga step inputs/outputs
- Use consumer-driven contract testing
- Validate event schemas

**Performance Testing:**
- Test saga execution under load
- Measure step execution times
- Test concurrent saga execution

### 5. Event Sourcing Integration

Combine with Event Sourcing for complete audit trails:

```java
@EventListener
public void handleDomainEvent(DomainSpringEvent event) {
    EventStore eventStore = eventStoreRegistry.getEventStore(event.getTopic());
    eventStore.append(event.getKey(), event);
}
```

## Conclusion

This tutorial has walked you through building a complete customer onboarding system using CQRS and Saga patterns with the Firefly Common Domain Library. You've learned:

- How to design and implement CQRS commands and queries
- How to create saga orchestrators with compensation logic
- How to integrate domain events for loose coupling
- How to test and monitor the complete solution
- Best practices for production deployments

The combination of CQRS and Saga patterns provides a powerful foundation for building resilient, scalable microservices that maintain data consistency across distributed transactions.

### Next Steps

1. **Extend the Example:** Add more complex business rules and additional saga steps
2. **Production Deployment:** Configure for your specific infrastructure requirements
3. **Monitoring:** Implement comprehensive monitoring and alerting
4. **Performance Optimization:** Profile and optimize based on your traffic patterns
5. **Security:** Add authentication, authorization, and data encryption

### Additional Resources

- [Firefly Common Domain Library Documentation](./README.md)
- [CQRS Framework Reference](./CQRS.md)
- [Domain Events Guide](./DOMAIN_EVENTS.md)
- [Configuration Reference](./CONFIGURATION.md)
- [Service Client Guide](./NEW_SERVICE_CLIENT_GUIDE.md)

---

**Happy coding with CQRS and Sagas!** 🚀
