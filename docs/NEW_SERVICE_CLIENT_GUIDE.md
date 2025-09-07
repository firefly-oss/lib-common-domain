# New ServiceClient Framework Guide

## Overview

The ServiceClient framework has been completely redesigned to provide a simplified, unified interface for service-to-service communication while maintaining all the power and flexibility of the original implementation.

## Key Improvements

### 1. Simplified Builder Pattern
- **Before**: Complex builder classes with many configuration options
- **After**: Streamlined builders with sensible defaults and fluent API

### 2. Unified Interface
- **Before**: Different method signatures across implementations
- **After**: Consistent interface across REST, gRPC, and SDK clients

### 3. Fluent Request API
- **Before**: Method calls with many parameters
- **After**: Fluent request builders with method chaining

### 4. Better Type Safety
- **Before**: Complex generic handling
- **After**: Simplified generics with TypeReference support

### 5. Improved Error Handling
- **Before**: Inconsistent exception handling
- **After**: Unified error handling with clear error messages

## Quick Start

### REST Client

```java
// Simple REST client
ServiceClient client = ServiceClient.rest("user-service")
    .baseUrl("http://user-service:8080")
    .build();

// Make requests with fluent API
Mono<User> user = client.get("/users/{id}", User.class)
    .withPathParam("id", "123")
    .withQueryParam("includeProfile", true)
    .withHeader("Authorization", "Bearer " + token)
    .execute();

// POST with request body
Mono<User> created = client.post("/users", User.class)
    .withBody(newUser)
    .withTimeout(Duration.ofSeconds(30))
    .execute();
```

### gRPC Client

```java
// gRPC client
ServiceClient grpcClient = ServiceClient.grpc("payment-service", PaymentServiceStub.class)
    .address("payment-service:9090")
    .usePlaintext()
    .stubFactory(channel -> PaymentServiceGrpc.newStub(channel))
    .build();

// Execute gRPC operations
Mono<PaymentResult> result = grpcClient.execute(stub -> {
    PaymentServiceStub paymentStub = (PaymentServiceStub) stub;
    return paymentStub.processPayment(paymentRequest);
});
```

### SDK Client (Redesigned for Better Developer Experience)

```java
// SDK client with new sdkSupplier() method
ServiceClient sdkClient = ServiceClient.sdk("aws-service", AWSSDK.class)
    .sdkSupplier(() -> AWSSDK.builder()
        .region(Region.US_EAST_1)
        .credentialsProvider(credentialsProvider)
        .build())
    .timeout(Duration.ofSeconds(45))
    .build();

// Option 1: Use regular ServiceClient (requires casting)
Mono<S3Object> object = sdkClient.call(sdk -> {
    AWSSDK awsSDK = (AWSSDK) sdk; // Casting required
    return awsSDK.s3().getObject(request);
});

// Option 2: Use TypedSdkClient for true type safety - NO CASTING REQUIRED!
TypedSdkClient<AWSSDK> typedClient = sdkClient.typed();

Mono<S3Object> typedObject = typedClient.call(sdk ->
    sdk.s3().getObject(request)  // No casting needed!
);

// NEW: Async SDK operations for reactive SDKs
Mono<S3Object> asyncObject = typedClient.callAsync(sdk ->
    sdk.s3().getObjectAsync(request)
);

// NEW: Direct SDK access for complex operations
AWSSDK sdk = typedClient.sdk();
S3Object directObject = sdk.s3().getObject(request);

// OLD: Legacy API (deprecated but still supported)
Mono<S3Object> legacyObject = sdkClient.execute(sdk -> {
    AWSSDK awsSDK = (AWSSDK) sdk; // Casting required
    return awsSDK.s3().getObject(request);
});
```

## SDK Client Improvements

The SDK client has been completely redesigned to provide a much better developer experience:

### Before vs After

**Before (Complex and Error-Prone):**
```java
// Required casting and type safety issues
Mono<PaymentResult> result = client.execute(sdk -> {
    PaymentSDK paymentSDK = (PaymentSDK) sdk; // Manual casting required
    return paymentSDK.processPayment(request);
});
```

**After (Simple and Type-Safe):**
```java
// Option 1: Regular ServiceClient (still requires casting)
Mono<PaymentResult> result = client.call(sdk -> {
    PaymentSDK paymentSDK = (PaymentSDK) sdk; // Casting still required
    return paymentSDK.processPayment(request);
});

// Option 2: TypedSdkClient (no casting, full type safety)
TypedSdkClient<PaymentSDK> typedClient = client.typed();
Mono<PaymentResult> result = typedClient.call(sdk ->
    sdk.processPayment(request)  // No casting needed!
);
```

### Key Improvements

1. **Type Safety**: No more casting or `@SuppressWarnings`
2. **Better API**: Clear method names (`call`, `callAsync`, `sdk`)
3. **IDE Support**: Full autocomplete and refactoring support
4. **Cleaner Code**: Reduced boilerplate and complexity

### Real-World Banking Example

```java
// Fraud detection service integration
ServiceClient fraudClient = ServiceClient.sdk("fraud-service", FraudDetectionSDK.class)
    .sdkSupplier(() -> new FraudDetectionSDK(apiKey, environment))
    .timeout(Duration.ofSeconds(30))
    .build();

// Get type-safe client for better developer experience
TypedSdkClient<FraudDetectionSDK> typedFraudClient = fraudClient.typed();

// Simple fraud check - no casting required!
Mono<FraudResult> fraudCheck = typedFraudClient.call(sdk ->
    sdk.checkTransaction(transactionId, amount, currency)
);

// Async fraud check for reactive SDKs
Mono<FraudResult> asyncFraudCheck = typedFraudClient.callAsync(sdk ->
    sdk.checkTransactionAsync(transactionId, amount, currency)
);

// Complex operations with direct SDK access
FraudDetectionSDK sdk = typedFraudClient.sdk();
FraudResult result = sdk.checkTransaction(transactionId, amount, currency);
FraudProfile profile = sdk.getCustomerProfile(customerId);
RiskScore score = sdk.calculateRiskScore(profile, result);
```

### Migration Guide

**Step 1**: Replace `execute()` with `call()` (still requires casting)
```java
// Old
client.execute(sdk -> {
    MySDK mySDK = (MySDK) sdk;
    return mySDK.doSomething();
});

// New (basic)
client.call(sdk -> {
    MySDK mySDK = (MySDK) sdk; // Still requires casting
    return mySDK.doSomething();
});

// New (type-safe)
TypedSdkClient<MySDK> typedClient = client.typed();
typedClient.call(sdk -> sdk.doSomething()); // No casting!
```

**Step 2**: Replace `executeAsync()` with `callAsync()`
```java
// Old
client.executeAsync(sdk -> {
    MySDK mySDK = (MySDK) sdk;
    return mySDK.doSomethingAsync();
});

// New (type-safe)
TypedSdkClient<MySDK> typedClient = client.typed();
typedClient.callAsync(sdk -> sdk.doSomethingAsync()); // No casting!
```

**Step 3**: Use `sdk()` for direct access
```java
// Old
MySDK sdk = (MySDK) client.getSdk();

// New (type-safe)
TypedSdkClient<MySDK> typedClient = client.typed();
MySDK sdk = typedClient.sdk(); // No casting!
```

## Banking Examples

### Customer Service Integration

```java
@Service
public class CustomerService {
    
    private final ServiceClient customerClient;
    
    public CustomerService() {
        this.customerClient = ServiceClient.rest("customer-service")
            .baseUrl("http://customer-service:8080")
            .jsonContentType()
            .timeout(Duration.ofSeconds(30))
            .build();
    }
    
    public Mono<Customer> getCustomer(String customerId) {
        return customerClient.get("/customers/{id}", Customer.class)
            .withPathParam("id", customerId)
            .withHeader("X-Request-ID", UUID.randomUUID().toString())
            .execute();
    }
    
    public Mono<List<Account>> getCustomerAccounts(String customerId) {
        return customerClient.get("/customers/{id}/accounts", new TypeReference<List<Account>>() {})
            .withPathParam("id", customerId)
            .withQueryParam("status", "ACTIVE")
            .withQueryParam("includeBalance", true)
            .execute();
    }
    
    public Mono<Account> createAccount(String customerId, CreateAccountRequest request) {
        return customerClient.post("/customers/{id}/accounts", Account.class)
            .withPathParam("id", customerId)
            .withBody(request)
            .withTimeout(Duration.ofSeconds(45))
            .execute();
    }
}
```

### Payment Processing with Multiple Clients

```java
@Service
public class PaymentProcessingService {
    
    private final ServiceClient fraudClient;
    private final ServiceClient paymentClient;
    private final ServiceClient notificationClient;
    
    public PaymentProcessingService() {
        // Fraud detection via SDK
        this.fraudClient = ServiceClient.sdk("fraud-detection", FraudDetectionSDK.class)
            .sdkSupplier(() -> new FraudDetectionSDK(apiKey, environment))
            .timeout(Duration.ofSeconds(30))
            .build();
        
        // Payment processing via gRPC
        this.paymentClient = ServiceClient.grpc("payment-service", PaymentServiceStub.class)
            .address("payment-service:9090")
            .useTransportSecurity()
            .stubFactory(channel -> PaymentServiceGrpc.newStub(channel))
            .build();
        
        // Notifications via REST
        this.notificationClient = ServiceClient.rest("notification-service")
            .baseUrl("http://notification-service:8080")
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
    
    public Mono<PaymentResult> processPayment(PaymentRequest request) {
        return performFraudCheck(request)
            .filter(FraudCheckResult::isApproved)
            .switchIfEmpty(Mono.error(new PaymentRejectedException("Payment blocked by fraud detection")))
            .flatMap(fraudResult -> executePayment(request))
            .flatMap(paymentResult -> sendNotification(paymentResult).thenReturn(paymentResult));
    }
    
    private Mono<FraudCheckResult> performFraudCheck(PaymentRequest request) {
        return fraudClient.execute(sdk -> {
            FraudDetectionSDK fraudSDK = (FraudDetectionSDK) sdk;
            return fraudSDK.checkTransaction(
                request.getTransactionId(),
                request.getAmount(),
                request.getCurrency()
            );
        });
    }
    
    private Mono<PaymentResult> executePayment(PaymentRequest request) {
        return paymentClient.execute(stub -> {
            PaymentServiceStub paymentStub = (PaymentServiceStub) stub;
            return paymentStub.processPayment(request);
        });
    }
    
    private Mono<Void> sendNotification(PaymentResult result) {
        NotificationRequest notification = new NotificationRequest(
            result.getCustomerId(),
            "Payment processed successfully",
            result.getTransactionId()
        );
        
        return notificationClient.post("/notifications", Void.class)
            .withBody(notification)
            .execute();
    }
}
```

## Migration Guide

### From Old REST Client

**Before:**
```java
RestServiceClient client = RestServiceClient.builder()
    .serviceName("user-service")
    .baseUrl("http://user-service:8080")
    .timeout(Duration.ofSeconds(30))
    .maxConnections(50)
    .authentication(AuthenticationConfiguration.bearer(tokenProvider))
    .defaultHeader("Accept", "application/json")
    .circuitBreaker(customCircuitBreaker)
    .retry(customRetry)
    .correlationContext(correlationContext)
    .build();

Mono<User> user = client.get("/users/{id}", User.class, Map.of("id", "123"));
```

**After:**
```java
ServiceClient client = ServiceClient.rest("user-service")
    .baseUrl("http://user-service:8080")
    .timeout(Duration.ofSeconds(30))
    .maxConnections(50)
    .defaultHeader("Accept", "application/json")
    .build();

Mono<User> user = client.get("/users/{id}", User.class)
    .withPathParam("id", "123")
    .execute();
```

### From Old SDK Client

**Before:**
```java
SdkServiceClient<PaymentSDK> sdkClient = SdkServiceClient.<PaymentSDK>builder()
    .serviceName("payment-service")
    .sdkFactory(() -> new PaymentSDK(apiKey, environment))
    .timeout(Duration.ofSeconds(30))
    .build();

Mono<PaymentResult> result = sdkClient.execute(sdk -> 
    sdk.processPayment(paymentRequest));
```

**After:**
```java
ServiceClient sdkClient = ServiceClient.sdk("payment-service", PaymentSDK.class)
    .sdkSupplier(() -> new PaymentSDK(apiKey, environment))
    .timeout(Duration.ofSeconds(30))
    .build();

Mono<PaymentResult> result = sdkClient.execute(sdk -> {
    PaymentSDK paymentSDK = (PaymentSDK) sdk;
    return paymentSDK.processPayment(paymentRequest);
});
```

## Configuration

The new ServiceClient framework maintains compatibility with existing configuration properties while adding new simplified options:

```yaml
firefly:
  service-client:
    enabled: true
    
    # REST client defaults
    rest:
      max-connections: 100
      timeout: 30s
      max-idle-time: 5m
      
    # gRPC client defaults  
    grpc:
      keep-alive-time: 5m
      keep-alive-timeout: 30s
      
    # Circuit breaker and retry settings remain the same
    circuit-breaker:
      failure-rate-threshold: 50
      wait-duration-in-open-state: 30s
      
    retry:
      max-attempts: 3
      wait-duration: 500ms
```

## Best Practices

1. **Use appropriate client types**: REST for HTTP APIs, gRPC for Protocol Buffer services, SDK for third-party integrations
2. **Set reasonable timeouts**: Different operations may require different timeout values
3. **Handle errors gracefully**: Use proper error handling and fallback mechanisms
4. **Use fluent API**: Take advantage of the request builder pattern for cleaner code
5. **Configure circuit breakers**: Set appropriate failure thresholds for resilience
6. **Monitor client health**: Use health check methods to monitor service availability

## Advanced Features

### Request/Response Interceptors

The new ServiceClient framework supports powerful interceptors for cross-cutting concerns:

```java
// Logging interceptor
LoggingInterceptor loggingInterceptor = LoggingInterceptor.builder()
    .logLevel(LogLevel.INFO)
    .logHeaders(true)
    .logBody(false) // Don't log sensitive banking data
    .slowRequestThreshold(Duration.ofSeconds(5))
    .sensitiveHeaders(Set.of("authorization", "x-api-key"))
    .build();

// Metrics interceptor
MetricsInterceptor metricsInterceptor = new MetricsInterceptor(
    new MetricsInterceptor.InMemoryMetricsCollector(),
    true, // Detailed metrics
    true  // Histogram enabled
);

// Apply interceptors to client
ServiceClient client = ServiceClient.rest("banking-service")
    .baseUrl("http://banking-service:8080")
    .addInterceptor(loggingInterceptor)
    .addInterceptor(metricsInterceptor)
    .build();
```

### Health Monitoring

Comprehensive health monitoring with automatic failure detection:

```java
// Health manager setup
ServiceClientHealthManager healthManager = new ServiceClientHealthManager(
    Duration.ofSeconds(30), // Check interval
    Duration.ofSeconds(5),  // Check timeout
    3                       // Max consecutive failures
);

// Register clients for monitoring
healthManager.registerClient(accountClient);
healthManager.registerClient(paymentClient);
healthManager.registerClient(fraudClient);

// Start monitoring
healthManager.start();

// Check overall health
OverallHealthState overallHealth = healthManager.getOverallHealthState();
if (overallHealth == OverallHealthState.UNHEALTHY) {
    log.warn("Some services are unhealthy, consider fallback strategies");
}
```

### Advanced Resilience Patterns

Beyond basic circuit breakers and retries:

```java
// Advanced resilience configuration
AdvancedResilienceManager.ResilienceConfig config = new AdvancedResilienceManager.ResilienceConfig(
    10,                        // Max concurrent calls (bulkhead)
    Duration.ofSeconds(1),     // Max wait time
    100.0,                     // Requests per second (rate limiting)
    20,                        // Burst capacity
    Duration.ofSeconds(2),     // Base timeout
    Duration.ofSeconds(30)     // Max adaptive timeout
);

AdvancedResilienceManager resilienceManager = new AdvancedResilienceManager(
    new SystemLoadSheddingStrategy(0.8, 0.9) // CPU and memory thresholds
);

// Apply resilience to operations
Mono<PaymentResult> resilientPayment = resilienceManager.applyResilience(
    "payment-service",
    paymentClient.post("/payments", PaymentResult.class)
        .withBody(paymentRequest)
        .execute(),
    config
);
```

### Metrics Collection

Comprehensive metrics for monitoring and alerting:

```java
// Get metrics snapshot
MetricsSnapshot snapshot = metricsCollector.getSnapshot();

// Request counts by service
Map<String, Long> requestCounts = snapshot.getRequestCounts();
log.info("Total requests to payment-service: {}",
    requestCounts.get("payment-service.payments.POST.REST"));

// Response time statistics
Map<String, Long> responseTimes = snapshot.getTotalResponseTimes();
Map<String, Long> maxTimes = snapshot.getMaxResponseTimes();

// Error rates
Map<String, Long> errorCounts = snapshot.getErrorCounts();
double errorRate = (double) errorCounts.getOrDefault("payment-service.payments.POST.REST.TimeoutException", 0L)
                  / requestCounts.getOrDefault("payment-service.payments.POST.REST", 1L);
```

### Streaming Support

```java
// REST streaming for real-time banking events
Flux<TransactionEvent> events = client.stream("/transactions/events", TransactionEvent.class);

// Request builder streaming with parameters
Flux<Transaction> transactions = client.get("/transactions", Transaction.class)
    .withQueryParam("accountId", accountId)
    .withQueryParam("status", "PENDING")
    .withQueryParam("limit", 100)
    .stream();

// Process streaming data
events.buffer(Duration.ofSeconds(10))
    .subscribe(batch -> processBatchTransactions(batch));
```

### Custom Configuration

```java
ServiceClient client = ServiceClient.rest("custom-service")
    .baseUrl("http://custom-service:8080")
    .webClient(customWebClient)
    .circuitBreaker(customCircuitBreaker)
    .retry(customRetry)
    .addInterceptor(customInterceptor)
    .build();
```

### Load Shedding and Adaptive Patterns

```java
// Custom load shedding strategy
LoadSheddingStrategy customStrategy = serviceName -> {
    // Check system metrics, queue sizes, etc.
    return getCurrentSystemLoad() > 0.85;
};

// Adaptive timeout based on service performance
AdaptiveTimeout adaptiveTimeout = new AdaptiveTimeout(
    Duration.ofSeconds(1),  // Base timeout
    Duration.ofSeconds(30)  // Max timeout
);

// Timeout adapts based on historical performance
Duration currentTimeout = adaptiveTimeout.calculateTimeout();
```

## Production Deployment Checklist

### Pre-Deployment
- [ ] Update configuration for production environment
- [ ] Configure appropriate timeouts and connection limits
- [ ] Set up monitoring and alerting for service health
- [ ] Configure circuit breaker thresholds
- [ ] Set up metrics collection and dashboards
- [ ] Test failover and recovery scenarios

### Security Configuration
- [ ] Enable SSL validation in production
- [ ] Configure authentication and authorization
- [ ] Set up request signing if required
- [ ] Review and sanitize logging configuration
- [ ] Configure sensitive header filtering

### Performance Optimization
- [ ] Tune connection pool sizes
- [ ] Configure appropriate bulkhead limits
- [ ] Set up rate limiting thresholds
- [ ] Enable compression for large payloads
- [ ] Configure adaptive timeout parameters

### Monitoring and Observability
- [ ] Set up health check endpoints
- [ ] Configure metrics export to monitoring systems
- [ ] Set up alerting for service degradation
- [ ] Configure distributed tracing
- [ ] Set up log aggregation

## Troubleshooting Guide

### Common Issues

**High Response Times**
- Check adaptive timeout configuration
- Review bulkhead and rate limiting settings
- Monitor system resource usage
- Check for network latency issues

**Circuit Breaker Tripping**
- Review failure rate thresholds
- Check service health and availability
- Monitor error rates and types
- Verify timeout configurations

**Connection Pool Exhaustion**
- Increase max connections if needed
- Check for connection leaks
- Review connection idle and lifetime settings
- Monitor connection pool metrics

**Rate Limiting Issues**
- Review requests per second limits
- Check burst capacity settings
- Monitor request patterns
- Consider implementing backoff strategies

## Summary

The redesigned ServiceClient framework provides:

✅ **Unified Interface**: Consistent API across REST, gRPC, and SDK clients
✅ **Simplified Configuration**: Sensible defaults with environment-specific settings
✅ **Advanced Resilience**: Bulkhead isolation, rate limiting, adaptive timeouts
✅ **Comprehensive Monitoring**: Health checks, metrics collection, alerting
✅ **Developer Experience**: Fluent API, clear error messages, extensive documentation
✅ **Production Ready**: Security, performance optimization, observability

The framework is designed specifically for banking and financial services with:
- High availability and fault tolerance
- Comprehensive security features
- Detailed monitoring and observability
- Regulatory compliance support
- Performance optimization for high-volume transactions

For support and additional documentation, visit the [Firefly OpenCore Banking Platform](https://getfirefly.io) website.
