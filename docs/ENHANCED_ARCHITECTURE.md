# Firefly Common Domain Library - Enhanced Deep Architecture Guide

## 🏗️ Overview

The Firefly Common Domain Library provides a **comprehensive, enterprise-grade CQRS framework** designed specifically for banking applications. The architecture emphasizes **zero-boilerplate development**, **automatic type detection**, **separation of concerns**, and **seamless Spring Boot integration**.

## 🎯 Core Philosophy

### The Single Best Way Approach
- **ONE way** to create command handlers: `@CommandHandlerComponent` + extend `CommandHandler<Command, Result>`
- **ONE way** to create query handlers: `@QueryHandlerComponent` + extend `QueryHandler<Query, Result>`
- **ZERO boilerplate**: No manual type detection, validation setup, or metrics configuration
- **FOCUS on business logic**: Write only the `doHandle()` method - everything else is automatic

## 🏛️ Enhanced CQRS Architecture with Separated Services

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           CQRS Framework Architecture                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────┐                    ┌─────────────────┐                    │
│  │   Commands      │                    │    Queries      │                    │
│  │   (Write Side)  │                    │   (Read Side)   │                    │
│  │                 │                    │                 │                    │
│  │ @NotNull        │                    │ @Cacheable      │                    │
│  │ @NotBlank       │                    │ @QueryHandler   │                    │
│  │ @CommandHandler │                    │                 │                    │
│  └─────────────────┘                    └─────────────────┘                    │
│           │                                       │                            │
│           ▼                                       ▼                            │
│  ┌─────────────────┐                    ┌─────────────────┐                    │
│  │  CommandBus     │                    │   QueryBus      │                    │
│  │  (Orchestrator) │                    │  (Orchestrator) │                    │
│  └─────────────────┘                    └─────────────────┘                    │
│           │                                       │                            │
│           ▼                                       ▼                            │
│  ┌─────────────────────────────────────┐ ┌─────────────────┐                    │
│  │        Dedicated Services           │ │  QueryHandler   │                    │
│  │                                     │ │                 │                    │
│  │ ┌─────────────────┐                 │ │ • Auto Caching  │                    │
│  │ │ HandlerRegistry │                 │ │ • Type Detection│                    │
│  │ │ • Discovery     │                 │ │ • Validation    │                    │
│  │ │ • Registration  │                 │ │ • Metrics       │                    │
│  │ └─────────────────┘                 │ └─────────────────┘                    │
│  │                                     │                                        │
│  │ ┌─────────────────┐                 │                                        │
│  │ │ValidationService│                 │                                        │
│  │ │ • Jakarta Bean  │                 │                                        │
│  │ │ • Custom Rules  │                 │                                        │
│  │ └─────────────────┘                 │                                        │
│  │                                     │                                        │
│  │ ┌─────────────────┐                 │                                        │
│  │ │ MetricsService  │                 │                                        │
│  │ │ • Success/Fail  │                 │                                        │
│  │ │ • Timing        │                 │                                        │
│  │ │ • Per-Type      │                 │                                        │
│  │ └─────────────────┘                 │                                        │
│  └─────────────────────────────────────┘                                        │
│           │                                                                     │
│           ▼                                                                     │
│  ┌─────────────────┐                                                            │
│  │ CommandHandler  │                                                            │
│  │                 │                                                            │
│  │ • Auto Type     │                                                            │
│  │ • Built-in      │                                                            │
│  │   Features      │                                                            │
│  │ • Zero          │                                                            │
│  │   Boilerplate   │                                                            │
│  └─────────────────┘                                                            │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
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
@Component
public abstract class CommandHandler<C extends Command<R>, R> {
    
    // Automatic type detection from generics
    public Class<C> getCommandType() { 
        return GenericTypeResolver.resolveCommandType(this.getClass()); 
    }
    
    // Main processing pipeline with built-in features
    public final Mono<R> handle(C command) {
        return Mono.fromCallable(() -> command)
            .flatMap(this::preProcess)      // Hook for custom pre-processing
            .flatMap(this::doHandle)        // Your business logic here
            .flatMap(result -> postProcess(command, result))  // Hook for post-processing
            .doOnSuccess(result -> onSuccess(command, result, duration))
            .doOnError(error -> onError(command, error, duration))
            .onErrorMap(this::mapError);
    }
    
    // The ONLY method you need to implement
    protected abstract Mono<R> doHandle(C command);
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
    long timeout() default 30000;      // Processing timeout in milliseconds
    int retries() default 0;           // Number of retries on failure
    boolean metrics() default true;    // Enable metrics collection
    boolean tracing() default true;    // Enable distributed tracing
    String[] tags() default {};        // Custom tags for metrics
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
