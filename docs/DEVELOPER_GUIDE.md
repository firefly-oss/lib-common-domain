# Developer Guide

This guide provides step-by-step tutorials, best practices, and integration patterns for building banking microservices with the Firefly Common Domain Library.

## Table of Contents

- [Getting Started](#getting-started)
- [Banking Domain Tutorials](#banking-domain-tutorials)
- [Best Practices](#best-practices)
- [Integration Patterns](#integration-patterns)
- [Testing Strategies](#testing-strategies)
- [Performance Optimization](#performance-optimization)
- [Troubleshooting](#troubleshooting)

## Getting Started

### Prerequisites

- Java 21 or higher
- Spring Boot 3.x
- Maven or Gradle
- Basic understanding of reactive programming (Project Reactor)

### Project Setup

1. **Add the dependency to your project:**

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2. **The framework is automatically configured:**

```java
@SpringBootApplication
public class BankingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingServiceApplication.class, args);
    }
}
```

The library uses Spring Boot's auto-configuration mechanism. When the dependency is on the classpath, the following components are automatically configured:
- CQRS Framework (CommandBus, QueryBus)
- Domain Events (DomainEventPublisher, messaging adapters)
- ServiceClient Framework (REST, gRPC, SDK clients)

3. **Configure basic properties:**

```yaml
firefly:
  cqrs:
    enabled: true
  events:
    enabled: true
    adapter: application_event  # For development
  service-client:
    enabled: true
```

## Banking Domain Tutorials

### Tutorial 1: Customer Account Management

Let's build a complete customer account management system using CQRS patterns.

#### Step 1: Define Domain Models

```java
// Account aggregate
@Data
@Builder
public class Account {
    private String accountId;
    private String customerId;
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal balance;
    private Currency currency;
    private AccountStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}

// Value objects
public enum AccountType {
    CHECKING, SAVINGS, CREDIT, LOAN
}

public enum AccountStatus {
    PENDING, ACTIVE, SUSPENDED, CLOSED
}
```

#### Step 2: Create Commands and Queries

```java
// Command to create an account
@Data
@Builder
public class CreateAccountCommand implements Command<AccountResult> {
    private final String customerId;
    private final AccountType accountType;
    private final BigDecimal initialDeposit;
    private final Currency currency;
    private final String correlationId;
    
    @Override
    public Mono<ValidationResult> validate() {
        ValidationResult.Builder builder = ValidationResult.builder();
        
        if (customerId == null || customerId.trim().isEmpty()) {
            builder.addError(ValidationError.builder()
                .fieldName("customerId")
                .message("Customer ID is required")
                .errorCode("REQUIRED")
                .build());
        }

        if (accountType == null) {
            builder.addError(ValidationError.builder()
                .fieldName("accountType")
                .message("Account type is required")
                .errorCode("REQUIRED")
                .build());
        }

        if (initialDeposit != null && initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            builder.addError(ValidationError.builder()
                .fieldName("initialDeposit")
                .message("Initial deposit cannot be negative")
                .errorCode("INVALID_VALUE")
                .build());
        }

        if (currency == null) {
            builder.addError(ValidationError.builder()
                .fieldName("currency")
                .message("Currency is required")
                .errorCode("REQUIRED")
                .build());
        }
        
        return Mono.just(builder.build());
    }
}

// Query to get account details
@Data
@Builder
public class GetAccountQuery implements Query<Account> {
    private final String accountId;
    private final String correlationId;
    
    @Override
    public boolean isCacheable() {
        return true;
    }
    
    @Override
    public String getCacheKey() {
        return "account_" + accountId;
    }
}
```

#### Step 3: Implement Command Handler

```java
@Component
@Slf4j
public class CreateAccountHandler implements CommandHandler<CreateAccountCommand, AccountResult> {
    
    private final AccountRepository accountRepository;
    private final CustomerServiceClient customerServiceClient;
    private final DomainEventPublisher eventPublisher;
    private final AccountNumberGenerator accountNumberGenerator;
    
    @Override
    public Mono<AccountResult> handle(CreateAccountCommand command) {
        log.info("Creating account for customer: {}", command.getCustomerId());
        
        return command.validate()
            .flatMap(validation -> {
                if (!validation.isValid()) {
                    return Mono.error(new ValidationException(validation));
                }
                return validateCustomerExists(command.getCustomerId());
            })
            .flatMap(customer -> createAccount(command))
            .flatMap(this::saveAccount)
            .flatMap(this::publishAccountCreatedEvent)
            .doOnSuccess(result -> log.info("Account created successfully: {}", result.getAccountId()))
            .doOnError(error -> log.error("Failed to create account for customer: {}", 
                command.getCustomerId(), error));
    }
    
    private Mono<Customer> validateCustomerExists(String customerId) {
        return customerServiceClient.get("/customers/{id}", Customer.class, 
            Map.of("id", customerId))
            .switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId)));
    }
    
    private Mono<Account> createAccount(CreateAccountCommand command) {
        return accountNumberGenerator.generateAccountNumber(command.getAccountType())
            .map(accountNumber -> Account.builder()
                .accountId(UUID.randomUUID().toString())
                .customerId(command.getCustomerId())
                .accountNumber(accountNumber)
                .accountType(command.getAccountType())
                .balance(command.getInitialDeposit() != null ? command.getInitialDeposit() : BigDecimal.ZERO)
                .currency(command.getCurrency())
                .status(AccountStatus.PENDING)
                .createdAt(Instant.now())
                .build());
    }
    
    private Mono<Account> saveAccount(Account account) {
        return accountRepository.save(account);
    }
    
    private Mono<AccountResult> publishAccountCreatedEvent(Account account) {
        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("banking.accounts")
            .type("account.created")
            .key(account.getAccountId())
            .payload(AccountCreatedEvent.from(account))
            .timestamp(Instant.now())
            .headers(Map.of("source", "account-service"))
            .build();
            
        return eventPublisher.publish(event)
            .thenReturn(AccountResult.from(account));
    }
    
    @Override
    public Class<CreateAccountCommand> getCommandType() {
        return CreateAccountCommand.class;
    }
}
```

#### Step 4: Implement Query Handler

```java
@Component
@Slf4j
public class GetAccountHandler implements QueryHandler<GetAccountQuery, Account> {
    
    private final AccountRepository accountRepository;
    
    @Override
    @Cacheable(value = "accounts", key = "#query.cacheKey")
    public Mono<Account> handle(GetAccountQuery query) {
        log.debug("Retrieving account: {}", query.getAccountId());
        
        return accountRepository.findById(query.getAccountId())
            .switchIfEmpty(Mono.error(new AccountNotFoundException(query.getAccountId())))
            .doOnSuccess(account -> log.debug("Account retrieved: {}", account.getAccountId()));
    }
    
    @Override
    public Class<GetAccountQuery> getQueryType() {
        return GetAccountQuery.class;
    }
    
    @Override
    public Long getCacheTtlSeconds() {
        return 300L; // 5 minutes
    }
}
```

#### Step 5: Create Service Layer

```java
@Service
@Slf4j
public class AccountService {
    
    private final CommandBus commandBus;
    private final QueryBus queryBus;
    
    public Mono<AccountResult> createAccount(CreateAccountRequest request) {
        CreateAccountCommand command = CreateAccountCommand.builder()
            .customerId(request.getCustomerId())
            .accountType(request.getAccountType())
            .initialDeposit(request.getInitialDeposit())
            .currency(request.getCurrency())
            .correlationId(CorrelationContext.current().getCorrelationId())
            .build();
            
        return commandBus.send(command);
    }
    
    public Mono<Account> getAccount(String accountId) {
        GetAccountQuery query = GetAccountQuery.builder()
            .accountId(accountId)
            .correlationId(CorrelationContext.current().getCorrelationId())
            .build();
            
        return queryBus.query(query);
    }
}
```

### Tutorial 2: Money Transfer with External Service Integration

Implement a money transfer system using ServiceClients for external service communication.

#### Step 1: Define Transfer Command

```java
@Data
@Builder
public class TransferMoneyCommand implements Command<TransferResult> {

    private final String fromAccountId;
    private final String toAccountId;
    private final BigDecimal amount;
    private final Currency currency;
    private final String description;
    private final String correlationId;

    @Override
    public Mono<ValidationResult> validate() {
        ValidationResult.Builder builder = ValidationResult.builder();

        if (fromAccountId == null || fromAccountId.trim().isEmpty()) {
            builder.addError(ValidationError.builder()
                .fieldName("fromAccountId")
                .message("Source account ID is required")
                .errorCode("REQUIRED")
                .build());
        }

        if (toAccountId == null || toAccountId.trim().isEmpty()) {
            builder.addError(ValidationError.builder()
                .fieldName("toAccountId")
                .message("Destination account ID is required")
                .errorCode("REQUIRED")
                .build());
        }

        if (fromAccountId != null && fromAccountId.equals(toAccountId)) {
            builder.addError(ValidationError.builder()
                .fieldName("toAccountId")
                .message("Source and destination accounts cannot be the same")
                .errorCode("INVALID_VALUE")
                .build());
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            builder.addError(ValidationError.builder()
                .fieldName("amount")
                .message("Transfer amount must be positive")
                .errorCode("INVALID_VALUE")
                .build());
        }

        if (currency == null) {
            builder.addError(ValidationError.builder()
                .fieldName("currency")
                .message("Currency is required")
                .errorCode("REQUIRED")
                .build());
        }

        return Mono.just(builder.build());
    }
}
```

#### Step 2: Implement Transfer Handler

```java
@Component
@Slf4j
public class TransferMoneyHandler implements CommandHandler<TransferMoneyCommand, TransferResult> {

    private final RestServiceClient accountServiceClient;
    private final RestServiceClient notificationClient;
    private final RestServiceClient fraudServiceClient;

    @Override
    public Mono<TransferResult> handle(TransferMoneyCommand command) {
        log.info("Processing money transfer from {} to {} for amount {}",
            command.getFromAccountId(), command.getToAccountId(), command.getAmount());

        return command.validate()
            .flatMap(validation -> {
                if (!validation.isValid()) {
                    return Mono.error(new ValidationException(validation));
                }
                return performFraudCheck(command);
            })
            .flatMap(fraudResult -> {
                if (!fraudResult.isApproved()) {
                    return Mono.error(new TransferBlockedException("Transfer blocked by fraud detection"));
                }
                return executeTransfer(command);
            })
            .flatMap(this::sendNotification);
    }

    private Mono<FraudCheckResult> performFraudCheck(TransferMoneyCommand command) {
        FraudCheckRequest request = FraudCheckRequest.builder()
            .fromAccountId(command.getFromAccountId())
            .toAccountId(command.getToAccountId())
            .amount(command.getAmount())
            .currency(command.getCurrency())
            .build();

        return fraudServiceClient.post("/fraud/check", request, FraudCheckResult.class);
    }

    private Mono<TransferResult> executeTransfer(TransferMoneyCommand command) {
        TransferRequest transferRequest = TransferRequest.builder()
            .fromAccountId(command.getFromAccountId())
            .toAccountId(command.getToAccountId())
            .amount(command.getAmount())
            .currency(command.getCurrency())
            .description(command.getDescription())
            .build();

        return accountServiceClient.post("/transfers", transferRequest, TransferResult.class);
    }

    private Mono<TransferResult> sendNotification(TransferResult result) {
        NotificationRequest notification = NotificationRequest.builder()
            .transferId(result.getTransferId())
            .fromAccountId(result.getFromAccountId())
            .toAccountId(result.getToAccountId())
            .amount(result.getAmount())
            .build();

        return notificationClient.post("/notifications/transfer", notification, Void.class)
            .thenReturn(result);
    }

    @Override
    public Class<TransferMoneyCommand> getCommandType() {
        return TransferMoneyCommand.class;
    }
}
```

### Tutorial 3: Event-Driven Customer Onboarding

Build a complete customer onboarding workflow using domain events.

#### Step 1: Define Onboarding Events

```java
// Customer registration event
@Data
@Builder
public class CustomerRegisteredEvent {
    private String customerId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Instant registeredAt;
}

// KYC completion event
@Data
@Builder
public class KycCompletedEvent {
    private String customerId;
    private String kycId;
    private KycStatus status;
    private String riskLevel;
    private Instant completedAt;
}
```

#### Step 2: Implement Event Handlers

```java
@Component
@Slf4j
public class CustomerOnboardingWorkflow {
    
    private final CommandBus commandBus;
    private final RestServiceClient kycServiceClient;
    private final RestServiceClient notificationClient;
    
    @EventListener
    public void handleCustomerRegistered(DomainSpringEvent event) {
        if ("customer.registered".equals(event.getEnvelope().getType())) {
            CustomerRegisteredEvent customerEvent = (CustomerRegisteredEvent) event.getEnvelope().getPayload();
            
            log.info("Customer registered: {}", customerEvent.getCustomerId());
            
            // Start KYC verification
            StartKycVerificationCommand kycCommand = StartKycVerificationCommand.builder()
                .customerId(customerEvent.getCustomerId())
                .email(customerEvent.getEmail())
                .firstName(customerEvent.getFirstName())
                .lastName(customerEvent.getLastName())
                .build();
                
            commandBus.send(kycCommand)
                .doOnSuccess(result -> log.info("KYC verification started for customer: {}", 
                    customerEvent.getCustomerId()))
                .doOnError(error -> log.error("Failed to start KYC for customer: {}", 
                    customerEvent.getCustomerId(), error))
                .subscribe();
        }
    }
    
    @EventListener
    public void handleKycCompleted(DomainSpringEvent event) {
        if ("kyc.completed".equals(event.getEnvelope().getType())) {
            KycCompletedEvent kycEvent = (KycCompletedEvent) event.getEnvelope().getPayload();
            
            log.info("KYC completed for customer: {} with status: {}", 
                kycEvent.getCustomerId(), kycEvent.getStatus());
            
            if (kycEvent.getStatus() == KycStatus.APPROVED) {
                // Create default accounts
                CreateDefaultAccountsCommand accountsCommand = CreateDefaultAccountsCommand.builder()
                    .customerId(kycEvent.getCustomerId())
                    .riskLevel(kycEvent.getRiskLevel())
                    .build();
                    
                commandBus.send(accountsCommand)
                    .doOnSuccess(result -> log.info("Default accounts created for customer: {}", 
                        kycEvent.getCustomerId()))
                    .subscribe();
            } else {
                // Send rejection notification
                sendRejectionNotification(kycEvent.getCustomerId(), kycEvent.getStatus())
                    .subscribe();
            }
        }
    }
    
    @EventListener
    public void handleAccountsCreated(DomainSpringEvent event) {
        if ("accounts.created".equals(event.getEnvelope().getType())) {
            AccountsCreatedEvent accountsEvent = (AccountsCreatedEvent) event.getEnvelope().getPayload();
            
            log.info("Accounts created for customer: {}", accountsEvent.getCustomerId());
            
            // Send welcome notification
            SendWelcomeNotificationCommand welcomeCommand = SendWelcomeNotificationCommand.builder()
                .customerId(accountsEvent.getCustomerId())
                .accountIds(accountsEvent.getAccountIds())
                .build();
                
            commandBus.send(welcomeCommand)
                .subscribe();
        }
    }
    
    private Mono<Void> sendRejectionNotification(String customerId, KycStatus status) {
        RejectionNotification notification = RejectionNotification.builder()
            .customerId(customerId)
            .reason(status.getDescription())
            .build();
            
        return notificationClient.post("/notifications/rejection", notification, Void.class);
    }
}
```

## Best Practices

### 1. Command Design

- **Single Responsibility**: Each command should represent one business intention
- **Immutability**: Commands should be immutable data structures
- **Validation**: Include comprehensive validation logic
- **Correlation**: Always include correlation IDs for tracing

```java
// Good: Focused, immutable command
@Data
@Builder
public class ProcessPaymentCommand implements Command<PaymentResult> {
    private final String paymentId;
    private final String accountId;
    private final BigDecimal amount;
    private final String merchantId;
    private final String correlationId;
    
    @Override
    public Mono<ValidationResult> validate() {
        // Comprehensive validation
    }
}

// Avoid: Mutable, overly broad command
public class ProcessTransactionCommand {
    private String type; // Too generic
    private Map<String, Object> data; // Untyped
    // Missing validation
}
```

### 2. Query Optimization

- **Caching Strategy**: Use appropriate cache keys and TTL
- **Read Models**: Design optimized read models for queries
- **Pagination**: Implement pagination for large result sets

```java
@Component
public class AccountBalanceHandler implements QueryHandler<GetAccountBalanceQuery, AccountBalance> {
    
    @Override
    @Cacheable(value = "account-balances", key = "#query.cacheKey")
    public Mono<AccountBalance> handle(GetAccountBalanceQuery query) {
        // Optimized query implementation
    }
    
    @Override
    public Long getCacheTtlSeconds() {
        return 300L; // 5 minutes for balance data
    }
}
```

### 3. Event Design

- **Event Versioning**: Plan for event schema evolution
- **Idempotency**: Ensure event handlers are idempotent
- **Ordering**: Consider event ordering requirements

```java
// Good: Versioned, specific event
@Data
@Builder
public class AccountCreatedEventV2 {
    private String accountId;
    private String customerId;
    private AccountType accountType;
    private BigDecimal initialBalance;
    private Currency currency;
    private Instant createdAt;
    private String version = "2.0";
}

// Event handler idempotency
@EventListener
public void handleAccountCreated(DomainSpringEvent event) {
    if (alreadyProcessed(event.getEnvelope().getKey())) {
        return; // Skip duplicate processing
    }
    // Process event
    markAsProcessed(event.getEnvelope().getKey());
}
```

### 4. ServiceClient Usage

- **Circuit Breakers**: Configure appropriate failure thresholds
- **Timeouts**: Set reasonable timeouts for different operations
- **Retry Logic**: Use exponential backoff for retries

```java
// Properly configured ServiceClient
RestServiceClient paymentClient = RestServiceClient.builder()
    .serviceName("payment-service")
    .baseUrl("https://payment-service:8443")
    .timeout(Duration.ofSeconds(30))
    .circuitBreaker(CircuitBreaker.ofDefaults("payment-service"))
    .retry(Retry.ofDefaults("payment-service"))
    .authentication(AuthenticationConfiguration.bearer(tokenProvider))
    .build();
```

## Integration Patterns

### 1. Microservice Communication

```java
// Service-to-service communication pattern
@Component
public class PaymentProcessingService {
    
    private final SdkServiceClient<PaymentProviderSDK> paymentProviderClient;
    private final RestServiceClient accountServiceClient;
    private final RestServiceClient notificationServiceClient;
    
    public Mono<PaymentResult> processPayment(PaymentRequest request) {
        return validateAccount(request.getAccountId())
            .flatMap(account -> chargePaymentProvider(request, account))
            .flatMap(charge -> updateAccountBalance(request, charge))
            .flatMap(balance -> sendNotification(request, balance))
            .map(this::buildPaymentResult);
    }
}
```

### 2. Event-Driven Workflow

```java
// Complex business process using event-driven patterns
@Component
public class LoanApplicationWorkflow {
    
    @EventListener
    public void handleLoanApplicationSubmitted(DomainSpringEvent event) {
        // Start credit check
        commandBus.send(new StartCreditCheckCommand(applicationId)).subscribe();
    }
    
    @EventListener
    public void handleCreditCheckCompleted(DomainSpringEvent event) {
        if (creditCheckPassed) {
            commandBus.send(new StartRiskAssessmentCommand(applicationId)).subscribe();
        } else {
            commandBus.send(new RejectLoanApplicationCommand(applicationId)).subscribe();
        }
    }
    
    @EventListener
    public void handleRiskAssessmentCompleted(DomainSpringEvent event) {
        if (riskAcceptable) {
            commandBus.send(new ApproveLoanCommand(applicationId)).subscribe();
        } else {
            commandBus.send(new RejectLoanApplicationCommand(applicationId)).subscribe();
        }
    }
}
```

## Testing Strategies

### 1. Unit Testing Commands and Queries

```java
@ExtendWith(MockitoExtension.class)
class CreateAccountHandlerTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerServiceClient customerServiceClient;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private CreateAccountHandler handler;

    @Test
    void shouldCreateAccountSuccessfully() {
        // Given
        CreateAccountCommand command = CreateAccountCommand.builder()
            .customerId("CUST-123")
            .accountType(AccountType.CHECKING)
            .initialDeposit(BigDecimal.valueOf(1000))
            .currency(Currency.USD)
            .build();

        Customer customer = Customer.builder()
            .customerId("CUST-123")
            .status(CustomerStatus.ACTIVE)
            .build();

        Account savedAccount = Account.builder()
            .accountId("ACC-456")
            .customerId("CUST-123")
            .accountNumber("1234567890")
            .build();

        when(customerServiceClient.get(anyString(), eq(Customer.class), any()))
            .thenReturn(Mono.just(customer));
        when(accountRepository.save(any(Account.class)))
            .thenReturn(Mono.just(savedAccount));
        when(eventPublisher.publish(any(DomainEventEnvelope.class)))
            .thenReturn(Mono.empty());

        // When
        StepVerifier.create(handler.handle(command))
            .assertNext(result -> {
                assertThat(result.getAccountId()).isEqualTo("ACC-456");
                assertThat(result.getStatus()).isEqualTo("CREATED");
            })
            .verifyComplete();

        // Then
        verify(eventPublisher).publish(argThat(envelope ->
            "account.created".equals(envelope.getType())));
    }
}
```

### 2. Integration Testing with TestContainers

```java
@SpringBootTest
@Testcontainers
class AccountServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

    @Autowired
    private AccountService accountService;

    @Autowired
    private TestKafkaConsumer testKafkaConsumer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Test
    void shouldCreateAccountAndPublishEvent() {
        // Given
        CreateAccountRequest request = CreateAccountRequest.builder()
            .customerId("CUST-123")
            .accountType(AccountType.CHECKING)
            .initialDeposit(BigDecimal.valueOf(1000))
            .build();

        // When
        StepVerifier.create(accountService.createAccount(request))
            .assertNext(result -> {
                assertThat(result.getAccountId()).isNotNull();
                assertThat(result.getStatus()).isEqualTo("CREATED");
            })
            .verifyComplete();

        // Then - verify event was published
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<DomainEventEnvelope> events = testKafkaConsumer.getReceivedEvents();
                assertThat(events).hasSize(1);
                assertThat(events.get(0).getType()).isEqualTo("account.created");
            });
    }
}
```

### 3. ServiceClient Testing

```java
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private SdkServiceClient<PaymentProviderSDK> paymentProviderClient;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldProcessPaymentSuccessfully() {
        // Given
        PaymentRequest request = PaymentRequest.builder()
            .amount(BigDecimal.valueOf(100))
            .currency(Currency.USD)
            .paymentMethod("card")
            .build();

        PaymentResult expectedResult = PaymentResult.builder()
            .paymentId("PAY-123")
            .status(PaymentStatus.SUCCESS)
            .build();

        when(paymentProviderClient.execute(any(Function.class)))
            .thenReturn(Mono.just(expectedResult));

        // When & Then
        StepVerifier.create(paymentService.processPayment(request))
            .expectNext(expectedResult)
            .verifyComplete();
    }
}
```

## Performance Optimization

### 1. Query Optimization

```java
// Use projection queries for better performance
@Component
public class AccountSummaryHandler implements QueryHandler<GetAccountSummaryQuery, AccountSummary> {

    @Override
    @Cacheable(value = "account-summaries", key = "#query.cacheKey")
    public Mono<AccountSummary> handle(GetAccountSummaryQuery query) {
        // Use projection to fetch only required fields
        return accountRepository.findSummaryById(query.getAccountId())
            .map(this::mapToAccountSummary);
    }
}

// Implement pagination for large datasets
@Component
public class TransactionHistoryHandler implements QueryHandler<GetTransactionHistoryQuery, PagedResult<Transaction>> {

    @Override
    public Mono<PagedResult<Transaction>> handle(GetTransactionHistoryQuery query) {
        Pageable pageable = PageRequest.of(query.getPage(), query.getSize());

        return transactionRepository.findByAccountId(query.getAccountId(), pageable)
            .map(page -> PagedResult.<Transaction>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build());
    }
}
```

### 2. Caching Strategies

```yaml
# Configure cache settings
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=5m

firefly:
  cqrs:
    query:
      cache:
        enabled: true
        default-ttl: 300
        max-size: 10000
```

### 3. Connection Pooling

```yaml
# Optimize ServiceClient connection pools
firefly:
  service-client:
    rest:
      max-connections: 200
      max-idle-time: 10m
      max-life-time: 30m
      pending-acquire-timeout: 10s
```

## Troubleshooting

### Common Issues and Solutions

#### 1. Handler Not Found

**Problem**: `CommandHandler not found for command type`

**Solution**: Ensure handler is annotated with `@Component` and implements correct interface:

```java
@Component // Missing annotation
public class MyCommandHandler implements CommandHandler<MyCommand, MyResult> {
    // Implementation
}
```

#### 2. Event Not Published

**Problem**: Domain events are not being published

**Solution**: Check adapter configuration and ensure messaging infrastructure is available:

```yaml
firefly:
  events:
    enabled: true
    adapter: auto  # Will auto-detect available infrastructure
```

#### 3. Circuit Breaker Always Open

**Problem**: ServiceClient circuit breaker is always in open state

**Solution**: Adjust circuit breaker thresholds:

```java
CircuitBreakerConfig config = CircuitBreakerConfig.custom()
    .failureRateThreshold(50)  // Increase threshold
    .minimumNumberOfCalls(10)  // Increase minimum calls
    .waitDurationInOpenState(Duration.ofSeconds(30))
    .build();
```

#### 4. Memory Leaks in Reactive Streams

**Problem**: Memory usage keeps increasing

**Solution**: Ensure proper subscription management:

```java
// Good: Proper subscription
commandBus.send(command)
    .doOnSuccess(result -> log.info("Success: {}", result))
    .doOnError(error -> log.error("Error", error))
    .subscribe(); // Don't forget to subscribe

// Bad: Missing subscription
commandBus.send(command)
    .doOnSuccess(result -> log.info("Success: {}", result)); // Never executes
```

### Debugging Tips

1. **Enable Debug Logging**:
```yaml
logging:
  level:
    com.firefly.common.domain: DEBUG
    reactor.core: DEBUG
```

2. **Use Reactor Debug Mode**:
```java
@PostConstruct
public void enableReactorDebug() {
    Hooks.onOperatorDebug();
}
```

3. **Monitor Metrics**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

---

This comprehensive developer guide provides everything needed to build robust, scalable banking microservices using the Firefly Common Domain Library. For additional support, refer to the API Reference and Configuration Guide.
