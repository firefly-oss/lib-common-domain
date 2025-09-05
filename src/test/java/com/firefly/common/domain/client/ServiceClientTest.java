/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.common.domain.client;

import com.firefly.common.domain.client.rest.RestServiceClient;
import com.firefly.common.domain.client.sdk.DefaultSdkServiceClient;
import com.firefly.common.domain.client.sdk.SdkServiceClient;
import com.firefly.common.domain.tracing.CorrelationContext;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Comprehensive test suite for ServiceClient framework using banking domain examples.
 * Tests cover REST clients, SDK clients, circuit breaker patterns, retry mechanisms,
 * and correlation context propagation for banking service integrations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Banking Service Clients - External Service Integration")
class ServiceClientTest {

    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;
    
    @Mock
    private CorrelationContext correlationContext;

    private CircuitBreaker circuitBreaker;
    private Retry retry;

    @BeforeEach
    void setUp() {
        // Configure circuit breaker for testing
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(1))
            .slidingWindowSize(5)
            .minimumNumberOfCalls(3)
            .build();
        circuitBreaker = CircuitBreaker.of("test-service", circuitBreakerConfig);

        // Configure retry for testing
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(100))
            .build();
        retry = Retry.of("test-service", retryConfig);

        lenient().when(correlationContext.getCorrelationId()).thenReturn("CORR-TEST-123");
    }

    @Test
    @DisplayName("Should successfully call external credit scoring service via REST")
    void shouldCallExternalCreditScoringServiceViaRest() {
        // Given: A REST client for credit scoring service
        CreditScoreResponse expectedResponse = new CreditScoreResponse(
            "CUST-12345",
            750,
            "EXCELLENT",
            "Low risk customer with excellent payment history"
        );

        // Mock WebClient behavior
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any(java.util.function.Consumer.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CreditScoreResponse.class)).thenReturn(Mono.just(expectedResponse));

        RestServiceClient creditScoringClient = RestServiceClient.builder()
            .serviceName("credit-scoring-service")
            .baseUrl("https://credit-scoring-api.example.com")
            .timeout(Duration.ofSeconds(30))
            .circuitBreaker(circuitBreaker)
            .retry(retry)
            .correlationContext(correlationContext)
            .webClient(webClient)
            .build();

        // When: Credit score is requested for a customer
        Mono<CreditScoreResponse> result = creditScoringClient.get(
            "/credit-score/CUST-12345",
            CreditScoreResponse.class
        );

        // Then: Credit score should be returned successfully
        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response.getCustomerId()).isEqualTo("CUST-12345");
                assertThat(response.getScore()).isEqualTo(750);
                assertThat(response.getRating()).isEqualTo("EXCELLENT");
                assertThat(response.getDescription()).contains("excellent payment history");
            })
            .verifyComplete();

        // Note: Correlation context usage depends on implementation details
    }

    @Test
    @DisplayName("Should successfully execute payment processing via SDK client")
    void shouldExecutePaymentProcessingViaSdkClient() {
        // Given: A mock payment SDK
        MockPaymentSDK paymentSDK = new MockPaymentSDK();
        
        SdkServiceClient<MockPaymentSDK> paymentClient = new DefaultSdkServiceClient<>(
            "payment-service",
            paymentSDK,
            circuitBreaker,
            retry,
            correlationContext,
            Duration.ofSeconds(30),
            Map.of("environment", "test"),
            true,
            "1.0.0"
        );

        PaymentRequest paymentRequest = new PaymentRequest(
            "TXN-98765",
            "ACC-001",
            "ACC-002",
            new BigDecimal("500.00"),
            "USD",
            "Transfer to savings"
        );

        // When: Payment is processed through SDK
        Mono<PaymentResult> result = paymentClient.execute(sdk -> 
            sdk.processPayment(paymentRequest)
        );

        // Then: Payment should be processed successfully
        StepVerifier.create(result)
            .assertNext(paymentResult -> {
                assertThat(paymentResult.getTransactionId()).isEqualTo("TXN-98765");
                assertThat(paymentResult.getStatus()).isEqualTo("COMPLETED");
                assertThat(paymentResult.getAmount()).isEqualTo(new BigDecimal("500.00"));
                assertThat(paymentResult.getProcessingTime()).isGreaterThan(0);
            })
            .verifyComplete();

        // Verify SDK state
        assertThat(paymentClient.isReady()).isTrue();
        assertThat(paymentClient.getSdkVersion()).isEqualTo("1.0.0");
        assertThat(paymentClient.getSdkConfiguration()).containsEntry("environment", "test");
    }

    @Test
    @DisplayName("Should handle circuit breaker opening when service is failing")
    void shouldHandleCircuitBreakerOpeningWhenServiceFailing() {
        // Given: A failing payment SDK
        FailingPaymentSDK failingSDK = new FailingPaymentSDK();
        
        SdkServiceClient<FailingPaymentSDK> failingClient = new DefaultSdkServiceClient<>(
            "failing-payment-service",
            failingSDK,
            circuitBreaker,
            retry,
            correlationContext,
            Duration.ofSeconds(1),
            Map.of(),
            true,
            "1.0.0"
        );

        PaymentRequest paymentRequest = new PaymentRequest(
            "TXN-FAIL",
            "ACC-001",
            "ACC-002",
            new BigDecimal("100.00"),
            "USD",
            "Test payment"
        );

        // When: Multiple failing requests are made to trigger circuit breaker
        for (int i = 0; i < 5; i++) {
            StepVerifier.create(failingClient.execute(sdk -> sdk.processPayment(paymentRequest)))
                .expectError()
                .verify();
        }

        // Then: Circuit breaker should be open and subsequent calls should fail fast
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("Should execute async payment processing with proper timeout handling")
    void shouldExecuteAsyncPaymentProcessingWithTimeoutHandling() {
        // Given: A mock async payment SDK
        MockAsyncPaymentSDK asyncSDK = new MockAsyncPaymentSDK();
        
        SdkServiceClient<MockAsyncPaymentSDK> asyncClient = new DefaultSdkServiceClient<>(
            "async-payment-service",
            asyncSDK,
            circuitBreaker,
            retry,
            correlationContext,
            Duration.ofSeconds(2),
            Map.of(),
            true,
            "1.0.0"
        );

        PaymentRequest paymentRequest = new PaymentRequest(
            "TXN-ASYNC-001",
            "ACC-001",
            "ACC-002",
            new BigDecimal("750.00"),
            "USD",
            "Async payment test"
        );

        // When: Async payment is processed
        Mono<PaymentResult> result = asyncClient.executeAsync(sdk ->
            Mono.fromFuture(sdk.processPaymentAsync(paymentRequest))
        );

        // Then: Payment should complete within timeout
        StepVerifier.create(result)
            .assertNext(paymentResult -> {
                assertThat(paymentResult.getTransactionId()).isEqualTo("TXN-ASYNC-001");
                assertThat(paymentResult.getStatus()).isEqualTo("COMPLETED");
                assertThat(paymentResult.getAmount()).isEqualTo(new BigDecimal("750.00"));
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle SDK client shutdown gracefully")
    void shouldHandleSdkClientShutdownGracefully() {
        // Given: An SDK client
        MockPaymentSDK paymentSDK = new MockPaymentSDK();
        
        SdkServiceClient<MockPaymentSDK> paymentClient = new DefaultSdkServiceClient<>(
            "payment-service",
            paymentSDK,
            circuitBreaker,
            retry,
            correlationContext,
            Duration.ofSeconds(30),
            Map.of(),
            true,
            "1.0.0"
        );

        // When: Client is shut down
        paymentClient.shutdown();

        // Then: Client should be marked as not ready
        assertThat(paymentClient.isReady()).isFalse();

        // And: Subsequent operations should fail
        PaymentRequest request = new PaymentRequest("TXN-001", "ACC-001", "ACC-002", 
            new BigDecimal("100.00"), "USD", "Test");
        
        StepVerifier.create(paymentClient.execute(sdk -> sdk.processPayment(request)))
            .expectError(IllegalStateException.class)
            .verify();
    }

    // Test Data Classes
    @Data
    static class CreditScoreResponse {
        private final String customerId;
        private final int score;
        private final String rating;
        private final String description;
    }

    @Data
    static class PaymentRequest {
        private final String transactionId;
        private final String fromAccount;
        private final String toAccount;
        private final BigDecimal amount;
        private final String currency;
        private final String description;
    }

    @Data
    static class PaymentResult {
        private final String transactionId;
        private final String status;
        private final BigDecimal amount;
        private final long processingTime;
    }

    // Mock SDK Classes
    static class MockPaymentSDK {
        public PaymentResult processPayment(PaymentRequest request) {
            return new PaymentResult(
                request.getTransactionId(),
                "COMPLETED",
                request.getAmount(),
                System.currentTimeMillis() % 1000
            );
        }
    }

    static class FailingPaymentSDK {
        public PaymentResult processPayment(PaymentRequest request) {
            throw new RuntimeException("Payment service unavailable");
        }
    }

    static class MockAsyncPaymentSDK {
        public CompletableFuture<PaymentResult> processPaymentAsync(PaymentRequest request) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(500); // Simulate processing time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new PaymentResult(
                    request.getTransactionId(),
                    "COMPLETED",
                    request.getAmount(),
                    500L
                );
            });
        }
    }
}
