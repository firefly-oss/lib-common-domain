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

package com.firefly.common.domain.client.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.firefly.common.domain.client.ClientType;
import com.firefly.common.domain.client.RequestBuilder;
import com.firefly.common.domain.client.ServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Comprehensive test suite for the redesigned ServiceClient framework.
 * 
 * <p>This test demonstrates the new simplified API and improved developer experience
 * across all client types (REST, gRPC, SDK) with banking domain examples.
 */
@DisplayName("New ServiceClient Framework - Banking Integration Tests")
class NewServiceClientTest {

    @Test
    @DisplayName("Should create REST client with simplified builder API")
    void shouldCreateRestClientWithSimplifiedBuilder() {
        // Given: Simple REST client creation
        ServiceClient client = ServiceClient.rest("customer-service")
            .baseUrl("http://customer-service:8080")
            .timeout(Duration.ofSeconds(30))
            .defaultHeader("Accept", "application/json")
            .build();

        // Then: Client should be properly configured
        assertThat(client).isNotNull();
        assertThat(client.getServiceName()).isEqualTo("customer-service");
        assertThat(client.getBaseUrl()).isEqualTo("http://customer-service:8080");
        assertThat(client.getClientType()).isEqualTo(ClientType.REST);
        assertThat(client.isReady()).isTrue();
    }

    @Test
    @DisplayName("Should demonstrate fluent request builder API for banking operations")
    void shouldDemonstrateFluentRequestBuilderApi() {
        // Given: A REST client for account service
        ServiceClient accountClient = ServiceClient.rest("account-service")
            .baseUrl("http://account-service:8080")
            .jsonContentType()
            .build();

        // When: Building a request with fluent API
        ServiceClient.RequestBuilder<Account> requestBuilder = accountClient.get("/accounts/{accountId}", Account.class)
            .withPathParam("accountId", "ACC-123456")
            .withQueryParam("includeBalance", true)
            .withQueryParam("includeTransactions", false)
            .withHeader("X-Customer-ID", "CUST-789")
            .withTimeout(Duration.ofSeconds(15));

        // Then: Request builder should be properly configured
        assertThat(requestBuilder).isNotNull();
        
        // Note: In a real test, we would mock the WebClient to verify the actual request
        // For this demonstration, we're just showing the API structure
    }

    @Test
    @DisplayName("Should create gRPC client with simplified configuration")
    void shouldCreateGrpcClientWithSimplifiedConfiguration() {
        // Given: gRPC client creation (simplified)
        ServiceClient grpcClient = ServiceClient.grpc("payment-service", PaymentServiceStub.class)
            .address("payment-service:9090")
            .usePlaintext()
            .timeout(Duration.ofSeconds(30))
            .stubFactory(channel -> new PaymentServiceStub())
            .build();

        // Then: Client should be properly configured
        assertThat(grpcClient).isNotNull();
        assertThat(grpcClient.getServiceName()).isEqualTo("payment-service");
        assertThat(grpcClient.getBaseUrl()).isEqualTo("payment-service:9090"); // For gRPC, this returns address
        assertThat(grpcClient.getClientType()).isEqualTo(ClientType.GRPC);
        assertThat(grpcClient.isReady()).isTrue();
    }

    @Test
    @DisplayName("Should create SDK client with simplified configuration")
    void shouldCreateSdkClientWithSimplifiedConfiguration() {
        // Given: SDK client creation
        ServiceClient sdkClient = ServiceClient.sdk("fraud-detection", FraudDetectionSDK.class)
            .sdkSupplier(() -> new FraudDetectionSDK("api-key", "production"))
            .timeout(Duration.ofSeconds(45))
            .autoShutdown(true)
            .build();

        // Then: Client should be properly configured
        assertThat(sdkClient).isNotNull();
        assertThat(sdkClient.getServiceName()).isEqualTo("fraud-detection");
        assertThat(sdkClient.getBaseUrl()).isNull(); // SDK clients don't have base URLs
        assertThat(sdkClient.getClientType()).isEqualTo(ClientType.SDK);
        assertThat(sdkClient.isReady()).isTrue();
    }

    @Test
    @DisplayName("Should demonstrate SDK client execute operations")
    void shouldDemonstrateSdkClientExecuteOperations() {
        // Given: A fraud detection SDK client
        ServiceClient fraudClient = ServiceClient.sdk("fraud-detection", FraudDetectionSDK.class)
            .sdkSupplier(() -> new MockFraudDetectionSDK())
            .build();

        // When: Executing SDK operation with new improved API
        Mono<FraudCheckResult> result = fraudClient.<FraudDetectionSDK, FraudCheckResult>call(sdk ->
            sdk.checkTransaction("TXN-123", new BigDecimal("1000.00"), "USD")
        );

        // Then: Operation should complete successfully
        StepVerifier.create(result)
            .assertNext(fraudResult -> {
                assertThat(fraudResult).isNotNull();
                assertThat(fraudResult.getTransactionId()).isEqualTo("TXN-123");
                assertThat(fraudResult.isApproved()).isTrue();
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should demonstrate direct SDK access for complex operations")
    void shouldDemonstrateDirectSDKAccess() {
        // Given: SDK client for fraud detection
        ServiceClient fraudClient = ServiceClient.sdk("fraud-detection", FraudDetectionSDK.class)
            .sdkSupplier(() -> new MockFraudDetectionSDK())
            .build();

        // When: Using direct SDK access for complex operations
        FraudDetectionSDK sdk = fraudClient.<FraudDetectionSDK>sdk();

        // Then: Should have direct access to all SDK methods
        assertThat(sdk).isNotNull();
        assertThat(sdk).isInstanceOf(FraudDetectionSDK.class);

        // Can use SDK directly for complex operations
        FraudCheckResult result = sdk.checkTransaction("TXN-456", new BigDecimal("500.00"), "EUR");
        assertThat(result.getTransactionId()).isEqualTo("TXN-456");
        assertThat(result.isApproved()).isTrue();
    }

    @Test
    @DisplayName("Should demonstrate async SDK operations")
    void shouldDemonstrateAsyncSDKOperations() {
        // Given: SDK client that supports async operations
        ServiceClient fraudClient = ServiceClient.sdk("fraud-detection", FraudDetectionSDK.class)
            .sdkSupplier(() -> new MockFraudDetectionSDK())
            .build();

        // When: Using callAsync for reactive SDK operations
        Mono<FraudCheckResult> asyncResult = fraudClient.<FraudDetectionSDK, FraudCheckResult>callAsync(sdk ->
            Mono.fromCallable(() -> sdk.checkTransaction("TXN-789", new BigDecimal("2000.00"), "GBP"))
        );

        // Then: Async operation should complete successfully
        StepVerifier.create(asyncResult)
            .assertNext(fraudResult -> {
                assertThat(fraudResult.getTransactionId()).isEqualTo("TXN-789");
                assertThat(fraudResult.isApproved()).isTrue();
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle client type validation correctly")
    void shouldHandleClientTypeValidationCorrectly() {
        // Given: Different client types
        ServiceClient restClient = ServiceClient.rest("test-service")
            .baseUrl("http://test:8080")
            .build();
        
        ServiceClient sdkClient = ServiceClient.sdk("test-sdk", MockSDK.class)
            .sdkSupplier(MockSDK::new)
            .build();

        // Then: Client types should be correctly identified
        assertThat(restClient.getClientType().isHttpBased()).isTrue();
        assertThat(restClient.getClientType().supportsStreaming()).isTrue();
        assertThat(restClient.getClientType().requiresSdkIntegration()).isFalse();

        assertThat(sdkClient.getClientType().isHttpBased()).isFalse();
        assertThat(sdkClient.getClientType().supportsStreaming()).isFalse();
        assertThat(sdkClient.getClientType().requiresSdkIntegration()).isTrue();
    }

    @Test
    @DisplayName("Should validate builder configuration properly")
    void shouldValidateBuilderConfigurationProperly() {
        // When: Creating REST client without base URL
        assertThrows(IllegalStateException.class, () -> {
            ServiceClient.rest("test-service").build();
        });

        // When: Creating SDK client without SDK factory
        assertThrows(IllegalStateException.class, () -> {
            ServiceClient.sdk("test-service", MockSDK.class).build();
        });

        // When: Creating client with invalid service name
        assertThrows(IllegalArgumentException.class, () -> {
            ServiceClient.rest("");
        });
    }

    @Test
    @DisplayName("Should demonstrate improved error handling")
    void shouldDemonstrateImprovedErrorHandling() {
        // Given: An SDK client
        ServiceClient sdkClient = ServiceClient.sdk("test-service", MockSDK.class)
            .sdkSupplier(MockSDK::new)
            .build();

        // When: Trying to use HTTP methods on SDK client
        ServiceClient.RequestBuilder<String> getRequest = sdkClient.get("/test", String.class);

        // Then: Should get appropriate error message
        StepVerifier.create(getRequest.execute())
            .expectErrorMatches(throwable -> 
                throwable instanceof UnsupportedOperationException &&
                throwable.getMessage().contains("SDK clients do not support HTTP operations"))
            .verify();
    }

    // ========================================
    // Mock Classes for Testing
    // ========================================

    static class Account {
        private String accountId;
        private String customerId;
        private BigDecimal balance;
        private String currency;
        private String status;

        // Constructors, getters, setters...
        public Account() {}
        
        public Account(String accountId, String customerId, BigDecimal balance, String currency, String status) {
            this.accountId = accountId;
            this.customerId = customerId;
            this.balance = balance;
            this.currency = currency;
            this.status = status;
        }

        // Getters and setters
        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    static class PaymentServiceStub {
        // Mock gRPC stub
    }

    static class FraudDetectionSDK {
        public FraudDetectionSDK() {}

        public FraudDetectionSDK(String apiKey, String environment) {
            // Constructor for initialization
        }

        public FraudCheckResult checkTransaction(String transactionId, BigDecimal amount, String currency) {
            return new FraudCheckResult(transactionId, true, "Low risk transaction");
        }
    }

    static class MockFraudDetectionSDK extends FraudDetectionSDK {
        @Override
        public FraudCheckResult checkTransaction(String transactionId, BigDecimal amount, String currency) {
            return new FraudCheckResult(transactionId, true, "Low risk transaction");
        }
    }

    static class FraudCheckResult {
        private final String transactionId;
        private final boolean approved;
        private final String reason;

        public FraudCheckResult(String transactionId, boolean approved, String reason) {
            this.transactionId = transactionId;
            this.approved = approved;
            this.reason = reason;
        }

        public String getTransactionId() { return transactionId; }
        public boolean isApproved() { return approved; }
        public String getReason() { return reason; }
    }

    static class MockSDK {
        public String performOperation() {
            return "success";
        }
    }
}
