# API Reference

Complete API reference for the Firefly Common Domain Library.

## CQRS Framework - Consolidated Zero-Boilerplate Implementation

The Firefly CQRS Framework provides a **single, consolidated approach** with **zero boilerplate code**. Everything is automatic: type detection, validation, caching, logging, metrics, and error handling.

### Core Philosophy

- **🎯 One Way to Do Things**: Only one approach - extend base classes with annotations
- **🚀 Zero Boilerplate**: No `getCommandType()`, `getResultType()`, or caching methods to override
- **⚡ Focus on Business Logic**: Write only the `doHandle()` method
- **📊 Everything Automatic**: Validation, logging, metrics, caching, error handling

### Core Interfaces

#### Command Interface

```java
public interface Command<R> {
    // Simple marker interface - no methods required
    // All metadata and validation handled automatically
}
```

#### Query Interface

```java
public interface Query<R> {
    // Simple marker interface - no methods required
    // All metadata and caching handled automatically
}
```

### THE ONLY WAY: Base Classes with Annotations

#### Command Handlers

```java
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountCreatedResult> {

    @Override
    protected Mono<AccountCreatedResult> doHandle(CreateAccountCommand command) {
        // Only business logic - everything else is automatic!
        return processAccount(command);
    }

    // ✅ NO BOILERPLATE NEEDED:
    // - No getCommandType() - automatically detected from generics
    // - No getResultType() - automatically detected from generics
    // - No validation setup - handled by annotation
    // - No metrics setup - handled by annotation
    // - No error handling - built-in
    // - No logging - built-in
}
```

#### Query Handlers

```java
@QueryHandlerComponent(cacheable = true, cacheTtl = 300, metrics = true)
public class GetAccountBalanceHandler extends QueryHandler<GetAccountBalanceQuery, AccountBalance> {

    @Override
    protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query) {
        // Only business logic - everything else is automatic!
        return retrieveBalance(query);
    }

    // ✅ NO BOILERPLATE NEEDED:
    // - No getQueryType() - automatically detected from generics
    // - No getResultType() - automatically detected from generics
    // - No supportsCaching() - handled by annotation
    // - No getCacheTtlSeconds() - handled by annotation
    // - No validation setup - handled by annotation
    // - No metrics setup - handled by annotation
}
### Annotations

#### @CommandHandlerComponent

```java
@CommandHandlerComponent(
    value = "",              // Spring component name (optional)
    timeout = 30000,         // Command timeout in milliseconds
    retries = 3,             // Number of retry attempts
    metrics = true,          // Enable metrics collection
    async = false,           // Process asynchronously
    priority = 0             // Execution priority
)
```

#### @QueryHandlerComponent

```java
@QueryHandlerComponent(
    value = "",              // Spring component name (optional)
    cacheable = true,        // Enable caching
    cacheTtl = 300,          // Cache TTL in seconds
    metrics = true,          // Enable metrics collection
    timeout = 15000          // Query timeout in milliseconds
)
```

### Bus Interfaces

#### CommandBus

```java
public interface CommandBus {
    <R> Mono<R> send(Command<R> command);

    // Registration handled automatically by Spring
    // No manual registration needed when using @CommandHandlerComponent
}
```

#### QueryBus

```java
public interface QueryBus {
    <R> Mono<R> query(Query<R> query);

    // Registration handled automatically by Spring
    // No manual registration needed when using @QueryHandlerComponent
}
```

## Complete Examples

### Command Example

**1. Define your Command:**
```java
import jakarta.validation.constraints.*;

public class CreateAccountCommand implements Command<AccountCreatedResult> {
    @NotBlank
    private final String customerId;

    @NotBlank
    private final String accountType;

    @NotNull
    @Positive
    private final BigDecimal initialBalance;

    public CreateAccountCommand(String customerId, String accountType, BigDecimal initialBalance) {
        this.customerId = customerId;
        this.accountType = accountType;
        this.initialBalance = initialBalance;
    }

    // Getters...
}
```

**2. Define your Result:**
```java
public class AccountCreatedResult {
    private final String accountNumber;
    private final String customerId;
    private final String accountType;
    private final BigDecimal initialBalance;
    private final String status;
    private final LocalDateTime createdAt;

    // Constructor and getters...
}
```

**3. Create your Handler (THE ONLY WAY):**
```java
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountCreatedResult> {

    @Override
    protected Mono<AccountCreatedResult> doHandle(CreateAccountCommand command) {
        // Only business logic - everything else is automatic!
        String accountNumber = generateAccountNumber();

        return accountService.createAccount(command)
            .map(account -> new AccountCreatedResult(
                accountNumber,
                command.getCustomerId(),
                command.getAccountType(),
                command.getInitialBalance(),
                "ACTIVE",
                LocalDateTime.now()
            ));
    }
}
```

**4. Use the CommandBus:**
```java
@Service
public class AccountService {

    @Autowired
    private CommandBus commandBus;

    public Mono<AccountCreatedResult> createAccount(String customerId, String type, BigDecimal amount) {
        CreateAccountCommand command = new CreateAccountCommand(customerId, type, amount);
        return commandBus.send(command);
    }
}
```

### Query Example

**1. Define your Query:**
```java
import jakarta.validation.constraints.*;

public class GetAccountBalanceQuery implements Query<AccountBalance> {
    @NotBlank
    private final String accountNumber;

    @NotBlank
    private final String customerId;

    public GetAccountBalanceQuery(String accountNumber, String customerId) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
    }

    // Getters...
}
```

**2. Define your Result:**
```java
public class AccountBalance {
    private final String accountNumber;
    private final BigDecimal currentBalance;
    private final BigDecimal availableBalance;
    private final String currency;
    private final LocalDateTime lastUpdated;

    // Constructor and getters...
}
```

**3. Create your Handler (THE ONLY WAY):**
```java
@QueryHandlerComponent(cacheable = true, cacheTtl = 300, metrics = true)
public class GetAccountBalanceHandler extends QueryHandler<GetAccountBalanceQuery, AccountBalance> {

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
}
```

**4. Use the QueryBus:**
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

## Built-in Features

### Automatic Features (Zero Configuration)

- **✅ Type Detection**: Automatic from generics - no `getCommandType()` or `getQueryType()` needed
- **✅ Validation**: Jakarta Bean Validation annotations processed automatically
- **✅ Logging**: Structured logging with correlation IDs and timing
- **✅ Metrics**: Micrometer metrics for success/failure rates and timing
- **✅ Error Handling**: Automatic retry logic and error wrapping
- **✅ Correlation**: Request correlation across the entire flow

### Caching (Query Handlers Only)

Caching is completely automatic based on annotation configuration:

```java
@QueryHandlerComponent(
    cacheable = true,        // Enable caching
    cacheTtl = 300          // Cache for 5 minutes
)
public class MyQueryHandler extends QueryHandler<MyQuery, MyResult> {
    // No caching methods needed - handled automatically!
}
```

## Cache Configuration

The CQRS framework supports two cache types for query results:

### Local Cache (Default)
- **Type**: In-memory cache using `ConcurrentMapCacheManager`
- **Scope**: Single application instance
- **Configuration**: No additional setup required
- **Use Case**: Development, testing, single-instance deployments

### Redis Cache (Distributed)
- **Type**: Distributed cache using Redis
- **Scope**: Shared across multiple application instances
- **Configuration**: Requires Redis server and explicit enablement
- **Use Case**: Production, multi-instance deployments

### Switching from Local to Redis Cache

**Step 1: Enable Redis Cache Type**
```yaml
firefly:
  cqrs:
    query:
      cache:
        type: REDIS              # Switch from LOCAL (default) to REDIS
```

**Step 2: Enable Redis Connection**
```yaml
firefly:
  cqrs:
    query:
      cache:
        type: REDIS
        redis:
          enabled: true          # Enable Redis (disabled by default)
```

**Step 3: Configure Redis Connection (Optional)**
```yaml
firefly:
  cqrs:
    query:
      cache:
        type: REDIS
        redis:
          enabled: true
          host: localhost        # Default: localhost
          port: 6379            # Default: 6379
          database: 0           # Default: 0
          password: your-password # Optional
          timeout: 2s           # Default: 2s
          key-prefix: "firefly:cqrs:" # Default: "firefly:cqrs:"
          statistics: true      # Default: true (reserved for future use)
```

### Complete Redis Configuration Example

```yaml
# Production Redis setup
firefly:
  cqrs:
    enabled: true
    query:
      caching-enabled: true     # Enable query caching (default: true)
      cache-ttl: 30m           # Cache TTL (default: 15m)
      cache:
        type: REDIS            # Use Redis instead of local cache
        redis:
          enabled: true        # Must be explicitly enabled
          host: redis.production.local
          port: 6379
          database: 1
          password: ${REDIS_PASSWORD}
          timeout: 5s
          key-prefix: "banking:cqrs:"
          statistics: true      # Reserved for future use
```

### Important Notes

- **Redis is disabled by default**: No Redis connections are attempted unless `firefly.cqrs.query.cache.redis.enabled=true`
- **Graceful fallback**: If Redis is configured but unavailable, the framework falls back to local cache with a warning
- **No Redis dependency required**: When using local cache only, Redis dependencies are optional
- **Cache keys**: Generated automatically based on query class name and parameters
- **Serialization**: Uses JSON serialization for cache values and string serialization for keys

### Cache Configuration Properties Reference

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `firefly.cqrs.query.caching-enabled` | boolean | `true` | Enable/disable query caching globally |
| `firefly.cqrs.query.cache-ttl` | Duration | `15m` | Default cache TTL for all queries |
| `firefly.cqrs.query.cache.type` | enum | `LOCAL` | Cache type: `LOCAL` or `REDIS` |
| `firefly.cqrs.query.cache.redis.enabled` | boolean | `false` | Enable Redis cache (must be explicitly enabled) |
| `firefly.cqrs.query.cache.redis.host` | string | `localhost` | Redis server hostname |
| `firefly.cqrs.query.cache.redis.port` | int | `6379` | Redis server port |
| `firefly.cqrs.query.cache.redis.database` | int | `0` | Redis database index |
| `firefly.cqrs.query.cache.redis.password` | string | `null` | Redis password (optional) |
| `firefly.cqrs.query.cache.redis.timeout` | Duration | `2s` | Redis connection timeout |
| `firefly.cqrs.query.cache.redis.key-prefix` | string | `firefly:cqrs:` | Prefix for all cache keys |
| `firefly.cqrs.query.cache.redis.statistics` | boolean | `true` | Enable cache statistics collection (reserved for future use) |

### Cache Migration Guide

**From Local to Redis (Zero Downtime):**

1. **Add Redis dependency** (if not already present):
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-data-redis</artifactId>
   </dependency>
   ```

2. **Configure Redis** without enabling it first:
   ```yaml
   firefly:
     cqrs:
       query:
         cache:
           redis:
             host: your-redis-host
             port: 6379
             # enabled: false (default)
   ```

3. **Switch cache type** to REDIS:
   ```yaml
   firefly:
     cqrs:
       query:
         cache:
           type: REDIS  # Still uses local cache until Redis is enabled
   ```

4. **Enable Redis** when ready:
   ```yaml
   firefly:
     cqrs:
       query:
         cache:
           type: REDIS
           redis:
             enabled: true  # Now switches to Redis
   ```

### Validation

Validation is automatic using Jakarta Bean Validation:

```java
public class MyCommand implements Command<MyResult> {
    @NotBlank(message = "Name is required")
    private final String name;

    @NotNull
    @Positive(message = "Amount must be positive")
    private final BigDecimal amount;

    // No validation methods needed - handled automatically!
}
```

## Spring Integration

### Auto-Configuration

The framework auto-configures when Spring Boot detects it on the classpath:

```java
@SpringBootApplication
public class MyApplication {
    // CommandBus and QueryBus beans are automatically available
    // MeterRegistry is auto-configured for metrics (SimpleMeterRegistry by default)
    // Validation, caching, and handler discovery are automatic
}
```

**What's Auto-Configured:**
- **CommandBus** and **QueryBus** beans
- **MeterRegistry** for metrics (SimpleMeterRegistry if none exists)
- **Validation** support (if Jakarta Validation is available)
- **Caching** support for queries
- **Handler discovery** via Spring component scanning

### Handler Discovery

Handlers are automatically discovered and registered:

```java
@CommandHandlerComponent  // Automatically discovered and registered
public class MyCommandHandler extends CommandHandler<MyCommand, MyResult> {
    // Implementation...
}

@QueryHandlerComponent   // Automatically discovered and registered
public class MyQueryHandler extends QueryHandler<MyQuery, MyResult> {
    // Implementation...
}
```

## Key Principles

### 1. Single Approach
- **Only one way** to create handlers: extend base classes with annotations
- **No interfaces to implement** - just extend `CommandHandler` or `QueryHandler`
- **No boilerplate methods** - everything is automatic

### 2. Zero Boilerplate
- **No type methods**: `getCommandType()`, `getQueryType()`, `getResultType()` - all automatic
- **No caching methods**: `supportsCaching()`, `getCacheTtlSeconds()` - handled by annotations
- **No validation setup**: Jakarta Bean Validation annotations work automatically
- **No metrics setup**: Micrometer metrics are **auto-configured by default** (no MeterRegistry bean required)

### 3. Annotation-Driven Configuration
- **@CommandHandlerComponent**: Configures timeout, retries, metrics
- **@QueryHandlerComponent**: Configures caching, TTL, metrics
- **Jakarta Validation**: `@NotNull`, `@NotBlank`, `@Positive`, etc.

### 4. Focus on Business Logic
- **Only implement `doHandle()`**: Contains your business logic
- **Everything else is automatic**: Validation, logging, metrics, caching, error handling

## Migration from Old Approaches

If you have existing handlers using interfaces or manual type methods, migrate to the consolidated approach:

**❌ Old Way (Don't Use):**
```java
// DON'T DO THIS - Multiple ways to do the same thing
public class OldHandler implements CommandHandler<MyCommand, MyResult> {
    @Override
    public Mono<MyResult> handle(MyCommand command) { ... }

    @Override
    public Class<MyCommand> getCommandType() { return MyCommand.class; }
}
```

**✅ New Way (Only Way):**
```java
// DO THIS - Single, consolidated approach
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class NewHandler extends CommandHandler<MyCommand, MyResult> {
    @Override
    protected Mono<MyResult> doHandle(MyCommand command) {
        // Only business logic - everything else automatic!
    }
}
```
