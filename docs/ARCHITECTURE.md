# Firefly Common Domain Library - Complete Architecture Guide

## 🏗️ Overview

The Firefly Common Domain Library provides a **comprehensive, enterprise-grade platform** for building distributed banking applications. The library includes multiple integrated frameworks:

- **🎯 CQRS Framework**: Zero-boilerplate command and query processing
- **📡 Domain Events**: Unified event publishing across messaging infrastructures
- **🔗 Service Client Framework**: Reactive service-to-service communication
- **🛡️ Resilience Patterns**: Circuit breakers, retries, and fault tolerance
- **🔍 Distributed Tracing**: End-to-end request tracking and correlation
- **⚙️ lib-transactional-engine Integration**: Saga step event publishing

The architecture emphasizes **zero-boilerplate development**, **automatic configuration**, **separation of concerns**, and **seamless Spring Boot integration**.

## 🎯 Core Philosophy

The Firefly Common Domain Library embodies a **radical simplification philosophy** that prioritizes developer productivity, maintainability, and enterprise-grade reliability. Our design principles are rooted in the belief that **complexity should be hidden, not eliminated** - the framework handles the intricate details so developers can focus on what matters most: **business value creation**.

### 🎯 The Single Best Way Approach

**"There should be one-- and preferably only one --obvious way to do it."** - *The Zen of Python*

We deliberately provide **exactly one way** to accomplish each task, eliminating decision paralysis and reducing cognitive load:

- **ONE way** to create command handlers: `@CommandHandlerComponent` + extend `CommandHandler<Command, Result>`
- **ONE way** to create query handlers: `@QueryHandlerComponent` + extend `QueryHandler<Query, Result>`
- **ONE way** to handle validation: Jakarta Bean Validation annotations + optional custom validation
- **ONE way** to configure behavior: Declarative annotations with sensible defaults

**Why This Matters:**
- **Eliminates Analysis Paralysis**: No time wasted choosing between multiple approaches
- **Reduces Onboarding Time**: New team members learn one pattern that works everywhere
- **Improves Code Consistency**: All handlers follow identical patterns across the codebase
- **Simplifies Maintenance**: Uniform structure makes debugging and refactoring predictable

### 🚀 Zero-Boilerplate Philosophy

**"The best code is no code at all."** - *Jeff Atwood*

We believe that **boilerplate code is a form of technical debt** that should be eliminated through intelligent automation:

#### What You DON'T Write Anymore:
```java
// ❌ ELIMINATED - No more type detection boilerplate
public Class<CreateAccountCommand> getCommandType() { return CreateAccountCommand.class; }
public Class<AccountResult> getResultType() { return AccountResult.class; }

// ❌ ELIMINATED - No more validation setup
public Mono<ValidationResult> validate(CreateAccountCommand command) { /* validation logic */ }

// ❌ ELIMINATED - No more metrics configuration
public void recordMetrics(String commandType, Duration duration, boolean success) { /* metrics */ }

// ❌ ELIMINATED - No more caching boilerplate
public boolean supportsCaching() { return true; }
public Duration getCacheTtl() { return Duration.ofMinutes(15); }
public String getCacheKey(GetAccountQuery query) { return "account:" + query.getAccountId(); }
```

#### What You ONLY Write Now:
```java
// ✅ FOCUS - Only business logic matters
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountResult> {

    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command) {
        // Pure business logic - everything else is automatic!
        return accountService.createAccount(command)
            .flatMap(this::publishAccountCreatedEvent);
    }
}
```

**The Mathematics of Productivity:**
- **Traditional Approach**: 80% boilerplate + 20% business logic = **Low Value Density**
- **Firefly Approach**: 0% boilerplate + 100% business logic = **Maximum Value Density**

### 🧠 Cognitive Load Reduction

**"The most important property of a program is whether it accomplishes the intention of its user."** - *C.A.R. Hoare*

We optimize for **human cognitive capacity**, not just machine performance:

#### Automatic Type Detection
```java
// ✅ INTELLIGENT - Framework automatically detects types from generics
public class TransferHandler extends CommandHandler<TransferCommand, TransferResult> {
    // Framework knows: Command = TransferCommand, Result = TransferResult
    // No manual type specification needed - ever!
}
```

#### Declarative Configuration
```java
// ✅ SELF-DOCUMENTING - Configuration intent is crystal clear
@CommandHandlerComponent(
    timeout = 30000,    // "This operation should complete within 30 seconds"
    retries = 3,        // "Retry up to 3 times on failure"
    metrics = true      // "Track performance metrics for this handler"
)
```

#### Intelligent Defaults
```java
// ✅ SENSIBLE DEFAULTS - Production-ready out of the box
@QueryHandlerComponent  // Automatically gets: caching=false, timeout=15s, metrics=true
```

### 🏗️ Enterprise-Grade Reliability by Default

**"Make it work, make it right, make it fast."** - *Kent Beck*

Every framework component is designed with **production reliability** as the baseline, not an afterthought:

#### Built-in Resilience Patterns
- **Circuit Breakers**: Automatic failure detection and recovery
- **Retry Logic**: Configurable retry strategies with exponential backoff
- **Timeout Management**: Prevents resource exhaustion from hanging operations
- **Bulkhead Isolation**: Handler failures don't cascade to other components

#### Comprehensive Observability
- **Automatic Metrics**: Success/failure rates, latency percentiles, throughput
- **Distributed Tracing**: End-to-end request correlation across service boundaries
- **Structured Logging**: Contextual information for debugging and monitoring
- **Health Checks**: Real-time system health and dependency status

#### Production-Ready Defaults
```yaml
# ✅ PRODUCTION-READY - No configuration needed for basic production deployment
firefly:
  cqrs:
    enabled: true                    # Framework active
    command:
      timeout: 30s                   # Reasonable default timeout
      metrics-enabled: true          # Observability enabled
      tracing-enabled: true          # Distributed tracing active
    query:
      timeout: 15s                   # Faster timeout for read operations
      caching-enabled: true          # Performance optimization enabled
      cache-ttl: 15m                 # Balanced cache duration
```

### 🔄 Evolutionary Architecture Principles

**"The only constant is change."** - *Heraclitus*

The framework is designed to **evolve gracefully** with changing requirements:

#### Backward Compatibility Guarantee
- **Existing handlers continue working** when new features are added
- **Gradual migration paths** for adopting new capabilities
- **Deprecation warnings** provide clear upgrade guidance

#### Extensibility Without Modification
- **Hook points** for custom behavior without framework changes
- **Plugin architecture** for adding new capabilities
- **Context-aware processing** enables feature flags and A/B testing

#### Future-Proof Design
- **Reactive foundations** support high-concurrency workloads
- **Cloud-native patterns** enable containerized deployments
- **Event-driven architecture** supports microservices evolution

### 🎯 Developer Experience Excellence

**"Developer experience is user experience for developers."** - *Kelsey Hightower*

Every design decision prioritizes the **daily experience** of developers using the framework:

#### Immediate Feedback Loops
- **Compile-time type safety** catches errors before runtime
- **Detailed error messages** with troubleshooting guidance
- **IDE integration** with auto-completion and validation

#### Minimal Learning Curve
- **Familiar patterns** based on Spring Boot conventions
- **Progressive disclosure** - simple cases are simple, complex cases are possible
- **Comprehensive documentation** with real-world examples

#### Debugging Excellence
- **Enhanced stack traces** with business context
- **Correlation IDs** for tracing requests across services
- **Structured logging** for efficient log analysis

### 🌟 The Firefly Advantage

This philosophy creates a **multiplicative effect** on team productivity:

1. **Faster Development**: Less code to write means faster feature delivery
2. **Fewer Bugs**: Less code means fewer places for bugs to hide
3. **Easier Maintenance**: Consistent patterns make changes predictable
4. **Better Performance**: Framework optimizations benefit all handlers automatically
5. **Improved Reliability**: Built-in resilience patterns prevent common failure modes
6. **Enhanced Observability**: Automatic metrics and tracing provide operational insights

**The Result**: Teams can focus on **solving business problems** instead of **fighting infrastructure complexity**.

## 🏛️ Complete Platform Architecture

### High-Level System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                    Firefly Common Domain Library                                            │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                                             │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐         │
│  │    CQRS Framework   │  │   Domain Events     │  │  Service Clients    │  │  Resilience         │         │
│  │                     │  │                     │  │                     │  │  Patterns           │         │
│  │ ┌─────────────────┐ │  │ ┌─────────────────┐ │  │ ┌─────────────────┐ │  │ ┌─────────────────┐ │         │
│  │ │   CommandBus    │ │  │ │ EventPublisher  │ │  │ │  REST Clients   │ │  │ │ CircuitBreaker  │ │         │
│  │ │   QueryBus      │ │  │ │ EventConsumer   │ │  │ │  gRPC Clients   │ │  │ │ Manager         │ │         │
│  │ └─────────────────┘ │  │ └─────────────────┘ │  │ └─────────────────┘ │  │ └─────────────────┘ │         │
│  │ ┌─────────────────┐ │  │ ┌─────────────────┐ │  │ ┌─────────────────┐ │  │ ┌─────────────────┐ │         │
│  │ │ CommandHandler  │ │  │ │ Kafka Adapter   │ │  │ │ Request Builder │ │  │ │ Sliding Window  │ │         │
│  │ │ QueryHandler    │ │  │ │ RabbitMQ        │ │  │ │ Response Mapper │ │  │ │ State Machine   │ │         │
│  │ └─────────────────┘ │  │ │ SQS/Kinesis     │ │  │ └─────────────────┘ │  │ └─────────────────┘ │         │
│  └─────────────────────┘  │ └─────────────────┘ │  └─────────────────────┘  └─────────────────────┘         │
│                           │ ┌─────────────────┐ │                                                           │
│                           │ │ ApplicationEvent│ │                                                           │
│                           │ └─────────────────┘ │                                                           │
│                           └─────────────────────┘                                                           │
│                                                                                                             │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐                                  │
│  │ Distributed Tracing │  │ lib-transactional   │  │   Auto-Configuration│                                  │
│  │                     │  │ engine Integration  │  │                     │                                  │
│  │ ┌─────────────────┐ │  │ ┌─────────────────┐ │  │ ┌─────────────────┐ │                                  │
│  │ │CorrelationCtx   │ │  │ │StepEventBridge  │ │  │ │ Spring Boot     │ │                                  │
│  │ │ TraceID/CorrID  │ │  │ │ Saga Events     │ │  │ │ Auto-Config     │ │                                  │
│  │ └─────────────────┘ │  │ └─────────────────┘ │  │ └─────────────────┘ │                                  │
│  │ ┌─────────────────┐ │  │ ┌─────────────────┐ │  │ ┌─────────────────┐ │                                  │
│  │ │ MDC Integration │ │  │ │ Metadata        │ │  │ │ Properties      │ │                                  │
│  │ │ Header Propagat │ │  │ │ Enrichment      │ │  │ │ Bean Creation   │ │                                  │
│  │ └─────────────────┘ │  │ └─────────────────┘ │  │ └─────────────────┘ │                                  │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘                                  │
│                                                                                                             │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Component Integration Flow with ExecutionContext

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                          Complete Request Processing Flow with ExecutionContext                             │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                                             │
│  1. HTTP Request + Headers (Authorization, X-Tenant-ID, X-User-ID, Feature-Flags)                           │
│       │                                                                                                     │
│       ▼                                                                                                     │
│  ┌─────────────────┐                                                                                        │
│  │  @RestController│  ← Web Layer (Controllers)                                                             │
│  │  - Extract      │    • Parse HTTP headers                                                                │
│  │    Headers      │    • Validate request format                                                           │
│  │  - Map to DTO   │    • Map to request DTOs                                                               │
│  │  - Call Service │    • Delegate to service layer                                                         │
│  └─────────────────┘                                                                                        │
│       │                                                                                                     │
│       ▼                                                                                                     │
│  ┌─────────────────┐                                                                                        │
│  │  @Service       │  ← Service Layer (Business Orchestration)                                              │
│  │  - Build        │    • Build ExecutionContext from headers                                               │
│  │    ExecutionCtx │    • Create commands/queries                                                           │
│  │  - Create       │    • Orchestrate business workflows                                                    │
│  │    Commands     │    • Handle cross-cutting concerns                                                     │
│  │  - Call CQRS    │                                                                                        │
│  └─────────────────┘                                                                                        │
│       │                                                                                                     │
│       ▼                                                                                                     │
│  ┌─────────────────┐    ┌─────────────────┐                                                                 │
│  │ ExecutionContext│───▶│ CQRS Framework  │  ← Domain Layer (Core Business Logic)                           │
│  │ Builder         │    │ CommandBus/     │    • Process commands with context                              │
│  │ - User/Tenant   │    │ QueryBus        │    • Execute business rules                                     │
│  │ - Features      │    │ + Context       │    • Validate business constraints                              │
│  │ - Session       │    │                 │    • Apply context-aware logic                                  │
│  │ - Custom Props  │    │                 │                                                                 │
│  └─────────────────┘    └─────────────────┘                                                                 │
│       │                          │                                                                          │
│       │                          ▼                                                                          │
│       │                 ┌─────────────────┐    ┌─────────────────┐                                          │
│       │                 │ CommandHandler/ │───▶│ Context-Aware   │                                          │
│       │                 │ QueryHandler    │    │ Business Logic  │                                          │
│       │                 │ - Type Detection│    │ - Tenant Logic  │                                          │
│       │                 │ - Validation    │    │ - Feature Flags │                                          │
│       │                 │ - Metrics       │    │ - User Perms    │                                          │
│       │                 │ - Caching       │    │ - Custom Rules  │                                          │
│       │                 └─────────────────┘    └─────────────────┘                                          │
│       │                          │                       │                                                  │
│       │                          │                       ▼                                                  │
│       │                          │              ┌─────────────────┐    ┌─────────────────┐                  │
│       │                          │              │ Domain Events   │───▶│ Messaging       │                  │
│       │                          │              │ with Context    │    │ Infrastructure  │                  │
│       │                          │              │ - Event Metadata│    │ (Kafka/RabbitMQ)│                  │
│       │                          │              │ - Correlation   │    │ + Context       │                  │
│       │                          │              │ - User Context  │    │   Headers       │                  │
│       │                          │              └─────────────────┘    └─────────────────┘                  │
│       │                          │                       │                                                  │
│       │                          │                       ▼                                                  │
│       │                          │              ┌─────────────────┐                                         │
│       │                          │              │ Service Client  │  ← Integration Layer                    │
│       │                          │              │ Framework       │    • Call downstream services           │
│       │                          │              │ (REST/gRPC)     │    • Propagate context headers          │
│       │                          │              │ + Context       │    • Apply circuit breaker patterns     │
│       │                          │              │   Propagation   │    • Handle resilience concerns         │
│       │                          │              └─────────────────┘                                         │
│       │                          │                       │                                                  │
│       │                          │                       ▼                                                  │
│       │                          │              ┌─────────────────┐                                         │
│       │                          │              │ Circuit Breaker │                                         │
│       │                          │              │ Protection      │                                         │
│       │                          │              │ - Failure Det.  │                                         │
│       │                          │              │ - Auto Recovery │                                         │
│       │                          │              └─────────────────┘                                         │
│       │                          │                       │                                                  │
│       │                          ▼                       ▼                                                  │
│       │                 ┌─────────────────┐    ┌─────────────────┐                                          │
│       │                 │ Context-Aware   │    │ Downstream      │                                          │
│       │                 │ Processing      │    │ Service Call    │                                          │
│       │                 │ - Validation    │    │ with Context    │                                          │
│       │                 │ - Metrics       │    │ Headers         │                                          │
│       │                 │ - Caching       │    │ - X-User-ID     │                                          │
│       │                 │ - Tracing       │    │ - X-Tenant-ID   │                                          │
│       │                 └─────────────────┘    │ - Correlation   │                                          │
│       │                                        └─────────────────┘                                          │
│       ▼                                                 │                                                   │
│  ┌─────────────────┐    ┌─────────────────┐            │                                                    │
│  │ Service Layer   │◄───│ Domain Result   │◄───────────┘                                                    │
│  │ Response        │    │ + Context Info  │                                                                 │
│  │ - Map to DTO    │    │ - Business Data │                                                                 │
│  │ - Add Headers   │    │ - Metadata      │                                                                 │
│  └─────────────────┘    └─────────────────┘                                                                 │
│       │                                                                                                     │
│       ▼                                                                                                     │
│  ┌─────────────────┐                                                                                        │
│  │ HTTP Response   │  ← Response to Client                                                                  │
│  │ + Trace Headers │    • Include correlation headers                                                       │
│  │ + Context Info  │    • Add performance metrics                                                           │
│  │ + Business Data │    • Return business results                                                           │
│  └─────────────────┘                                                                                        │
│                                                                                                             │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

```

## 🔧 Core Components Deep Dive

### 1. ExecutionContext Framework

The ExecutionContext framework provides a powerful mechanism for passing additional context values to command and query handlers that are not part of the command/query itself. This is essential for multi-tenant applications, user authentication, feature flags, and request-specific metadata.

#### ExecutionContext Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           ExecutionContext Framework                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  Application Layer                                                              │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐              │
│  │ CommandBus      │    │ QueryBus        │    │ ExecutionContext│              │
│  │ .send(cmd, ctx) │    │ .query(qry,ctx) │    │ Builder         │              │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘              │
│           │                       │                       │                     │
│           └───────────────────────┼───────────────────────┘                     │
│                                   ▼                                             │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                    Context-Aware Processing Pipeline                        ││
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌───────────┐  ││
│  │  │ Context         │ │ Handler         │ │ Flexible        │ │ Context   │  ││
│  │  │ Validation      │ │ Resolution      │ │ vs Required     │ │ Logging   │  ││
│  │  │                 │ │                 │ │ Context         │ │           │  ││
│  │  └─────────────────┘ └─────────────────┘ └─────────────────┘ └───────────┘  ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                   │                                             │
│                                   ▼                                             │
│  Handler Layer                                                                  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                │
│  │ Flexible    │ │ Context-    │ │ Optional    │ │ Required    │                │
│  │ Handler     │ │ Aware       │ │ Context     │ │ Context     │                │
│  │ (Optional)  │ │ Handler     │ │ Support     │ │ Validation  │                │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘                │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### ExecutionContext Interface

```java
public interface ExecutionContext {
    // User and tenant context
    String getUserId();
    String getTenantId();
    String getOrganizationId();

    // Session and request context
    String getSessionId();
    String getRequestId();
    String getSource();
    String getClientIp();
    String getUserAgent();

    // Feature flags
    boolean getFeatureFlag(String flagName, boolean defaultValue);
    Map<String, Boolean> getFeatureFlags();

    // Custom properties
    <T> T getProperty(String key, Class<T> type);
    <T> T getProperty(String key, Class<T> type, T defaultValue);
    Map<String, Object> getProperties();

    // Metadata
    Instant getCreatedAt();
    boolean isEmpty();

    // Builder pattern
    static Builder builder() { return new DefaultExecutionContext.Builder(); }
    static ExecutionContext empty() { return DefaultExecutionContext.EMPTY; }
}
```

#### Context-Aware Handler Patterns

**1. Flexible Handlers (Optional Context)**
```java
@CommandHandlerComponent
public class FlexibleAccountHandler extends CommandHandler<CreateAccountCommand, AccountResult> {

    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command) {
        // Standard implementation without context
        return createStandardAccount(command);
    }

    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command, ExecutionContext context) {
        // Enhanced implementation with context
        String tenantId = context.getTenantId();
        boolean premiumFeatures = context.getFeatureFlag("premium-features", false);
        return createAccountWithContext(command, tenantId, premiumFeatures);
    }
}
```

**2. Context-Aware Handlers (Required Context)**
```java
@CommandHandlerComponent
public class TenantAccountHandler extends ContextAwareCommandHandler<CreateAccountCommand, AccountResult> {

    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command, ExecutionContext context) {
        // Context is always provided and validated
        String tenantId = context.getTenantId();
        if (tenantId == null) {
            return Mono.error(new IllegalArgumentException("Tenant ID is required"));
        }

        return createTenantAccount(command, tenantId);
    }
}
```

#### ExecutionContext Builder Pattern

```java
// Comprehensive context creation
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

// Minimal context
ExecutionContext context = ExecutionContext.builder()
    .withUserId("user-123")
    .withTenantId("tenant-456")
    .build();
```

#### Context Integration with CQRS Pipeline

The ExecutionContext seamlessly integrates with the existing CQRS pipeline:

```java
// Command processing with context
public <R> Mono<R> send(Command<R> command, ExecutionContext context) {
    return findHandler(command)
        .flatMap(handler -> {
            // Enhanced logging with context
            logCommandStartWithContext(command, context);

            // Context-aware handler resolution
            if (handler instanceof ContextAwareCommandHandler) {
                return ((ContextAwareCommandHandler<Command<R>, R>) handler).handle(command, context);
            } else {
                // Try context-aware method first, fallback to standard
                return tryContextAwareHandle(handler, command, context)
                    .switchIfEmpty(handler.handle(command));
            }
        })
        .doOnSuccess(result -> logCommandSuccessWithContext(command, context, result))
        .doOnError(error -> logCommandErrorWithContext(command, context, error));
}
```

### 2. Command Processing Pipeline

#### Command Interface
```java
public interface Command<R> {
    default String getCommandId() { return UUID.randomUUID().toString(); }
    default Instant getTimestamp() { return Instant.now(); }
    default String getCorrelationId() { return null; }
    default Map<String, Object> getMetadata() { return Map.of(); }
    
    // Validation pipeline
    default Mono<ValidationResult> validate() { return customValidate(); }
    default Mono<ValidationResult> customValidate() { return Mono.just(ValidationResult.success()); }
}
```

#### Command Handler Base Class with ExecutionContext Support

```java
public abstract class CommandHandler<C extends Command<R>, R> {

    private final Class<C> commandType;
    private final Class<R> resultType;

    // Constructor with automatic type detection from generics
    protected CommandHandler() {
        this.commandType = (Class<C>) GenericTypeResolver.resolveCommandType(this.getClass());
        this.resultType = (Class<R>) GenericTypeResolver.resolveCommandResultType(this.getClass());
    }

    // Main processing pipeline with built-in features
    public final Mono<R> handle(C command) {
        return handleInternal(command, null);
    }

    // Context-aware processing pipeline
    public final Mono<R> handle(C command, ExecutionContext context) {
        return handleInternal(command, context);
    }

    private Mono<R> handleInternal(C command, ExecutionContext context) {
        Instant startTime = Instant.now();
        String commandId = command.getCommandId();
        String commandTypeName = commandType.getSimpleName();

        return Mono.fromCallable(() -> {
                if (context != null) {
                    log.debug("Starting command processing with context: {} [{}] - Context: {}",
                             commandTypeName, commandId, context);
                } else {
                    log.debug("Starting command processing: {} [{}]", commandTypeName, commandId);
                }
                return command;
            })
            .flatMap(cmd -> preProcess(cmd, context))      // Hook for custom pre-processing
            .flatMap(cmd -> {
                // Try context-aware doHandle first, fallback to standard
                if (context != null) {
                    try {
                        return doHandle(cmd, context);
                    } catch (UnsupportedOperationException e) {
                        // Fallback to standard doHandle if context-aware not implemented
                        return doHandle(cmd);
                    }
                } else {
                    return doHandle(cmd);
                }
            })
            .flatMap(result -> postProcess(command, result, context))  // Hook for post-processing
            .doOnSuccess(result -> {
                Duration duration = Duration.between(startTime, Instant.now());
                log.info("Command processed successfully: {} [{}] in {}ms",
                    commandTypeName, commandId, duration.toMillis());
                onSuccess(command, result, duration, context);
            })
            .doOnError(error -> {
                Duration duration = Duration.between(startTime, Instant.now());
                log.error("Command processing failed: {} [{}] in {}ms - {}",
                    commandTypeName, commandId, duration.toMillis(), error.getMessage());
                onError(command, error, duration, context);
            })
            .onErrorMap(this::mapError);
    }

    // The ONLY method you need to implement (standard)
    protected abstract Mono<R> doHandle(C command);

    // Optional context-aware implementation
    protected Mono<R> doHandle(C command, ExecutionContext context) {
        // Default implementation ignores context and delegates to standard doHandle
        return doHandle(command);
    }

    // Extensibility hooks with context support
    protected Mono<C> preProcess(C command, ExecutionContext context) {
        return preProcess(command);
    }
    protected Mono<C> preProcess(C command) {
        return Mono.just(command);
    }

    protected Mono<R> postProcess(C command, R result, ExecutionContext context) {
        return postProcess(command, result);
    }
    protected Mono<R> postProcess(C command, R result) {
        return Mono.just(result);
    }

    protected void onSuccess(C command, R result, Duration duration, ExecutionContext context) {
        onSuccess(command, result, duration);
    }
    protected void onSuccess(C command, R result, Duration duration) { }

    protected void onError(C command, Throwable error, Duration duration, ExecutionContext context) {
        onError(command, error, duration);
    }
    protected void onError(C command, Throwable error, Duration duration) { }

    protected Throwable mapError(Throwable error) { return error; }

    // Automatic type detection - no need to override
    public final Class<C> getCommandType() { return commandType; }
    public final Class<R> getResultType() { return resultType; }
}
```

### 2. Separated Service Architecture

#### CommandHandlerRegistry
**Responsibility**: Handler discovery, registration, and lookup
```java
@Component
public class CommandHandlerRegistry {
    private final Map<Class<? extends Command<?>>, CommandHandler<?, ?>> handlers;
    
    // Automatic discovery from Spring context
    private void discoverAndRegisterHandlers() {
        Map<String, CommandHandler> handlerBeans = applicationContext.getBeansOfType(CommandHandler.class);
        // Register each handler with automatic type detection
    }
    
    // Thread-safe handler lookup
    public <C extends Command<R>, R> Optional<CommandHandler<C, R>> findHandler(Class<C> commandType);
}
```

#### CommandValidationService
**Responsibility**: Jakarta Bean Validation + custom validation
```java
@Component
public class CommandValidationService {
    
    public Mono<Void> validateCommand(Command<?> command) {
        // Phase 1: Jakarta Bean Validation (@NotNull, @NotBlank, etc.)
        return autoValidationProcessor.validate(command)
            .flatMap(autoValidationResult -> {
                if (!autoValidationResult.isValid()) {
                    return Mono.error(new ValidationException(enrichedResult));
                }
                
                // Phase 2: Custom Business Validation
                return command.validate()
                    .flatMap(customValidationResult -> {
                        if (!customValidationResult.isValid()) {
                            return Mono.error(new ValidationException(enrichedResult));
                        }
                        return Mono.<Void>empty();
                    });
            });
    }
}
```

#### CommandMetricsService
**Responsibility**: Metrics collection and monitoring
```java
@Component
public class CommandMetricsService {
    private final MeterRegistry meterRegistry;
    
    // Global metrics
    private final Counter commandProcessedCounter;
    private final Counter commandFailedCounter;
    private final Timer commandProcessingTimer;
    
    // Per-command-type metrics cache
    private final ConcurrentMap<String, Counter> commandTypeSuccessCounters;
    
    public void recordCommandSuccess(Command<?> command, Duration processingTime) {
        commandProcessedCounter.increment();
        commandProcessingTimer.record(processingTime);
        getOrCreateCommandTypeSuccessCounter(commandType).increment();
    }
}
```

#### DefaultCommandBus (Orchestrator) with ExecutionContext Support
**Responsibility**: Clean orchestration of the processing pipeline with context awareness
```java
@Component
public class DefaultCommandBus implements CommandBus {

    // Standard command processing
    public <R> Mono<R> send(Command<R> command) {
        return sendInternal(command, null);
    }

    // Context-aware command processing
    public <R> Mono<R> send(Command<R> command, ExecutionContext context) {
        return sendInternal(command, context);
    }

    private <R> Mono<R> sendInternal(Command<R> command, ExecutionContext context) {
        Instant startTime = Instant.now();

        // Step 1: Find handler
        CommandHandler<Command<R>, R> handler = handlerRegistry.findHandler(command.getClass())
            .orElseThrow(() -> CommandHandlerNotFoundException.forCommand(command, availableHandlers));

        // Step 2: Set correlation context
        if (command.getCorrelationId() != null) {
            correlationContext.setCorrelationId(command.getCorrelationId());
        }

        // Step 3: Enhanced logging with context
        if (context != null) {
            log.info("CQRS Command Processing Started with Context - Type: {}, ID: {}, Context: {}",
                    command.getClass().getSimpleName(), command.getCommandId(), context);
        }

        // Step 4: Validate command
        return validationService.validateCommand(command)
            .then(Mono.defer(() -> {
                // Step 5: Execute handler with context awareness
                if (context != null) {
                    return executeWithMetrics(command, context, handler, startTime);
                } else {
                    return executeWithMetrics(command, handler, startTime);
                }
            }))
            .doFinally(signalType -> correlationContext.clear());
    }

    private <R> Mono<R> executeWithMetrics(Command<R> command, ExecutionContext context,
                                          CommandHandler<Command<R>, R> handler, Instant startTime) {
        return handler.handle(command, context)
            .doOnSuccess(result -> {
                Duration processingTime = Duration.between(startTime, Instant.now());
                metricsService.recordCommandSuccess(command, processingTime);
                log.info("CQRS Command Processing Completed with Context - Type: {}, ID: {}, Result: Success",
                        command.getClass().getSimpleName(), command.getCommandId());
            })
            .doOnError(error -> {
                Duration processingTime = Duration.between(startTime, Instant.now());
                metricsService.recordCommandFailure(command, error, processingTime);
                log.error("CQRS Command Processing Failed with Context - Type: {}, ID: {}, Error: {}",
                         command.getClass().getSimpleName(), command.getCommandId(), error.getMessage());
            });
    }

    private <R> Mono<R> executeWithMetrics(Command<R> command, CommandHandler<Command<R>, R> handler,
                                          Instant startTime) {
        return handler.handle(command)
            .doOnSuccess(result -> {
                Duration processingTime = Duration.between(startTime, Instant.now());
                metricsService.recordCommandSuccess(command, processingTime);
            })
            .doOnError(error -> {
                Duration processingTime = Duration.between(startTime, Instant.now());
                metricsService.recordCommandFailure(command, error, processingTime);
            });
    }
}
```

## 📋 Annotation System

### @CommandHandlerComponent
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface CommandHandlerComponent {
    String value() default "";         // Spring component name (optional)
    long timeout() default -1;         // Processing timeout in milliseconds (-1 = use default)
    int retries() default -1;          // Number of retries on failure (-1 = use default)
    long backoffMs() default 1000;     // Backoff delay between retries in milliseconds
    boolean metrics() default true;    // Enable metrics collection
    boolean tracing() default true;    // Enable distributed tracing
    boolean validation() default true; // Enable automatic validation
    int priority() default 0;          // Handler priority for registration
    String[] tags() default {};        // Custom tags for categorization
    String description() default "";   // Handler description for documentation
}
```

### Real Usage Example
```java
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class TransferMoneyHandler extends CommandHandler<TransferMoneyCommand, TransferResult> {

    private final ServiceClient accountService;
    private final DomainEventPublisher eventPublisher;

    @Override
    protected Mono<TransferResult> doHandle(TransferMoneyCommand command) {
        // ONLY business logic - validation, logging, metrics handled automatically!
        return executeTransfer(command)
            .flatMap(this::publishTransferEvent);
    }

    private Mono<TransferResult> executeTransfer(TransferMoneyCommand command) {
        return accountService.post("/transfers", TransferResult.class)
            .withBody(command)
            .execute();
    }

    private Mono<TransferResult> publishTransferEvent(TransferResult result) {
        return eventPublisher.publish(createTransferEvent(result))
            .thenReturn(result);
    }

    // No getCommandType() needed - automatic from generics
    // No validation setup needed - handled by annotations
    // No metrics setup needed - handled by annotation
    // No error handling needed - built-in
}
```

## 🔄 Complete Processing Flow

### Command Processing Sequence with ExecutionContext Support

#### Standard Command Processing
```
1. Client Code
   ↓ commandBus.send(command)

2. DefaultCommandBus.send()
   ↓ Find handler via CommandHandlerRegistry

3. Set Correlation Context
   ↓ correlationContext.setCorrelationId()

4. CommandValidationService.validateCommand()
   ↓ Phase 1: Jakarta Bean Validation (@NotNull, @NotBlank, etc.)
   ↓ Phase 2: Custom Business Validation (command.validate())

5. CommandHandler.handle()
   ↓ preProcess() hook
   ↓ doHandle() - YOUR BUSINESS LOGIC
   ↓ postProcess() hook

6. CommandMetricsService
   ↓ Record success/failure metrics
   ↓ Record processing time
   ↓ Record per-command-type metrics

7. Return Result
   ↓ Clean up correlation context
```

#### Context-Aware Command Processing
```
1. Client Code
   ↓ commandBus.send(command, executionContext)

2. DefaultCommandBus.send(command, context)
   ↓ Find handler via CommandHandlerRegistry
   ↓ Enhanced logging with context information

3. Set Correlation Context
   ↓ correlationContext.setCorrelationId()

4. CommandValidationService.validateCommand()
   ↓ Phase 1: Jakarta Bean Validation (@NotNull, @NotBlank, etc.)
   ↓ Phase 2: Custom Business Validation (command.validate())

5. Context-Aware Handler Resolution
   ↓ Check if handler supports ExecutionContext
   ↓ Route to appropriate doHandle method

6. CommandHandler.handle(command, context)
   ↓ preProcess(command, context) hook
   ↓ doHandle(command, context) - YOUR BUSINESS LOGIC WITH CONTEXT
   ↓ postProcess(command, result, context) hook

7. CommandMetricsService
   ↓ Record success/failure metrics with context
   ↓ Record processing time
   ↓ Record per-command-type metrics
   ↓ Enhanced logging with context details

8. Return Result
   ↓ Clean up correlation context
```

### Validation Pipeline Detail
```
Command with Jakarta Annotations:
@NotNull @NotBlank @Email @Min @Max

↓ AutoValidationProcessor.validate()
  ├─ Null check
  ├─ Jakarta constraint validation
  ├─ Error aggregation
  └─ ValidationResult creation

↓ Command.validate() / customValidate()
  ├─ Business rule validation
  ├─ External service validation
  └─ Custom ValidationResult

↓ ValidationException (if any failures)
  ├─ Enhanced with command context
  ├─ Correlation ID preservation
  └─ Detailed error messages
```

## 🚀 Spring Boot Integration

### Auto-Configuration
```java
@AutoConfiguration
@EnableConfigurationProperties(CqrsProperties.class)
@ConditionalOnProperty(prefix = "firefly.cqrs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CqrsAutoConfiguration {
    
    @Bean
    public CommandHandlerRegistry commandHandlerRegistry(ApplicationContext applicationContext) {
        return new CommandHandlerRegistry(applicationContext);
    }
    
    @Bean
    public CommandValidationService commandValidationService(AutoValidationProcessor processor) {
        return new CommandValidationService(processor);
    }
    
    @Bean
    public CommandMetricsService commandMetricsService(MeterRegistry meterRegistry) {
        return new CommandMetricsService(meterRegistry);
    }
    
    @Bean
    public CommandBus commandBus(CommandHandlerRegistry registry,
                               CommandValidationService validation,
                               CommandMetricsService metrics,
                               CorrelationContext context) {
        return new DefaultCommandBus(registry, validation, metrics, context);
    }
}
```

### Configuration Properties
```yaml
firefly:
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
      cache:
        type: LOCAL  # LOCAL or REDIS
        redis:
          enabled: false  # Disabled by default
          host: localhost
          port: 6379
```

## 🎯 Key Benefits

### For Developers
- **Zero Boilerplate**: Just implement `doHandle()` method
- **Automatic Type Detection**: No manual type specification needed
- **Built-in Validation**: Jakarta annotations work automatically
- **Comprehensive Metrics**: Success/failure/timing tracked automatically
- **Enhanced Error Handling**: Detailed context and troubleshooting
- **ExecutionContext Support**: Pass additional context values seamlessly
- **Flexible Context Usage**: Optional or required context patterns
- **Multi-Tenant Ready**: Built-in tenant isolation and context awareness

### For Operations
- **Production Ready**: Built-in resilience and monitoring
- **Observability**: Automatic logging, metrics, and tracing with context
- **Configuration Driven**: Extensive configuration options
- **Cache Support**: Local and Redis caching for queries with context-aware keys
- **Flexible Deployment**: Works with any Spring Boot application
- **Context Logging**: Enhanced debugging with execution context details
- **Feature Flag Integration**: Runtime behavior modification through context

### For Architecture
- **Clean Separation**: Commands vs Queries with clear boundaries
- **Testable**: Each service can be tested independently with or without context
- **Maintainable**: Single responsibility principle throughout
- **Extensible**: Hook points for custom behavior with context support
- **Standards Compliant**: Jakarta validation, Micrometer metrics, Spring patterns
- **Context-Aware Design**: Supports multi-tenant, user-specific, and feature-flagged operations
- **Backward Compatible**: Existing handlers work unchanged while supporting new context features

## 🔍 Advanced Features Deep Dive

### Automatic Type Detection System

#### GenericTypeResolver
The framework uses sophisticated reflection to automatically detect command and result types from handler generics:

```java
public class GenericTypeResolver {

    public static <C extends Command<R>, R> Class<C> resolveCommandType(Class<?> handlerClass) {
        // Traverse class hierarchy to find CommandHandler<C, R>
        Type[] genericInterfaces = handlerClass.getGenericInterfaces();
        Type[] genericSuperclass = ((ParameterizedType) handlerClass.getGenericSuperclass()).getActualTypeArguments();

        // Extract C from CommandHandler<C, R>
        return (Class<C>) genericSuperclass[0];
    }

    public static <R> Class<R> resolveResultType(Class<?> handlerClass) {
        // Extract R from CommandHandler<C, R>
        return (Class<R>) genericSuperclass[1];
    }
}
```

#### Type Detection Examples
```java
// ✅ CORRECT - Framework automatically detects types
@CommandHandlerComponent
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountResult> {
    // getCommandType() returns CreateAccountCommand.class automatically
    // getResultType() returns AccountResult.class automatically
}

// ❌ INCORRECT - Raw types break detection
public class BadHandler extends CommandHandler {
    // Type detection fails - compilation error with helpful message
}

// ❌ INCORRECT - Missing generics
public class AnotherBadHandler extends CommandHandler<Command, Object> {
    // Too generic - runtime error with troubleshooting guide
}
```

### Enhanced Validation System

#### Two-Phase Validation Pipeline

**Phase 1: Jakarta Bean Validation (Automatic)**
```java
public class CreateAccountCommand implements Command<AccountResult> {
    @NotNull(message = "Customer ID is required")
    private final String customerId;

    @NotBlank
    @Email(message = "Please provide a valid email address")
    private final String email;

    @NotNull
    @Min(value = 0, message = "Initial deposit cannot be negative")
    @Max(value = 1000000, message = "Initial deposit exceeds maximum limit")
    private final BigDecimal initialDeposit;

    // Jakarta validation handled automatically by CommandValidationService
    // No validate() method needed for basic constraints
}
```

**Phase 2: Custom Business Validation (Optional)**
```java
public class TransferMoneyCommand implements Command<TransferResult> {
    @NotNull private final String fromAccount;
    @NotNull private final String toAccount;
    @NotNull @Min(0) private final BigDecimal amount;

    @Override
    public Mono<ValidationResult> customValidate() {
        // Only custom business rules here - Jakarta validation happens first
        if (fromAccount != null && fromAccount.equals(toAccount)) {
            return Mono.just(ValidationResult.failure("accounts", "Cannot transfer to the same account"));
        }

        // Async validation with external services
        return validateAccountExists(fromAccount)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.just(ValidationResult.failure("fromAccount", "Account does not exist"));
                }
                return Mono.just(ValidationResult.success());
            });
    }
}
```

#### AutoValidationProcessor Integration
```java
@Component
public class AutoValidationProcessor {
    private final Validator validator;  // Jakarta Validator

    public Mono<ValidationResult> validate(Object object) {
        if (validator == null) {
            log.debug("No Jakarta Validator available - skipping validation");
            return Mono.just(ValidationResult.success());
        }

        Set<ConstraintViolation<Object>> violations = validator.validate(object);

        if (violations.isEmpty()) {
            return Mono.just(ValidationResult.success());
        }

        ValidationResult.Builder builder = ValidationResult.builder();
        for (ConstraintViolation<Object> violation : violations) {
            String fieldName = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            String code = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
            builder.addError(fieldName, message, code);
        }

        return Mono.just(builder.build());
    }
}
```

### Error Handling and Context Preservation

#### Enhanced Exception Classes
```java
public class CommandHandlerNotFoundException extends RuntimeException {
    private final String commandType;
    private final String commandId;
    private final List<String> availableHandlers;
    private final String troubleshootingGuide;

    public static CommandHandlerNotFoundException forCommand(Command<?> command, List<String> availableHandlers) {
        return CommandHandlerNotFoundException.builder()
            .commandType(command.getClass().getSimpleName())
            .commandId(command.getCommandId())
            .availableHandlers(availableHandlers)
            .troubleshootingGuide(generateTroubleshootingGuide(command.getClass()))
            .build();
    }

    private static String generateTroubleshootingGuide(Class<?> commandType) {
        return String.format("""
            Troubleshooting Guide for %s:

            1. Verify handler exists:
               @CommandHandlerComponent
               public class %sHandler extends CommandHandler<%s, YourResult> { ... }

            2. Check component scanning includes handler package
            3. Verify handler is not abstract or interface
            4. Ensure proper generic types are specified

            Available handlers: %s
            """, commandType.getSimpleName(), commandType.getSimpleName(), commandType.getSimpleName(), availableHandlers);
    }
}
```

#### CommandProcessingException with Context
```java
public class CommandProcessingException extends RuntimeException {
    private final String commandType;
    private final String commandId;
    private final String handlerClass;
    private final Duration processingTime;
    private final Map<String, Object> executionContext;

    public static CommandProcessingException.Builder builder() {
        return new Builder();
    }

    // Preserves complete stack trace with enhanced context
    public CommandProcessingException(String message, Throwable cause, String commandType,
                                    String commandId, String handlerClass, Duration processingTime) {
        super(enhanceMessage(message, commandType, commandId, handlerClass, processingTime), cause);
        this.commandType = commandType;
        this.commandId = commandId;
        this.handlerClass = handlerClass;
        this.processingTime = processingTime;
    }
}
```

### Metrics and Observability

#### Comprehensive Metrics Collection
```java
@Component
public class CommandMetricsService {

    // Global metrics
    private final Counter commandProcessedCounter;
    private final Counter commandFailedCounter;
    private final Counter validationFailedCounter;
    private final Timer commandProcessingTimer;

    // Per-command-type metrics (cached for performance)
    private final ConcurrentMap<String, Counter> commandTypeSuccessCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> commandTypeFailureCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> commandTypeTimers = new ConcurrentHashMap<>();

    public void recordCommandSuccess(Command<?> command, Duration processingTime) {
        String commandType = command.getClass().getSimpleName();

        // Global metrics
        commandProcessedCounter.increment();
        commandProcessingTimer.record(processingTime);

        // Per-command-type metrics
        getOrCreateCommandTypeSuccessCounter(commandType).increment();
        getOrCreateCommandTypeTimer(commandType).record(processingTime);

        log.debug("Command processed successfully: {} [{}] in {}ms",
                 commandType, command.getCommandId(), processingTime.toMillis());
    }

    public void recordCommandFailure(Command<?> command, Throwable error, Duration processingTime) {
        String commandType = command.getClass().getSimpleName();

        // Global metrics
        commandFailedCounter.increment();

        // Per-command-type metrics
        getOrCreateCommandTypeFailureCounter(commandType).increment();

        log.error("Command processing failed: {} [{}] after {}ms - Error: {}",
                 commandType, command.getCommandId(), processingTime.toMillis(), error.getMessage());
    }

    public void recordValidationFailure(Command<?> command, ValidationResult validationResult) {
        validationFailedCounter.increment();

        log.warn("Command validation failed: {} [{}] - Violations: {}",
                command.getClass().getSimpleName(), command.getCommandId(), validationResult.getSummary());
    }
}
```

#### Available Metrics
- `firefly.cqrs.commands.processed.total` - Total commands processed
- `firefly.cqrs.commands.failed.total` - Total commands failed
- `firefly.cqrs.commands.validation.failed.total` - Total validation failures
- `firefly.cqrs.commands.processing.time` - Command processing time distribution
- `firefly.cqrs.commands.{type}.success.total` - Per-command-type success count
- `firefly.cqrs.commands.{type}.failure.total` - Per-command-type failure count
- `firefly.cqrs.commands.{type}.processing.time` - Per-command-type processing time

## 🔍 Query Side Architecture

### Query Processing Pipeline

#### Query Handler Base Class with ExecutionContext Support
```java
@Component
public abstract class QueryHandler<Q extends Query<R>, R> {

    // Automatic type detection from generics
    public Class<Q> getQueryType() {
        return GenericTypeResolver.resolveQueryType(this.getClass());
    }

    // Main processing pipeline with caching
    public final Mono<R> handle(Q query) {
        return handleInternal(query, null);
    }

    // Context-aware processing pipeline
    public final Mono<R> handle(Q query, ExecutionContext context) {
        return handleInternal(query, context);
    }

    private Mono<R> handleInternal(Q query, ExecutionContext context) {
        return Mono.fromCallable(() -> query)
            .flatMap(q -> checkCache(q, context))          // Context-aware cache check
            .switchIfEmpty(
                Mono.defer(() -> {
                    // Try context-aware doHandle first, fallback to standard
                    Mono<R> result;
                    if (context != null) {
                        try {
                            result = this.doHandle(q, context);
                        } catch (UnsupportedOperationException e) {
                            result = this.doHandle(q);
                        }
                    } else {
                        result = this.doHandle(q);
                    }
                    return result.flatMap(r -> cacheResult(q, r, context));  // Context-aware caching
                })
            )
            .doOnSuccess(result -> onSuccess(query, result, duration, context))
            .doOnError(error -> onError(query, error, duration, context));
    }

    // The ONLY method you need to implement (standard)
    protected abstract Mono<R> doHandle(Q query);

    // Optional context-aware implementation
    protected Mono<R> doHandle(Q query, ExecutionContext context) {
        // Default implementation ignores context and delegates to standard doHandle
        return doHandle(query);
    }

    // Context-aware caching hooks
    protected Mono<R> checkCache(Q query, ExecutionContext context) {
        return checkCache(query);
    }
    protected Mono<R> checkCache(Q query) {
        return Mono.empty(); // Default: no caching
    }

    protected Mono<R> cacheResult(Q query, R result, ExecutionContext context) {
        return cacheResult(query, result);
    }
    protected Mono<R> cacheResult(Q query, R result) {
        return Mono.just(result); // Default: no caching
    }

    // Context-aware lifecycle hooks
    protected void onSuccess(Q query, R result, Duration duration, ExecutionContext context) {
        onSuccess(query, result, duration);
    }
    protected void onSuccess(Q query, R result, Duration duration) { }

    protected void onError(Q query, Throwable error, Duration duration, ExecutionContext context) {
        onError(query, error, duration);
    }
    protected void onError(Q query, Throwable error, Duration duration) { }
}
```

#### @QueryHandlerComponent Annotation
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface QueryHandlerComponent {
    String value() default "";         // Spring component name (optional)
    boolean cacheable() default true;  // Enable caching
    long cacheTtl() default -1;        // Cache TTL in seconds (-1 = use default)
    String[] cacheKeyFields() default {}; // Fields for cache key generation
    String cacheKeyPrefix() default "";   // Custom cache key prefix
    boolean metrics() default true;    // Enable metrics collection
    boolean tracing() default true;    // Enable distributed tracing
    long timeout() default -1;         // Query timeout in milliseconds (-1 = use default)
    int priority() default 0;          // Handler priority for registration
    String[] tags() default {};        // Custom tags for categorization
    String description() default "";   // Handler description for documentation
    boolean autoEvictCache() default false; // Enable automatic cache eviction
    String[] evictOnCommands() default {};  // Commands that trigger cache eviction
}
```

#### Real Query Handler Examples with ExecutionContext

**Standard Query Handler (Optional Context)**
```java
@QueryHandlerComponent(cacheable = true, cacheTtl = 300, metrics = true)
public class GetAccountBalanceHandler extends QueryHandler<GetAccountBalanceQuery, AccountBalance> {

    private final ServiceClient accountService;

    @Override
    protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query) {
        // Standard implementation without context
        return accountService.getAccountBalance(query.getAccountId())
            .map(this::mapToAccountBalance);
    }

    @Override
    protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query, ExecutionContext context) {
        // Enhanced implementation with context
        String tenantId = context.getTenantId();
        boolean enhancedView = context.getFeatureFlag("enhanced-view", false);

        return accountService.getTenantAccountBalance(query.getAccountId(), tenantId, enhancedView)
            .map(balance -> enhancedView ? mapToEnhancedBalance(balance) : mapToAccountBalance(balance));
    }

    // No caching setup needed - handled by annotation
    // No cache key generation needed - automatic from query + context
    // No metrics setup needed - handled by annotation
    // No validation setup needed - handled by framework
}
```

**Context-Aware Query Handler (Required Context)**
```java
@QueryHandlerComponent(cacheable = true, cacheTtl = 300, metrics = true)
public class GetTenantAccountBalanceHandler extends ContextAwareQueryHandler<GetAccountBalanceQuery, AccountBalance> {

    private final ServiceClient accountService;

    @Override
    protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query, ExecutionContext context) {
        // Context is always provided and validated
        String tenantId = context.getTenantId();
        String userId = context.getUserId();

        if (tenantId == null) {
            return Mono.error(new IllegalArgumentException("Tenant ID is required"));
        }

        return accountService.getTenantAccountBalance(query.getAccountId(), tenantId)
            .flatMap(balance -> validateUserAccess(balance, userId))
            .map(this::mapToAccountBalance);
    }

    private Mono<AccountBalance> validateUserAccess(AccountBalance balance, String userId) {
        // Tenant-specific access validation
        return accountService.validateUserAccess(balance.getAccountId(), userId)
            .filter(hasAccess -> hasAccess)
            .switchIfEmpty(Mono.error(new AccessDeniedException("User does not have access to this account")))
            .thenReturn(balance);
    }
}
```

### Caching System

#### Automatic Cache Key Generation
```java
public class QueryCacheKeyGenerator {

    public String generateKey(Query<?> query) {
        // Automatic key generation based on query type and properties
        String queryType = query.getClass().getSimpleName();
        String queryHash = calculateQueryHash(query);
        return String.format("%s:%s", queryType, queryHash);
    }

    private String calculateQueryHash(Query<?> query) {
        // Use reflection to create hash from all query properties
        return DigestUtils.md5Hex(JsonUtils.toJson(query));
    }
}
```

#### Cache Configuration Options
```yaml
firefly:
  cqrs:
    query:
      caching-enabled: true
      cache-ttl: 15m
      cache:
        type: LOCAL  # LOCAL (default) or REDIS
        redis:
          enabled: false  # Disabled by default - no connection attempts
          host: localhost
          port: 6379
          database: 0
          timeout: 2s
          key-prefix: "firefly:cqrs:"
          statistics: true
```

#### Redis Cache Integration (Optional)
```java
@Configuration
@ConditionalOnProperty(prefix = "firefly.cqrs.query.cache.redis", name = "enabled", havingValue = "true")
public class RedisCacheConfiguration {

    @Bean
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory, CqrsProperties properties) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(properties.getQuery().getCacheTtl())
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

## 🔧 Configuration and Customization

### Complete Configuration Reference
```yaml
firefly:
  cqrs:
    # Global framework toggle
    enabled: true

    # Command processing configuration
    command:
      timeout: 30s                    # Default command timeout
      metrics-enabled: true           # Enable command metrics
      tracing-enabled: true           # Enable distributed tracing

    # Query processing configuration
    query:
      timeout: 15s                    # Default query timeout
      caching-enabled: true           # Enable query caching
      cache-ttl: 15m                  # Default cache TTL
      metrics-enabled: true           # Enable query metrics
      tracing-enabled: true           # Enable distributed tracing

      # Cache configuration
      cache:
        type: LOCAL                   # LOCAL or REDIS
        redis:
          enabled: false              # Redis disabled by default
          host: localhost
          port: 6379
          database: 0
          password: null              # Optional password
          timeout: 2s
          key-prefix: "firefly:cqrs:"
          statistics: true

  # Domain Events integration
  events:
    enabled: true
    adapter: auto                     # AUTO, KAFKA, RABBIT, SQS, APPLICATION_EVENT, NOOP

  # Step Events integration (lib-transactional-engine)
  stepevents:
    enabled: true                     # Uses Domain Events infrastructure

  # ServiceClient framework
  service-client:
    enabled: true
    default-timeout: 30s

# Domain topic for step events
domain:
  topic: "banking-domain-events"
```

### Environment-Specific Configurations

#### Development Profile
```yaml
spring:
  profiles:
    active: development

firefly:
  cqrs:
    enabled: true
    command:
      timeout: 60s                    # Longer timeout for debugging
      metrics-enabled: true
    query:
      caching-enabled: false          # Disable caching for development

  events:
    adapter: application_event        # Local events for development
```

#### Production Profile
```yaml
spring:
  profiles:
    active: production

firefly:
  cqrs:
    enabled: true
    command:
      timeout: 30s
      metrics-enabled: true
      tracing-enabled: true
    query:
      caching-enabled: true
      cache-ttl: 30m                  # Longer TTL for production
      cache:
        type: REDIS
        redis:
          enabled: true
          host: redis-cluster.internal
          port: 6379
          timeout: 1s

  events:
    adapter: kafka                    # Kafka for production
    kafka:
      bootstrap-servers: kafka-cluster.internal:9092
      retries: 3
      acks: all
```

## 🧪 Testing Support

### Test Configuration
```java
@TestConfiguration
public class CqrsTestConfiguration {

    @Bean
    @Primary
    public CommandBus testCommandBus() {
        // Simplified command bus for testing
        return new TestCommandBus();
    }

    @Bean
    @Primary
    public CacheManager testCacheManager() {
        // No-op cache manager for testing
        return new NoOpCacheManager();
    }
}
```

### Handler Testing Example
```java
@ExtendWith(MockitoExtension.class)
class TransferMoneyHandlerTest {

    @Mock private ServiceClient accountService;
    @Mock private DomainEventPublisher eventPublisher;

    @InjectMocks private TransferMoneyHandler handler;

    @Test
    void shouldTransferMoneySuccessfully() {
        // Given
        TransferMoneyCommand command = TransferMoneyCommand.builder()
            .fromAccount("ACC-001")
            .toAccount("ACC-002")
            .amount(new BigDecimal("100.00"))
            .build();

        when(accountService.transfer(any())).thenReturn(Mono.just(transferResult));

        // When
        StepVerifier.create(handler.doHandle(command))
            .expectNext(expectedResult)
            .verifyComplete();

        // Then
        verify(accountService).transfer(any());
        verify(eventPublisher).publish(any(MoneyTransferredEvent.class));
    }
}
```

## 🚀 Best Practices and Patterns

### Command Design Patterns
```java
// ✅ GOOD - Immutable command with validation
@Data
@Builder
public class CreateAccountCommand implements Command<AccountResult> {
    @NotNull private final String customerId;
    @NotBlank @Email private final String email;
    @NotNull @Min(0) private final BigDecimal initialDeposit;

    // Only custom business validation needed
    @Override
    public Mono<ValidationResult> customValidate() {
        if (initialDeposit.compareTo(new BigDecimal("1000000")) > 0) {
            return Mono.just(ValidationResult.failure("initialDeposit", "Exceeds maximum limit"));
        }
        return Mono.just(ValidationResult.success());
    }
}

// ❌ BAD - Mutable command without validation
public class BadCommand implements Command<String> {
    public String customerId;  // Mutable field
    public String email;       // No validation
    // No validation implementation
}
```

### Handler Design Patterns with ExecutionContext

#### Flexible Handler Pattern (Recommended)
```java
// ✅ EXCELLENT - Supports both standard and context-aware processing
@CommandHandlerComponent(timeout = 30000, retries = 3)
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountResult> {

    private final AccountService accountService;
    private final DomainEventPublisher eventPublisher;

    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command) {
        // Standard implementation for backward compatibility
        return accountService.createAccount(command)
            .flatMap(account -> publishAccountCreatedEvent(account)
                .thenReturn(AccountResult.from(account)));
    }

    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command, ExecutionContext context) {
        // Enhanced implementation with context
        String tenantId = context.getTenantId();
        String userId = context.getUserId();
        boolean premiumFeatures = context.getFeatureFlag("premium-features", false);

        return accountService.createTenantAccount(command, tenantId, premiumFeatures)
            .flatMap(account -> publishEnhancedAccountCreatedEvent(account, userId, context)
                .thenReturn(AccountResult.from(account)));
    }

    private Mono<Void> publishAccountCreatedEvent(Account account) {
        return eventPublisher.publish(AccountCreatedEvent.from(account));
    }

    private Mono<Void> publishEnhancedAccountCreatedEvent(Account account, String userId, ExecutionContext context) {
        AccountCreatedEvent event = AccountCreatedEvent.builder()
            .account(account)
            .createdBy(userId)
            .tenantId(context.getTenantId())
            .source(context.getSource())
            .build();
        return eventPublisher.publish(event);
    }
}
```

#### Context-Aware Handler Pattern (For Context-Required Operations)
```java
// ✅ GOOD - Enforces context requirement for multi-tenant operations
@CommandHandlerComponent(timeout = 30000, retries = 3)
public class CreateTenantAccountHandler extends ContextAwareCommandHandler<CreateAccountCommand, AccountResult> {

    private final TenantAccountService tenantAccountService;
    private final DomainEventPublisher eventPublisher;

    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command, ExecutionContext context) {
        // Context is guaranteed to be present
        String tenantId = context.getTenantId();
        String userId = context.getUserId();

        if (tenantId == null) {
            return Mono.error(new IllegalArgumentException("Tenant ID is required for tenant account creation"));
        }

        return tenantAccountService.createAccountForTenant(command, tenantId, userId)
            .flatMap(account -> publishTenantAccountCreatedEvent(account, context)
                .thenReturn(AccountResult.from(account)));
    }

    private Mono<Void> publishTenantAccountCreatedEvent(Account account, ExecutionContext context) {
        TenantAccountCreatedEvent event = TenantAccountCreatedEvent.builder()
            .account(account)
            .tenantId(context.getTenantId())
            .createdBy(context.getUserId())
            .organizationId(context.getOrganizationId())
            .source(context.getSource())
            .build();
        return eventPublisher.publish(event);
    }
}
```

#### Anti-Patterns to Avoid
```java
// ❌ BAD - Multiple responsibilities, complex logic
public class BadHandler extends CommandHandler<SomeCommand, String> {
    @Override
    protected Mono<String> doHandle(SomeCommand command) {
        // Multiple unrelated operations
        // Complex business logic mixed with infrastructure concerns
        // No clear separation of concerns
    }
}

// ❌ BAD - Ignoring ExecutionContext when it should be used
@CommandHandlerComponent
public class IgnoresContextHandler extends CommandHandler<CreateAccountCommand, AccountResult> {
    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command, ExecutionContext context) {
        // Ignores valuable context information
        return doHandle(command); // Just delegates to standard method
    }
}
```
```

### Error Handling Patterns
```java
// ✅ GOOD - Specific exceptions with context
@Override
protected Mono<AccountResult> doHandle(CreateAccountCommand command) {
    return accountService.createAccount(command)
        .onErrorMap(DuplicateAccountException.class,
                   ex -> new BusinessRuleViolationException("Account already exists", ex))
        .onErrorMap(InsufficientFundsException.class,
                   ex -> new BusinessRuleViolationException("Insufficient funds for initial deposit", ex));
}

// ❌ BAD - Generic error handling
@Override
protected Mono<String> doHandle(SomeCommand command) {
    return someService.doSomething(command)
        .onErrorReturn("ERROR");  // Loses error context
}
```

---

## 📡 Domain Events Architecture

### Event-Driven Communication

The Domain Events framework provides a unified approach to event publishing and consumption across different messaging infrastructures.

#### Adapter Pattern Implementation

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           Domain Events Architecture                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  Application Layer                                                              │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐              │
│  │ @EventPublisher │    │ Programmatic    │    │ StepEvent       │              │
│  │ Annotation      │    │ Publishing      │    │ Bridge          │              │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘              │
│           │                       │                       │                     │
│           └───────────────────────┼───────────────────────┘                     │
│                                   ▼                                             │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                    DomainEventPublisher Interface                           ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                   │                                             │
│                                   ▼                                             │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                      Auto-Configuration Layer                               ││
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌───────────┐  ││
│  │  │ Adapter         │ │ Bean Detection  │ │ Priority        │ │ Fallback  │  ││
│  │  │ Selection       │ │ (KafkaTemplate, │ │ Resolution      │ │ Strategy  │  ││
│  │  │ Logic           │ │ ConnectionFact) │ │                 │ │           │  ││
│  │  └─────────────────┘ └─────────────────┘ └─────────────────┘ └───────────┘  ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                   │                                             │
│                                   ▼                                             │
│  Infrastructure Layer                                                           │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────┐  │
│  │   Kafka     │ │  RabbitMQ   │ │    SQS      │ │   Kinesis   │ │   Spring  │  │
│  │  Adapter    │ │   Adapter   │ │   Adapter   │ │   Adapter   │ │   Events  │  │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ └───────────┘  │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### Event Envelope Structure

```java
public final class DomainEventEnvelope {
    private String topic;           // Routing destination
    private String type;            // Event classification
    private String key;             // Partitioning/routing key
    private Object payload;         // Event data
    private Instant timestamp;      // Event occurrence time
    private Map<String, Object> headers;   // Transport metadata
    private Map<String, Object> metadata;  // Event context
}
```

### Messaging Infrastructure Integration

#### Automatic Adapter Selection

The framework automatically selects the best available messaging infrastructure:

1. **Kafka** (highest priority) - High throughput, ordering guarantees
2. **RabbitMQ** - Flexible routing, reliable delivery
3. **AWS Kinesis** - Real-time streaming, analytics integration
4. **AWS SQS** - Cloud-native, FIFO queues
5. **Spring ApplicationEvent** (fallback) - In-process events

#### Configuration-Driven Behavior

```yaml
firefly:
  events:
    adapter: auto  # Automatic selection
    kafka:
      topic: banking-events
      partition-key: "${key}"
    rabbit:
      exchange: banking.events
      routing-key: "${type}"
```

---

## 🔗 Service Client Framework Architecture

### Unified Communication Interface

The Service Client Framework provides a consistent API for all service-to-service communication patterns.

#### Client Type Abstraction

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        Service Client Architecture                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  Application Layer                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                      ServiceClient Interface                                ││
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌───────────┐  ││
│  │  │ HTTP Methods    │ │ Request Builder │ │ Response Mapper │ │ Streaming │  ││
│  │  │ (GET/POST/PUT)  │ │ Pattern         │ │ Type Safety     │ │ Support   │  ││
│  │  └─────────────────┘ └─────────────────┘ └─────────────────┘ └───────────┘  ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                   │                                             │
│                                   ▼                                             │
│  Implementation Layer                                                           │
│  ┌─────────────────┐              ┌─────────────────┐                           │
│  │ REST Client     │              │ gRPC Client     │                           │
│  │ Implementation  │              │ Implementation  │                           │
│  │                 │              │                 │                           │
│  │ ┌─────────────┐ │              │ ┌─────────────┐ │                           │
│  │ │ WebClient   │ │              │ │ ManagedChan │ │                           │
│  │ │ Integration │ │              │ │ nel + Stub  │ │                           │
│  │ └─────────────┘ │              │ └─────────────┘ │                           │
│  └─────────────────┘              └─────────────────┘                           │
│           │                                │                                    │
│           └────────────────┬───────────────┘                                    │
│                            ▼                                                    │
│  Resilience Layer                                                               │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                    Circuit Breaker Manager                                  ││
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌───────────┐  ││
│  │  │ State Machine   │ │ Sliding Window  │ │ Failure Rate    │ │ Auto      │  ││
│  │  │ (CLOSED/OPEN/   │ │ Tracking        │ │ Calculation     │ │ Recovery  │  ││
│  │  │ HALF_OPEN)      │ │                 │ │                 │ │           │  ││
│  │  └─────────────────┘ └─────────────────┘ └─────────────────┘ └───────────┘  ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### Builder Pattern Implementation

```java
// Fluent API for request construction
ServiceClient.rest("user-service")
    .baseUrl("http://user-service:8080")
    .timeout(Duration.ofSeconds(30))
    .circuitBreakerManager(circuitBreakerManager)
    .build()
    .get("/users/{id}", User.class)
    .withPathParam("id", userId)
    .withHeader("Authorization", "Bearer " + token)
    .execute();
```

### Protocol-Specific Optimizations

#### REST Client Features
- **WebClient Integration**: Reactive HTTP client
- **Connection Pooling**: Configurable connection management
- **Request/Response Mapping**: Automatic JSON serialization
- **Header Management**: Default and per-request headers

#### gRPC Client Features
- **ManagedChannel**: Connection lifecycle management
- **Stub Management**: Type-safe service stubs
- **Streaming Support**: Bidirectional streaming operations
- **Metadata Propagation**: Automatic context headers

## 🛡️ Resilience Patterns Architecture

### Circuit Breaker State Management

The resilience framework implements sophisticated circuit breaker patterns with real state management.

#### State Machine Implementation

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         Circuit Breaker State Machine                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│                              ┌─────────────────┐                                │
│                              │     CLOSED      │                                │
│                              │  (Normal Ops)   │                                │
│                              │                 │                                │
│                              │ • Allow all     │                                │
│                              │   requests      │                                │
│                              │ • Monitor       │                                │
│                              │   failures      │                                │
│                              └─────────────────┘                                │
│                                       │                                         │
│                                       │ Failure rate                            │
│                                       │ exceeds threshold                       │
│                                       ▼                                         │
│                              ┌─────────────────┐                                │
│                              │      OPEN       │                                │
│                              │ (Service Down)  │                                │
│                              │                 │                                │
│                              │ • Reject all    │                                │
│                              │   requests      │                                │
│                              │ • Fast fail     │                                │
│                              │ • Wait timer    │                                │
│                              └─────────────────┘                                │
│                                       │                                         │
│                                       │ Wait duration                           │
│                                       │ expires                                 │
│                                       ▼                                         │
│                              ┌─────────────────┐                                │
│                              │   HALF_OPEN     │                                │
│                              │ (Testing)       │                                │
│                              │                 │                                │
│                              │ • Allow limited │                                │
│                              │   requests      │                                │
│                              │ • Test recovery │                                │
│                              └─────────────────┘                                │
│                                   │       │                                     │
│                        Success    │       │    Failure                          │
│                        threshold  │       │    detected                         │
│                        reached    │       │                                     │
│                                   ▼       ▼                                     │
│                          Back to CLOSED  Back to OPEN                           │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### Sliding Window Failure Tracking

```java
public class SlidingWindow {
    private final boolean[] callResults;  // Circular buffer
    private final AtomicInteger currentIndex;
    private final AtomicLong totalCalls;
    private final AtomicInteger successCount;
    private final AtomicInteger failureCount;

    // Thread-safe failure rate calculation
    public double getFailureRate() {
        int failures = failureCount.get();
        int total = Math.min(totalCalls.get(), windowSize);
        return total == 0 ? 0.0 : (double) failures / total;
    }
}
```

### Advanced Resilience Patterns

#### Multi-Pattern Integration

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      Advanced Resilience Manager                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  Request Flow                                                                   │
│       │                                                                         │
│       ▼                                                                         │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐              │
│  │ Load Shedding   │───▶│ Rate Limiting   │───▶│ Bulkhead        │              │
│  │ • High load     │    │ • Requests/sec  │    │ • Resource      │              │
│  │   detection     │    │ • Burst capacity│    │   isolation     │              │
│  │ • Request drop  │    │ • Token bucket  │    │ • Concurrency   │              │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘              │
│                                   │                       │                     │
│                                   ▼                       ▼                     │
│                          ┌─────────────────┐    ┌─────────────────┐             │
│                          │ Adaptive        │    │ Circuit Breaker │             │
│                          │ Timeout         │    │ Protection      │             │
│                          │ • Historical    │    │ • State machine │             │
│                          │   performance   │    │ • Failure rate  │             │
│                          │ • Dynamic adj   │    │ • Auto recovery │             │
│                          └─────────────────┘    └─────────────────┘             │
│                                   │                       │                     │
│                                   └───────────┬───────────┘                     │
│                                               ▼                                 │
│                                      ┌─────────────────┐                        │
│                                      │ Service Call    │                        │
│                                      │ Execution       │                        │
│                                      └─────────────────┘                        │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔍 Distributed Tracing Architecture

### Correlation Context Management

The tracing framework provides comprehensive request correlation across distributed services.

#### Context Propagation Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        Distributed Tracing Flow                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  Service A                    Service B                    Service C            │
│  ┌─────────────────┐          ┌─────────────────┐          ┌─────────────────┐  │
│  │ HTTP Request    │          │ HTTP Request    │          │ HTTP Request    │  │
│  │ (Entry Point)   │          │ (Downstream)    │          │ (Downstream)    │  │
│  └─────────────────┘          └─────────────────┘          └─────────────────┘  │
│           │                            │                            │           │
│           ▼                            ▼                            ▼           │
│  ┌─────────────────┐          ┌─────────────────┐          ┌─────────────────┐  │
│  │ Generate        │          │ Extract Headers │          │ Extract Headers │  │
│  │ Correlation ID  │          │ X-Correlation-ID│          │ X-Correlation-ID│  │
│  │ Generate        │          │ X-Trace-ID      │          │ X-Trace-ID      │  │
│  │ Trace ID        │          │                 │          │                 │  │
│  └─────────────────┘          └─────────────────┘          └─────────────────┘  │
│           │                            │                            │           │
│           ▼                            ▼                            ▼           │
│  ┌─────────────────┐          ┌─────────────────┐          ┌─────────────────┐  │
│  │ Set MDC Context │          │ Set MDC Context │          │ Set MDC Context │  │
│  │ correlationId   │          │ correlationId   │          │ correlationId   │  │
│  │ traceId         │          │ traceId         │          │ traceId         │  │
│  └─────────────────┘          └─────────────────┘          └─────────────────┘  │
│           │                            │                            │           │
│           ▼                            ▼                            ▼           │
│  ┌─────────────────┐          ┌─────────────────┐          ┌─────────────────┐  │
│  │ Business Logic  │          │ Business Logic  │          │ Business Logic  │  │
│  │ Processing      │          │ Processing      │          │ Processing      │  │
│  └─────────────────┘          └─────────────────┘          └─────────────────┘  │
│           │                            │                            │           │
│           ▼                            ▼                            ▼           │
│  ┌─────────────────┐          ┌─────────────────┐          ┌─────────────────┐  │
│  │ Propagate to    │          │ Propagate to    │          │ Event Publishing│  │
│  │ Service B       │          │ Service C       │          │ with Context    │  │
│  │ (Auto Headers)  │          │ (Auto Headers)  │          │                 │  │
│  └─────────────────┘          └─────────────────┘          └─────────────────┘  │
│                                                                                 │
│  Same Correlation ID: 550e8400-e29b-41d4-a716-446655440000                      │
│  Same Trace ID: 1234567890abcdef1234567890abcdef                                │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### Context Storage Strategy

```java
public class CorrelationContext {
    // Thread-local for synchronous operations
    private static final ThreadLocal<String> CURRENT_CORRELATION_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_TRACE_ID = new ThreadLocal<>();

    // Concurrent map for async operations
    private final ConcurrentMap<String, ContextInfo> contextStorage = new ConcurrentHashMap<>();

    // Reactive context propagation
    public <T> Mono<T> withContext(Mono<T> mono) {
        String correlationId = getOrCreateCorrelationId();
        String traceId = getOrCreateTraceId();

        return mono.contextWrite(Context.of(
            CORRELATION_ID_KEY, correlationId,
            TRACE_ID_KEY, traceId
        ));
    }
}
```

### Integration Points

#### Automatic Header Propagation

- **Service Clients**: Automatically add tracing headers to outbound requests
- **Domain Events**: Include correlation context in event metadata
- **MDC Integration**: Automatic logging context for all log statements
- **Reactive Streams**: Context propagation through Reactor Context

## ⚙️ lib-transactional-engine Integration Architecture

### Bridge Pattern Implementation

The Firefly Common Domain Library provides a **bridge pattern integration** with the [lib-transactional-engine](https://github.com/firefly-oss/lib-transactional-engine) saga orchestration framework. This integration allows saga step events to leverage the existing domain events infrastructure.

> **Important**: This library does **NOT** implement saga orchestration. It only provides a bridge to publish step events from lib-transactional-engine through the domain events infrastructure. For saga implementation, use the lib-transactional-engine directly.

#### Integration Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    lib-transactional-engine Integration                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  Developer Code (Using lib-transactional-engine)                                │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐              │
│  │ @Saga           │    │ @SagaStep       │    │ @SagaStep       │              │
│  │ Definitions     │    │ Methods         │    │ Compensation    │              │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘              │
│           │                       │                       │                     │
│           ▼                       ▼                       ▼                     │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                    lib-transactional-engine                                 ││
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌───────────┐  ││
│  │  │ SagaEngine      │ │ Step Executor   │ │ State Machine   │ │ Event     │  ││
│  │  │ (Orchestrator)  │ │ (Reactive)      │ │ (Compensation)  │ │ Publisher │  ││
│  │  └─────────────────┘ └─────────────────┘ └─────────────────┘ └───────────┘  ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                   │                                             │
│                                   ▼ StepEventEnvelope                           │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │              StepEventPublisherBridge (lib-common-domain)                   ││
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌───────────┐  ││
│  │  │ Event           │ │ Metadata        │ │ Key Generation  │ │ Topic     │  ││
│  │  │ Transformation  │ │ Enrichment      │ │ (Auto)          │ │ Resolution│  ││
│  │  └─────────────────┘ └─────────────────┘ └─────────────────┘ └───────────┘  ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                   │                                             │
│                                   ▼ DomainEventEnvelope                         │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                      Domain Events Infrastructure                           ││
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌───────────┐  ││
│  │  │ Event Publisher │ │ Adapter         │ │ Messaging       │ │ Event     │  ││
│  │  │ Interface       │ │ Selection       │ │ Infrastructure  │ │ Consumers │  ││
│  │  └─────────────────┘ └─────────────────┘ └─────────────────┘ └───────────┘  ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### Step Event Transformation

```java
// StepEventEnvelope from lib-transactional-engine
StepEventEnvelope stepEvent = new StepEventEnvelope(
    "MoneyTransferSaga",           // sagaName
    "SAGA-67890",                  // sagaId
    "step-transfer",               // stepId
    "banking-step-events",         // topic
    "transfer.step.completed",     // type
    "TXN-12345",                   // key
    transferPayload,               // payload
    headers,                       // headers
    attempts,                      // execution attempts
    latencyMs,                     // execution time
    startedAt,                     // start timestamp
    completedAt,                   // completion timestamp
    "SUCCESS"                      // result type
);

// Transformed to DomainEventEnvelope
DomainEventEnvelope domainEvent = DomainEventEnvelope.builder()
    .topic(stepEvent.getTopic())
    .type(stepEvent.getType())
    .key(stepEvent.getKey())
    .payload(stepEvent)            // Entire step event as payload
    .timestamp(stepEvent.getCompletedAt())
    .headers(stepEvent.getHeaders())
    .metadata(Map.of(              // Enriched metadata
        "step.attempts", stepEvent.getAttempts(),
        "step.latency_ms", stepEvent.getLatencyMs(),
        "step.started_at", stepEvent.getStartedAt(),
        "step.completed_at", stepEvent.getCompletedAt(),
        "step.result_type", stepEvent.getResultType()
    ))
    .build();
```

### Benefits of Integration

#### Unified Infrastructure
- **Single Configuration**: Both domain and step events use same messaging setup
- **Shared Adapters**: Leverage Kafka, RabbitMQ, SQS, etc. for step events
- **Consistent Resilience**: Circuit breakers and retries apply to step events
- **Unified Monitoring**: Step events appear in same observability systems

#### Developer Experience
- **Manual Integration**: Developers maintain full control over saga orchestration
- **Flexible Patterns**: Support for any saga implementation approach
- **No Lock-in**: Bridge pattern allows easy migration or customization
- **Rich Metadata**: Step events include execution metrics and correlation data

#### Event Processing
- **Event Handlers**: Use same `@EventHandler` pattern for step events
- **Type Safety**: Full type safety for step event payloads
- **Filtering**: Route step events based on saga type, step type, etc.
- **Correlation**: Automatic correlation context propagation

---

## 🔧 Auto-Configuration Architecture

### Spring Boot Integration

The library provides comprehensive auto-configuration for zero-setup development experience.

#### Configuration Classes

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        Auto-Configuration Architecture                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                      CqrsAutoConfiguration                                  ││
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌───────────┐  ││
│  │  │ CommandBus      │ │ QueryBus        │ │ Handler         │ │ Validation│  ││
│  │  │ Bean Creation   │ │ Bean Creation   │ │ Registry        │ │ Service   │  ││
│  │  └─────────────────┘ └─────────────────┘ └─────────────────┘ └───────────┘  ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                   DomainEventsAutoConfiguration                             ││
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌───────────┐  ││
│  │  │ Adapter         │ │ Publisher       │ │ Subscriber      │ │ Properties│  ││
│  │  │ Detection       │ │ Bean Creation   │ │ Bean Creation   │ │ Binding   │  ││
│  │  └─────────────────┘ └─────────────────┘ └─────────────────┘ └───────────┘  ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                 ServiceClientAutoConfiguration                              ││
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌───────────┐  ││
│  │  │ WebClient       │ │ Circuit Breaker │ │ Client Builders │ │ gRPC      │  ││
│  │  │ Builder         │ │ Manager         │ │ (REST/gRPC)     │ │ Factory   │  ││
│  │  └─────────────────┘ └─────────────────┘ └─────────────────┘ └───────────┘  ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                    StepBridgeConfiguration                                  ││
│  │  ┌─────────────────┐ ┌─────────────────┐                                    ││
│  │  │ Bridge Bean     │ │ Topic           │                                    ││
│  │  │ Creation        │ │ Configuration   │                                    ││
│  │  └─────────────────┘ └─────────────────┘                                    ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### Conditional Bean Creation

```java
@Configuration
@ConditionalOnProperty(prefix = "firefly.cqrs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CqrsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CommandBus commandBus(CommandHandlerRegistry registry,
                                CommandValidationService validation,
                                CommandMetricsService metrics) {
        return new DefaultCommandBus(registry, validation, metrics);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    public DomainEventPublisher kafkaEventPublisher(ApplicationContext ctx) {
        return new KafkaDomainEventPublisher(ctx, properties.getKafka());
    }
}
```

This comprehensive architecture provides a complete platform for building distributed banking applications with enterprise-grade patterns and zero-boilerplate development experience.

---
