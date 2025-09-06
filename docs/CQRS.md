# CQRS Framework Documentation

This document provides comprehensive documentation for the Command Query Responsibility Segregation (CQRS) framework implementation in the Firefly Common Domain Library.

## Table of Contents

- [Overview](#overview)
- [Core Concepts](#core-concepts)
- [Commands](#commands)
- [Queries](#queries)
- [Command Handlers](#command-handlers)
- [Query Handlers](#query-handlers)
- [Command Bus](#command-bus)
- [Query Bus](#query-bus)
- [lib-transactional-engine Integration](#lib-transactional-engine-integration)
- [Configuration](#configuration)
- [Banking Examples](#banking-examples)
- [Best Practices](#best-practices)

## Overview

The CQRS framework provides a clean separation between command (write) and query (read) operations, enabling:

- **Scalability**: Independent scaling of read and write operations
- **Performance**: Optimized data models for different access patterns
- **Maintainability**: Clear separation of concerns
- **Flexibility**: Different storage mechanisms for reads and writes

### Key Features

- **Reactive Programming**: Built on Project Reactor for non-blocking operations
- **Automatic Handler Discovery**: Zero-configuration handler registration
- **Validation Framework**: Built-in command validation with async support
- **Correlation Context**: Distributed tracing across operations
- **Caching Support**: Query result caching with configurable TTL

## Core Concepts

### Command Query Separation

```java
// Commands - Write Operations (State Changes)
public interface Command<R> {
    default String getCommandId() { return UUID.randomUUID().toString(); }
    default Instant getTimestamp() { return Instant.now(); }
    default String getCorrelationId() { return null; }
    default String getInitiatedBy() { return null; }
    default Map<String, Object> getMetadata() { return null; }
    default Class<R> getResultType() { return (Class<R>) Object.class; }
    default Mono<ValidationResult> validate() { return Mono.just(ValidationResult.success()); }
}

// Queries - Read Operations (Data Retrieval)
public interface Query<R> {
    default String getQueryId() { return UUID.randomUUID().toString(); }
    default Instant getTimestamp() { return Instant.now(); }
    default String getCorrelationId() { return null; }
    default String getInitiatedBy() { return null; }
    default Map<String, Object> getMetadata() { return null; }
    default Class<R> getResultType() { return (Class<R>) Object.class; }
    default boolean isCacheable() { return true; }
    default String getCacheKey() { /* implementation */ }
}
```

### Processing Flow

```
Command Flow:
Client → CommandBus → Validation → CommandHandler → Result/Events

Query Flow:
Client → QueryBus → Cache Check → QueryHandler → Cache Store → Result
```

## Commands

Commands represent intentions to change state and encapsulate all data needed for the operation.

### Command Interface

````java
public interface Command<R> {

    default String getCommandId() {
        return UUID.randomUUID().toString();
    }

    default Instant getTimestamp() {
        return Instant.now();
    }

    default String getCorrelationId() {
        return null;
    }

    default String getInitiatedBy() {
        return null;
    }

    default Map<String, Object> getMetadata() {
        return null;
    }

    default Class<R> getResultType() {
        return (Class<R>) Object.class;
    }

    default Mono<ValidationResult> validate() {
        return Mono.just(ValidationResult.success());
    }
}
````
</augment_code_snippet>

### Command Implementation Examples

#### Basic Command Implementation

```java
@Data
@Builder
public class CreateAccountCommand implements Command<AccountResult> {

    private final String customerId;
    private final String accountType;
    private final BigDecimal initialDeposit;
    private final String correlationId;
    private final String sagaId; // For saga integration

    @Override
    public Mono<ValidationResult> validate() {
        ValidationResult.Builder builder = ValidationResult.builder();

        // Field validation
        if (customerId == null || customerId.trim().isEmpty()) {
            builder.addError("customerId", "Customer ID is required");
        }

        if (accountType == null || !isValidAccountType(accountType)) {
            builder.addError("accountType", "Valid account type is required");
        }

        if (initialDeposit != null && initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            builder.addError("initialDeposit", "Initial deposit cannot be negative");
        }

        return Mono.just(builder.build());
    }

    private boolean isValidAccountType(String type) {
        return List.of("CHECKING", "SAVINGS", "BUSINESS").contains(type);
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("customerId", customerId);
        metadata.put("accountType", accountType);
        if (sagaId != null) {
            metadata.put("sagaId", sagaId);
        }
        return metadata;
    }
}
```

#### Complex Command with Async Validation

```java
@Data
@Builder
public class ProcessLoanApplicationCommand implements Command<LoanApplicationResult> {

    private final String customerId;
    private final BigDecimal requestedAmount;
    private final String loanType;
    private final Integer termMonths;
    private final String correlationId;
    private final String sagaId;

    @Override
    public Mono<ValidationResult> validate() {
        // Synchronous validation first
        ValidationResult.Builder builder = ValidationResult.builder();

        if (customerId == null || customerId.trim().isEmpty()) {
            builder.addError("customerId", "Customer ID is required");
        }

        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            builder.addError("requestedAmount", "Loan amount must be positive");
        }

        if (requestedAmount != null && requestedAmount.compareTo(new BigDecimal("1000000")) > 0) {
            builder.addError("requestedAmount", "Loan amount exceeds maximum limit");
        }

        if (termMonths == null || termMonths < 12 || termMonths > 360) {
            builder.addError("termMonths", "Loan term must be between 12 and 360 months");
        }

        ValidationResult syncResult = builder.build();
        if (!syncResult.isValid()) {
            return Mono.just(syncResult);
        }

        // Return successful validation - async validation can be done in handler
        return Mono.just(ValidationResult.success());
    }

    // Async validation method for use in handlers
    public Mono<ValidationResult> validateAsync(CustomerService customerService,
                                              CreditService creditService) {
        return customerService.exists(customerId)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.just(ValidationResult.failure("customerId", "Customer not found"));
                }
                return creditService.checkCreditScore(customerId);
            })
            .map(creditScore -> {
                if (creditScore < 600) {
                    return ValidationResult.failure("creditScore", "Insufficient credit score");
                }
                return ValidationResult.success();
            });
    }
}
```

### Command Metadata and Context

Commands support rich metadata for auditing and tracing. All these methods are already included in the base Command interface:

```java
// Correlation tracking
String correlationId = command.getCorrelationId();

// Audit information
String initiator = command.getInitiatedBy();
Instant timestamp = command.getTimestamp();

// Additional metadata
Map<String, Object> metadata = command.getMetadata();

// Result type information
Class<R> resultType = command.getResultType();
```

## Queries

Queries represent requests for data and should be idempotent read operations.

### Query Interface

````java
public interface Query<R> {

    default String getQueryId() {
        return UUID.randomUUID().toString();
    }

    default Instant getTimestamp() {
        return Instant.now();
    }

    default String getCorrelationId() {
        return null;
    }

    default String getInitiatedBy() {
        return null;
    }

    default Map<String, Object> getMetadata() {
        return null;
    }

    default Class<R> getResultType() {
        return (Class<R>) Object.class;
    }

    default boolean isCacheable() {
        return true;
    }

    default String getCacheKey() {
        if (!isCacheable()) {
            return null;
        }

        String baseKey = this.getClass().getSimpleName();
        Map<String, Object> metadata = getMetadata();

        if (metadata != null && !metadata.isEmpty()) {
            return baseKey + "_" + metadata.hashCode();
        }

        return baseKey;
    }
}
````
</augment_code_snippet>

### Query Implementation Examples

#### Basic Query Implementation

```java
@Data
@Builder
public class GetAccountBalanceQuery implements Query<AccountBalance> {

    private final String accountNumber;
    private final String correlationId;
    private final boolean includeHolds;
    private final String sagaId; // For saga integration
    private final String sagaName; // For saga integration

    @Override
    public boolean isCacheable() {
        return true; // Balance queries can be cached
    }

    @Override
    public String getCacheKey() {
        return String.format("account_balance_%s_%s", accountNumber, includeHolds);
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("accountNumber", accountNumber);
        metadata.put("includeHolds", includeHolds);
        if (sagaId != null) {
            metadata.put("sagaId", sagaId);
            metadata.put("sagaName", sagaName);
        }
        return metadata;
    }

    // Saga integration helpers
    public String getSagaId() {
        return sagaId;
    }

    public String getSagaName() {
        return sagaName;
    }
}
```

#### Complex Query with Pagination

```java
@Data
@Builder
public class GetTransactionHistoryQuery implements Query<TransactionHistory> {

    private final String accountNumber;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final String transactionType; // DEPOSIT, WITHDRAWAL, TRANSFER
    private final int pageSize;
    private final int pageNumber;
    private final String sortBy; // date, amount, type
    private final String sortDirection; // ASC, DESC
    private final String correlationId;

    @Override
    public boolean isCacheable() {
        // Cache recent queries but not real-time data
        return fromDate != null && fromDate.isBefore(LocalDate.now().minusDays(1));
    }

    @Override
    public String getCacheKey() {
        return String.format("transaction_history_%s_%s_%s_%s_%d_%d_%s_%s",
            accountNumber, fromDate, toDate, transactionType,
            pageSize, pageNumber, sortBy, sortDirection);
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("accountNumber", accountNumber);
        metadata.put("fromDate", fromDate);
        metadata.put("toDate", toDate);
        metadata.put("transactionType", transactionType);
        metadata.put("pageSize", pageSize);
        metadata.put("pageNumber", pageNumber);
        metadata.put("sortBy", sortBy);
        metadata.put("sortDirection", sortDirection);
        return metadata;
    }

    // Validation helper
    public boolean isValid() {
        return accountNumber != null && !accountNumber.trim().isEmpty()
            && pageSize > 0 && pageSize <= 100
            && pageNumber >= 0
            && (fromDate == null || toDate == null || !fromDate.isAfter(toDate));
    }
}
```

#### Non-Cacheable Query Example

```java
@Data
@Builder
public class GetRealTimeAccountStatusQuery implements Query<AccountStatus> {

    private final String accountNumber;
    private final String correlationId;

    @Override
    public boolean isCacheable() {
        return false; // Real-time data should not be cached
    }

    @Override
    public String getCacheKey() {
        return null; // No caching
    }

    @Override
    public Map<String, Object> getMetadata() {
        return Map.of(
            "accountNumber", accountNumber,
            "realTime", true
        );
    }
}
```

## Command Handlers

Command handlers contain the business logic for processing commands.

### CommandHandler Interface

````java
public interface CommandHandler<C extends Command<R>, R> {
    
    Mono<R> handle(C command);
    
    Class<C> getCommandType();
    
    default Class<R> getResultType() {
        return null;
    }
    
    default boolean canHandle(Command<?> command) {
        return getCommandType().isInstance(command);
    }
}
````
</augment_code_snippet>

### Command Handler Implementation

```java
@Component
@Slf4j
public class CreateAccountHandler implements CommandHandler<CreateAccountCommand, AccountResult> {
    
    private final ServiceClient accountServiceClient;
    private final ServiceClient customerServiceClient;
    private final DomainEventPublisher eventPublisher;
    
    @Override
    public Mono<AccountResult> handle(CreateAccountCommand command) {
        log.info("Creating account for customer: {}", command.getCustomerId());
        
        return command.validate()
            .flatMap(validation -> {
                if (!validation.isValid()) {
                    return Mono.error(new ValidationException(validation));
                }
                return createAccount(command);
            })
            .flatMap(this::publishAccountCreatedEvent)
            .doOnSuccess(result -> log.info("Account created: {}", result.getAccountId()))
            .doOnError(error -> log.error("Failed to create account", error));
    }
    
    private Mono<AccountResult> createAccount(CreateAccountCommand command) {
        CreateAccountRequest request = CreateAccountRequest.builder()
            .customerId(command.getCustomerId())
            .accountType(command.getAccountType())
            .initialDeposit(command.getInitialDeposit() != null ? command.getInitialDeposit() : BigDecimal.ZERO)
            .currency("USD")
            .build();
            
        return accountServiceClient.post("/accounts", AccountResult.class)
            .withBody(request)
            .execute();
    }
    
    private Mono<AccountResult> publishAccountCreatedEvent(AccountResult result) {
        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("banking.accounts")
            .type("account.created")
            .key(result.getAccountId())
            .payload(result)
            .timestamp(Instant.now())
            .build();
            
        return eventPublisher.publish(event)
            .thenReturn(result);
    }
    
    @Override
    public Class<CreateAccountCommand> getCommandType() {
        return CreateAccountCommand.class;
    }
}
```

## Query Handlers

Query handlers process queries and return data, with optional caching support.

### QueryHandler Interface

````java
public interface QueryHandler<Q extends Query<R>, R> {

    Mono<R> handle(Q query);

    Class<Q> getQueryType();

    default Class<R> getResultType() {
        return null; // Can be overridden for explicit type information
    }

    default boolean canHandle(Query<?> query) {
        return getQueryType().isInstance(query);
    }

    default boolean supportsCaching() {
        return true;
    }

    default Long getCacheTtlSeconds() {
        return null; // Use system default
    }
}
````
</augment_code_snippet>

### Query Handler Implementation

```java
@Component
@Slf4j
public class GetAccountBalanceHandler implements QueryHandler<GetAccountBalanceQuery, AccountBalance> {
    
    private final SdkServiceClient<CoreBankingSDK> coreBankingClient;
    private final RestServiceClient cacheServiceClient;
    
    @Override
    @Cacheable(value = "account-balances", key = "#query.cacheKey")
    public Mono<AccountBalance> handle(GetAccountBalanceQuery query) {
        log.debug("Retrieving balance for account: {}", query.getAccountNumber());
        
        return coreBankingClient.execute(sdk -> 
            sdk.accounts().getBalance(query.getAccountNumber(), query.isIncludeHolds()))
            .map(this::mapToAccountBalance)
            .doOnSuccess(balance -> log.debug("Retrieved balance: {} for account: {}", 
                balance.getAvailableBalance(), query.getAccountNumber()));
    }
    
    private AccountBalance mapToAccountBalance(CoreBankingBalance coreBalance) {
        return AccountBalance.builder()
            .accountNumber(coreBalance.getAccountNumber())
            .currentBalance(coreBalance.getCurrentBalance())
            .availableBalance(coreBalance.getAvailableBalance())
            .currency(coreBalance.getCurrency())
            .lastUpdated(coreBalance.getLastUpdated())
            .build();
    }
    
    @Override
    public Class<GetAccountBalanceQuery> getQueryType() {
        return GetAccountBalanceQuery.class;
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

## Command Bus

The CommandBus routes commands to their appropriate handlers and manages the execution lifecycle.

### CommandBus Interface

```java
public interface CommandBus {
    <R> Mono<R> send(Command<R> command);
    <C extends Command<R>, R> void registerHandler(CommandHandler<C, R> handler);
    <C extends Command<?>> void unregisterHandler(Class<C> commandType);
    boolean hasHandler(Class<? extends Command<?>> commandType);
}
```

### Usage Examples

```java
@Service
public class AccountService {
    
    private final CommandBus commandBus;
    
    public Mono<AccountResult> createAccount(CreateAccountRequest request) {
        CreateAccountCommand command = CreateAccountCommand.builder()
            .customerId(request.getCustomerId())
            .accountType(request.getAccountType())
            .initialDeposit(request.getInitialDeposit())
            .correlationId(CorrelationContext.current().getCorrelationId())
            .build();
            
        return commandBus.send(command);
    }
}
```

## Query Bus

The QueryBus routes queries to handlers and manages caching.

### QueryBus Interface

```java
public interface QueryBus {
    <R> Mono<R> query(Query<R> query);
    <Q extends Query<R>, R> void registerHandler(QueryHandler<Q, R> handler);
    <Q extends Query<?>> void unregisterHandler(Class<Q> queryType);
    boolean hasHandler(Class<? extends Query<?>> queryType);
    Mono<Void> clearCache(String cacheKey);
    Mono<Void> clearAllCache();
}
```

### Usage Examples

```java
@Service
public class AccountQueryService {
    
    private final QueryBus queryBus;
    
    public Mono<AccountBalance> getAccountBalance(String accountNumber) {
        GetAccountBalanceQuery query = GetAccountBalanceQuery.builder()
            .accountNumber(accountNumber)
            .includeHolds(true)
            .correlationId(CorrelationContext.current().getCorrelationId())
            .build();
            
        return queryBus.query(query);
    }
}
```

## lib-transactional-engine Integration

The CQRS framework provides seamless integration with lib-transactional-engine for complex saga orchestration. Commands and Queries are executed within saga steps, providing a powerful combination of CQRS patterns with distributed transaction management.

### Integration Architecture

```
Saga Engine
    ↓
@SagaStep Methods ←→ CommandBus/QueryBus ←→ Command/Query Handlers
    ↓                        ↓                        ↓
Step Inputs/Outputs    Business Logic         StepEventPublisherBridge
    ↓                        ↓                        ↓
Compensation Logic     Domain Services        Domain Events Infrastructure
```

### Complete Customer Registration Saga

This example demonstrates a complex saga that combines CQRS commands and queries with saga orchestration:

```java
@Component
@Saga(name = "customer-registration")
@EnableTransactionalEngine
public class CustomerRegistrationSaga {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    // Step 1: Validate customer data using CQRS Query
    @SagaStep(id = "validate-customer", retry = 3, backoffMs = 1000)
    public Mono<CustomerValidationResult> validateCustomer(@Input CustomerRegistrationRequest request) {
        ValidateCustomerQuery query = ValidateCustomerQuery.builder()
            .email(request.getEmail())
            .phoneNumber(request.getPhoneNumber())
            .correlationId(request.getCorrelationId())
            .build();

        return queryBus.query(query)
            .map(validation -> CustomerValidationResult.builder()
                .customerId(request.getCustomerId())
                .isValid(validation.isValid())
                .validationErrors(validation.getErrors())
                .build());
    }

    // Step 2: Create customer profile using CQRS Command
    @SagaStep(id = "create-profile",
              dependsOn = "validate-customer",
              compensate = "deleteProfile",
              timeoutMs = 30000)
    public Mono<CustomerProfileResult> createProfile(
            @Input CustomerRegistrationRequest request,
            @FromStep("validate-customer") CustomerValidationResult validation) {

        if (!validation.isValid()) {
            return Mono.error(new CustomerValidationException(validation.getValidationErrors()));
        }

        CreateCustomerProfileCommand command = CreateCustomerProfileCommand.builder()
            .customerId(request.getCustomerId())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .phoneNumber(request.getPhoneNumber())
            .correlationId(request.getCorrelationId())
            .build();

        return commandBus.send(command)
            .map(result -> CustomerProfileResult.builder()
                .customerId(result.getCustomerId())
                .profileId(result.getProfileId())
                .status("CREATED")
                .build());
    }

    // Step 3: Perform KYC verification using external service
    @SagaStep(id = "kyc-verification",
              dependsOn = "create-profile",
              compensate = "cancelKyc",
              retry = 2,
              timeoutMs = 60000)
    public Mono<KycResult> performKycVerification(
            @Input CustomerRegistrationRequest request,
            @FromStep("create-profile") CustomerProfileResult profile) {

        StartKycVerificationCommand command = StartKycVerificationCommand.builder()
            .customerId(profile.getCustomerId())
            .profileId(profile.getProfileId())
            .documentType(request.getDocumentType())
            .documentNumber(request.getDocumentNumber())
            .correlationId(request.getCorrelationId())
            .build();

        return commandBus.send(command);
    }

    // Step 4: Create initial account using CQRS Command
    @SagaStep(id = "create-account",
              dependsOn = "kyc-verification",
              compensate = "closeAccount")
    public Mono<AccountCreationResult> createInitialAccount(
            @Input CustomerRegistrationRequest request,
            @FromStep("create-profile") CustomerProfileResult profile,
            @FromStep("kyc-verification") KycResult kyc) {

        if (!kyc.isApproved()) {
            return Mono.error(new KycRejectionException("KYC verification failed"));
        }

        CreateAccountCommand command = CreateAccountCommand.builder()
            .customerId(profile.getCustomerId())
            .accountType("CHECKING")
            .initialDeposit(request.getInitialDeposit())
            .currency("USD")
            .correlationId(request.getCorrelationId())
            .build();

        return commandBus.send(command);
    }

    // Compensation Methods
    public Mono<Void> deleteProfile(@FromStep("create-profile") CustomerProfileResult profile) {
        DeleteCustomerProfileCommand command = DeleteCustomerProfileCommand.builder()
            .customerId(profile.getCustomerId())
            .profileId(profile.getProfileId())
            .build();
        return commandBus.send(command).then();
    }

    public Mono<Void> cancelKyc(@FromStep("kyc-verification") KycResult kyc) {
        CancelKycVerificationCommand command = CancelKycVerificationCommand.builder()
            .kycId(kyc.getKycId())
            .build();
        return commandBus.send(command).then();
    }

    public Mono<Void> closeAccount(@FromStep("create-account") AccountCreationResult account) {
        CloseAccountCommand command = CloseAccountCommand.builder()
            .accountId(account.getAccountId())
            .reason("SAGA_COMPENSATION")
            .build();
        return commandBus.send(command).then();
    }
}
```

### Saga Orchestration Service

```java
@Service
public class CustomerRegistrationService {

    private final SagaEngine sagaEngine;
    private final CommandBus commandBus;
    private final QueryBus queryBus;

    /**
     * Orchestrates the complete customer registration process using saga pattern
     */
    public Mono<CustomerRegistrationResult> registerCustomer(CustomerRegistrationRequest request) {
        // Create saga inputs
        StepInputs inputs = StepInputs.of("validate-customer", request);

        // Execute the saga
        return sagaEngine.execute("customer-registration", inputs)
            .map(this::buildRegistrationResult)
            .doOnSuccess(result -> logSuccessfulRegistration(result))
            .doOnError(error -> logFailedRegistration(request, error));
    }

    /**
     * Alternative: Execute saga by class reference (type-safe)
     */
    public Mono<CustomerRegistrationResult> registerCustomerTypeSafe(CustomerRegistrationRequest request) {
        StepInputs inputs = StepInputs.of("validate-customer", request);

        return sagaEngine.execute(CustomerRegistrationSaga.class, inputs)
            .map(this::buildRegistrationResult);
    }

    /**
     * Query customer registration status using CQRS Query
     */
    public Mono<RegistrationStatus> getRegistrationStatus(String customerId) {
        GetCustomerRegistrationStatusQuery query = GetCustomerRegistrationStatusQuery.builder()
            .customerId(customerId)
            .build();

        return queryBus.query(query);
    }

    private CustomerRegistrationResult buildRegistrationResult(SagaResult sagaResult) {
        if (sagaResult.isSuccess()) {
            // Extract results from completed steps
            CustomerProfileResult profile = sagaResult.resultOf("create-profile", CustomerProfileResult.class)
                .orElseThrow(() -> new IllegalStateException("Profile creation step not found"));

            AccountCreationResult account = sagaResult.resultOf("create-account", AccountCreationResult.class)
                .orElseThrow(() -> new IllegalStateException("Account creation step not found"));

            return CustomerRegistrationResult.builder()
                .customerId(profile.getCustomerId())
                .profileId(profile.getProfileId())
                .accountId(account.getAccountId())
                .accountNumber(account.getAccountNumber())
                .status("COMPLETED")
                .registrationDate(Instant.now())
                .build();
        } else {
            // Handle saga failure
            List<String> failedSteps = sagaResult.failedSteps();
            List<String> compensatedSteps = sagaResult.compensatedSteps();

            return CustomerRegistrationResult.builder()
                .status("FAILED")
                .failedSteps(failedSteps)
                .compensatedSteps(compensatedSteps)
                .errorMessage("Registration failed at steps: " + String.join(", ", failedSteps))
                .build();
        }
    }
}
```

### Command Handlers within Saga Steps

Commands executed within saga steps automatically benefit from saga context and compensation:

```java
@Component
public class CreateCustomerProfileHandler implements CommandHandler<CreateCustomerProfileCommand, CustomerProfileResult> {

    private final CustomerProfileService profileService;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Mono<CustomerProfileResult> handle(CreateCustomerProfileCommand command) {
        return profileService.createProfile(command)
            .flatMap(this::publishDomainEvent)
            .doOnSuccess(result -> log.info("Customer profile created: {}", result.getProfileId()))
            .doOnError(error -> log.error("Failed to create customer profile: {}", command.getCustomerId(), error));
    }

    private Mono<CustomerProfileResult> publishDomainEvent(CustomerProfileResult result) {
        CustomerProfileCreatedEvent event = CustomerProfileCreatedEvent.builder()
            .customerId(result.getCustomerId())
            .profileId(result.getProfileId())
            .email(result.getEmail())
            .createdAt(Instant.now())
            .build();

        return eventPublisher.publish(event)
            .thenReturn(result);
    }

    @Override
    public Class<CreateCustomerProfileCommand> getCommandType() {
        return CreateCustomerProfileCommand.class;
    }
}
```

### Query Handlers within Saga Steps

Queries can be used within saga steps for validation and data retrieval:

```java
@Component
public class ValidateCustomerHandler implements QueryHandler<ValidateCustomerQuery, CustomerValidationResult> {

    private final CustomerValidationService validationService;

    @Override
    public Mono<CustomerValidationResult> handle(ValidateCustomerQuery query) {
        return validationService.validateCustomer(query)
            .doOnSuccess(result -> log.info("Customer validation completed: valid={}", result.isValid()))
            .doOnError(error -> log.error("Customer validation failed: {}", query.getEmail(), error));
    }

    @Override
    public Class<ValidateCustomerQuery> getQueryType() {
        return ValidateCustomerQuery.class;
    }

    @Override
    public boolean supportsCaching() {
        return true; // Cache validation results
    }

    @Override
    public Long getCacheTtlSeconds() {
        return 300L; // 5 minutes
    }
}
```

## Configuration

### Enable CQRS Framework

```yaml
firefly:
  cqrs:
    enabled: true
    query:
      cache:
        enabled: true
        default-ttl: 300  # 5 minutes
```

### Auto-Configuration

The CQRS framework is automatically configured through Spring Boot's auto-configuration mechanism:

```java
@SpringBootApplication
public class BankingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingServiceApplication.class, args);
    }
    // CQRS components (CommandBus, QueryBus) are automatically configured
}
```

The `CqrsAutoConfiguration` class automatically provides:
- `CommandBus` implementation (`DefaultCommandBus`)
- `QueryBus` implementation (`DefaultQueryBus`)
- `CacheManager` for query caching

## Banking Examples

### Customer Onboarding Workflow

```java
// Command: Start customer onboarding
@Component
public class StartCustomerOnboardingHandler 
        implements CommandHandler<StartCustomerOnboardingCommand, OnboardingResult> {
    
    @Override
    public Mono<OnboardingResult> handle(StartCustomerOnboardingCommand command) {
        return validateCustomerData(command)
            .flatMap(this::createCustomerProfile)
            .flatMap(this::initiateKycProcess)
            .flatMap(this::publishOnboardingStartedEvent);
    }
}

// Query: Get onboarding status
@Component
public class GetOnboardingStatusHandler 
        implements QueryHandler<GetOnboardingStatusQuery, OnboardingStatus> {
    
    @Override
    @Cacheable("onboarding-status")
    public Mono<OnboardingStatus> handle(GetOnboardingStatusQuery query) {
        return onboardingService.getStatus(query.getCustomerId());
    }
}
```

### Loan Application Processing

```java
// Event-driven loan application processing
@Component
public class ProcessLoanApplicationHandler
        implements CommandHandler<ProcessLoanApplicationCommand, LoanResult> {

    @Override
    public Mono<LoanResult> handle(ProcessLoanApplicationCommand command) {
        // Process loan application and publish events for downstream processing
        return processLoanApplication(command)
            .flatMap(this::publishLoanEvents)
            .map(this::buildLoanResult);
    }
}
```

## Best Practices

### Command Design Patterns

1. **Immutable Commands**: Always design commands as immutable data structures
2. **Rich Validation**: Include both synchronous and asynchronous validation
3. **Correlation Context**: Always include correlation IDs for tracing
4. **Saga Integration**: Include saga metadata when commands are part of workflows
5. **Meaningful Names**: Use descriptive command names that reflect business intent

```java
// Good: Descriptive and business-focused
public class ProcessLoanApplicationCommand implements Command<LoanApplicationResult> { }

// Avoid: Technical or generic names
public class UpdateDataCommand implements Command<GenericResult> { }
```

### Query Design Patterns

1. **Caching Strategy**: Carefully consider what should and shouldn't be cached
2. **Pagination**: Always include pagination for potentially large result sets
3. **Real-time vs Cached**: Distinguish between real-time and cacheable queries
4. **Metadata Rich**: Include comprehensive metadata for filtering and sorting

```java
// Good: Specific query with clear caching strategy
@Override
public boolean isCacheable() {
    // Don't cache real-time balance queries
    return !includeRealTimeHolds;
}

// Good: Meaningful cache keys
@Override
public String getCacheKey() {
    return String.format("account_balance_%s_%s_%s",
        accountNumber, includeHolds, currency);
}
```

### Handler Implementation Patterns

1. **Single Responsibility**: Each handler should handle exactly one command/query type
2. **Error Handling**: Implement comprehensive error handling and logging
3. **Step Event Publishing**: Publish step events for saga coordination when appropriate
4. **Reactive Patterns**: Use reactive operators effectively for composition

```java
@Override
public Mono<TransferResult> handle(ProcessTransferCommand command) {
    return command.validate()
        .filter(ValidationResult::isValid)
        .switchIfEmpty(Mono.error(new ValidationException("Invalid command")))
        .flatMap(validation -> executeTransfer(command))
        .flatMap(result -> publishStepEvent(command, result).thenReturn(result))
        .doOnSuccess(result -> log.info("Transfer completed: {}", result.getTransactionId()))
        .doOnError(error -> log.error("Transfer failed: {}", command.getTransactionId(), error));
}
```

### Integration Patterns

1. **Manual Wiring**: Explicitly wire CQRS components with lib-transactional-engine
2. **Event Publishing**: Use StepEventPublisherBridge for saga coordination
3. **Correlation Propagation**: Maintain correlation context across all operations
4. **Error Propagation**: Ensure errors are properly published as step events

### Performance Considerations

1. **Query Caching**: Use appropriate TTL values based on data freshness requirements
2. **Command Validation**: Keep synchronous validation lightweight
3. **Async Operations**: Use reactive patterns to avoid blocking operations
4. **Resource Management**: Properly manage database connections and external service calls

### Testing Strategies

1. **Unit Testing**: Test handlers in isolation with mocked dependencies
2. **Integration Testing**: Test complete command/query flows
3. **Saga Testing**: Test step event publishing and saga coordination
4. **Performance Testing**: Validate caching behavior and response times

```java
@Test
void shouldHandleTransferCommandSuccessfully() {
    // Given
    ProcessTransferCommand command = ProcessTransferCommand.builder()
        .fromAccount("ACC-001")
        .toAccount("ACC-002")
        .amount(new BigDecimal("100.00"))
        .build();

    when(transferService.transfer(any(), any(), any()))
        .thenReturn(Mono.just(TransferResult.success("TXN-123")));

    // When
    Mono<TransferResult> result = handler.handle(command);

    // Then
    StepVerifier.create(result)
        .assertNext(transferResult -> {
            assertThat(transferResult.isSuccess()).isTrue();
            assertThat(transferResult.getTransactionId()).isEqualTo("TXN-123");
        })
        .verifyComplete();

    // Verify step event was published
    verify(stepEventBridge).publish(any(StepEventEnvelope.class));
}
```

---

The CQRS framework provides a robust foundation for building scalable, maintainable banking microservices with clear separation of concerns and powerful orchestration capabilities through manual integration with lib-transactional-engine.
