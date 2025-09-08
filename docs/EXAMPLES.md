# Examples

Real working examples based on the actual codebase implementation, demonstrating the **consolidated zero-boilerplate CQRS framework**.

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

## Summary

The Firefly CQRS Framework provides:

- **🎯 Single Approach**: Only one way to create handlers - extend base classes with annotations
- **🚀 Zero Boilerplate**: No type methods, caching methods, or validation setup needed
- **⚡ Focus on Business Logic**: Write only the `doHandle()` method
- **📊 Everything Automatic**: Validation, logging, metrics, caching, error handling
- **🔧 Annotation-Driven**: All configuration through `@CommandHandlerComponent` and `@QueryHandlerComponent`
- **✅ Production Ready**: Built-in resilience, monitoring, and best practices
