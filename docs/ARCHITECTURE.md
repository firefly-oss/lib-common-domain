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

### The Single Best Way Approach
- **ONE way** to create command handlers: `@CommandHandlerComponent` + extend `CommandHandler<Command, Result>`
- **ONE way** to create query handlers: `@QueryHandlerComponent` + extend `QueryHandler<Query, Result>`
- **ZERO boilerplate**: No manual type detection, validation setup, or metrics configuration
- **FOCUS on business logic**: Write only the `doHandle()` method - everything else is automatic

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

### Component Integration Flow

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                        Request Processing Flow                                              │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                                             │
│  HTTP Request                                                                                               │
│       │                                                                                                     │
│       ▼                                                                                                     │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐                   │
│  │ Correlation     │───▶│ CQRS Framework  │───▶│ Business Logic  │───▶│ Domain Events   │                   │
│  │ Context         │    │ Command/Query   │    │ Processing      │    │ Publishing      │                   │
│  │ (Trace/Corr ID) │    │ Bus             │    │                 │    │                 │                   │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘                   │
│       │                          │                       │                       │                          │
│       │                          │                       │                       ▼                          │
│       │                          │                       │              ┌─────────────────┐                 │
│       │                          │                       │              │ Messaging       │                 │
│       │                          │                       │              │ Infrastructure  │                 │
│       │                          │                       │              │ (Kafka/RabbitMQ)│                 │
│       │                          │                       │              └─────────────────┘                 │
│       │                          │                       │                                                  │
│       │                          │                       ▼                                                  │
│       │                          │              ┌─────────────────┐                                         │
│       │                          │              │ Service Client  │                                         │
│       │                          │              │ Framework       │                                         │
│       │                          │              │ (REST/gRPC)     │                                         │
│       │                          │              └─────────────────┘                                         │
│       │                          │                       │                                                  │
│       │                          │                       ▼                                                  │
│       │                          │              ┌─────────────────┐                                         │
│       │                          │              │ Circuit Breaker │                                         │
│       │                          │              │ Protection      │                                         │
│       │                          │              └─────────────────┘                                         │
│       │                          │                       │                                                  │
│       │                          ▼                       ▼                                                  │
│       │                 ┌─────────────────┐    ┌─────────────────┐                                          │
│       │                 │ Validation      │    │ Downstream      │                                          │
│       │                 │ Metrics         │    │ Service Call    │                                          │
│       │                 │ Caching         │    │                 │                                          │
│       │                 └─────────────────┘    └─────────────────┘                                          │
│       │                                                                                                     │
│       ▼                                                                                                     │
│  ┌─────────────────┐                                                                                        │
│  │ Response with   │                                                                                        │
│  │ Trace Headers   │                                                                                        │
│  └─────────────────┘                                                                                        │
│                                                                                                             │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

```

## 🔧 Core Components Deep Dive

### 1. Command Processing Pipeline

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

#### Command Handler Base Class
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
        Instant startTime = Instant.now();
        String commandId = command.getCommandId();
        String commandTypeName = commandType.getSimpleName();

        return Mono.fromCallable(() -> {
                log.debug("Starting command processing: {} [{}]", commandTypeName, commandId);
                return command;
            })
            .flatMap(this::preProcess)      // Hook for custom pre-processing
            .flatMap(this::doHandle)        // Your business logic here
            .flatMap(result -> postProcess(command, result))  // Hook for post-processing
            .doOnSuccess(result -> {
                Duration duration = Duration.between(startTime, Instant.now());
                log.info("Command processed successfully: {} [{}] in {}ms",
                    commandTypeName, commandId, duration.toMillis());
                onSuccess(command, result, duration);
            })
            .doOnError(error -> {
                Duration duration = Duration.between(startTime, Instant.now());
                log.error("Command processing failed: {} [{}] in {}ms - {}",
                    commandTypeName, commandId, duration.toMillis(), error.getMessage());
                onError(command, error, duration);
            })
            .onErrorMap(this::mapError);
    }

    // The ONLY method you need to implement
    protected abstract Mono<R> doHandle(C command);

    // Extensibility hooks
    protected Mono<C> preProcess(C command) { return Mono.just(command); }
    protected Mono<R> postProcess(C command, R result) { return Mono.just(result); }
    protected void onSuccess(C command, R result, Duration duration) { }
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

#### DefaultCommandBus (Orchestrator)
**Responsibility**: Clean orchestration of the processing pipeline
```java
@Component
public class DefaultCommandBus implements CommandBus {
    
    public <R> Mono<R> send(Command<R> command) {
        // Step 1: Find handler
        CommandHandler<Command<R>, R> handler = handlerRegistry.findHandler(command.getClass())
            .orElseThrow(() -> CommandHandlerNotFoundException.forCommand(command, availableHandlers));
        
        // Step 2: Set correlation context
        if (command.getCorrelationId() != null) {
            correlationContext.setCorrelationId(command.getCorrelationId());
        }
        
        // Step 3: Validate command
        return validationService.validateCommand(command)
            .then(Mono.defer(() -> {
                // Step 4: Execute handler
                return handler.handle(command);
            }))
            // Step 5: Record metrics and handle completion
            .doOnSuccess(result -> metricsService.recordCommandSuccess(command, processingTime))
            .doOnError(error -> metricsService.recordCommandFailure(command, error, processingTime))
            .doFinally(signalType -> correlationContext.clear());
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

### Command Processing Sequence
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

### For Operations
- **Production Ready**: Built-in resilience and monitoring
- **Observability**: Automatic logging, metrics, and tracing
- **Configuration Driven**: Extensive configuration options
- **Cache Support**: Local and Redis caching for queries
- **Flexible Deployment**: Works with any Spring Boot application

### For Architecture
- **Clean Separation**: Commands vs Queries with clear boundaries
- **Testable**: Each service can be tested independently
- **Maintainable**: Single responsibility principle throughout
- **Extensible**: Hook points for custom behavior
- **Standards Compliant**: Jakarta validation, Micrometer metrics, Spring patterns

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

#### Query Handler Base Class
```java
@Component
public abstract class QueryHandler<Q extends Query<R>, R> {

    // Automatic type detection from generics
    public Class<Q> getQueryType() {
        return GenericTypeResolver.resolveQueryType(this.getClass());
    }

    // Main processing pipeline with caching
    public final Mono<R> handle(Q query) {
        return Mono.fromCallable(() -> query)
            .flatMap(this::checkCache)          // Check cache first
            .switchIfEmpty(
                Mono.defer(() ->
                    this.doHandle(query)        // Your business logic
                        .flatMap(result -> cacheResult(query, result))  // Cache result
                )
            )
            .doOnSuccess(result -> onSuccess(query, result, duration))
            .doOnError(error -> onError(query, error, duration));
    }

    // The ONLY method you need to implement
    protected abstract Mono<R> doHandle(Q query);
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

#### Real Query Handler Example
```java
@QueryHandlerComponent(cacheable = true, cacheTtl = 300, metrics = true)
public class GetAccountBalanceHandler extends QueryHandler<GetAccountBalanceQuery, AccountBalance> {

    private final ServiceClient accountService;

    @Override
    protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query) {
        // ONLY business logic - caching, validation, metrics handled automatically!
        return accountService.getAccountBalance(query.getAccountId())
            .map(this::mapToAccountBalance);
    }

    // No caching setup needed - handled by annotation
    // No cache key generation needed - automatic from query
    // No metrics setup needed - handled by annotation
    // No validation setup needed - handled by framework
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

### Handler Design Patterns
```java
// ✅ GOOD - Single responsibility, clear dependencies
@CommandHandlerComponent(timeout = 30000, retries = 3)
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountResult> {

    private final AccountService accountService;
    private final DomainEventPublisher eventPublisher;

    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command) {
        return accountService.createAccount(command)
            .flatMap(account -> publishAccountCreatedEvent(account)
                .thenReturn(AccountResult.from(account)));
    }

    private Mono<Void> publishAccountCreatedEvent(Account account) {
        return eventPublisher.publish(AccountCreatedEvent.from(account));
    }
}

// ❌ BAD - Multiple responsibilities, complex logic
public class BadHandler extends CommandHandler<SomeCommand, String> {
    @Override
    protected Mono<String> doHandle(SomeCommand command) {
        // Multiple unrelated operations
        // Complex business logic mixed with infrastructure concerns
        // No clear separation of concerns
    }
}
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
