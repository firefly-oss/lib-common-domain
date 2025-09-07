# Enhanced SDK Client Examples

This document demonstrates the enhanced SDK client features and best practices for developers.

## Quick Start - Basic SDK Client

```java
// Simple SDK client with minimal configuration
ServiceClient client = ServiceClient.sdk("payment-service", PaymentSDK.class)
    .sdkSupplier(() -> new PaymentSDK(apiKey))
    .build();

// Type-safe operations
TypedSdkClient<PaymentSDK> typedClient = client.typed();
Mono<PaymentResult> result = typedClient.call(sdk -> 
    sdk.processPayment(request)
);
```

## Enhanced Features

### 1. Convenience Methods for SDK Creation

```java
// Using existing SDK instance
PaymentSDK existingSDK = PaymentSDK.builder()
    .apiKey(apiKey)
    .environment(Environment.PRODUCTION)
    .build();

ServiceClient client = ServiceClient.sdk("payment-service", PaymentSDK.class)
    .sdkInstance(existingSDK)  // Use existing instance
    .build();

// Using supplier (recommended)
ServiceClient client = ServiceClient.sdk("payment-service", PaymentSDK.class)
    .sdkSupplier(() -> new PaymentSDK(apiKey))  // Create fresh instances
    .build();
```

### 2. Observability Features

```java
// Enable logging with default settings
ServiceClient client = ServiceClient.sdk("payment-service", PaymentSDK.class)
    .sdkSupplier(() -> new PaymentSDK(apiKey))
    .withLogging()  // Enables request/response logging
    .build();

// Enable metrics collection
ServiceClient client = ServiceClient.sdk("payment-service", PaymentSDK.class)
    .sdkSupplier(() -> new PaymentSDK(apiKey))
    .withMetrics()  // Enables metrics collection
    .build();

// Enable both logging and metrics
ServiceClient client = ServiceClient.sdk("payment-service", PaymentSDK.class)
    .sdkSupplier(() -> new PaymentSDK(apiKey))
    .withObservability()  // Enables both logging and metrics
    .build();
```

### 3. Resilience Patterns

```java
// Enable circuit breaker with default settings
ServiceClient client = ServiceClient.sdk("payment-service", PaymentSDK.class)
    .sdkSupplier(() -> new PaymentSDK(apiKey))
    .withCircuitBreaker()  // 50% failure rate, 60s wait time
    .build();

// Enable retry with default settings
ServiceClient client = ServiceClient.sdk("payment-service", PaymentSDK.class)
    .sdkSupplier(() -> new PaymentSDK(apiKey))
    .withRetry()  // 3 attempts, 1s wait time
    .build();

// Enable both circuit breaker and retry
ServiceClient client = ServiceClient.sdk("payment-service", PaymentSDK.class)
    .sdkSupplier(() -> new PaymentSDK(apiKey))
    .withResilience()  // Enables both patterns
    .build();
```

### 4. Complete Configuration

```java
// Production-ready configuration with all features
ServiceClient client = ServiceClient.sdk("payment-service", PaymentSDK.class)
    .sdkSupplier(() -> PaymentSDK.builder()
        .apiKey(apiKey)
        .environment(Environment.PRODUCTION)
        .timeout(Duration.ofSeconds(30))
        .build())
    .timeout(Duration.ofSeconds(45))
    .withDefaults()  // Enables resilience + observability
    .build();
```

## Type-Safe Operations

### Basic Operations

```java
TypedSdkClient<PaymentSDK> typedClient = client.typed();

// Synchronous operations
Mono<PaymentResult> payment = typedClient.call(sdk -> 
    sdk.processPayment(paymentRequest)
);

// Asynchronous operations (for reactive SDKs)
Mono<PaymentResult> asyncPayment = typedClient.callAsync(sdk -> 
    sdk.processPaymentAsync(paymentRequest)
);

// Direct SDK access
PaymentSDK sdk = typedClient.sdk();
PaymentResult directResult = sdk.processPayment(paymentRequest);
```

### Complex Operations

```java
TypedSdkClient<PaymentSDK> typedClient = client.typed();

// Chaining operations
Mono<String> result = typedClient.call(sdk -> {
    // Validate payment
    ValidationResult validation = sdk.validatePayment(request);
    if (!validation.isValid()) {
        throw new PaymentValidationException(validation.getErrors());
    }
    
    // Process payment
    PaymentResult payment = sdk.processPayment(request);
    
    // Send confirmation
    sdk.sendConfirmation(payment.getTransactionId());
    
    return payment.getTransactionId();
});
```

## Error Handling

### Enhanced Error Messages

```java
// The enhanced SDK client provides detailed error messages
TypedSdkClient<PaymentSDK> typedClient = client.typed();

typedClient.call(sdk -> sdk.processPayment(request))
    .doOnError(error -> {
        // Enhanced error messages include:
        // - Service name
        // - SDK type information
        // - Timeout details
        // - Type safety hints
        log.error("Payment processing failed: {}", error.getMessage());
    });
```

### Timeout Handling

```java
ServiceClient client = ServiceClient.sdk("payment-service", PaymentSDK.class)
    .sdkSupplier(() -> new PaymentSDK(apiKey))
    .timeout(Duration.ofSeconds(30))  // Custom timeout
    .build();

TypedSdkClient<PaymentSDK> typedClient = client.typed();

typedClient.call(sdk -> sdk.processPayment(request))
    .doOnError(TimeoutException.class, error -> {
        log.warn("Payment processing timed out after 30 seconds");
        // Handle timeout scenario
    });
```

## Diagnostics and Monitoring

### Client Diagnostics

```java
if (client instanceof SdkServiceClientImpl) {
    Map<String, Object> diagnostics = ((SdkServiceClientImpl<?>) client).getDiagnostics();
    
    log.info("Service: {}", diagnostics.get("serviceName"));
    log.info("SDK Type: {}", diagnostics.get("sdkType"));
    log.info("Timeout: {}", diagnostics.get("timeout"));
    log.info("Has Circuit Breaker: {}", diagnostics.get("hasCircuitBreaker"));
    log.info("Has Retry: {}", diagnostics.get("hasRetry"));
    log.info("Interceptor Count: {}", diagnostics.get("interceptorCount"));
}
```

## Best Practices

### 1. Use Type-Safe Client

```java
// ✅ Recommended: Use TypedSdkClient for type safety
TypedSdkClient<PaymentSDK> typedClient = client.typed();
Mono<PaymentResult> result = typedClient.call(sdk -> sdk.processPayment(request));

// ❌ Avoid: Using raw ServiceClient requires casting
Mono<PaymentResult> result = client.call(sdk -> {
    PaymentSDK paymentSDK = (PaymentSDK) sdk;  // Casting required
    return paymentSDK.processPayment(request);
});
```

### 2. Enable Observability in Production

```java
// ✅ Production configuration
ServiceClient client = ServiceClient.sdk("payment-service", PaymentSDK.class)
    .sdkSupplier(() -> new PaymentSDK(apiKey))
    .withDefaults()  // Includes logging, metrics, circuit breaker, retry
    .build();
```

### 3. Handle Errors Gracefully

```java
TypedSdkClient<PaymentSDK> typedClient = client.typed();

typedClient.call(sdk -> sdk.processPayment(request))
    .onErrorResume(PaymentException.class, error -> {
        log.warn("Payment failed, attempting fallback: {}", error.getMessage());
        return fallbackPaymentService.processPayment(request);
    })
    .onErrorResume(TimeoutException.class, error -> {
        log.error("Payment timed out, queuing for retry");
        return queueForRetry(request);
    });
```

### 4. Resource Management

```java
// Ensure proper cleanup
try (ServiceClient client = ServiceClient.sdk("payment-service", PaymentSDK.class)
        .sdkSupplier(() -> new PaymentSDK(apiKey))
        .withDefaults()
        .build()) {
    
    TypedSdkClient<PaymentSDK> typedClient = client.typed();
    
    // Use client...
    
} // Client automatically shuts down
```

## Migration from Legacy API

### Before (Legacy)

```java
ServiceClient client = ServiceClient.sdk("payment-service", PaymentSDK.class)
    .sdkFactory(unused -> new PaymentSDK(apiKey))  // Old API
    .build();

// Required casting
Mono<PaymentResult> result = client.execute(sdk -> {
    PaymentSDK paymentSDK = (PaymentSDK) sdk;  // Manual casting
    return paymentSDK.processPayment(request);
});
```

### After (Enhanced)

```java
ServiceClient client = ServiceClient.sdk("payment-service", PaymentSDK.class)
    .sdkSupplier(() -> new PaymentSDK(apiKey))  // New API
    .withDefaults()  // Enhanced features
    .build();

// Type-safe operations
TypedSdkClient<PaymentSDK> typedClient = client.typed();
Mono<PaymentResult> result = typedClient.call(sdk -> 
    sdk.processPayment(request)  // No casting needed!
);
```
