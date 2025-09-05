# ServiceClient Framework Documentation

This document provides comprehensive documentation for the ServiceClient framework, which provides a unified, reactive abstraction for service-to-service communication in the Firefly Common Domain Library.

## Table of Contents

- [Overview](#overview)
- [Core Concepts](#core-concepts)
- [REST ServiceClient](#rest-serviceclient)
- [gRPC ServiceClient](#grpc-serviceclient)
- [SDK ServiceClient](#sdk-serviceclient)
- [Builder Pattern](#builder-pattern)
- [Resilience Patterns](#resilience-patterns)
- [Configuration](#configuration)
- [Banking Examples](#banking-examples)

## Overview

The ServiceClient framework provides a unified interface for different types of service communication while maintaining protocol-specific optimizations and features.

### Key Features

- **Multi-Protocol Support**: REST, gRPC, and SDK-based communication
- **Reactive Programming**: Built on Project Reactor for non-blocking operations
- **Resilience Patterns**: Circuit breakers, retries, and timeouts
- **Authentication Integration**: Automatic auth token propagation
- **Correlation Context**: Distributed tracing across service boundaries
- **Health Monitoring**: Built-in health check capabilities

### ServiceClient Interface

<augment_code_snippet path="src/main/java/com/firefly/common/domain/client/ServiceClient.java" mode="EXCERPT">
````java
public interface ServiceClient<T> {

    // HTTP-style methods (REST clients)
    <R> Mono<R> get(String endpoint, Class<R> responseType);
    <R> Mono<R> get(String endpoint, Map<String, Object> queryParams, Class<R> responseType);
    <R> Mono<R> post(String endpoint, Object request, Class<R> responseType);
    <R> Mono<R> put(String endpoint, Object request, Class<R> responseType);
    <R> Mono<R> delete(String endpoint, Class<R> responseType);
    <R> Mono<R> patch(String endpoint, Object request, Class<R> responseType);

    // Service information
    String getServiceName();
    String getBaseUrl();

    // Health monitoring
    Mono<Void> healthCheck();

    // Enhanced methods with configuration support
    <R> Mono<R> get(String endpoint, RequestConfiguration requestConfig, Class<R> responseType);
    <R> Mono<R> post(String endpoint, Object request, RequestConfiguration requestConfig, Class<R> responseType);
    // ... additional configuration methods
}
````
</augment_code_snippet>

## Core Concepts

### Unified Interface

All ServiceClient implementations share a common interface while providing protocol-specific optimizations:

```java
// REST Client - HTTP operations
RestServiceClient restClient = RestServiceClient.builder()
    .serviceName("user-service")
    .baseUrl("http://user-service:8080")
    .build();

// gRPC Client - Protocol buffer operations  
GrpcServiceClient<UserServiceStub> grpcClient = GrpcServiceClient.builder()
    .serviceName("user-service")
    .address("user-service:9090")
    .stubFactory(channel -> UserServiceGrpc.newStub(channel))
    .build();

// SDK Client - SDK-specific operations
SdkServiceClient<PaymentSDK> sdkClient = SdkServiceClient.builder()
    .serviceName("payment-service")
    .sdkFactory(() -> new PaymentSDK(apiKey, environment))
    .build();
```

### Reactive Programming Model

All operations return `Mono<T>` for non-blocking, composable operations:

```java
// Chaining operations
Mono<PaymentResult> result = userClient.get("/users/{id}", User.class, Map.of("id", userId))
    .flatMap(user -> paymentClient.execute(sdk -> 
        sdk.processPayment(createPaymentRequest(user))))
    .flatMap(payment -> notificationClient.post("/notifications", 
        createNotification(payment), Void.class)
        .thenReturn(payment));
```

## REST ServiceClient

The REST ServiceClient provides HTTP-based communication with comprehensive features.

### Basic Operations

<augment_code_snippet path="src/main/java/com/firefly/common/domain/client/rest/RestServiceClient.java" mode="EXCERPT">
````java
// GET with query parameters
public <R> Mono<R> get(String endpoint, Map<String, Object> queryParams, Class<R> responseType) {
    // Implementation with WebClient, circuit breaker, and retry
}

// POST with request body
public <R> Mono<R> post(String endpoint, Object request, Class<R> responseType) {
    // Implementation with WebClient, circuit breaker, and retry
}
````
</augment_code_snippet>

### REST Client Examples

```java
@Component
public class CustomerService {
    
    private final RestServiceClient customerClient;
    
    // GET operation with path parameters
    public Mono<Customer> getCustomer(String customerId) {
        return customerClient.get("/customers/{id}", Customer.class, 
            Map.of("id", customerId));
    }
    
    // GET with query parameters
    public Mono<List<Account>> getCustomerAccounts(String customerId, String accountType) {
        Map<String, Object> queryParams = Map.of(
            "customerId", customerId,
            "type", accountType,
            "status", "ACTIVE"
        );
        
        return customerClient.get("/accounts", queryParams, 
            new TypeReference<List<Account>>() {});
    }
    
    // POST operation
    public Mono<Account> createAccount(CreateAccountRequest request) {
        return customerClient.post("/accounts", request, Account.class);
    }
    
    // PUT operation
    public Mono<Account> updateAccount(String accountId, UpdateAccountRequest request) {
        return customerClient.put("/accounts/{id}", request, Account.class,
            Map.of("id", accountId));
    }
    
    // DELETE operation
    public Mono<Void> closeAccount(String accountId) {
        return customerClient.delete("/accounts/{id}", Void.class,
            Map.of("id", accountId));
    }
}
```

### Advanced REST Features

```java
// Custom headers and authentication
RestServiceClient client = RestServiceClient.builder()
    .serviceName("secure-service")
    .baseUrl("https://secure-service:8443")
    .defaultHeader("Authorization", "Bearer " + token)
    .defaultHeader("Content-Type", "application/json")
    .authentication(AuthenticationConfiguration.bearer(tokenProvider))
    .build();

// Connection pooling configuration
RestServiceClient client = RestServiceClient.builder()
    .serviceName("high-volume-service")
    .baseUrl("http://service:8080")
    .maxConnections(200)
    .maxIdleTime(Duration.ofMinutes(10))
    .connectTimeout(Duration.ofSeconds(5))
    .responseTimeout(Duration.ofSeconds(30))
    .build();
```

## gRPC ServiceClient

The gRPC ServiceClient provides Protocol Buffer-based communication with streaming support.

### Basic gRPC Operations

<augment_code_snippet path="src/main/java/com/firefly/common/domain/client/grpc/GrpcServiceClient.java" mode="EXCERPT">
````java
public <REQ extends Message, RES> Mono<RES> call(REQ request, 
                                                Function<T, CompletableFuture<RES>> callFunction) {
    // Implementation with circuit breaker, retry, and metadata propagation
}

public <REQ extends Message, RES> Mono<RES> unaryCall(REQ request, 
                                                     Function<T, Function<REQ, CompletableFuture<RES>>> callFunction) {
    // Simplified unary call implementation
}
````
</augment_code_snippet>

### gRPC Client Examples

```java
@Component
public class PaymentGrpcService {
    
    private final GrpcServiceClient<PaymentServiceGrpc.PaymentServiceStub> paymentClient;
    
    // Unary call
    public Mono<ProcessPaymentResponse> processPayment(ProcessPaymentRequest request) {
        return paymentClient.unaryCall(request, 
            stub -> stub::processPayment);
    }
    
    // Direct call with custom logic
    public Mono<ValidateAccountResponse> validateAccount(ValidateAccountRequest request) {
        return paymentClient.call(request, stub -> {
            CompletableFuture<ValidateAccountResponse> future = new CompletableFuture<>();
            
            stub.validateAccount(request, new StreamObserver<ValidateAccountResponse>() {
                @Override
                public void onNext(ValidateAccountResponse response) {
                    future.complete(response);
                }
                
                @Override
                public void onError(Throwable throwable) {
                    future.completeExceptionally(throwable);
                }
                
                @Override
                public void onCompleted() {
                    // Response already completed in onNext
                }
            });
            
            return future;
        });
    }
}
```

### gRPC Builder Configuration

```java
// Basic gRPC client
GrpcServiceClient<PaymentServiceStub> client = GrpcServiceClient.builder()
    .serviceName("payment-service")
    .address("payment-service:9090")
    .stubFactory(channel -> PaymentServiceGrpc.newStub(channel))
    .usePlaintext()
    .build();

// Secure gRPC client with TLS
GrpcServiceClient<PaymentServiceStub> secureClient = GrpcServiceClient.builder()
    .serviceName("payment-service")
    .address("payment-service:9443")
    .stubFactory(channel -> PaymentServiceGrpc.newStub(channel))
    .useTransportSecurity()
    .keepAliveTime(Duration.ofMinutes(5))
    .keepAliveTimeout(Duration.ofSeconds(30))
    .build();
```

## SDK ServiceClient

The SDK ServiceClient provides first-class support for SDK-based integrations with automatic lifecycle management.

### SDK Operations

<augment_code_snippet path="src/main/java/com/firefly/common/domain/client/sdk/DefaultSdkServiceClient.java" mode="EXCERPT">
````java
public <R> Mono<R> execute(Function<S, R> operation) {
    // Synchronous SDK operation with circuit breaker and retry
}

public <R> Mono<R> executeAsync(Function<S, Mono<R>> operation) {
    // Asynchronous SDK operation with circuit breaker and retry
}
````
</augment_code_snippet>

### SDK Client Examples

```java
@Component
public class PaymentSdkService {
    
    private final SdkServiceClient<StripeSDK> stripeClient;
    private final SdkServiceClient<PayPalSDK> paypalClient;
    
    // Synchronous SDK operation
    public Mono<PaymentResult> processStripePayment(PaymentRequest request) {
        return stripeClient.execute(sdk -> {
            ChargeCreateParams params = ChargeCreateParams.builder()
                .setAmount(request.getAmount().longValue())
                .setCurrency(request.getCurrency())
                .setSource(request.getPaymentToken())
                .setDescription(request.getDescription())
                .build();
                
            return sdk.charges().create(params);
        }).map(this::mapToPaymentResult);
    }
    
    // Asynchronous SDK operation
    public Mono<PaymentResult> processPayPalPayment(PaymentRequest request) {
        return paypalClient.executeAsync(sdk -> {
            PaymentCreateRequest paypalRequest = buildPayPalRequest(request);
            return Mono.fromFuture(sdk.payments().create(paypalRequest));
        }).map(this::mapToPaymentResult);
    }
    
    // Direct SDK access (bypasses resilience patterns)
    public Mono<String> getStripeApiVersion() {
        return Mono.fromCallable(() -> stripeClient.getSdk().getApiVersion());
    }
}
```

### SDK Lifecycle Management

```java
// SDK with custom initialization
SdkServiceClient<BankingSDK> bankingClient = SdkServiceClient.builder()
    .serviceName("core-banking")
    .sdkFactory(() -> {
        BankingSDK sdk = new BankingSDK();
        sdk.configure(BankingConfig.builder()
            .environment(Environment.PRODUCTION)
            .apiKey(apiKeyProvider.getApiKey())
            .timeout(Duration.ofSeconds(30))
            .build());
        return sdk;
    })
    .autoShutdown(true)  // Automatically shutdown SDK on application shutdown
    .build();

// SDK with version tracking
SdkServiceClient<AWSSDK> awsClient = SdkServiceClient.builder()
    .serviceName("aws-services")
    .sdkFactory(() -> AWSSDK.builder()
        .region(Region.US_EAST_1)
        .credentialsProvider(credentialsProvider)
        .build())
    .sdkVersion("2.20.0")
    .build();
```

## Builder Pattern

All ServiceClient implementations use the builder pattern for flexible configuration.

### Common Builder Methods

```java
public interface ServiceClientBuilder<T extends ServiceClient<?>, B extends ServiceClientBuilder<T, B>> {
    
    B serviceName(String serviceName);
    B circuitBreaker(CircuitBreaker circuitBreaker);
    B retry(Retry retry);
    B correlationContext(CorrelationContext correlationContext);
    B timeout(Duration timeout);
    
    T build();
}
```

### Builder Examples

```java
// REST client with full configuration
RestServiceClient restClient = RestServiceClient.builder()
    .serviceName("user-service")
    .baseUrl("http://user-service:8080")
    .timeout(Duration.ofSeconds(30))
    .maxConnections(100)
    .authentication(AuthenticationConfiguration.bearer(tokenProvider))
    .defaultHeader("Accept", "application/json")
    .circuitBreaker(customCircuitBreaker)
    .retry(customRetry)
    .correlationContext(correlationContext)
    .build();

// gRPC client with custom channel
GrpcServiceClient<UserServiceStub> grpcClient = GrpcServiceClient.builder()
    .serviceName("user-service")
    .address("user-service:9090")
    .channel(customManagedChannel)
    .circuitBreaker(customCircuitBreaker)
    .build();

// SDK client with configuration
SdkServiceClient<PaymentSDK> sdkClient = SdkServiceClient.builder()
    .serviceName("payment-service")
    .sdkFactory(paymentSdkFactory)
    .timeout(Duration.ofSeconds(45))
    .sdkConfiguration(Map.of("environment", "production"))
    .build();
```

## Resilience Patterns

All ServiceClient implementations include built-in resilience patterns.

### Circuit Breaker

```java
// Custom circuit breaker configuration
CircuitBreakerConfig config = CircuitBreakerConfig.custom()
    .failureRateThreshold(50)
    .waitDurationInOpenState(Duration.ofSeconds(30))
    .slidingWindowSize(10)
    .minimumNumberOfCalls(5)
    .build();

CircuitBreaker circuitBreaker = CircuitBreaker.of("payment-service", config);

RestServiceClient client = RestServiceClient.builder()
    .serviceName("payment-service")
    .baseUrl("http://payment-service:8080")
    .circuitBreaker(circuitBreaker)
    .build();
```

### Retry Configuration

```java
// Custom retry configuration
RetryConfig retryConfig = RetryConfig.custom()
    .maxAttempts(3)
    .waitDuration(Duration.ofMillis(500))
    .retryExceptions(ConnectException.class, SocketTimeoutException.class)
    .ignoreExceptions(IllegalArgumentException.class)
    .build();

Retry retry = Retry.of("payment-service", retryConfig);

SdkServiceClient<PaymentSDK> client = SdkServiceClient.builder()
    .serviceName("payment-service")
    .sdkFactory(sdkFactory)
    .retry(retry)
    .build();
```

### Timeout Configuration

```java
// Global timeout
RestServiceClient client = RestServiceClient.builder()
    .serviceName("slow-service")
    .baseUrl("http://slow-service:8080")
    .timeout(Duration.ofMinutes(2))
    .build();

// Per-operation timeout
Mono<Result> result = client.get("/slow-endpoint", Result.class)
    .timeout(Duration.ofSeconds(30));
```

## Configuration

### Application Properties

```yaml
firefly:
  service-client:
    enabled: true
    
    # REST client defaults
    rest:
      max-connections: 100
      max-idle-time: 5m
      max-life-time: 30m
      pending-acquire-timeout: 10s
      response-timeout: 30s
      connect-timeout: 10s
      max-in-memory-size: 1048576  # 1MB
      
    # gRPC client defaults
    grpc:
      keep-alive-time: 5m
      keep-alive-timeout: 30s
      keep-alive-without-calls: true
      max-inbound-message-size: 4194304  # 4MB
      max-inbound-metadata-size: 8192    # 8KB
      
    # Circuit breaker defaults
    circuit-breaker:
      failure-rate-threshold: 50
      wait-duration-in-open-state: 30s
      sliding-window-size: 10
      minimum-number-of-calls: 5
      
    # Retry defaults
    retry:
      max-attempts: 3
      wait-duration: 500ms
      exponential-backoff-multiplier: 2.0
```

### Auto-Configuration

```java
@SpringBootApplication
@EnableFireflyCommonDomain
public class BankingServiceApplication {
    // ServiceClient framework is automatically configured
}
```

## Banking Examples

### Customer Onboarding Service

```java
@Component
public class CustomerOnboardingService {
    
    private final RestServiceClient customerServiceClient;
    private final SdkServiceClient<KycSDK> kycClient;
    private final RestServiceClient notificationClient;
    
    public Mono<OnboardingResult> onboardCustomer(OnboardingRequest request) {
        return createCustomerProfile(request)
            .flatMap(this::initiateKycVerification)
            .flatMap(this::sendWelcomeNotification)
            .map(this::buildOnboardingResult);
    }
    
    private Mono<Customer> createCustomerProfile(OnboardingRequest request) {
        return customerServiceClient.post("/customers", request, Customer.class);
    }
    
    private Mono<KycResult> initiateKycVerification(Customer customer) {
        return kycClient.execute(sdk -> 
            sdk.startVerification(KycRequest.builder()
                .customerId(customer.getId())
                .documentType(customer.getDocumentType())
                .documentNumber(customer.getDocumentNumber())
                .build()));
    }
    
    private Mono<Void> sendWelcomeNotification(KycResult kycResult) {
        WelcomeNotification notification = WelcomeNotification.builder()
            .customerId(kycResult.getCustomerId())
            .kycStatus(kycResult.getStatus())
            .build();
            
        return notificationClient.post("/notifications/welcome", notification, Void.class);
    }
}
```

### Payment Processing Service

```java
@Component
public class PaymentProcessingService {
    
    private final SdkServiceClient<StripeSDK> stripeClient;
    private final SdkServiceClient<PayPalSDK> paypalClient;
    private final RestServiceClient fraudServiceClient;
    private final GrpcServiceClient<AccountServiceStub> accountServiceClient;
    
    public Mono<PaymentResult> processPayment(PaymentRequest request) {
        return validateAccount(request.getAccountId())
            .flatMap(account -> performFraudCheck(request, account))
            .flatMap(fraudResult -> {
                if (!fraudResult.isApproved()) {
                    return Mono.error(new PaymentRejectedException("Payment blocked by fraud detection"));
                }
                return processPaymentWithProvider(request);
            });
    }
    
    private Mono<Account> validateAccount(String accountId) {
        ValidateAccountRequest grpcRequest = ValidateAccountRequest.newBuilder()
            .setAccountId(accountId)
            .build();
            
        return accountServiceClient.unaryCall(grpcRequest, 
            stub -> stub::validateAccount)
            .map(this::mapToAccount);
    }
    
    private Mono<FraudResult> performFraudCheck(PaymentRequest request, Account account) {
        FraudCheckRequest fraudRequest = FraudCheckRequest.builder()
            .accountId(account.getId())
            .amount(request.getAmount())
            .merchantId(request.getMerchantId())
            .build();
            
        return fraudServiceClient.post("/fraud/check", fraudRequest, FraudResult.class);
    }
    
    private Mono<PaymentResult> processPaymentWithProvider(PaymentRequest request) {
        return switch (request.getProvider()) {
            case STRIPE -> processStripePayment(request);
            case PAYPAL -> processPayPalPayment(request);
            default -> Mono.error(new UnsupportedPaymentProviderException(request.getProvider()));
        };
    }
}
```

---

The ServiceClient framework provides a powerful, unified approach to service communication that scales from simple REST calls to complex SDK integrations while maintaining consistency, resilience, and observability across all communication patterns.
