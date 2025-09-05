# API Reference

This document provides detailed API reference documentation for the Firefly Common Domain Library, including complete method signatures, parameters, return types, and usage examples.

## Table of Contents

- [CQRS Framework](#cqrs-framework)
- [ServiceClient Framework](#serviceclient-framework)
- [Domain Events](#domain-events)
- [Validation Framework](#validation-framework)
- [Correlation Context](#correlation-context)
- [Configuration Properties](#configuration-properties)

## CQRS Framework

### Command Interface

```java
package com.firefly.common.domain.cqrs.command;

public interface Command<R> {
    
    /**
     * Returns a unique identifier for this command instance.
     * @return the unique command identifier, never null
     */
    default String getCommandId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Returns the timestamp when this command was created.
     * @return the creation timestamp, never null
     */
    default Instant getTimestamp() {
        return Instant.now();
    }
    
    /**
     * Returns the correlation ID for distributed tracing.
     * @return the correlation ID, or null if not set
     */
    default String getCorrelationId() {
        return null;
    }
    
    /**
     * Returns the user or system identifier that initiated this command.
     * @return the initiator ID, or null if not set
     */
    default String getInitiatedBy() {
        return null;
    }
    
    /**
     * Returns additional metadata associated with this command.
     * @return metadata map, or null if no metadata
     */
    default Map<String, Object> getMetadata() {
        return null;
    }

    /**
     * Returns the expected result type for this command.
     * @return the result type class
     */
    default Class<R> getResultType() {
        return (Class<R>) Object.class;
    }

    /**
     * Validates this command and returns a validation result.
     * @return a Mono containing the validation result, never null
     */
    default Mono<ValidationResult> validate() {
        return Mono.just(ValidationResult.success());
    }
}
```

### CommandHandler Interface

```java
package com.firefly.common.domain.cqrs.command;

public interface CommandHandler<C extends Command<R>, R> {
    
    /**
     * Handles the given command asynchronously.
     * @param command the command to handle, guaranteed to be non-null
     * @return a Mono containing the result of command processing
     * @throws IllegalArgumentException if the command is invalid
     * @throws RuntimeException for business logic errors
     */
    Mono<R> handle(C command);
    
    /**
     * Returns the command type this handler can process.
     * @return the command class, must not be null
     */
    Class<C> getCommandType();
    
    /**
     * Returns the result type this handler produces.
     * @return the result class, may be null
     */
    default Class<R> getResultType() {
        return null;
    }
    
    /**
     * Validates whether this handler can process the given command.
     * @param command the command to validate, may be null
     * @return true if this handler can process the command, false otherwise
     */
    default boolean canHandle(Command<?> command) {
        return getCommandType().isInstance(command);
    }
}
```

### CommandBus Interface

```java
package com.firefly.common.domain.cqrs.command;

public interface CommandBus {
    
    /**
     * Sends a command for processing.
     * @param command the command to send
     * @param <R> the result type
     * @return a Mono containing the result of command processing
     */
    <R> Mono<R> send(Command<R> command);
    
    /**
     * Registers a command handler with the bus.
     * @param handler the handler to register
     * @param <C> the command type
     * @param <R> the result type
     */
    <C extends Command<R>, R> void registerHandler(CommandHandler<C, R> handler);
    
    /**
     * Unregisters a command handler from the bus.
     * @param commandType the command type to unregister
     * @param <C> the command type
     */
    <C extends Command<?>> void unregisterHandler(Class<C> commandType);
    
    /**
     * Checks if a handler is registered for the given command type.
     * @param commandType the command type to check
     * @return true if a handler is registered, false otherwise
     */
    boolean hasHandler(Class<? extends Command<?>> commandType);
}
```

### Query Interface

```java
package com.firefly.common.domain.cqrs.query;

public interface Query<R> {
    
    /**
     * Unique identifier for this query instance.
     * @return the query ID
     */
    default String getQueryId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Timestamp when the query was created.
     * @return the creation timestamp
     */
    default Instant getTimestamp() {
        return Instant.now();
    }
    
    /**
     * Correlation ID for tracing across system boundaries.
     * @return the correlation ID, or null if not set
     */
    default String getCorrelationId() {
        return null;
    }
    
    /**
     * User or system identifier that initiated this query.
     * @return the initiator ID, or null if not set
     */
    default String getInitiatedBy() {
        return null;
    }
    
    /**
     * Additional metadata associated with this query.
     * @return metadata map, or null if no metadata
     */
    default Map<String, Object> getMetadata() {
        return null;
    }
    
    /**
     * Expected result type for this query.
     * @return the result type class
     */
    @SuppressWarnings("unchecked")
    default Class<R> getResultType() {
        return (Class<R>) Object.class;
    }
    
    /**
     * Indicates if this query supports caching.
     * @return true if the result can be cached, false otherwise
     */
    default boolean isCacheable() {
        return true;
    }
    
    /**
     * Cache key for this query if caching is enabled.
     * @return cache key, or null to disable caching for this instance
     */
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
```

### QueryHandler Interface

```java
package com.firefly.common.domain.cqrs.query;

public interface QueryHandler<Q extends Query<R>, R> {
    
    /**
     * Handles the given query asynchronously.
     * @param query the query to handle
     * @return a Mono containing the result of query processing
     */
    Mono<R> handle(Q query);
    
    /**
     * Returns the query type this handler can process.
     * @return the query class
     */
    Class<Q> getQueryType();
    
    /**
     * Returns the result type this handler produces.
     * @return the result class
     */
    default Class<R> getResultType() {
        return null;
    }
    
    /**
     * Validates whether this handler can process the given query.
     * @param query the query to validate
     * @return true if this handler can process the query
     */
    default boolean canHandle(Query<?> query) {
        return getQueryType().isInstance(query);
    }
    
    /**
     * Indicates if this handler supports caching of results.
     * @return true if results can be cached, false otherwise
     */
    default boolean supportsCaching() {
        return true;
    }
    
    /**
     * Returns the cache TTL (Time To Live) in seconds for results.
     * @return cache TTL in seconds, or null for default TTL
     */
    default Long getCacheTtlSeconds() {
        return null;
    }
}
```

### QueryBus Interface

```java
package com.firefly.common.domain.cqrs.query;

public interface QueryBus {
    
    /**
     * Executes a query and returns the result.
     * @param query the query to execute
     * @param <R> the result type
     * @return a Mono containing the result of query execution
     */
    <R> Mono<R> query(Query<R> query);
    
    /**
     * Registers a query handler with the bus.
     * @param handler the handler to register
     * @param <Q> the query type
     * @param <R> the result type
     */
    <Q extends Query<R>, R> void registerHandler(QueryHandler<Q, R> handler);
    
    /**
     * Unregisters a query handler from the bus.
     * @param queryType the query type to unregister
     * @param <Q> the query type
     */
    <Q extends Query<?>> void unregisterHandler(Class<Q> queryType);
    
    /**
     * Checks if a handler is registered for the given query type.
     * @param queryType the query type to check
     * @return true if a handler is registered, false otherwise
     */
    boolean hasHandler(Class<? extends Query<?>> queryType);

    /**
     * Clears cached results for the specified cache key.
     * @param cacheKey the cache key to clear
     * @return a Mono that completes when the cache is cleared
     */
    Mono<Void> clearCache(String cacheKey);

    /**
     * Clears all cached query results.
     * @return a Mono that completes when all cache is cleared
     */
    Mono<Void> clearAllCache();
}
```

## ServiceClient Framework

### ServiceClient Interface

```java
package com.firefly.common.domain.client;

public interface ServiceClient<T> {

    /**
     * Executes a GET request.
     * @param endpoint the endpoint path
     * @param responseType the expected response type
     * @return a Mono containing the response
     */
    <R> Mono<R> get(String endpoint, Class<R> responseType);

    /**
     * Executes a GET request with query parameters.
     * @param endpoint the endpoint path
     * @param queryParams the query parameters
     * @param responseType the expected response type
     * @return a Mono containing the response
     */
    <R> Mono<R> get(String endpoint, Map<String, Object> queryParams, Class<R> responseType);

    /**
     * Executes a POST request with a request body.
     * @param endpoint the endpoint path
     * @param request the request body
     * @param responseType the expected response type
     * @return a Mono containing the response
     */
    <R> Mono<R> post(String endpoint, Object request, Class<R> responseType);

    /**
     * Executes a PUT request with a request body.
     * @param endpoint the endpoint path
     * @param request the request body
     * @param responseType the expected response type
     * @return a Mono containing the response
     */
    <R> Mono<R> put(String endpoint, Object request, Class<R> responseType);

    /**
     * Executes a DELETE request.
     * @param endpoint the endpoint path
     * @param responseType the expected response type
     * @return a Mono containing the response
     */
    <R> Mono<R> delete(String endpoint, Class<R> responseType);

    /**
     * Executes a PATCH request with a request body.
     * @param endpoint the endpoint path
     * @param request the request body
     * @param responseType the expected response type
     * @return a Mono containing the response
     */
    <R> Mono<R> patch(String endpoint, Object request, Class<R> responseType);

    /**
     * Returns the base URL for this service client.
     * @return the base URL
     */
    String getBaseUrl();

    /**
     * Returns the service name for this client.
     * @return the service name
     */
    String getServiceName();

    /**
     * Performs a health check for this service.
     * @return a Mono that completes when the health check is successful
     */
    Mono<Void> healthCheck();
}
```

### SdkServiceClient Interface

```java
package com.firefly.common.domain.client.sdk;

public interface SdkServiceClient<S> extends ServiceClient<S> {
    
    /**
     * Executes a synchronous operation with the SDK.
     * @param operation function that uses the SDK to perform an operation
     * @param <R> the result type
     * @return a Mono containing the operation result
     */
    <R> Mono<R> execute(Function<S, R> operation);
    
    /**
     * Executes an asynchronous operation with the SDK.
     * @param operation function that uses the SDK to perform an async operation
     * @param <R> the result type
     * @return a Mono containing the operation result
     */
    <R> Mono<R> executeAsync(Function<S, Mono<R>> operation);
    
    /**
     * Gets the managed SDK instance directly.
     * @return the SDK instance
     * @throws IllegalStateException if the SDK is not properly initialized
     */
    S getSdk();
    
    /**
     * Checks if the SDK is properly initialized and ready for use.
     * @return true if the SDK is ready, false otherwise
     */
    boolean isReady();
    
    /**
     * Gets the SDK version information.
     * @return the SDK version string, or null if not available
     */
    String getSdkVersion();
    
    /**
     * Gets the SDK configuration.
     * @return the SDK configuration map
     */
    Map<String, Object> getSdkConfiguration();
    
    /**
     * Shuts down the SDK and releases resources.
     * @return a Mono that completes when shutdown is finished
     */
    Mono<Void> shutdown();
}
```

## Domain Events

### DomainEventEnvelope

```java
package com.firefly.common.domain.events;

@Builder
@Data
public final class DomainEventEnvelope {
    
    /**
     * The topic/channel for this event.
     */
    private String topic;
    
    /**
     * The event type identifier.
     */
    private String type;
    
    /**
     * The event key for partitioning/routing.
     */
    private String key;
    
    /**
     * The event payload/data.
     */
    private Object payload;
    
    /**
     * The timestamp when the event occurred.
     */
    private Instant timestamp;
    
    /**
     * Additional headers for the event.
     */
    private Map<String, Object> headers;
    
    /**
     * Metadata for the event.
     */
    private Map<String, Object> metadata;
}
```

### DomainEventPublisher Interface

```java
package com.firefly.common.domain.events.outbound;

public interface DomainEventPublisher {
    
    /**
     * Publishes a domain event.
     * @param envelope the event envelope to publish
     * @return a Mono that completes when the event is published
     */
    Mono<Void> publish(DomainEventEnvelope envelope);
}
```

### EventPublisher Annotation

```java
package com.firefly.common.domain.events.outbound;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventPublisher {
    
    /**
     * The topic to publish the event to.
     * @return the topic name
     */
    String topic();
    
    /**
     * The event type. Defaults to empty string.
     * @return the event type
     */
    String type() default "";
    
    /**
     * The event key. Defaults to empty string.
     * @return the event key
     */
    String key() default "";
    
    /**
     * SpEL expression for the payload. Default is '#result' (method return value).
     * @return the payload expression
     */
    String payload() default "#result";
}
```

## Validation Framework

### ValidationResult

```java
package com.firefly.common.domain.validation;

public class ValidationResult {
    
    /**
     * Checks if the validation was successful.
     * @return true if valid, false otherwise
     */
    public boolean isValid();
    
    /**
     * Gets all validation errors.
     * @return list of validation errors
     */
    public List<ValidationError> getErrors();
    
    /**
     * Gets a summary of all validation errors.
     * @return error summary string
     */
    public String getSummary();
    
    /**
     * Creates a successful validation result.
     * @return successful validation result
     */
    public static ValidationResult success();
    
    /**
     * Creates a failed validation result with a single error.
     * @param error the validation error
     * @return failed validation result
     */
    public static ValidationResult failure(ValidationError error);

    /**
     * Creates a failed validation result with multiple errors.
     * @param errors the validation errors
     * @return failed validation result
     */
    public static ValidationResult failure(List<ValidationError> errors);

    /**
     * Creates a failed validation result with a simple error message.
     * @param fieldName the field name
     * @param message the error message
     * @return failed validation result
     */
    public static ValidationResult failure(String fieldName, String message);
    
    /**
     * Creates a builder for constructing validation results.
     * @return validation result builder
     */
    public static Builder builder();
    
    public static class Builder {
        public Builder addError(String field, String message);
        public Builder addError(ValidationError error);
        public ValidationResult build();
    }
}
```

### ValidationError

```java
package com.firefly.common.domain.validation;

@Builder
public class ValidationError {

    /**
     * Gets the name of the field that failed validation.
     * @return the field name, or null if not field-specific
     */
    public String getFieldName();

    /**
     * Gets the error message.
     * @return the error message
     */
    public String getMessage();

    /**
     * Gets the error code.
     * @return the error code
     */
    public String getErrorCode();

    /**
     * Gets the validation severity.
     * @return the validation severity
     */
    public ValidationSeverity getSeverity();

    /**
     * Gets the value that was rejected during validation.
     * @return the rejected value, or null if not available
     */
    public Object getRejectedValue();

    /**
     * Validation severity levels.
     */
    public enum ValidationSeverity {
        WARNING, ERROR, CRITICAL
    }
}
```

## Correlation Context

### CorrelationContext

```java
package com.firefly.common.domain.tracing;

@Component
public class CorrelationContext {
    
    /**
     * Generates a new correlation ID.
     * @return new correlation ID
     */
    public String generateCorrelationId();
    
    /**
     * Generates a new trace ID.
     * @return new trace ID
     */
    public String generateTraceId();
    
    /**
     * Sets the current correlation ID for the thread.
     * @param correlationId the correlation ID to set
     */
    public void setCorrelationId(String correlationId);
    
    /**
     * Gets the current correlation ID for the thread.
     * @return the current correlation ID, or null if not set
     */
    public String getCorrelationId();
    
    /**
     * Sets the current trace ID for the thread.
     * @param traceId the trace ID to set
     */
    public void setTraceId(String traceId);
    
    /**
     * Gets the current trace ID for the thread.
     * @return the current trace ID, or null if not set
     */
    public String getTraceId();
    
    /**
     * Sets the current user ID for the thread.
     * @param userId the user ID to set
     */
    public void setUserId(String userId);
    
    /**
     * Gets the current user ID for the thread.
     * @return the current user ID, or null if not set
     */
    public String getUserId();
    
    /**
     * Clears all correlation context for the current thread.
     */
    public void clear();
    
    /**
     * Gets the current correlation context as a map.
     * @return correlation context map
     */
    public Map<String, String> getContextMap();
    
    /**
     * Sets the correlation context from a map.
     * @param contextMap the context map to set
     */
    public void setContextMap(Map<String, String> contextMap);
}
```

---

This API reference provides the complete method signatures and documentation for all major interfaces and classes in the Firefly Common Domain Library. For implementation examples and usage patterns, refer to the other documentation files.
