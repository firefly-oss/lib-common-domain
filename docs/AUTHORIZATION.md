# Authorization System

The Firefly Common Domain Library provides a comprehensive authorization system that integrates seamlessly with the CQRS framework, offering zero-trust security for banking applications.

## Overview

The authorization system provides:

- **Dual Authorization**: Integration with lib-common-auth + custom authorization logic
- **Zero-Trust Architecture**: All operations must be explicitly authorized
- **Context-Aware**: Uses ExecutionContext for tenant, user, and feature flag information
- **Reactive**: Non-blocking authorization with Mono return types
- **Configurable**: Flexible configuration through properties and environment variables
- **Observable**: Comprehensive metrics and logging for production monitoring

## Core Components

### AuthorizationService

The central service handling all authorization logic:

```java
@Service
public class AuthorizationService {
    
    // Authorize commands without context
    public Mono<Void> authorizeCommand(Command<?> command)
    
    // Authorize commands with execution context
    public Mono<Void> authorizeCommand(Command<?> command, ExecutionContext context)
    
    // Authorize queries without context
    public Mono<Void> authorizeQuery(Query<?> query)
    
    // Authorize queries with execution context
    public Mono<Void> authorizeQuery(Query<?> query, ExecutionContext context)
}
```

### Authorization Flow

1. **lib-common-auth Check**: Validates roles, scopes, and ownership (if available)
2. **Custom Authorization**: Executes command/query-specific authorization logic
3. **Result Combination**: Combines results based on configuration
4. **Error Handling**: Provides detailed authorization failure information

## Configuration

### Basic Configuration

```yaml
firefly:
  cqrs:
    authorization:
      enabled: true                    # Enable/disable authorization globally
      lib-common-auth:
        enabled: true                  # Enable lib-common-auth integration
        fail-fast: false              # Continue to custom auth if lib-common-auth fails
        require-both: false           # Both lib-common-auth and custom must pass
      custom:
        enabled: true                  # Enable custom authorization logic
        allow-override: true          # Allow custom auth to override lib-common-auth
        timeout-ms: 5000             # Timeout for custom authorization
      logging:
        enabled: true                  # Enable authorization logging
        log-successful: false         # Log successful authorizations
        log-failures: true           # Log authorization failures
      performance:
        cache-enabled: true           # Enable authorization result caching
        cache-ttl-seconds: 300       # Cache TTL in seconds
        async-enabled: true          # Enable asynchronous authorization
```

### Environment Variables

All configuration can be overridden with environment variables:

```bash
# Global settings
FIREFLY_CQRS_AUTHORIZATION_ENABLED=true

# lib-common-auth settings
FIREFLY_CQRS_AUTHORIZATION_LIB_COMMON_AUTH_ENABLED=true
FIREFLY_CQRS_AUTHORIZATION_LIB_COMMON_AUTH_FAIL_FAST=false

# Custom authorization settings
FIREFLY_CQRS_AUTHORIZATION_CUSTOM_ENABLED=true
FIREFLY_CQRS_AUTHORIZATION_CUSTOM_ALLOW_OVERRIDE=true
FIREFLY_CQRS_AUTHORIZATION_CUSTOM_TIMEOUT_MS=5000

# Logging settings
FIREFLY_CQRS_AUTHORIZATION_LOGGING_ENABLED=true
FIREFLY_CQRS_AUTHORIZATION_LOGGING_LOG_SUCCESSFUL=false
```

## Implementation Patterns

### Pattern 1: lib-common-auth + Custom Authorization

Most common pattern combining standard authentication with business logic:

```java
@RequiresRole("CUSTOMER")
@RequiresScope("accounts.transfer")
@RequiresOwnership(resource = "account", paramName = "sourceAccountId")
@CustomAuthorization(description = "Transfer limits validation")
public class TransferMoneyCommand implements Command<TransferResult> {
    
    private String sourceAccountId;
    private String destinationAccountId;
    private BigDecimal amount;
    
    @Override
    public Mono<AuthorizationResult> authorize(ExecutionContext context) {
        // Custom business logic validation
        return validateTransferLimits(amount, context.getUserId())
            .flatMap(limitsOk -> {
                if (!limitsOk) {
                    return Mono.just(AuthorizationResult.failure("limits", "Transfer exceeds daily limit"));
                }
                return validateAccountStatus(sourceAccountId)
                    .map(statusOk -> statusOk ? 
                        AuthorizationResult.success() : 
                        AuthorizationResult.failure("account", "Source account is frozen"));
            });
    }
}
```

### Pattern 2: Custom Authorization Override

Custom logic can override lib-common-auth decisions:

```java
@RequiresRole("CUSTOMER")
@CustomAuthorization(
    description = "Emergency access override", 
    overrideLibCommonAuth = true
)
public class EmergencyAccessCommand implements Command<AccessResult> {
    
    @Override
    public Mono<AuthorizationResult> authorize(ExecutionContext context) {
        // Check if this is an emergency situation
        return emergencyService.isEmergencyActive(context.getTenantId())
            .map(isEmergency -> isEmergency ? 
                AuthorizationResult.success() : 
                AuthorizationResult.failure("emergency", "Emergency access not active"));
    }
}
```

### Pattern 3: Custom Only Authorization

Skip lib-common-auth for specialized scenarios:

```java
@CustomAuthorization(
    description = "System maintenance authorization",
    skipLibCommonAuth = true
)
public class SystemMaintenanceCommand implements Command<MaintenanceResult> {
    
    @Override
    public Mono<AuthorizationResult> authorize(ExecutionContext context) {
        // Only allow during maintenance windows
        return maintenanceWindowService.isMaintenanceWindow()
            .map(inWindow -> inWindow ? 
                AuthorizationResult.success() : 
                AuthorizationResult.failure("timing", "Maintenance only allowed during maintenance window"));
    }
}
```

### Pattern 4: Both Must Pass (Highest Security)

Require both lib-common-auth and custom authorization to succeed:

```java
@RequiresRole("ADMIN")
@RequiresScope("admin.delete")
@CustomAuthorization(
    description = "Additional deletion validation",
    requiresBothToPass = true
)
public class DeleteUserCommand implements Command<DeleteResult> {
    
    @Override
    public Mono<AuthorizationResult> authorize(ExecutionContext context) {
        // Additional validation beyond lib-common-auth
        return validateDeletionRules(userIdToDelete, context);
    }
}
```

## Integration with CQRS

### Command Authorization

Commands are automatically authorized before execution:

```java
// In CommandBus
public <R> Mono<R> send(Command<R> command) {
    return authorizationService.authorizeCommand(command)
        .then(executeCommand(command));
}

// With execution context
public <R> Mono<R> send(Command<R> command, ExecutionContext context) {
    return authorizationService.authorizeCommand(command, context)
        .then(executeCommand(command, context));
}
```

### Query Authorization

Queries are authorized before execution and results may be filtered:

```java
// In QueryBus
public <R> Mono<R> query(Query<R> query) {
    return authorizationService.authorizeQuery(query)
        .then(executeQuery(query));
}

// With execution context
public <R> Mono<R> query(Query<R> query, ExecutionContext context) {
    return authorizationService.authorizeQuery(query, context)
        .then(executeQuery(query, context));
}
```

## Monitoring and Metrics

### AuthorizationMetrics

Comprehensive metrics collection for production monitoring:

```java
@Component
public class AuthorizationMetrics {
    
    // Counters
    private final Counter authorizationAttempts;
    private final Counter authorizationSuccesses;
    private final Counter authorizationFailures;
    private final Counter libCommonAuthSuccesses;
    private final Counter libCommonAuthFailures;
    private final Counter customAuthSuccesses;
    private final Counter customAuthFailures;
    private final Counter cacheHits;
    private final Counter cacheMisses;
    
    // Timers
    private final Timer authorizationTimer;
    private final Timer libCommonAuthTimer;
    private final Timer customAuthTimer;
    
    // Gauges
    private final AtomicLong activeAuthorizationRequests;
    private final AtomicLong cacheSize;
}
```

### Available Metrics

- `firefly.authorization.attempts` - Total authorization attempts
- `firefly.authorization.successes` - Successful authorizations
- `firefly.authorization.failures` - Failed authorizations
- `firefly.authorization.duration` - Authorization timing
- `firefly.authorization.cache.hits` - Cache hit rate
- `firefly.authorization.active_requests` - Active authorization requests

## Error Handling

### AuthorizationException

Detailed authorization failure information:

```java
public class AuthorizationException extends RuntimeException {
    private final String commandType;
    private final String commandId;
    private final List<String> violations;
    private final Map<String, Object> context;
}
```

### Error Response Format

```json
{
  "error": "AUTHORIZATION_FAILED",
  "message": "Authorization failed for TransferMoneyCommand",
  "commandType": "TransferMoneyCommand",
  "commandId": "abc-123-def",
  "violations": [
    "Transfer exceeds daily limit",
    "Source account is frozen"
  ],
  "context": {
    "userId": "user-123",
    "tenantId": "tenant-456"
  }
}
```

## Best Practices

### 1. Use Appropriate Patterns

- **Standard Operations**: lib-common-auth + minimal custom logic
- **Complex Business Rules**: Custom authorization with context
- **Emergency Scenarios**: Custom override capabilities
- **High Security**: Both must pass pattern

### 2. Performance Optimization

- Enable caching for frequently accessed resources
- Use async authorization for non-blocking operations
- Set appropriate timeouts for external service calls

### 3. Security Guidelines

- Always validate resource ownership
- Implement proper error handling without information leakage
- Use execution context for tenant isolation
- Log authorization failures for security monitoring

### 4. Testing

- Test all authorization paths (success and failure)
- Verify integration between lib-common-auth and custom logic
- Test timeout and error scenarios
- Validate metrics collection

## Configuration Examples

### Development Environment

```yaml
firefly:
  cqrs:
    authorization:
      enabled: true
      lib-common-auth:
        enabled: true
        fail-fast: false
      custom:
        enabled: true
        allow-override: true
      logging:
        enabled: true
        log-successful: true  # More verbose in dev
```

### Production Environment

```yaml
firefly:
  cqrs:
    authorization:
      enabled: true
      lib-common-auth:
        enabled: true
        fail-fast: true      # Fail fast in production
        require-both: true   # Higher security
      custom:
        enabled: true
        allow-override: false # No overrides in production
        timeout-ms: 2000     # Shorter timeout
      logging:
        enabled: true
        log-successful: false # Less verbose
        log-failures: true
      performance:
        cache-enabled: true
        cache-ttl-seconds: 600
```

### Testing Environment

```yaml
firefly:
  cqrs:
    authorization:
      enabled: false  # Disable for integration tests
```

## Troubleshooting

### Common Issues

1. **Authorization Always Fails**
   - Check if authorization is enabled
   - Verify lib-common-auth configuration
   - Check custom authorization logic

2. **Performance Issues**
   - Enable caching
   - Increase timeout values
   - Check external service dependencies

3. **Configuration Not Applied**
   - Verify property names and structure
   - Check environment variable format
   - Ensure Spring profile is active

### Debug Logging

Enable debug logging for detailed authorization flow:

```yaml
logging:
  level:
    com.firefly.common.domain.authorization: DEBUG
```
