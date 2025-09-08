# Architecture Guide

Comprehensive overview of the Firefly Common Domain Library architecture and design patterns.

## Overview

The Firefly Common Domain Library provides a **consolidated zero-boilerplate CQRS framework** for building banking microservices with:

- **🎯 Consolidated CQRS Framework**: Single approach with zero boilerplate - extend base classes with annotations
- **🚀 Automatic Everything**: Type detection, validation, caching, logging, metrics, error handling
- **⚡ Focus on Business Logic**: Write only the `doHandle()` method - everything else is automatic
- **📊 Built-in Observability**: Automatic logging, metrics, and distributed tracing
- **🔧 Annotation-Driven**: All configuration through `@CommandHandlerComponent` and `@QueryHandlerComponent`
- **✅ Production Ready**: Built-in resilience, monitoring, and best practices

## 4-Tier Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  REST Controllers, GraphQL Resolvers, Message Handlers      │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
│     Command/Query Handlers, Workflow Orchestrators          │
│              (Uses Firefly Common Domain)                   │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                     Domain Layer                            │
│    Domain Models, Business Rules, Domain Services           │
│              (Pure business logic)                          │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                 Infrastructure Layer                        │
│   ServiceClients, Event Publishers, External APIs           │
│              (Uses Firefly Common Domain)                   │
└─────────────────────────────────────────────────────────────┘
```

## Core Design Patterns

### Consolidated CQRS Pattern

**Single Approach Philosophy**
- **Only one way** to create handlers: extend base classes with annotations
- **Zero boilerplate**: No type methods, caching methods, or validation setup
- **Focus on business logic**: Write only the `doHandle()` method

**Command Side (Write Operations)**
- Commands are simple POJOs implementing `Command<R>` interface
- CommandHandlers extend `CommandHandler<C, R>` with `@CommandHandlerComponent` annotation
- Automatic type detection from generics - no `getCommandType()` needed
- Built-in validation, logging, metrics, error handling, and retry logic
- CommandBus automatically routes and processes commands
- Spring auto-discovery of handlers with `@CommandHandlerComponent`
- Built-in metrics, logging, correlation, and error handling

**Query Side (Read Operations)**
- Queries are simple POJOs implementing `Query<R>` interface
- QueryHandlers extend `QueryHandler<Q, R>` with `@QueryHandlerComponent` annotation
- Automatic type detection from generics - no `getQueryType()` needed
- Automatic caching based on annotation configuration - no `supportsCaching()` methods
- Built-in validation, logging, metrics, and performance monitoring

## Component Architecture

### Consolidated CQRS Framework Components

```
┌─────────────────┐    ┌──────────────────────────────────┐    ┌─────────────────┐
│   CommandBus    │───►│  @CommandHandlerComponent        │───►│ Business Logic  │
│                 │    │  extends CommandHandler<C,R>     │    │                 │
│ • Auto-routing  │    │                                  │    │ • Only doHandle │
│ • Validation    │    │ • Automatic type detection       │    │ • Pure business │
│ • Metrics       │    │ • Built-in validation            │    │ • No boilerplate│
│ • Logging       │    │ • Timeout/retry from annotation  │    │                 │
└─────────────────┘    └──────────────────────────────────┘    └─────────────────┘

┌─────────────────┐    ┌──────────────────────────────────┐    ┌─────────────────┐
│    QueryBus     │───►│  @QueryHandlerComponent          │───►│ Business Logic  │
│                 │    │  extends QueryHandler<Q,R>       │    │                 │
│ • Auto-routing  │    │                                  │    │ • Only doHandle │
│ • Caching       │    │ • Automatic type detection       │    │ • Pure business │
│ • Metrics       │    │ • Caching from annotation        │    │ • No boilerplate│
│ • Logging       │    │ • TTL from annotation            │    │                 │
└─────────────────┘    └──────────────────────────────────┘    └─────────────────┘

                       ┌──────────────────────────────────┐
                       │        Automatic Features        │
                       │                                  │
                       │ • Jakarta Bean Validation        │
                       │ • Micrometer Metrics (Auto-Cfg)  │
                       │ • Structured Logging             │
                       │ • Correlation Context            │
                       │ • Error Handling & Retries       │
                       │ • Cache Management               │
                       └──────────────────────────────────┘
```

**Key Components:**
- **CommandBus/QueryBus**: Central routing with automatic handler discovery via Spring
- **@CommandHandlerComponent/@QueryHandlerComponent**: Annotation-driven configuration
- **CommandHandler/QueryHandler**: Zero-boilerplate base classes with automatic type detection
- **AutoValidationProcessor**: Jakarta Bean Validation integration
- **GenericTypeResolver**: Automatic type detection from generics
- **Automatic Caching**: Annotation-based cache management for queries

## Design Principles

### 1. Single Approach - Eliminate Confusion
```java
// ✅ THE ONLY WAY - Extend base classes with annotations
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountCreatedResult> {
    @Override
    protected Mono<AccountCreatedResult> doHandle(CreateAccountCommand command) {
        // Only business logic
    }
}

// ❌ DON'T DO THIS - Multiple approaches create confusion
public class OldHandler implements CommandHandler<MyCommand, MyResult> {
    // Old interface-based approach - don't use
}
```

### 2. Zero Boilerplate - Focus on Business Logic
```java
// ✅ NO BOILERPLATE NEEDED
@QueryHandlerComponent(cacheable = true, cacheTtl = 300, metrics = true)
public class GetBalanceHandler extends QueryHandler<GetBalanceQuery, Balance> {
    @Override
    protected Mono<Balance> doHandle(GetBalanceQuery query) {
        // Only business logic - everything else automatic!
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
    timeout = 30000,    // Command timeout in milliseconds
    retries = 3,        // Number of retry attempts
    metrics = true      // Enable metrics collection
)

@QueryHandlerComponent(
    cacheable = true,   // Enable caching
    cacheTtl = 300,     // Cache TTL in seconds
    metrics = true      // Enable metrics collection
)
```

## Integration Patterns

### Auto-Configured Metrics

The framework automatically provides metrics collection **by default** with zero configuration:

```java
// ✅ NO METRICS CONFIGURATION NEEDED
@SpringBootApplication
public class BankingApplication {
    // MeterRegistry is auto-configured (SimpleMeterRegistry by default)
    // Command and Query metrics are automatically collected
}
```

**Automatic Metrics Collection:**
- **Command Metrics**: `firefly.cqrs.command.processed`, `firefly.cqrs.command.processing.time`
- **Query Metrics**: `firefly.cqrs.query.processed`, `firefly.cqrs.query.processing.time`
- **Auto-Configured MeterRegistry**: SimpleMeterRegistry provided if none exists
- **Zero Configuration**: No manual MeterRegistry bean required

**Custom MeterRegistry (Optional):**
```java
@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistry meterRegistry() {
        // Your custom MeterRegistry (Prometheus, CloudWatch, etc.)
        // Framework will use this instead of auto-configured SimpleMeterRegistry
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
}
```

### Handler Implementation Pattern
```java
// ✅ CURRENT PATTERN - Consolidated approach
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class TransferMoneyHandler extends CommandHandler<TransferMoneyCommand, TransferResult> {

    @Autowired
    private TransferService transferService;

    @Override
    protected Mono<TransferResult> doHandle(TransferMoneyCommand command) {
        // Only business logic - everything else automatic!
        return transferService.executeTransfer(command)
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

### Spring Integration Pattern
```java
// ✅ AUTOMATIC DISCOVERY - No manual registration needed
@SpringBootApplication
public class BankingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingApplication.class, args);
        // CommandBus and QueryBus beans automatically available
        // Handlers automatically discovered and registered
    }
}

// ✅ DEPENDENCY INJECTION - Standard Spring patterns
@Service
public class AccountService {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private QueryBus queryBus;

    public Mono<AccountCreatedResult> createAccount(CreateAccountRequest request) {
        CreateAccountCommand command = new CreateAccountCommand(
            request.getCustomerId(),
            request.getAccountType(),
            request.getInitialBalance()
        );
        return commandBus.send(command);
    }
}
```

## Framework Benefits

### 1. Developer Experience
- **🎯 Single Approach**: Only one way to do things - no confusion
- **🚀 Zero Boilerplate**: Focus only on business logic
- **⚡ Fast Development**: Automatic everything - validation, caching, metrics
- **📚 Easy Learning**: Simple patterns, clear documentation

### 2. Production Readiness
- **📊 Built-in Observability**: Automatic logging, metrics, tracing
- **🔧 Resilience**: Automatic retries, timeouts, error handling
- **⚡ Performance**: Automatic caching, optimized routing
- **🛡️ Reliability**: Validation, correlation, structured logging

### 3. Maintainability
- **🎯 Consistency**: Single approach across all handlers
- **🔍 Testability**: Easy to unit test business logic
- **📖 Readability**: Clean, focused code without boilerplate
- **🔧 Extensibility**: Hook points for customization

## Best Practices

### Handler Design
```java
// ✅ GOOD - Single responsibility, focused business logic
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountCreatedResult> {

    @Override
    protected Mono<AccountCreatedResult> doHandle(CreateAccountCommand command) {
        // Only business logic - everything else automatic
        return accountService.createAccount(command)
            .map(account -> mapToResult(account, command));
    }
}

// ❌ AVOID - Complex handlers with multiple responsibilities
public class ComplexHandler extends CommandHandler<ComplexCommand, ComplexResult> {
    // Don't put multiple business operations in one handler
}
```

### Validation Strategy
```java
// ✅ GOOD - Use Jakarta Bean Validation for simple validation
public class CreateAccountCommand implements Command<AccountCreatedResult> {
    @NotBlank(message = "Customer ID is required")
    private final String customerId;

    @NotNull @Positive(message = "Amount must be positive")
    private final BigDecimal initialBalance;
}

// ✅ GOOD - Complex business validation in service layer
@Service
public class AccountValidationService {
    public Mono<ValidationResult> validateAccountCreation(CreateAccountCommand command) {
        // Complex business rules here
    }
}
```

### Testing Approach
```java
// ✅ GOOD - Test business logic, not framework features
@ExtendWith(MockitoExtension.class)
class CreateAccountHandlerTest {

    @Mock private AccountService accountService;
    @InjectMocks private CreateAccountHandler handler;

    @Test
    void shouldCreateAccount() {
        // Test only the business logic in doHandle()
        StepVerifier.create(handler.doHandle(command))
            .expectNextMatches(result -> result.getStatus().equals("ACTIVE"))
            .verifyComplete();
    }
}
