# Examples

Real working examples based on the actual codebase implementation, demonstrating all components of the **Firefly Common Domain Library**.

## Table of Contents

1. [Quick Start](#quick-start)
2. [CQRS Framework Examples](#cqrs-framework-examples)
3. [Domain Events Examples](#domain-events-examples)
4. [Service Client Examples](#service-client-examples)
5. [Resilience Patterns Examples](#resilience-patterns-examples)
6. [Distributed Tracing Examples](#distributed-tracing-examples)
7. [lib-transactional-engine Integration Examples](#lib-transactional-engine-integration-examples)
8. [Complete Banking Application Example](#complete-banking-application-example)

## Quick Start

### Maven Dependencies

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Auto-Configuration

The framework auto-configures when detected on the classpath. **No manual configuration needed!**

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
        // ✅ CommandBus and QueryBus beans are automatically available
        // ✅ MeterRegistry is auto-configured for metrics (SimpleMeterRegistry by default)
        // ✅ Validation, caching, and handler discovery are automatic
    }
}
```

**What's Auto-Configured:**
- **CommandBus** and **QueryBus** beans
- **MeterRegistry** for metrics (**provided by default** - no manual bean required)
- **Validation** support (if Jakarta Validation is available)
- **Caching** support for queries
- **Handler discovery** via Spring component scanning

### Metrics by Default Example

**No metrics configuration needed** - the framework provides metrics automatically:

```java
@RestController
public class MetricsController {

    @Autowired
    private MeterRegistry meterRegistry; // Auto-configured by framework

    @GetMapping("/metrics")
    public Map<String, Double> getMetrics() {
        return Map.of(
            "commands_processed", meterRegistry.find("firefly.cqrs.command.processed").counter().count(),
            "queries_processed", meterRegistry.find("firefly.cqrs.query.processed").counter().count(),
            "avg_command_time", meterRegistry.find("firefly.cqrs.command.processing.time").timer().mean(TimeUnit.MILLISECONDS),
            "avg_query_time", meterRegistry.find("firefly.cqrs.query.processing.time").timer().mean(TimeUnit.MILLISECONDS)
        );
    }
}
```

**Custom MeterRegistry (Optional):**
```java
@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistry meterRegistry() {
        // Override default SimpleMeterRegistry with your preferred implementation
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
}
```

## Complete Banking Example

### 1. Create Account Command

**Command (Simple POJO):**
```java
import jakarta.validation.constraints.*;

public class CreateAccountCommand implements Command<AccountCreatedResult> {
    @NotBlank(message = "Customer ID is required")
    private final String customerId;

    @NotBlank(message = "Account type is required")
    private final String accountType;

    @NotNull(message = "Initial balance is required")
    @Positive(message = "Initial balance must be positive")
    private final BigDecimal initialBalance;

    public CreateAccountCommand(String customerId, String accountType, BigDecimal initialBalance) {
        this.customerId = customerId;
        this.accountType = accountType;
        this.initialBalance = initialBalance;
    }

    // Getters
    public String getCustomerId() { return customerId; }
    public String getAccountType() { return accountType; }
    public BigDecimal getInitialBalance() { return initialBalance; }
}
```

**Result (Simple POJO):**
```java
public class AccountCreatedResult {
    private final String accountNumber;
    private final String customerId;
    private final String accountType;
    private final BigDecimal initialBalance;
    private final String status;
    private final LocalDateTime createdAt;

    public AccountCreatedResult(String accountNumber, String customerId, String accountType,
                               BigDecimal initialBalance, String status, LocalDateTime createdAt) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.accountType = accountType;
        this.initialBalance = initialBalance;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters...
}
```

**Handler (THE ONLY WAY - Zero Boilerplate):**
```java
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountCreatedResult> {

    @Autowired
    private AccountService accountService;

    @Override
    protected Mono<AccountCreatedResult> doHandle(CreateAccountCommand command) {
        // Only business logic - everything else is automatic!
        return accountService.createAccount(command)
            .map(account -> new AccountCreatedResult(
                account.getAccountNumber(),
                command.getCustomerId(),
                command.getAccountType(),
                command.getInitialBalance(),
                "ACTIVE",
                LocalDateTime.now()
            ));
    }

    // ✅ NO BOILERPLATE NEEDED:
    // - No getCommandType() - automatically detected from generics
    // - No getResultType() - automatically detected from generics
    // - No validation setup - handled by Jakarta Bean Validation
    // - No metrics setup - handled by @CommandHandlerComponent
    // - No error handling - built-in
    // - No logging - built-in
}
```

**Using the CommandBus:**
```java
@Service
public class AccountService {

    @Autowired
    private CommandBus commandBus;

    public Mono<AccountCreatedResult> createAccount(String customerId, String accountType, BigDecimal amount) {
        CreateAccountCommand command = new CreateAccountCommand(customerId, accountType, amount);
        return commandBus.send(command);
    }
}
```

### 2. Get Account Balance Query

**Query (Simple POJO):**
```java
import jakarta.validation.constraints.*;

public class GetAccountBalanceQuery implements Query<AccountBalance> {
    @NotBlank(message = "Account number is required")
    private final String accountNumber;

    @NotBlank(message = "Customer ID is required")
    private final String customerId;

    public GetAccountBalanceQuery(String accountNumber, String customerId) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
    }

    // Getters
    public String getAccountNumber() { return accountNumber; }
    public String getCustomerId() { return customerId; }
}
```

**Result (Simple POJO):**
```java
public class AccountBalance {
    private final String accountNumber;
    private final BigDecimal currentBalance;
    private final BigDecimal availableBalance;
    private final String currency;
    private final LocalDateTime lastUpdated;

    public AccountBalance(String accountNumber, BigDecimal currentBalance, BigDecimal availableBalance,
                         String currency, LocalDateTime lastUpdated) {
        this.accountNumber = accountNumber;
        this.currentBalance = currentBalance;
        this.availableBalance = availableBalance;
        this.currency = currency;
        this.lastUpdated = lastUpdated;
    }

    // Getters...
}
```

**Handler (THE ONLY WAY - Zero Boilerplate):**
```java
@QueryHandlerComponent(cacheable = true, cacheTtl = 300, metrics = true)
public class GetAccountBalanceHandler extends QueryHandler<GetAccountBalanceQuery, AccountBalance> {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query) {
        // Only business logic - everything else is automatic!
        return accountRepository.findByAccountNumber(query.getAccountNumber())
            .map(account -> new AccountBalance(
                account.getAccountNumber(),
                account.getCurrentBalance(),
                account.getAvailableBalance(),
                account.getCurrency(),
                LocalDateTime.now()
            ));
    }

    // ✅ NO BOILERPLATE NEEDED:
    // - No getQueryType() - automatically detected from generics
    // - No getResultType() - automatically detected from generics
    // - No supportsCaching() - handled by @QueryHandlerComponent annotation
    // - No getCacheTtlSeconds() - handled by @QueryHandlerComponent annotation
    // - No validation setup - handled by Jakarta Bean Validation
    // - No metrics setup - handled by @QueryHandlerComponent
}
```

**Using the QueryBus:**
```java
@Service
public class AccountQueryService {

    @Autowired
    private QueryBus queryBus;

    public Mono<AccountBalance> getAccountBalance(String accountNumber, String customerId) {
        GetAccountBalanceQuery query = new GetAccountBalanceQuery(accountNumber, customerId);
        return queryBus.query(query);
    }
}
```

### 3. REST Controller Example

```java
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private QueryBus queryBus;

    @PostMapping
    public Mono<AccountCreatedResult> createAccount(@RequestBody CreateAccountRequest request) {
        // Simple command creation and execution
        CreateAccountCommand command = new CreateAccountCommand(
            request.getCustomerId(),
            request.getAccountType(),
            request.getInitialBalance()
        );
        return commandBus.send(command);
    }

    @GetMapping("/{accountNumber}/balance")
    public Mono<AccountBalance> getBalance(@PathVariable String accountNumber,
                                         @RequestParam String customerId) {
        // Simple query creation and execution - caching handled automatically by annotation
        GetAccountBalanceQuery query = new GetAccountBalanceQuery(accountNumber, customerId);
        return queryBus.query(query);
    }
}
```

### 4. Transfer Money Example

**Command:**
```java
public class TransferMoneyCommand implements Command<TransferResult> {
    @NotBlank
    private final String fromAccount;

    @NotBlank
    private final String toAccount;

    @NotNull
    @Positive
    private final BigDecimal amount;

    private final String description;

    public TransferMoneyCommand(String fromAccount, String toAccount, BigDecimal amount, String description) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.description = description;
    }

    // Getters...
}
```

**Handler:**
```java
@CommandHandlerComponent(timeout = 15000, retries = 2, metrics = true)
public class TransferMoneyHandler extends CommandHandler<TransferMoneyCommand, TransferResult> {

    @Autowired
    private TransferService transferService;

    @Override
    protected Mono<TransferResult> doHandle(TransferMoneyCommand command) {
        // Only business logic - everything else is automatic!
        return transferService.transfer(command)
            .map(transfer -> new TransferResult(
                transfer.getTransactionId(),
                command.getFromAccount(),
                command.getToAccount(),
                command.getAmount(),
                "COMPLETED",
                LocalDateTime.now()
            ));
    }
}
```

## Key Principles Demonstrated

### 1. Single Approach - No Confusion
```java
// ✅ THE ONLY WAY - Extend base classes with annotations
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class MyCommandHandler extends CommandHandler<MyCommand, MyResult> {
    @Override
    protected Mono<MyResult> doHandle(MyCommand command) {
        // Only business logic
    }
}

// ❌ DON'T DO THIS - Multiple ways create confusion
public class OldHandler implements CommandHandler<MyCommand, MyResult> {
    // Old interface-based approach - don't use
}
```

### 2. Zero Boilerplate - Focus on Business Logic
```java
// ✅ ZERO BOILERPLATE - Everything automatic
@QueryHandlerComponent(cacheable = true, cacheTtl = 300, metrics = true)
public class MyQueryHandler extends QueryHandler<MyQuery, MyResult> {
    @Override
    protected Mono<MyResult> doHandle(MyQuery query) {
        // Only business logic - everything else automatic!
        return processQuery(query);
    }

    // ✅ NO METHODS TO OVERRIDE:
    // - No getQueryType() - automatic from generics
    // - No supportsCaching() - from annotation
    // - No getCacheTtlSeconds() - from annotation
}
```

### 3. Annotation-Driven Configuration
```java
// ✅ ALL CONFIGURATION IN ANNOTATIONS
@CommandHandlerComponent(
    timeout = 30000,    // Command timeout
    retries = 3,        // Retry attempts
    metrics = true      // Enable metrics
)

@QueryHandlerComponent(
    cacheable = true,   // Enable caching
    cacheTtl = 300,     // Cache TTL in seconds
    metrics = true      // Enable metrics
)
```

### 4. Automatic Validation
```java
// ✅ VALIDATION IS AUTOMATIC
public class MyCommand implements Command<MyResult> {
    @NotBlank(message = "Name is required")
    private final String name;

    @NotNull
    @Positive(message = "Amount must be positive")
    private final BigDecimal amount;

    // No validation methods needed - Jakarta Bean Validation handles it!
}
```

## Testing Examples

### Unit Testing Handlers
```java
@ExtendWith(MockitoExtension.class)
class CreateAccountHandlerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private CreateAccountHandler handler;

    @Test
    void shouldCreateAccount() {
        // Given
        CreateAccountCommand command = new CreateAccountCommand("CUST-123", "SAVINGS", new BigDecimal("1000"));
        Account mockAccount = new Account("ACC-456", "CUST-123", "SAVINGS", new BigDecimal("1000"));

        when(accountService.createAccount(command)).thenReturn(Mono.just(mockAccount));

        // When
        StepVerifier.create(handler.doHandle(command))
            .expectNextMatches(result ->
                result.getAccountNumber().equals("ACC-456") &&
                result.getCustomerId().equals("CUST-123") &&
                result.getStatus().equals("ACTIVE")
            )
            .verifyComplete();
    }
}
```

### Integration Testing with CommandBus
```java
@SpringBootTest
class AccountIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private QueryBus queryBus;

    @Test
    void shouldCreateAndQueryAccount() {
        // Create account
        CreateAccountCommand createCommand = new CreateAccountCommand("CUST-123", "SAVINGS", new BigDecimal("1000"));

        StepVerifier.create(commandBus.send(createCommand))
            .expectNextMatches(result -> result.getStatus().equals("ACTIVE"))
            .verifyComplete();

        // Query account balance
        GetAccountBalanceQuery balanceQuery = new GetAccountBalanceQuery("ACC-123", "CUST-123");

        StepVerifier.create(queryBus.query(balanceQuery))
            .expectNextMatches(balance -> balance.getCurrentBalance().equals(new BigDecimal("1000")))
            .verifyComplete();
    }
}
```

## Best Practices

### 1. Command and Query Design
```java
// ✅ GOOD - Simple, focused, immutable
public class CreateAccountCommand implements Command<AccountCreatedResult> {
    @NotBlank private final String customerId;
    @NotBlank private final String accountType;
    @NotNull @Positive private final BigDecimal initialBalance;

    // Constructor and getters only - no business logic
}

// ❌ AVOID - Complex commands with business logic
public class ComplexCommand implements Command<Result> {
    // Don't put business logic in commands
    public void validateBusinessRules() { ... }
    public void processData() { ... }
}
```

### 2. Handler Implementation
```java
// ✅ GOOD - Only business logic in doHandle()
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountCreatedResult> {

    @Override
    protected Mono<AccountCreatedResult> doHandle(CreateAccountCommand command) {
        // Only business logic - everything else is automatic
        return accountService.createAccount(command)
            .map(account -> mapToResult(account, command));
    }

    private AccountCreatedResult mapToResult(Account account, CreateAccountCommand command) {
        return new AccountCreatedResult(
            account.getAccountNumber(),
            command.getCustomerId(),
            command.getAccountType(),
            command.getInitialBalance(),
            "ACTIVE",
            LocalDateTime.now()
        );
    }
}
```

### 3. Validation Strategy
```java
// ✅ GOOD - Use Jakarta Bean Validation for simple validation
public class MyCommand implements Command<MyResult> {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private final String name;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @DecimalMax(value = "1000000.00", message = "Amount cannot exceed $1,000,000")
    private final BigDecimal amount;
}

// ✅ GOOD - Complex business validation in service layer
@Service
public class AccountValidationService {
    public Mono<ValidationResult> validateAccountCreation(CreateAccountCommand command) {
        // Complex business rules here
        if (isWeekend() && command.getInitialBalance().compareTo(new BigDecimal("10000")) > 0) {
            return Mono.just(ValidationResult.failure("Large deposits not allowed on weekends"));
        }
        return Mono.just(ValidationResult.success());
    }
}
```

### 4. Error Handling
```java
// ✅ GOOD - Let the framework handle errors, focus on business logic
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class MyHandler extends CommandHandler<MyCommand, MyResult> {

    @Override
    protected Mono<MyResult> doHandle(MyCommand command) {
        // Framework handles timeouts, retries, logging automatically
        return businessService.process(command)
            .onErrorMap(BusinessException.class, ex ->
                new DomainException("Business processing failed: " + ex.getMessage(), ex))
            .map(this::mapToResult);
    }
}
```

### 5. Testing Strategy
```java
// ✅ GOOD - Test business logic, not framework features
@ExtendWith(MockitoExtension.class)
class CreateAccountHandlerTest {

    @Mock private AccountService accountService;
    @InjectMocks private CreateAccountHandler handler;

    @Test
    void shouldCreateAccountSuccessfully() {
        // Given
        CreateAccountCommand command = new CreateAccountCommand("CUST-123", "SAVINGS", new BigDecimal("1000"));
        Account mockAccount = new Account("ACC-456", "CUST-123", "SAVINGS", new BigDecimal("1000"));

        when(accountService.createAccount(command)).thenReturn(Mono.just(mockAccount));

        // When & Then - Test only the business logic
        StepVerifier.create(handler.doHandle(command))
            .expectNextMatches(result ->
                result.getAccountNumber().equals("ACC-456") &&
                result.getStatus().equals("ACTIVE")
            )
            .verifyComplete();
    }
}
```

## Cache Configuration Examples

### Local Cache (Default)

**Configuration:**
```yaml
firefly:
  cqrs:
    query:
      caching-enabled: true
      cache-ttl: 15m
      cache:
        type: LOCAL  # Default - uses ConcurrentMapCacheManager
```

**Query Handler:**
```java
@QueryHandlerComponent(cacheable = true, cacheTtl = 300)
public class GetAccountBalanceHandler extends QueryHandler<GetAccountBalanceQuery, AccountBalance> {

    @Override
    protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query) {
        // Business logic - caching handled automatically
        return accountService.getBalance(query.getAccountNumber());
    }

    // Cache key automatically generated from query class name and parameters
    // Cache TTL: 300 seconds (from annotation)
    // Cache type: Local in-memory (from configuration)
}
```

### Redis Cache (Production)

**Configuration:**
```yaml
firefly:
  cqrs:
    query:
      caching-enabled: true
      cache-ttl: 30m  # Default TTL for all queries
      cache:
        type: REDIS   # Use Redis instead of local cache
        redis:
          enabled: true              # Must be explicitly enabled
          host: redis.production.local
          port: 6379
          database: 1
          password: ${REDIS_PASSWORD}
          timeout: 5s
          key-prefix: "banking:cqrs:"
          statistics: true      # Reserved for future use
```

**Query Handler (Same Code):**
```java
@QueryHandlerComponent(cacheable = true, cacheTtl = 600)  // 10 minutes
public class GetCustomerProfileHandler extends QueryHandler<GetCustomerProfileQuery, CustomerProfile> {

    @Override
    protected Mono<CustomerProfile> doHandle(GetCustomerProfileQuery query) {
        // Same business logic - Redis caching handled automatically
        return customerService.getProfile(query.getCustomerId());
    }

    // Cache key: "banking:cqrs:query-cache:GetCustomerProfileQuery"
    // Cache TTL: 600 seconds (from annotation, overrides default)
    // Cache type: Redis distributed cache (from configuration)
    // Serialization: JSON (automatic)
}
```

### Cache Migration Example

**Step 1: Start with Local Cache**
```yaml
firefly:
  cqrs:
    query:
      cache:
        type: LOCAL  # Start with local cache
```

**Step 2: Add Redis Configuration (Not Enabled)**
```yaml
firefly:
  cqrs:
    query:
      cache:
        type: LOCAL  # Still using local cache
        redis:
          host: redis.example.com
          port: 6379
          # enabled: false (default)
```

**Step 3: Switch to Redis**
```yaml
firefly:
  cqrs:
    query:
      cache:
        type: REDIS  # Switch cache type
        redis:
          enabled: true  # Enable Redis connections
          host: redis.example.com
          port: 6379
```

**Handler Code (No Changes Needed):**
```java
@QueryHandlerComponent(cacheable = true, cacheTtl = 300)
public class MyQueryHandler extends QueryHandler<MyQuery, MyResult> {
    @Override
    protected Mono<MyResult> doHandle(MyQuery query) {
        // Same code works with both local and Redis cache
        return processQuery(query);
    }
}
```

### Cache Behavior Examples

**Cache Hit (Redis):**
```
2025-09-09 00:15:28.184 INFO  - CQRS Query Cache Hit - CacheKey: GetAccountBalanceQuery, ResultType: AccountBalance
```

**Cache Miss (Redis):**
```
2025-09-09 00:15:28.033 INFO  - CQRS Query Processing Started - Type: GetAccountBalanceQuery, Cacheable: true
2025-09-09 00:15:28.173 INFO  - CQRS Query Result Cached - CacheKey: GetAccountBalanceQuery, ResultType: AccountBalance
```

**Redis Fallback (When Redis Unavailable):**
```
2025-09-09 00:15:27.479 WARN  - Redis cache is configured but Redis auto-configuration did not activate. Falling back to local cache.
```

---

## Domain Events Examples

### Basic Event Publishing

#### Programmatic Event Publishing

```java
@Service
public class AccountService {
    private final DomainEventPublisher eventPublisher;

    public Mono<Account> createAccount(CreateAccountRequest request) {
        return processAccountCreation(request)
            .flatMap(account -> {
                // Create domain event
                DomainEventEnvelope event = DomainEventEnvelope.builder()
                    .topic("banking-events")
                    .type("account.created")
                    .key(account.getId())
                    .payload(new AccountCreatedEvent(
                        account.getId(),
                        account.getCustomerId(),
                        account.getAccountType(),
                        account.getInitialBalance(),
                        Instant.now()
                    ))
                    .timestamp(Instant.now())
                    .headers(Map.of(
                        "source", "account-service",
                        "version", "v1"
                    ))
                    .build();

                // Publish event
                return eventPublisher.publish(event)
                    .thenReturn(account);
            });
    }
}
```

#### Annotation-Based Event Publishing

```java
@Service
public class TransferService {

    @EventPublisher(
        topic = "banking-events",
        type = "transfer.completed",
        key = "#result.transactionId"
    )
    public Mono<TransferResult> processTransfer(TransferRequest request) {
        return validateTransfer(request)
            .flatMap(this::executeTransfer)
            .flatMap(this::updateBalances)
            .map(transfer -> new TransferResult(
                transfer.getTransactionId(),
                transfer.getFromAccount(),
                transfer.getToAccount(),
                transfer.getAmount(),
                "COMPLETED",
                Instant.now()
            ));
        // Event automatically published with TransferResult as payload
    }

    @EventPublisher(
        topic = "banking-events",
        type = "transfer.failed",
        key = "#args[0].transactionId",
        payload = "new TransferFailedEvent(#args[0].transactionId, #ex.message)"
    )
    public Mono<TransferResult> processTransferWithFailureEvent(TransferRequest request) {
        return processTransfer(request)
            .onErrorMap(InsufficientFundsException.class,
                ex -> new TransferFailedException("Transfer failed: " + ex.getMessage()));
        // Failure event automatically published on error
    }
}
```

### Event Consumption

#### Event Handler Registration

```java
@EventHandler
@Component
public class AccountEventHandler {

    @EventListener(topic = "banking-events", type = "account.created")
    public void handleAccountCreated(AccountCreatedEvent event) {
        log.info("Account created: {} for customer: {}",
            event.getAccountId(), event.getCustomerId());

        // Process account creation
        sendWelcomeEmail(event.getCustomerId());
        createInitialStatements(event.getAccountId());
    }

    @EventListener(topic = "banking-events", type = "transfer.completed")
    public Mono<Void> handleTransferCompleted(TransferCompletedEvent event) {
        log.info("Transfer completed: {} from {} to {} amount {}",
            event.getTransactionId(),
            event.getFromAccount(),
            event.getToAccount(),
            event.getAmount());

        return updateAccountStatements(event)
            .then(sendTransferNotification(event))
            .then(updateAnalytics(event));
    }

    @EventListener(topic = "banking-events", type = "transfer.failed")
    public void handleTransferFailed(TransferFailedEvent event) {
        log.warn("Transfer failed: {} - {}",
            event.getTransactionId(), event.getReason());

        // Handle failure
        notifyCustomerOfFailure(event);
        recordFailureMetrics(event);
    }
}
```

### Messaging Infrastructure Configuration

#### Automatic Adapter Selection

```yaml
# application.yml - Automatic adapter selection
firefly:
  events:
    enabled: true
    adapter: auto  # Automatically selects best available adapter
```

#### Kafka Configuration

```yaml
# application.yml - Kafka configuration
firefly:
  events:
    enabled: true
    adapter: kafka
    kafka:
      topic: banking-domain-events
      partition-key: "${key}"

# Spring Kafka configuration
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: banking-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
```

#### RabbitMQ Configuration

```yaml
# application.yml - RabbitMQ configuration
firefly:
  events:
    enabled: true
    adapter: rabbit
    rabbit:
      exchange: banking.events
      routing-key: "${type}"

# Spring RabbitMQ configuration
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

---

## Service Client Examples

### REST Service Client

#### Basic REST Client Usage

```java
@Service
public class UserService {
    private final ServiceClient userServiceClient;

    public UserService() {
        this.userServiceClient = ServiceClient.rest("user-service")
            .baseUrl("http://user-service:8080")
            .timeout(Duration.ofSeconds(30))
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    public Mono<User> getUser(String userId) {
        return userServiceClient.get("/users/{id}", User.class)
            .withPathParam("id", userId)
            .withHeader("X-Request-ID", UUID.randomUUID().toString())
            .execute();
    }

    public Mono<User> createUser(CreateUserRequest request) {
        return userServiceClient.post("/users", User.class)
            .withBody(request)
            .withQueryParam("notify", true)
            .execute();
    }

    public Mono<User> updateUser(String userId, UpdateUserRequest request) {
        return userServiceClient.put("/users/{id}", User.class)
            .withPathParam("id", userId)
            .withBody(request)
            .execute();
    }

    public Mono<Void> deleteUser(String userId) {
        return userServiceClient.delete("/users/{id}", Void.class)
            .withPathParam("id", userId)
            .execute()
            .then();
    }
}
```

#### Advanced REST Client Configuration

```java
@Configuration
public class ServiceClientConfig {

    @Bean
    public ServiceClient paymentServiceClient(CircuitBreakerManager circuitBreakerManager) {
        return ServiceClient.rest("payment-service")
            .baseUrl("https://payment-service.example.com")
            .timeout(Duration.ofSeconds(45))
            .maxConnections(100)
            .defaultHeader("Authorization", "Bearer " + getApiToken())
            .defaultHeader("X-API-Version", "v2")
            .defaultHeader("User-Agent", "banking-service/1.0")
            .circuitBreakerManager(circuitBreakerManager)
            .build();
    }

    private String getApiToken() {
        // Retrieve API token from secure storage
        return tokenService.getToken();
    }
}

@Service
public class PaymentService {
    private final ServiceClient paymentServiceClient;

    public Mono<PaymentResult> processPayment(PaymentRequest request) {
        return paymentServiceClient.post("/payments", PaymentResult.class)
            .withBody(request)
            .withHeader("Idempotency-Key", request.getIdempotencyKey())
            .withQueryParam("async", false)
            .execute()
            .timeout(Duration.ofSeconds(30))
            .retry(2);
    }

    public Flux<PaymentStatus> streamPaymentUpdates(String paymentId) {
        return paymentServiceClient.stream("/payments/{id}/status", PaymentStatus.class)
            .withPathParam("id", paymentId)
            .execute();
    }
}
```

### gRPC Service Client

#### Basic gRPC Client Usage

```java
@Service
public class AccountService {
    private final ServiceClient accountServiceClient;

    public AccountService() {
        this.accountServiceClient = ServiceClient.grpc("account-service",
                AccountServiceGrpc.AccountServiceBlockingStub.class)
            .address("account-service:9090")
            .timeout(Duration.ofSeconds(30))
            .build();
    }

    public Mono<GetAccountResponse> getAccount(String accountId) {
        GetAccountRequest request = GetAccountRequest.newBuilder()
            .setAccountId(accountId)
            .build();

        return accountServiceClient.execute(stub ->
            Mono.fromCallable(() -> stub.getAccount(request))
        );
    }

    public Mono<CreateAccountResponse> createAccount(CreateAccountRequest request) {
        return accountServiceClient.execute(stub ->
            Mono.fromCallable(() -> stub.createAccount(request))
        );
    }

    public Flux<TransactionEvent> streamTransactions(String accountId) {
        StreamTransactionsRequest request = StreamTransactionsRequest.newBuilder()
            .setAccountId(accountId)
            .build();

        return accountServiceClient.executeStream(stub ->
            Flux.fromIterable(() -> stub.streamTransactions(request))
        );
    }
}
```

#### Advanced gRPC Client with Custom Configuration

```java
@Configuration
public class GrpcClientConfig {

    @Bean
    public ServiceClient notificationServiceClient(CircuitBreakerManager circuitBreakerManager) {
        return ServiceClient.grpc("notification-service",
                NotificationServiceGrpc.NotificationServiceBlockingStub.class)
            .address("notification-service:9090")
            .timeout(Duration.ofSeconds(15))
            .circuitBreakerManager(circuitBreakerManager)
            .build();
    }
}

@Service
public class NotificationService {
    private final ServiceClient notificationServiceClient;

    public Mono<SendNotificationResponse> sendNotification(String userId, String message) {
        SendNotificationRequest request = SendNotificationRequest.newBuilder()
            .setUserId(userId)
            .setMessage(message)
            .setType(NotificationType.EMAIL)
            .build();

        return notificationServiceClient.execute(stub ->
            Mono.fromCallable(() -> stub.sendNotification(request))
        );
    }
}
```

## Resilience Patterns Examples

### Circuit Breaker Configuration

#### Auto-Configuration (Default)

```yaml
# application.yml - Circuit breaker auto-configured with defaults
firefly:
  circuit-breaker:
    enabled: true  # Default: true
    failure-rate-threshold: 50.0  # Default: 50%
    minimum-number-of-calls: 10   # Default: 10
    sliding-window-size: 100      # Default: 100
    wait-duration-in-open-state: 60s  # Default: 60 seconds
    permitted-number-of-calls-in-half-open-state: 3  # Default: 3
```

#### Manual Circuit Breaker Usage

```java
@Service
public class ExternalPaymentService {
    private final CircuitBreakerManager circuitBreakerManager;
    private final WebClient webClient;

    public Mono<PaymentResponse> processPayment(PaymentRequest request) {
        return circuitBreakerManager.executeWithCircuitBreaker(
            "external-payment-service",
            () -> webClient.post()
                .uri("/payments")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
        ).onErrorMap(CircuitBreakerOpenException.class,
            ex -> new ServiceUnavailableException("Payment service is currently unavailable"));
    }

    public Mono<Boolean> checkServiceHealth() {
        return circuitBreakerManager.executeWithCircuitBreaker(
            "external-payment-service",
            () -> webClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> "OK".equals(response))
        ).onErrorReturn(false);
    }
}
```

#### Circuit Breaker State Monitoring

```java
@RestController
public class CircuitBreakerController {
    private final CircuitBreakerManager circuitBreakerManager;

    @GetMapping("/circuit-breakers")
    public Mono<Map<String, CircuitBreakerInfo>> getCircuitBreakerStates() {
        return Mono.fromCallable(() -> {
            Map<String, CircuitBreakerInfo> states = new HashMap<>();

            // Get all circuit breaker states
            circuitBreakerManager.getAllCircuitBreakers()
                .forEach((serviceName, circuitBreaker) -> {
                    states.put(serviceName, CircuitBreakerInfo.builder()
                        .serviceName(serviceName)
                        .state(circuitBreaker.getState())
                        .failureRate(circuitBreaker.getFailureRate())
                        .totalCalls(circuitBreaker.getTotalCalls())
                        .failedCalls(circuitBreaker.getFailedCalls())
                        .lastFailureTime(circuitBreaker.getLastFailureTime())
                        .build());
                });

            return states;
        });
    }

    @PostMapping("/circuit-breakers/{serviceName}/reset")
    public Mono<Void> resetCircuitBreaker(@PathVariable String serviceName) {
        return Mono.fromRunnable(() ->
            circuitBreakerManager.resetCircuitBreaker(serviceName)
        );
    }
}
```

### Service Client Resilience Integration

#### Automatic Circuit Breaker Protection

```java
@Service
public class ResilientUserService {
    private final ServiceClient userServiceClient;

    public ResilientUserService() {
        // Circuit breaker automatically applied to all service calls
        this.userServiceClient = ServiceClient.rest("user-service")
            .baseUrl("http://user-service:8080")
            .timeout(Duration.ofSeconds(30))
            .build();
    }

    public Mono<User> getUserWithFallback(String userId) {
        return userServiceClient.get("/users/{id}", User.class)
            .withPathParam("id", userId)
            .execute()
            .onErrorResume(CircuitBreakerOpenException.class,
                ex -> getCachedUser(userId))
            .onErrorResume(TimeoutException.class,
                ex -> getDefaultUser(userId));
    }

    private Mono<User> getCachedUser(String userId) {
        // Fallback to cached data
        return cacheService.getUser(userId);
    }

    private Mono<User> getDefaultUser(String userId) {
        // Fallback to default user
        return Mono.just(User.builder()
            .id(userId)
            .name("Unknown User")
            .status("UNAVAILABLE")
            .build());
    }
}
```

---

## Distributed Tracing Examples

### Automatic Context Propagation

#### Service-to-Service Tracing

```java
@RestController
public class TransferController {
    private final TransferService transferService;
    private final ServiceClient accountServiceClient;

    @PostMapping("/transfers")
    public Mono<TransferResponse> createTransfer(@RequestBody TransferRequest request) {
        // Correlation context automatically created from HTTP headers
        return transferService.validateTransfer(request)
            .flatMap(this::checkAccountBalances)  // Context propagated automatically
            .flatMap(transferService::executeTransfer)  // Context propagated automatically
            .map(this::toTransferResponse);
    }

    private Mono<TransferRequest> checkAccountBalances(TransferRequest request) {
        // Service client automatically propagates correlation headers
        return accountServiceClient.get("/accounts/{id}/balance", AccountBalance.class)
            .withPathParam("id", request.getFromAccountId())
            .execute()
            .flatMap(fromBalance ->
                accountServiceClient.get("/accounts/{id}/balance", AccountBalance.class)
                    .withPathParam("id", request.getToAccountId())
                    .execute()
                    .map(toBalance -> validateBalances(request, fromBalance, toBalance))
            );
    }
}
```

#### Manual Context Management

```java
@Service
public class AuditService {
    private final CorrelationContext correlationContext;
    private final DomainEventPublisher eventPublisher;

    public Mono<Void> auditTransaction(TransactionEvent event) {
        // Create new correlation context for audit trail
        String auditCorrelationId = correlationContext.generateCorrelationId();
        String auditTraceId = correlationContext.generateTraceId();

        return correlationContext.withContext(
            auditCorrelationId,
            auditTraceId,
            () -> processAuditEvent(event)
                .flatMap(this::publishAuditEvent)
                .then()
        );
    }

    private Mono<AuditEvent> processAuditEvent(TransactionEvent event) {
        // Current correlation context available in logs
        log.info("Processing audit for transaction: {}", event.getTransactionId());

        return Mono.fromCallable(() -> AuditEvent.builder()
            .transactionId(event.getTransactionId())
            .auditType("TRANSACTION_COMPLETED")
            .timestamp(Instant.now())
            .correlationId(correlationContext.getCurrentCorrelationId())
            .traceId(correlationContext.getCurrentTraceId())
            .build());
    }

    private Mono<Void> publishAuditEvent(AuditEvent auditEvent) {
        // Event published with current correlation context
        return eventPublisher.publish(DomainEventEnvelope.builder()
            .topic("audit-events")
            .type("audit.transaction.completed")
            .key(auditEvent.getTransactionId())
            .payload(auditEvent)
            .timestamp(Instant.now())
            .build());
    }
}
```

#### Reactive Context Propagation

```java
@Service
public class ReactiveTransactionService {
    private final CorrelationContext correlationContext;

    public Mono<TransactionResult> processTransactionChain(TransactionRequest request) {
        return correlationContext.withContext(
            Mono.just(request)
                .flatMap(this::validateTransaction)
                .flatMap(this::reserveFunds)
                .flatMap(this::executeTransaction)
                .flatMap(this::confirmTransaction)
                .flatMap(this::notifyParties)
        );
    }

    private Mono<TransactionRequest> validateTransaction(TransactionRequest request) {
        // Context automatically available in reactive chain
        return Mono.fromCallable(() -> {
            log.info("Validating transaction: {} [correlation: {}]",
                request.getId(),
                correlationContext.getCurrentCorrelationId());
            return request;
        });
    }

    private Mono<TransactionRequest> reserveFunds(TransactionRequest request) {
        return Mono.fromCallable(() -> {
            log.info("Reserving funds for transaction: {} [trace: {}]",
                request.getId(),
                correlationContext.getCurrentTraceId());
            return request;
        });
    }
}
```

### Logging Integration

#### Automatic MDC Population

```java
@Service
public class LoggingExampleService {

    public Mono<String> processWithLogging(String data) {
        // MDC automatically populated with correlation context
        log.info("Starting processing"); // Includes correlationId and traceId

        return Mono.fromCallable(() -> {
            log.debug("Processing data: {}", data); // Context preserved
            return processData(data);
        })
        .doOnSuccess(result -> log.info("Processing completed successfully"))
        .doOnError(error -> log.error("Processing failed", error));
    }

    private String processData(String data) {
        log.trace("Internal processing step"); // Context preserved
        return data.toUpperCase();
    }
}
```

**Log Output Example:**
```
2025-09-09 10:15:30.123 INFO  [correlationId=550e8400-e29b-41d4-a716-446655440000, traceId=1234567890abcdef] - Starting processing
2025-09-09 10:15:30.125 DEBUG [correlationId=550e8400-e29b-41d4-a716-446655440000, traceId=1234567890abcdef] - Processing data: example
2025-09-09 10:15:30.127 TRACE [correlationId=550e8400-e29b-41d4-a716-446655440000, traceId=1234567890abcdef] - Internal processing step
2025-09-09 10:15:30.129 INFO  [correlationId=550e8400-e29b-41d4-a716-446655440000, traceId=1234567890abcdef] - Processing completed successfully
```

## lib-transactional-engine Integration Examples

> **Important**: The Firefly Common Domain Library does **NOT** implement saga orchestration. It only provides a bridge to publish step events from [lib-transactional-engine](https://github.com/firefly-oss/lib-transactional-engine) through the domain events infrastructure.

### Step Event Bridge Configuration

#### Auto-Configuration

```yaml
# application.yml - Step event bridge auto-configured
firefly:
  step-events:
    enabled: true
    topic: banking-step-events  # Default topic for step events
```

#### Manual Bridge Configuration

```java
@Configuration
public class StepEventConfig {

    @Bean
    public StepEventPublisherBridge stepEventBridge(DomainEventPublisher eventPublisher) {
        return new StepEventPublisherBridge(eventPublisher, "banking-step-events");
    }
}
```

### Saga Implementation (Using lib-transactional-engine)

#### Dependencies Required

```xml
<!-- Only lib-common-domain needed - includes lib-transactional-engine as dependency -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

> **Note**: lib-common-domain automatically includes lib-transactional-engine as a transitive dependency, so you don't need to add it separately.

#### Enable Transactional Engine

```java
@SpringBootApplication
@EnableTransactionalEngine  // From lib-transactional-engine
public class BankingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingApplication.class, args);
        // ✅ SagaEngine auto-configured
        // ✅ StepEventPublisherBridge auto-configured (from lib-common-domain)
        // ✅ Domain events infrastructure available for step events
    }
}
```

#### Money Transfer Saga Definition

```java
@Component
@Saga(name = "money-transfer-saga")  // From lib-transactional-engine
public class MoneyTransferSaga {

    private final AccountService accountService;
    private final NotificationService notificationService;

    @SagaStep(id = "validate-accounts", retry = 3, backoffMs = 1000)
    public Mono<ValidationResult> validateAccounts(@Input("transferRequest") TransferRequest request) {
        return accountService.validateAccountExists(request.getFromAccountId())
            .then(accountService.validateAccountExists(request.getToAccountId()))
            .then(accountService.validateSufficientFunds(request.getFromAccountId(), request.getAmount()))
            .thenReturn(new ValidationResult("VALID", request));
    }

    @SagaStep(id = "reserve-funds",
              dependsOn = "validate-accounts",
              compensate = "releaseReservation",
              timeoutMs = 30000)
    public Mono<ReservationResult> reserveFunds(
            @FromStep("validate-accounts") ValidationResult validation) {
        TransferRequest request = validation.getRequest();
        return accountService.reserveFunds(request.getFromAccountId(), request.getAmount());
    }

    @SagaStep(id = "transfer-funds",
              dependsOn = "reserve-funds",
              compensate = "reverseTransfer",
              timeoutMs = 30000)
    public Mono<TransferResult> transferFunds(
            @FromStep("validate-accounts") ValidationResult validation,
            @FromStep("reserve-funds") ReservationResult reservation) {
        TransferRequest request = validation.getRequest();
        return accountService.transferFunds(
            request.getFromAccountId(),
            request.getToAccountId(),
            request.getAmount(),
            reservation.getReservationId()
        );
    }

    @SagaStep(id = "send-notifications",
              dependsOn = "transfer-funds",
              retry = 2)  // Non-critical step
    public Mono<NotificationResult> sendNotifications(
            @FromStep("transfer-funds") TransferResult transfer) {
        return notificationService.sendTransferNotification(transfer)
            .onErrorReturn(new NotificationResult("FAILED", "Notification failed but transfer succeeded"));
    }

    // Compensation methods
    public Mono<Void> releaseReservation(@FromStep("reserve-funds") ReservationResult reservation) {
        return accountService.releaseReservation(reservation.getReservationId());
    }

    public Mono<Void> reverseTransfer(@FromStep("transfer-funds") TransferResult transfer) {
        return accountService.reverseTransfer(transfer.getTransactionId());
    }
}
```

#### Saga Execution

```java
@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final SagaEngine sagaEngine;  // From lib-transactional-engine

    @PostMapping
    public Mono<ResponseEntity<TransferResponse>> createTransfer(@RequestBody CreateTransferRequest request) {

        TransferRequest transferRequest = TransferRequest.builder()
            .fromAccountId(request.getFromAccountId())
            .toAccountId(request.getToAccountId())
            .amount(request.getAmount())
            .reference(request.getReference())
            .build();

        StepInputs inputs = StepInputs.of("transferRequest", transferRequest);

        return sagaEngine.execute("money-transfer-saga", inputs)
            .map(sagaResult -> {
                if (sagaResult.isSuccess()) {
                    TransferResult transfer = sagaResult.resultOf("transfer-funds", TransferResult.class)
                        .orElseThrow(() -> new IllegalStateException("Transfer result not found"));

                    return ResponseEntity.ok(TransferResponse.builder()
                        .transactionId(transfer.getTransactionId())
                        .status("COMPLETED")
                        .timestamp(Instant.now())
                        .build());
                } else {
                    throw new TransferFailedException("Transfer failed: " + sagaResult.failedSteps());
                }
            });
    }
}
```

#### Step Event Handling (Bridge Integration)

The StepEventPublisherBridge automatically publishes step events from lib-transactional-engine through the domain events infrastructure. You can handle these events using the same `@EventHandler` pattern:

```java
@EventHandler
@Component
public class SagaStepEventHandler {

    private final MeterRegistry meterRegistry;
    private final AlertingService alertingService;
    private final DomainEventPublisher eventPublisher;

    // Handle step events published by the bridge
    @EventListener(topic = "banking-step-events", type = "step.completed")
    public void handleStepCompleted(StepEventEnvelope stepEvent) {
        log.info("Saga step completed: {} in saga: {} [{}]",
            stepEvent.getStepId(),
            stepEvent.getSagaName(),
            stepEvent.getSagaId());

        // Extract step metadata (automatically added by lib-transactional-engine)
        Map<String, Object> metadata = stepEvent.getMetadata();
        long latencyMs = (Long) metadata.getOrDefault("step.latency_ms", 0L);
        int attempts = (Integer) metadata.getOrDefault("step.attempts", 1);
        String resultType = (String) metadata.getOrDefault("step.result_type", "SUCCESS");

        // Record metrics
        meterRegistry.timer("saga.step.duration",
            "saga", stepEvent.getSagaName(),
            "step", stepEvent.getStepId()
        ).record(latencyMs, TimeUnit.MILLISECONDS);

        if (attempts > 1) {
            meterRegistry.counter("saga.step.retries",
                "saga", stepEvent.getSagaName(),
                "step", stepEvent.getStepId()
            ).increment();
        }

        // Process based on step type
        switch (stepEvent.getStepId()) {
            case "validate-accounts":
                handleValidationCompleted(stepEvent);
                break;
            case "reserve-funds":
                handleReserveFundsCompleted(stepEvent);
                break;
            case "transfer-funds":
                handleTransferFundsCompleted(stepEvent);
                break;
            case "send-notifications":
                handleNotificationsCompleted(stepEvent);
                break;
        }
    }

    @EventListener(topic = "banking-step-events", type = "step.failed")
    public void handleStepFailed(StepEventEnvelope stepEvent) {
        log.error("Saga step failed: {} in saga: {} [{}]",
            stepEvent.getStepId(),
            stepEvent.getSagaName(),
            stepEvent.getSagaId());

        // Handle step failure
        alertingService.sendStepFailureAlert(stepEvent);

        // Record failure metrics
        meterRegistry.counter("saga.step.failures",
            "saga", stepEvent.getSagaName(),
            "step", stepEvent.getStepId()
        ).increment();

        // Trigger additional business logic based on failed step
        if ("transfer-funds".equals(stepEvent.getStepId())) {
            // Critical step failed - send immediate alert
            alertingService.sendCriticalTransferFailureAlert(stepEvent);
        }
    }

    @EventListener(topic = "banking-step-events", type = "compensation.completed")
    public void handleCompensationCompleted(StepEventEnvelope stepEvent) {
        log.info("Compensation completed for step: {} in saga: {} [{}]",
            stepEvent.getStepId(),
            stepEvent.getSagaName(),
            stepEvent.getSagaId());

        // Record compensation metrics
        meterRegistry.counter("saga.compensation.success",
            "saga", stepEvent.getSagaName(),
            "step", stepEvent.getStepId()
        ).increment();
    }

    private void handleReserveFundsCompleted(StepEventEnvelope stepEvent) {
        // The step event contains the result from the saga step
        // Note: stepEvent.getPayload() contains the StepEventEnvelope from lib-transactional-engine

        // Publish additional domain event for business processes
        eventPublisher.publish(DomainEventEnvelope.builder()
            .topic("banking-events")
            .type("funds.reserved.saga")
            .key(stepEvent.getSagaId())
            .payload(Map.of(
                "sagaId", stepEvent.getSagaId(),
                "stepId", stepEvent.getStepId(),
                "completedAt", stepEvent.getCompletedAt()
            ))
            .timestamp(stepEvent.getCompletedAt())
            .build());
    }

    private void handleTransferFundsCompleted(StepEventEnvelope stepEvent) {
        // Publish business event for completed transfer
        eventPublisher.publish(DomainEventEnvelope.builder()
            .topic("banking-events")
            .type("transfer.completed.saga")
            .key(stepEvent.getSagaId())
            .payload(Map.of(
                "sagaId", stepEvent.getSagaId(),
                "stepId", stepEvent.getStepId(),
                "completedAt", stepEvent.getCompletedAt()
            ))
            .timestamp(stepEvent.getCompletedAt())
            .build());
    }

    private void handleValidationCompleted(StepEventEnvelope stepEvent) {
        // Log validation completion
        log.debug("Account validation completed for saga: {}", stepEvent.getSagaId());
    }

    private void handleNotificationsCompleted(StepEventEnvelope stepEvent) {
        // Log notification completion (may have failed but saga continued)
        log.info("Notification step completed for saga: {}", stepEvent.getSagaId());
    }
}
```

### Integration Benefits

#### Unified Event Processing

The bridge allows you to handle both domain events and saga step events using the same infrastructure:

```java
@EventHandler
@Component
public class UnifiedEventProcessor {

    // Handle regular domain events
    @EventListener(topic = "banking-events", type = "account.created")
    public void handleAccountCreated(AccountCreatedEvent event) {
        log.info("Account created: {}", event.getAccountId());
        // Process account creation
        updateCustomerProfile(event);
    }

    // Handle saga step events using same infrastructure
    @EventListener(topic = "banking-step-events", type = "step.completed")
    public void handleSagaStepCompleted(StepEventEnvelope stepEvent) {
        log.info("Saga step completed: {} in {}", stepEvent.getStepId(), stepEvent.getSagaName());
        // Process step completion
        updateSagaMetrics(stepEvent);
    }

    // Handle both domain and step events for analytics
    @EventListener(topic = {"banking-events", "banking-step-events"})
    public void handleAllBankingEvents(Object event) {
        if (event instanceof AccountCreatedEvent) {
            analyticsService.recordAccountCreation((AccountCreatedEvent) event);
        } else if (event instanceof StepEventEnvelope) {
            analyticsService.recordSagaStepExecution((StepEventEnvelope) event);
        }
    }

    private void updateCustomerProfile(AccountCreatedEvent event) {
        // Business logic for account creation
    }

    private void updateSagaMetrics(StepEventEnvelope stepEvent) {
        // Record saga execution metrics
        meterRegistry.counter("saga.steps.total",
            "saga", stepEvent.getSagaName(),
            "step", stepEvent.getStepId()
        ).increment();
    }
}
```

#### Key Benefits

- **Single Infrastructure**: Both domain and saga step events use the same messaging infrastructure
- **Unified Configuration**: Same Kafka/RabbitMQ/SQS configuration for all events
- **Consistent Patterns**: Same `@EventHandler` and `@EventListener` annotations
- **Shared Resilience**: Circuit breakers and retries apply to step events
- **Correlation Context**: Automatic correlation propagation for step events
- **No Additional Setup**: Bridge auto-configured when both libraries are present

---

## Complete Banking Application Example

### Application Structure

```java
@SpringBootApplication
public class BankingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingApplication.class, args);
        // All components auto-configured:
        // ✅ CQRS Framework (CommandBus, QueryBus)
        // ✅ Domain Events (Publisher, Adapters)
        // ✅ Service Clients (REST, gRPC with Circuit Breakers)
        // ✅ Distributed Tracing (Correlation Context)
        // ✅ Step Event Bridge (lib-transactional-engine integration)
    }
}
```

### Configuration

```yaml
# application.yml - Complete banking service configuration
spring:
  application:
    name: banking-service
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: banking-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer

firefly:
  cqrs:
    enabled: true
    validation:
      enabled: true
    caching:
      enabled: true
      redis:
        enabled: true

  events:
    enabled: true
    adapter: kafka
    kafka:
      topic: banking-domain-events

  step-events:
    enabled: true
    topic: banking-step-events

  circuit-breaker:
    enabled: true
    failure-rate-threshold: 50.0
    minimum-number-of-calls: 10
    sliding-window-size: 100
    wait-duration-in-open-state: 60s

  tracing:
    enabled: true
    correlation-header: X-Correlation-ID
    trace-header: X-Trace-ID

logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [correlationId=%X{correlationId:-}, traceId=%X{traceId:-}] - %msg%n"
```

### Complete Money Transfer Flow

#### 1. Command Handler

```java
@CommandHandlerComponent
public class TransferMoneyCommandHandler extends BaseCommandHandler<TransferMoneyCommand, TransferResult> {

    private final AccountService accountService;
    private final TransferService transferService;
    private final DomainEventPublisher eventPublisher;

    @Override
    protected Mono<TransferResult> doHandle(TransferMoneyCommand command) {
        return validateTransfer(command)
            .flatMap(this::executeTransfer)
            .flatMap(this::publishTransferEvent);
    }

    private Mono<TransferMoneyCommand> validateTransfer(TransferMoneyCommand command) {
        return accountService.validateAccountExists(command.getFromAccountId())
            .then(accountService.validateAccountExists(command.getToAccountId()))
            .then(accountService.validateSufficientFunds(command.getFromAccountId(), command.getAmount()))
            .thenReturn(command);
    }

    private Mono<TransferResult> executeTransfer(TransferMoneyCommand command) {
        return transferService.processTransfer(TransferRequest.builder()
            .fromAccountId(command.getFromAccountId())
            .toAccountId(command.getToAccountId())
            .amount(command.getAmount())
            .reference(command.getReference())
            .build());
    }

    private Mono<TransferResult> publishTransferEvent(TransferResult result) {
        return eventPublisher.publish(DomainEventEnvelope.builder()
            .topic("banking-events")
            .type("transfer.completed")
            .key(result.getTransactionId())
            .payload(new TransferCompletedEvent(
                result.getTransactionId(),
                result.getFromAccountId(),
                result.getToAccountId(),
                result.getAmount(),
                Instant.now()
            ))
            .build())
            .thenReturn(result);
    }
}
```

#### 2. Query Handler

```java
@QueryHandlerComponent
@Cacheable(cacheNames = "transfer-history", key = "#query.accountId")
public class GetTransferHistoryQueryHandler extends BaseQueryHandler<GetTransferHistoryQuery, TransferHistoryResult> {

    private final TransferRepository transferRepository;

    @Override
    protected Mono<TransferHistoryResult> doHandle(GetTransferHistoryQuery query) {
        return transferRepository.findByAccountId(
                query.getAccountId(),
                query.getFromDate(),
                query.getToDate(),
                PageRequest.of(query.getPage(), query.getSize())
            )
            .collectList()
            .map(transfers -> TransferHistoryResult.builder()
                .accountId(query.getAccountId())
                .transfers(transfers)
                .totalCount(transfers.size())
                .build());
    }
}
```

#### 3. Service Layer with Circuit Breaker

```java
@Service
public class TransferService {
    private final ServiceClient accountServiceClient;
    private final ServiceClient notificationServiceClient;

    public TransferService() {
        this.accountServiceClient = ServiceClient.rest("account-service")
            .baseUrl("http://account-service:8080")
            .timeout(Duration.ofSeconds(30))
            .build();

        this.notificationServiceClient = ServiceClient.rest("notification-service")
            .baseUrl("http://notification-service:8080")
            .timeout(Duration.ofSeconds(15))
            .build();
    }

    public Mono<TransferResult> processTransfer(TransferRequest request) {
        return debitAccount(request.getFromAccountId(), request.getAmount())
            .flatMap(debitResult -> creditAccount(request.getToAccountId(), request.getAmount())
                .flatMap(creditResult -> createTransferRecord(request, debitResult, creditResult))
                .flatMap(this::sendNotifications)
                .onErrorResume(error -> compensateDebit(debitResult).then(Mono.error(error)))
            );
    }

    private Mono<AccountOperationResult> debitAccount(String accountId, BigDecimal amount) {
        return accountServiceClient.post("/accounts/{id}/debit", AccountOperationResult.class)
            .withPathParam("id", accountId)
            .withBody(new DebitRequest(amount))
            .execute();
    }

    private Mono<AccountOperationResult> creditAccount(String accountId, BigDecimal amount) {
        return accountServiceClient.post("/accounts/{id}/credit", AccountOperationResult.class)
            .withPathParam("id", accountId)
            .withBody(new CreditRequest(amount))
            .execute();
    }

    private Mono<TransferResult> sendNotifications(TransferResult result) {
        return notificationServiceClient.post("/notifications", Void.class)
            .withBody(new TransferNotificationRequest(result))
            .execute()
            .thenReturn(result)
            .onErrorResume(error -> {
                log.warn("Failed to send transfer notification: {}", error.getMessage());
                return Mono.just(result); // Non-critical failure
            });
    }
}
```

#### 4. Event Handler

```java
@EventHandler
@Component
public class TransferEventHandler {

    @EventListener(topic = "banking-events", type = "transfer.completed")
    public Mono<Void> handleTransferCompleted(TransferCompletedEvent event) {
        log.info("Transfer completed: {} from {} to {} amount {}",
            event.getTransactionId(),
            event.getFromAccountId(),
            event.getToAccountId(),
            event.getAmount());

        return updateAccountStatements(event)
            .then(updateAnalytics(event))
            .then(checkForFraud(event));
    }

    private Mono<Void> updateAccountStatements(TransferCompletedEvent event) {
        // Update account statements
        return statementService.addTransferEntry(event);
    }

    private Mono<Void> updateAnalytics(TransferCompletedEvent event) {
        // Update analytics
        return analyticsService.recordTransfer(event);
    }

    private Mono<Void> checkForFraud(TransferCompletedEvent event) {
        // Fraud detection
        return fraudService.analyzeTransfer(event);
    }
}
```

#### 5. REST Controller

```java
@RestController
@RequestMapping("/transfers")
public class TransferController {
    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @PostMapping
    public Mono<ResponseEntity<TransferResponse>> createTransfer(@RequestBody @Valid CreateTransferRequest request) {
        TransferMoneyCommand command = TransferMoneyCommand.builder()
            .fromAccountId(request.getFromAccountId())
            .toAccountId(request.getToAccountId())
            .amount(request.getAmount())
            .reference(request.getReference())
            .build();

        return commandBus.execute(command)
            .map(result -> ResponseEntity.ok(TransferResponse.builder()
                .transactionId(result.getTransactionId())
                .status("COMPLETED")
                .timestamp(Instant.now())
                .build()));
    }

    @GetMapping("/history/{accountId}")
    public Mono<ResponseEntity<TransferHistoryResponse>> getTransferHistory(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        GetTransferHistoryQuery query = GetTransferHistoryQuery.builder()
            .accountId(accountId)
            .page(page)
            .size(size)
            .build();

        return queryBus.execute(query)
            .map(result -> ResponseEntity.ok(TransferHistoryResponse.builder()
                .transfers(result.getTransfers())
                .totalCount(result.getTotalCount())
                .build()));
    }
}
```

This complete example demonstrates how all components of the Firefly Common Domain Library work together to create a robust, scalable banking application with enterprise-grade patterns and zero-boilerplate development experience.

---

## Summary

The Firefly Common Domain Library provides:

### 🎯 CQRS Framework
- **Single Approach**: Only one way to create handlers - extend base classes with annotations
- **Zero Boilerplate**: No type methods, caching methods, or validation setup needed
- **Focus on Business Logic**: Write only the `doHandle()` method
- **Everything Automatic**: Validation, logging, metrics, caching, error handling

### 📡 Domain Events
- **Multiple Adapters**: Kafka, RabbitMQ, SQS, Kinesis, ApplicationEvent support
- **Auto-Configuration**: Automatic adapter selection and configuration
- **Type Safety**: Full type safety for event payloads and handlers
- **Correlation Context**: Automatic context propagation across services

### 🌐 Service Clients
- **Unified API**: Same interface for REST and gRPC clients
- **Built-in Resilience**: Circuit breakers, timeouts, retries
- **Auto-Configuration**: Zero-setup service client creation
- **Reactive Support**: Full reactive programming support

### 🛡️ Resilience Patterns
- **Circuit Breakers**: Real state management with sliding window failure tracking
- **Auto-Protection**: Automatic circuit breaker protection for service clients
- **Monitoring**: Built-in metrics and state monitoring
- **Fallback Support**: Easy fallback pattern implementation

### 🔍 Distributed Tracing
- **Automatic Propagation**: Context propagation across services and events
- **MDC Integration**: Automatic logging context for all log statements
- **Reactive Support**: Context propagation through Reactor Context
- **Header Management**: Automatic header propagation in service calls

### ⚙️ lib-transactional-engine Integration
- **Bridge Pattern**: Step events from lib-transactional-engine published through domain events infrastructure
- **No Saga Implementation**: This library only provides the bridge - use lib-transactional-engine for saga orchestration
- **Unified Event Processing**: Same event handlers and infrastructure for domain and step events
- **Auto-Configuration**: Bridge automatically configured when both libraries are present
- **Rich Metadata**: Step events include execution metrics, attempts, and correlation data

### 🔧 Auto-Configuration
- **Zero Setup**: Everything auto-configured with sensible defaults
- **Conditional Beans**: Smart bean creation based on available dependencies
- **Property Driven**: Easy customization through application properties
- **Production Ready**: Built-in resilience, monitoring, and best practices

---
