package com.firefly.common.domain.client;

import com.firefly.common.domain.client.impl.SdkServiceClientImpl;
import com.firefly.common.domain.client.interceptor.LoggingInterceptor;
import com.firefly.common.domain.client.interceptor.MetricsInterceptor;
import com.firefly.common.domain.client.test.MockAWSSDK;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for enhanced SDK client features.
 */
class EnhancedSdkClientTest {

    @Test
    @DisplayName("Should create SDK client with convenience methods")
    void shouldCreateSdkClientWithConvenienceMethods() {
        // Test sdkInstance() convenience method
        MockAWSSDK existingSDK = new MockAWSSDK();
        ServiceClient client1 = ServiceClient.sdk("test-service", MockAWSSDK.class)
            .sdkInstance(existingSDK)
            .timeout(Duration.ofSeconds(30))
            .build();

        assertNotNull(client1);
        
        // Test sdkSupplier() convenience method
        ServiceClient client2 = ServiceClient.sdk("test-service", MockAWSSDK.class)
            .sdkSupplier(() -> new MockAWSSDK())
            .timeout(Duration.ofSeconds(30))
            .build();

        assertNotNull(client2);
    }

    @Test
    @DisplayName("Should create SDK client with observability features")
    void shouldCreateSdkClientWithObservability() {
        ServiceClient client = ServiceClient.sdk("test-service", MockAWSSDK.class)
            .sdkSupplier(() -> new MockAWSSDK())
            .withObservability() // Enables logging and metrics
            .build();

        assertNotNull(client);
        
        // Verify diagnostics include interceptors
        if (client instanceof SdkServiceClientImpl) {
            Map<String, Object> diagnostics = ((SdkServiceClientImpl<?>) client).getDiagnostics();
            assertEquals(2, diagnostics.get("interceptorCount"));
        }
    }

    @Test
    @DisplayName("Should create SDK client with resilience features")
    void shouldCreateSdkClientWithResilience() {
        ServiceClient client = ServiceClient.sdk("test-service", MockAWSSDK.class)
            .sdkSupplier(() -> new MockAWSSDK())
            .withResilience() // Enables circuit breaker and retry
            .build();

        assertNotNull(client);
        
        // Verify diagnostics include resilience features
        if (client instanceof SdkServiceClientImpl) {
            Map<String, Object> diagnostics = ((SdkServiceClientImpl<?>) client).getDiagnostics();
            assertTrue((Boolean) diagnostics.get("hasCircuitBreaker"));
            assertTrue((Boolean) diagnostics.get("hasRetry"));
        }
    }

    @Test
    @DisplayName("Should create SDK client with all defaults")
    void shouldCreateSdkClientWithDefaults() {
        ServiceClient client = ServiceClient.sdk("test-service", MockAWSSDK.class)
            .sdkSupplier(() -> new MockAWSSDK())
            .withDefaults() // Enables everything with defaults
            .build();

        assertNotNull(client);
        
        // Verify diagnostics include all features
        if (client instanceof SdkServiceClientImpl) {
            Map<String, Object> diagnostics = ((SdkServiceClientImpl<?>) client).getDiagnostics();
            assertTrue((Boolean) diagnostics.get("hasCircuitBreaker"));
            assertTrue((Boolean) diagnostics.get("hasRetry"));
            assertEquals(2, diagnostics.get("interceptorCount")); // Logging + Metrics
        }
    }

    @Test
    @DisplayName("Should provide enhanced error handling")
    void shouldProvideEnhancedErrorHandling() {
        ServiceClient client = ServiceClient.sdk("test-service", MockAWSSDK.class)
            .sdkSupplier(() -> new MockAWSSDK())
            .build();

        // Test null operation error
        StepVerifier.create(client.call(null))
            .expectErrorMatches(error -> 
                error instanceof IllegalArgumentException &&
                error.getMessage().contains("Operation cannot be null"))
            .verify();

        // Test null async operation error
        StepVerifier.create(client.callAsync(null))
            .expectErrorMatches(error -> 
                error instanceof IllegalArgumentException &&
                error.getMessage().contains("Async operation cannot be null"))
            .verify();
    }

    @Test
    @DisplayName("Should provide type-safe operations with TypedSdkClient")
    void shouldProvideTypeSafeOperations() {
        ServiceClient client = ServiceClient.sdk("test-service", MockAWSSDK.class)
            .sdkSupplier(() -> new MockAWSSDK())
            .withDefaults()
            .build();

        TypedSdkClient<MockAWSSDK> typedClient = client.typed();

        // Test type-safe call
        Mono<String> result = typedClient.call(sdk -> 
            sdk.getS3Object("bucket", "key")
        );

        StepVerifier.create(result)
            .expectNext("S3Object from bucket/key")
            .verifyComplete();

        // Test type-safe async call
        Mono<String> asyncResult = typedClient.callAsync(sdk -> 
            sdk.getS3ObjectAsync("bucket", "key")
        );

        StepVerifier.create(asyncResult)
            .expectNext("Async S3Object from bucket/key")
            .verifyComplete();

        // Test type-safe direct access
        MockAWSSDK sdk = typedClient.sdk();
        assertNotNull(sdk);
        assertEquals("S3Object from bucket/key", sdk.getS3Object("bucket", "key"));
    }

    @Test
    @DisplayName("Should handle shutdown state properly")
    void shouldHandleShutdownStateProperly() {
        ServiceClient client = ServiceClient.sdk("test-service", MockAWSSDK.class)
            .sdkSupplier(() -> new MockAWSSDK())
            .build();

        // Shutdown the client
        client.shutdown();

        // Test that operations fail after shutdown
        StepVerifier.create(client.call(sdk -> "test"))
            .expectErrorMatches(error -> 
                error instanceof IllegalStateException &&
                error.getMessage().contains("has been shut down"))
            .verify();

        StepVerifier.create(client.callAsync(sdk -> Mono.just("test")))
            .expectErrorMatches(error -> 
                error instanceof IllegalStateException &&
                error.getMessage().contains("has been shut down"))
            .verify();

        assertThrows(IllegalStateException.class, () -> client.sdk());
    }

    @Test
    @DisplayName("Should provide comprehensive diagnostics")
    void shouldProvideComprehensiveDiagnostics() {
        ServiceClient client = ServiceClient.sdk("test-service", MockAWSSDK.class)
            .sdkSupplier(() -> new MockAWSSDK())
            .timeout(Duration.ofSeconds(45))
            .withDefaults()
            .build();

        if (client instanceof SdkServiceClientImpl) {
            Map<String, Object> diagnostics = ((SdkServiceClientImpl<?>) client).getDiagnostics();
            
            assertEquals("test-service", diagnostics.get("serviceName"));
            assertEquals(MockAWSSDK.class.getName(), diagnostics.get("sdkType"));
            assertEquals("PT45S", diagnostics.get("timeout"));
            assertTrue((Boolean) diagnostics.get("autoShutdown"));
            assertFalse((Boolean) diagnostics.get("isShutdown"));
            assertTrue((Boolean) diagnostics.get("hasCircuitBreaker"));
            assertTrue((Boolean) diagnostics.get("hasRetry"));
            assertEquals(2, diagnostics.get("interceptorCount"));
            
            assertNotNull(diagnostics.get("interceptors"));
        }
    }

    @Test
    @DisplayName("Should demonstrate complete enhanced API")
    void shouldDemonstrateCompleteEnhancedAPI() {
        // Create a fully-featured SDK client with all enhancements
        ServiceClient client = ServiceClient.sdk("enhanced-service", MockAWSSDK.class)
            .sdkSupplier(() -> new MockAWSSDK())
            .timeout(Duration.ofSeconds(30))
            .withDefaults() // Circuit breaker, retry, logging, metrics
            .build();

        // Get type-safe client
        TypedSdkClient<MockAWSSDK> typedClient = client.typed();

        // Perform operations
        Mono<String> result = typedClient.call(sdk -> 
            sdk.s3().getObject("enhanced-bucket")
        );

        StepVerifier.create(result)
            .expectNext("S3Object from request: enhanced-bucket")
            .verifyComplete();

        // Verify client is fully functional
        assertNotNull(client);
        assertNotNull(typedClient);
    }
}
