# API Reference

Complete API reference for the Firefly Common Domain Library.

## Table of Contents

1. [CQRS Framework](#cqrs-framework)
2. [ExecutionContext](#executioncontext)
3. [Domain Events](#domain-events)
4. [Service Client Framework](#service-client-framework)
5. [Resilience Patterns](#resilience-patterns)
6. [Distributed Tracing](#distributed-tracing)
7. [lib-transactional-engine Integration](#lib-transactional-engine-integration)

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

---

## ExecutionContext

The ExecutionContext provides a way to pass additional values to command and query handlers that are not part of the command/query itself. This is essential for multi-tenant applications, user authentication, feature flags, and request-specific metadata.

### Core Interface

#### ExecutionContext

```java
public interface ExecutionContext {
    // User and tenant context
    String getUserId();
    String getTenantId();
    String getOrganizationId();
    String getSessionId();
    String getRequestId();

    // Request metadata
    String getSource();
    String getClientIp();
    String getUserAgent();
    Instant getCreatedAt();

    // Feature flags
    boolean getFeatureFlag(String flagName, boolean defaultValue);
    Map<String, Boolean> getFeatureFlags();
    boolean hasFeatureFlags();

    // Custom properties
    Optional<Object> getProperty(String key);
    <T> Optional<T> getProperty(String key, Class<T> type);
    Map<String, Object> getProperties();
    boolean hasProperties();

    // Factory methods
    static Builder builder();
    static ExecutionContext empty();
}
```

#### ExecutionContext.Builder

```java
public interface Builder {
    Builder withUserId(String userId);
    Builder withTenantId(String tenantId);
    Builder withOrganizationId(String organizationId);
    Builder withSessionId(String sessionId);
    Builder withRequestId(String requestId);
    Builder withSource(String source);
    Builder withClientIp(String clientIp);
    Builder withUserAgent(String userAgent);
    Builder withFeatureFlag(String flagName, boolean value);
    Builder withProperty(String key, Object value);
    ExecutionContext build();
}
```

### Enhanced Bus Interfaces

#### CommandBus with ExecutionContext

```java
public interface CommandBus {
    // Standard method
    <R> Mono<R> send(Command<R> command);

    // Enhanced method with ExecutionContext
    <R> Mono<R> send(Command<R> command, ExecutionContext context);
}
```

#### QueryBus with ExecutionContext

```java
public interface QueryBus {
    // Standard method
    <R> Mono<R> query(Query<R> query);

    // Enhanced method with ExecutionContext
    <R> Mono<R> query(Query<R> query, ExecutionContext context);
}
```

### Enhanced Handler Base Classes

#### CommandHandler with ExecutionContext Support

```java
public abstract class CommandHandler<C extends Command<R>, R> {
    // Standard handle method
    public final Mono<R> handle(C command);

    // Enhanced handle method with ExecutionContext
    public final Mono<R> handle(C command, ExecutionContext context);

    // Standard doHandle method (must be implemented)
    protected abstract Mono<R> doHandle(C command);

    // Optional doHandle method with ExecutionContext
    protected Mono<R> doHandle(C command, ExecutionContext context) {
        return doHandle(command); // Default implementation ignores context
    }
}
```

#### QueryHandler with ExecutionContext Support

```java
public abstract class QueryHandler<Q extends Query<R>, R> {
    // Standard handle method
    public final Mono<R> handle(Q query);

    // Enhanced handle method with ExecutionContext
    public final Mono<R> handle(Q query, ExecutionContext context);

    // Standard doHandle method (must be implemented)
    protected abstract Mono<R> doHandle(Q query);

    // Optional doHandle method with ExecutionContext
    protected Mono<R> doHandle(Q query, ExecutionContext context) {
        return doHandle(query); // Default implementation ignores context
    }
}
```

### Context-Aware Handler Classes

#### ContextAwareCommandHandler

For handlers that always require ExecutionContext:

```java
public abstract class ContextAwareCommandHandler<C extends Command<R>, R>
    extends CommandHandler<C, R> {

    // Standard doHandle throws UnsupportedOperationException
    @Override
    protected final Mono<R> doHandle(C command) {
        throw new UnsupportedOperationException(
            "ContextAwareCommandHandler requires ExecutionContext. " +
            "Use CommandBus.send(command, context) instead"
        );
    }

    // Must implement this method with ExecutionContext
    @Override
    protected abstract Mono<R> doHandle(C command, ExecutionContext context);
}
```

#### ContextAwareQueryHandler

For handlers that always require ExecutionContext:

```java
public abstract class ContextAwareQueryHandler<Q extends Query<R>, R>
    extends QueryHandler<Q, R> {

    // Standard doHandle throws UnsupportedOperationException
    @Override
    protected final Mono<R> doHandle(Q query) {
        throw new UnsupportedOperationException(
            "ContextAwareQueryHandler requires ExecutionContext. " +
            "Use QueryBus.query(query, context) instead"
        );
    }

    // Must implement this method with ExecutionContext
    @Override
    protected abstract Mono<R> doHandle(Q query, ExecutionContext context);
}
```

### Usage Patterns

#### Creating ExecutionContext

```java
// Minimal context
ExecutionContext context = ExecutionContext.builder()
    .withUserId("user-123")
    .withTenantId("tenant-456")
    .build();

// Full context
ExecutionContext context = ExecutionContext.builder()
    .withUserId("user-123")
    .withTenantId("tenant-456")
    .withOrganizationId("org-789")
    .withSessionId("session-abc")
    .withRequestId("request-def")
    .withSource("mobile-app")
    .withClientIp("192.168.1.100")
    .withUserAgent("Mozilla/5.0")
    .withFeatureFlag("premium-features", true)
    .withFeatureFlag("enhanced-view", false)
    .withProperty("priority", "HIGH")
    .withProperty("channel", "MOBILE")
    .withProperty("customData", 42)
    .build();

// Empty context
ExecutionContext context = ExecutionContext.empty();
```

#### Using with Commands

```java
// Send command with context
commandBus.send(transferCommand, context)
    .subscribe(result -> log.info("Transfer completed: {}", result));
```

#### Using with Queries

```java
// Query with context
queryBus.query(balanceQuery, context)
    .subscribe(balance -> log.info("Balance: {}", balance));
```

---

## Domain Events

The Domain Events framework provides a unified, adapter-based approach for publishing and consuming events across different messaging infrastructures.

### Core Components

#### DomainEventEnvelope

The standard envelope for all domain events:

```java
public final class DomainEventEnvelope {
    private String topic;           // Event topic/stream
    private String type;            // Event type identifier
    private String key;             // Partition/routing key
    private Object payload;         // Event payload
    private Instant timestamp;      // Event timestamp
    private Map<String, Object> headers;   // Transport headers
    private Map<String, Object> metadata;  // Event metadata
}
```

#### DomainEventPublisher

Core interface for publishing events:

```java
public interface DomainEventPublisher {
    Mono<Void> publish(DomainEventEnvelope envelope);
}
```

### Event Publishing

#### Programmatic Publishing

```java
@Service
public class AccountService {
    private final DomainEventPublisher eventPublisher;

    public Mono<Account> createAccount(CreateAccountRequest request) {
        return processAccountCreation(request)
            .flatMap(account -> {
                DomainEventEnvelope event = DomainEventEnvelope.builder()
                    .topic("banking-events")
                    .type("account.created")
                    .key(account.getId())
                    .payload(new AccountCreatedEvent(account))
                    .timestamp(Instant.now())
                    .build();

                return eventPublisher.publish(event)
                    .thenReturn(account);
            });
    }
}
```

#### Annotation-Based Publishing

```java
@Service
public class TransferService {

    @EventPublisher(
        topic = "banking-events",
        type = "transfer.completed",
        key = "#result.transactionId"
    )
    public Mono<TransferResult> processTransfer(TransferRequest request) {
        // Business logic - event published automatically after success
        return executeTransfer(request);
    }
}
```

### Messaging Adapters

The framework supports multiple messaging infrastructures with automatic adapter selection:

#### Adapter Priority (AUTO mode)
1. **Kafka** (if KafkaTemplate available)
2. **RabbitMQ** (if ConnectionFactory available)
3. **AWS Kinesis** (if KinesisAsyncClient available)
4. **AWS SQS** (if SqsAsyncClient available)
5. **Spring ApplicationEvent** (fallback)

#### Configuration

```yaml
firefly:
  events:
    enabled: true
    adapter: auto  # AUTO, KAFKA, RABBIT, SQS, KINESIS, APPLICATION_EVENT, NOOP

    # Kafka configuration
    kafka:
      topic: banking-domain-events
      partition-key: "${key}"

    # RabbitMQ configuration
    rabbit:
      exchange: banking.events
      routing-key: "${type}"

    # AWS SQS configuration
    sqs:
      queue-name: banking-events-queue
      message-group-id: "${key}"

    # AWS Kinesis configuration
    kinesis:
      stream-name: banking-events-stream
      partition-key: "${key}"
```

### Event Consumption

#### Event Handler Registration

```java
@EventHandler
@Component
public class AccountEventHandler {

    @EventListener(topic = "banking-events", type = "account.created")
    public void handleAccountCreated(AccountCreatedEvent event) {
        // Process account creation event
        log.info("Account created: {}", event.getAccountId());
    }

    @EventListener(topic = "banking-events", type = "transfer.completed")
    public Mono<Void> handleTransferCompleted(TransferCompletedEvent event) {
        // Reactive event processing
        return updateAccountBalances(event);
    }
}
```

### Available Adapters

#### Kafka Adapter
- **Publisher**: `KafkaDomainEventPublisher`
- **Subscriber**: `KafkaDomainEventsSubscriber`
- **Features**: Partitioning, ordering guarantees, high throughput

#### RabbitMQ Adapter
- **Publisher**: `RabbitMqDomainEventPublisher`
- **Subscriber**: `RabbitMqDomainEventsSubscriber`
- **Features**: Routing, exchanges, reliable delivery

#### AWS SQS Adapter
- **Publisher**: `SqsAsyncClientDomainEventPublisher`
- **Features**: FIFO queues, message deduplication, cloud-native

#### AWS Kinesis Adapter
- **Publisher**: `KinesisDomainEventPublisher`
- **Features**: Real-time streaming, sharding, analytics integration

#### Application Event Adapter
- **Publisher**: `ApplicationEventDomainEventPublisher`
- **Features**: In-process events, Spring integration, testing

## Service Client Framework

The Service Client Framework provides a unified, reactive interface for service-to-service communication with built-in resilience patterns.

### Core Interface

#### ServiceClient

Unified interface for all service communication types:

```java
public interface ServiceClient {
    // Static factory methods
    static RestClientBuilder rest(String serviceName);
    static <T> GrpcClientBuilder<T> grpc(String serviceName, Class<T> stubType);

    // HTTP methods
    <R> RequestBuilder<R> get(String path, Class<R> responseType);
    <R> RequestBuilder<R> post(String path, Class<R> responseType);
    <R> RequestBuilder<R> put(String path, Class<R> responseType);
    <R> RequestBuilder<R> delete(String path, Class<R> responseType);

    // Streaming operations
    <R> Flux<R> stream(String path, Class<R> responseType);

    // Health and lifecycle
    boolean isReady();
    Mono<Void> healthCheck();
    void shutdown();
}
```

### REST Service Clients

#### Basic Usage

```java
@Service
public class UserService {
    private final ServiceClient userServiceClient;

    public UserService() {
        this.userServiceClient = ServiceClient.rest("user-service")
            .baseUrl("http://user-service:8080")
            .timeout(Duration.ofSeconds(30))
            .build();
    }

    public Mono<User> getUser(String userId) {
        return userServiceClient.get("/users/{id}", User.class)
            .withPathParam("id", userId)
            .execute();
    }

    public Mono<User> createUser(CreateUserRequest request) {
        return userServiceClient.post("/users", User.class)
            .withBody(request)
            .withHeader("Content-Type", "application/json")
            .execute();
    }
}
```

#### Advanced Configuration

```java
ServiceClient client = ServiceClient.rest("payment-service")
    .baseUrl("https://payment-service.example.com")
    .timeout(Duration.ofSeconds(45))
    .maxConnections(100)
    .defaultHeader("Authorization", "Bearer " + token)
    .defaultHeader("X-API-Version", "v2")
    .circuitBreakerManager(circuitBreakerManager)
    .build();
```

### gRPC Service Clients

#### Basic Usage

```java
@Service
public class AccountService {
    private final ServiceClient accountServiceClient;

    public AccountService() {
        this.accountServiceClient = ServiceClient.grpc("account-service", AccountServiceGrpc.AccountServiceBlockingStub.class)
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
}
```

#### Streaming Operations

```java
public Flux<TransactionEvent> streamTransactions(String accountId) {
    StreamTransactionsRequest request = StreamTransactionsRequest.newBuilder()
        .setAccountId(accountId)
        .build();

    return accountServiceClient.executeStream(stub ->
        Flux.fromIterable(() -> stub.streamTransactions(request))
    );
}
```

### Request Builder Pattern

The framework uses a fluent builder pattern for constructing requests:

```java
// Path parameters
.withPathParam("id", userId)
.withPathParams(Map.of("id", userId, "type", "premium"))

// Query parameters
.withQueryParam("limit", 10)
.withQueryParams(Map.of("page", 1, "size", 20))

// Headers
.withHeader("X-Correlation-ID", correlationId)
.withHeaders(Map.of("Accept", "application/json"))

// Request body
.withBody(requestObject)

// Execute
.execute()  // Returns Mono<T>
```

### Auto-Configuration

The framework provides automatic configuration with sensible defaults:

```yaml
firefly:
  service-client:
    enabled: true
    circuit-breaker:
      failure-rate-threshold: 50.0
      minimum-number-of-calls: 5
      sliding-window-size: 10
      wait-duration-in-open-state: 30s
      permitted-number-of-calls-in-half-open-state: 3
      call-timeout: 10s
      automatic-transition-from-open-to-half-open-enabled: true
```

### Circuit Breaker Integration

All service clients automatically include circuit breaker protection:

```java
// Circuit breaker is automatically applied
Mono<User> user = userServiceClient.get("/users/{id}", User.class)
    .withPathParam("id", userId)
    .execute(); // Circuit breaker protection applied automatically
```

## Resilience Patterns

The library provides comprehensive resilience patterns for building fault-tolerant distributed systems.

### Circuit Breaker Manager

Enhanced circuit breaker implementation with real state management:

#### Core Interface

```java
public class CircuitBreakerManager {
    // Execute operation with circuit breaker protection
    public <T> Mono<T> executeWithCircuitBreaker(String serviceName, Supplier<Mono<T>> operation);

    // Get current circuit breaker state
    public CircuitBreakerState getState(String serviceName);

    // Get circuit breaker metrics
    public CircuitBreakerMetrics getMetrics(String serviceName);

    // Force state transition (for testing/admin)
    public void transitionTo(String serviceName, CircuitBreakerState newState);
}
```

#### Circuit Breaker States

```java
public enum CircuitBreakerState {
    CLOSED,     // Normal operation - requests allowed
    OPEN,       // Service unavailable - requests rejected
    HALF_OPEN   // Testing recovery - limited requests allowed
}
```

#### Usage Example

```java
@Service
public class PaymentService {
    private final CircuitBreakerManager circuitBreakerManager;

    public Mono<PaymentResult> processPayment(PaymentRequest request) {
        return circuitBreakerManager.executeWithCircuitBreaker("payment-gateway",
            () -> callPaymentGateway(request)
        );
    }
}
```

#### Configuration

```java
@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.builder()
            .failureRateThreshold(50.0)           // 50% failure rate threshold
            .minimumNumberOfCalls(5)              // Minimum calls before evaluation
            .slidingWindowSize(10)                // Sliding window size
            .waitDurationInOpenState(Duration.ofSeconds(30))  // Wait before half-open
            .permittedNumberOfCallsInHalfOpenState(3)         // Calls in half-open
            .callTimeout(Duration.ofSeconds(10))              // Individual call timeout
            .build();
    }
}
```

### Predefined Configurations

#### High Availability Configuration

```java
CircuitBreakerConfig config = CircuitBreakerConfig.highAvailabilityConfig();
// failureRateThreshold: 30%
// minimumNumberOfCalls: 3
// waitDurationInOpenState: 30s
```

#### Fault Tolerant Configuration

```java
CircuitBreakerConfig config = CircuitBreakerConfig.faultTolerantConfig();
// failureRateThreshold: 70%
// minimumNumberOfCalls: 10
// waitDurationInOpenState: 2 minutes
```

### Advanced Resilience Manager

For complex scenarios requiring multiple resilience patterns:

```java
public class AdvancedResilienceManager {
    // Apply multiple resilience patterns
    public <T> Mono<T> applyResilience(String serviceName, Mono<T> operation, ResilienceConfig config);
}
```

#### Resilience Patterns Included

- **Circuit Breaker**: Prevents cascading failures
- **Bulkhead Isolation**: Resource isolation
- **Rate Limiting**: Controls request throughput
- **Adaptive Timeout**: Dynamic timeout adjustment
- **Load Shedding**: Drops requests under high load

#### Usage Example

```java
ResilienceConfig config = new ResilienceConfig(
    50,                           // maxConcurrentCalls
    Duration.ofSeconds(5),        // maxWaitTime
    100.0,                        // requestsPerSecond
    20,                           // burstCapacity
    Duration.ofSeconds(1),        // baseTimeout
    Duration.ofSeconds(10)        // maxTimeout
);

return advancedResilienceManager.applyResilience("critical-service",
    () -> callCriticalService(request), config);
```

### Sliding Window Implementation

The circuit breaker uses a sliding window for failure rate calculation:

```java
public class SlidingWindow {
    // Record successful call
    public void recordSuccess();

    // Record failed call
    public void recordFailure();

    // Get current failure rate
    public double getFailureRate();

    // Check if window has sufficient data
    public boolean hasSufficientData(int minimumCalls);
}
```

### Retry Configuration

Domain events include retry mechanisms with exponential backoff:

```java
@Bean("domainEventsRetryTemplate")
public RetryTemplate domainEventsRetryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();

    // 3 retries with exponential backoff
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions);
    retryTemplate.setRetryPolicy(retryPolicy);

    // Exponential backoff: 1s, 2s, 4s, max 30s
    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setInitialInterval(1000L);
    backOffPolicy.setMultiplier(2.0);
    backOffPolicy.setMaxInterval(30000L);
    retryTemplate.setBackOffPolicy(backOffPolicy);

    return retryTemplate;
}
```

## Distributed Tracing

The library provides comprehensive distributed tracing capabilities for tracking requests across microservices.

### CorrelationContext

Central component for managing correlation and trace IDs:

#### Core Interface

```java
@Component
public class CorrelationContext {
    // Generate new IDs
    public String generateCorrelationId();
    public String generateTraceId();

    // Set current context
    public void setCorrelationId(String correlationId);
    public void setTraceId(String traceId);

    // Get current context
    public String getCorrelationId();
    public String getTraceId();

    // Get or create if missing
    public String getOrCreateCorrelationId();
    public String getOrCreateTraceId();

    // Context propagation
    public ConcurrentMap<String, Object> createContextHeaders();
    public void propagateContext(Map<String, Object> headers);

    // Reactive context support
    public <T> Mono<T> withContext(Mono<T> mono);
    public <T> Flux<T> withContext(Flux<T> flux);
}
```

#### Usage in Services

```java
@Service
public class OrderService {
    private final CorrelationContext correlationContext;
    private final ServiceClient paymentClient;

    public Mono<Order> processOrder(OrderRequest request) {
        // Set correlation context
        correlationContext.setCorrelationId(correlationContext.generateCorrelationId());
        correlationContext.setTraceId(correlationContext.generateTraceId());

        return createOrder(request)
            .flatMap(order ->
                // Context automatically propagated to downstream services
                paymentClient.post("/payments", PaymentResult.class)
                    .withBody(createPaymentRequest(order))
                    .execute()
                    .map(payment -> order.withPayment(payment))
            );
    }
}
```

#### Reactive Context Propagation

```java
public Mono<Result> processWithTracing(Request request) {
    return correlationContext.withContext(
        businessLogic(request)
            .flatMap(this::callDownstreamService)
            .flatMap(this::processResult)
    );
}
```

### Automatic Context Propagation

#### HTTP Headers

The framework automatically adds tracing headers to outbound requests:

```
X-Correlation-ID: 550e8400-e29b-41d4-a716-446655440000
X-Trace-ID: 1234567890abcdef1234567890abcdef
```

#### Event Headers

Domain events automatically include tracing context:

```java
// Automatically added to all published events
Map<String, Object> headers = {
    "X-Correlation-ID": "550e8400-e29b-41d4-a716-446655440000",
    "X-Trace-ID": "1234567890abcdef1234567890abcdef",
    "timestamp": 1640995200000,
    "source": "domain-events"
}
```

#### MDC Integration

Correlation context is automatically added to SLF4J MDC:

```java
// Automatically available in log statements
log.info("Processing order");
// Output: [correlationId=550e8400-e29b-41d4-a716-446655440000] [traceId=1234567890abcdef] Processing order
```

### Context Storage

#### Thread-Local Storage

```java
// Thread-local storage for synchronous operations
private static final ThreadLocal<String> CURRENT_CORRELATION_ID = new ThreadLocal<>();
private static final ThreadLocal<String> CURRENT_TRACE_ID = new ThreadLocal<>();
```

#### Reactive Context Storage

```java
// Context storage for async operations
private final ConcurrentMap<String, ContextInfo> contextStorage = new ConcurrentHashMap<>();

public static class ContextInfo {
    private final String correlationId;
    private final String traceId;
    private final Instant timestamp;
    private final Map<String, Object> metadata;
}
```

### Integration with Service Clients

Service clients automatically propagate tracing context:

```java
// REST client - context headers added automatically
Mono<User> user = userServiceClient.get("/users/{id}", User.class)
    .withPathParam("id", userId)
    .execute(); // X-Correlation-ID and X-Trace-ID headers added automatically

// gRPC client - context propagated via metadata
Mono<Account> account = accountServiceClient.execute(stub ->
    Mono.fromCallable(() -> stub.getAccount(request))
); // Tracing metadata added automatically
```

### Manual Context Management

For advanced scenarios requiring manual context control:

```java
@Service
public class AdvancedService {

    public Mono<Result> processWithCustomContext(Request request) {
        // Save current context
        String originalCorrelationId = correlationContext.getCorrelationId();
        String originalTraceId = correlationContext.getTraceId();

        try {
            // Set custom context
            correlationContext.setCorrelationId("custom-correlation-id");
            correlationContext.setTraceId("custom-trace-id");

            return businessLogic(request);
        } finally {
            // Restore original context
            correlationContext.setCorrelationId(originalCorrelationId);
            correlationContext.setTraceId(originalTraceId);
        }
    }
}
```

## lib-transactional-engine Integration

The library provides a **bridge pattern integration** with the [lib-transactional-engine](https://github.com/firefly-oss/lib-transactional-engine) saga orchestration framework. This integration allows saga step events to be published through the domain events infrastructure.

> **Important**: This library does **NOT** implement saga orchestration. It only provides a bridge to publish step events from lib-transactional-engine through the domain events infrastructure. For saga implementation, use the lib-transactional-engine directly.

### StepEventPublisherBridge

Core bridge component that connects saga step events to domain events:

#### Interface

```java
public class StepEventPublisherBridge implements StepEventPublisher {
    public StepEventPublisherBridge(String defaultTopic, DomainEventPublisher delegate);

    @Override
    public Mono<Void> publish(StepEventEnvelope stepEvent);
}
```

#### Auto-Configuration

The bridge is automatically configured and available as a bean:

```java
@Configuration
public class StepBridgeConfiguration {

    @Bean
    @Primary
    public StepEventPublisherBridge stepEventDomainPublisher(DomainEventPublisher domainEventPublisher) {
        return new StepEventPublisherBridge(defaultTopic, domainEventPublisher);
    }
}
```

### Step Event Envelope

Step events are automatically enriched with metadata:

```java
public class StepEventEnvelope {
    private String sagaName;        // Saga identifier
    private String sagaId;          // Saga instance ID
    private String stepId;          // Step identifier
    private String topic;           // Event topic
    private String type;            // Event type
    private String key;             // Routing key
    private Object payload;         // Step payload
    private Map<String, Object> headers;     // Transport headers
    private int attempts;           // Execution attempts
    private long latencyMs;         // Step execution latency
    private Instant startedAt;      // Step start time
    private Instant completedAt;    // Step completion time
    private String resultType;      // SUCCESS, FAILURE, etc.
}
```

### Metadata Enrichment

The bridge automatically enriches step events with execution metadata:

```java
// Metadata added to domain event envelope
Map<String, Object> metadata = {
    "step.attempts": 3,
    "step.latency_ms": 1200,
    "step.started_at": "2025-01-15T10:30:00Z",
    "step.completed_at": "2025-01-15T10:30:01.2Z",
    "step.result_type": "SUCCESS"
}
```

### Usage Examples

#### Banking Money Transfer Saga

```java
// Step event from money transfer saga
StepEventEnvelope stepEvent = new StepEventEnvelope(
    "MoneyTransferSaga",           // sagaName
    "SAGA-67890",                  // sagaId
    "step-transfer",               // stepId
    "banking-step-events",         // topic
    "transfer.step.completed",     // type
    "TXN-12345",                   // key
    new MoneyTransferStepPayload(  // payload
        "TXN-12345",
        "ACC-001",
        "ACC-002",
        new BigDecimal("1000.00"),
        "USD",
        "COMPLETED"
    ),
    Map.of("source", "transfer-service"), // headers
    1,                             // attempts
    250L,                          // latencyMs
    Instant.now().minusMillis(250), // startedAt
    Instant.now(),                 // completedAt
    "SUCCESS"                      // resultType
);

// Published through bridge to domain events infrastructure
stepEventBridge.publish(stepEvent);
```

#### Account Opening Saga

```java
// Step event from account opening saga
StepEventEnvelope stepEvent = new StepEventEnvelope(
    "AccountOpeningSaga",
    "SAGA-12345",
    "step-validation",
    "banking-step-events",
    "account.validation.completed",
    null, // Auto-generated from saga name and ID
    "Account validation successful",
    Map.of("validation-score", 95),
    1,
    500L,
    Instant.now().minusMillis(500),
    Instant.now(),
    "SUCCESS"
);
```

### Key Generation

The bridge automatically generates keys when not provided:

```java
// If key is null or empty, auto-generate from saga context
if (stepEvent.getKey() == null || stepEvent.getKey().isEmpty()) {
    stepEvent.setKey(stepEvent.getSagaName() + ":" + stepEvent.getSagaId());
}
// Result: "MoneyTransferSaga:SAGA-67890"
```

### Topic Resolution

Default topic is used when not specified:

```java
// If topic is null or empty, use default
if (stepEvent.getTopic() == null || stepEvent.getTopic().isEmpty()) {
    stepEvent.setTopic(defaultTopic); // "domain-events" by default
}
```

### Configuration

```yaml
firefly:
  stepevents:
    enabled: true  # Enable step events bridge

# Default topic for step events
domain:
  topic: banking-step-events
```

### Integration Pattern

The bridge follows the adapter pattern, allowing step events to leverage all domain events features:

1. **Unified Infrastructure**: Step events use the same messaging adapters (Kafka, RabbitMQ, etc.)
2. **Consistent Configuration**: Same configuration for both domain and step events
3. **Shared Resilience**: Circuit breakers, retries, and error handling apply to step events
4. **Tracing Integration**: Step events automatically include correlation context
5. **Monitoring**: Step events appear in the same metrics and observability systems
---