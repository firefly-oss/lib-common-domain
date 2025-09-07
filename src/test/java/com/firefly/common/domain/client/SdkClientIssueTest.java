package com.firefly.common.domain.client;

import com.firefly.common.domain.client.test.MockAWSSDK;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to reproduce the SDK client issues mentioned by the user.
 */
class SdkClientIssueTest {

    @Test
    @DisplayName("Should work with the new sdkSupplier() API")
    void shouldWorkWithSdkSupplierAPI() {
        // Now this should work with sdkSupplier()
        ServiceClient sdkClient = ServiceClient.sdk("aws-service", MockAWSSDK.class)
            .sdkSupplier(() -> new MockAWSSDK())
            .timeout(Duration.ofSeconds(45))
            .build();

        assertNotNull(sdkClient);
    }

    @Test
    @DisplayName("Should demonstrate the improved API as shown in documentation")
    void shouldDemonstrateImprovedAPI() {
        // This is the exact API from the documentation - now it works!
        ServiceClient sdkClient = ServiceClient.sdk("aws-service", MockAWSSDK.class)
            .sdkSupplier(() -> new MockAWSSDK())
            .timeout(Duration.ofSeconds(45))
            .build();

        // Test that sdkSupplier() method works
        assertNotNull(sdkClient);

        // For now, we still need casting, but the sdkSupplier() API works
        Mono<String> object = sdkClient.call(sdk -> {
            MockAWSSDK awsSDK = (MockAWSSDK) sdk;
            return awsSDK.s3().getObject("test-request");
        });

        // Verify results
        StepVerifier.create(object)
            .expectNext("S3Object from request: test-request")
            .verifyComplete();
    }

    @Test
    @DisplayName("Should provide true type safety with TypedSdkClient")
    void shouldProvideTypeSafetyWithTypedSdkClient() {
        // Create a regular SDK client
        ServiceClient sdkClient = ServiceClient.sdk("aws-service", MockAWSSDK.class)
            .sdkSupplier(() -> new MockAWSSDK())
            .timeout(Duration.ofSeconds(45))
            .build();

        // Get a type-safe wrapper - NO CASTING REQUIRED!
        TypedSdkClient<MockAWSSDK> typedClient = sdkClient.typed();

        // NEW: Type-safe SDK operations - no casting required!
        Mono<String> object = typedClient.call(sdk ->
            sdk.s3().getObject("test-request")
        );

        // NEW: Async SDK operations for reactive SDKs
        Mono<String> asyncObject = typedClient.callAsync(sdk ->
            sdk.s3().getObjectAsync("test-request")
        );

        // NEW: Direct SDK access for complex operations
        MockAWSSDK sdk = typedClient.sdk();
        String directObject = sdk.s3().getObject("test-request");

        // Verify results
        StepVerifier.create(object)
            .expectNext("S3Object from request: test-request")
            .verifyComplete();

        StepVerifier.create(asyncObject)
            .expectNext("Async S3Object from request: test-request")
            .verifyComplete();

        assertEquals("S3Object from request: test-request", directObject);
    }

    @Test
    @DisplayName("Should demonstrate the complete fixed API as requested by user")
    void shouldDemonstrateCompleteFixedAPI() {
        // This is the EXACT API the user wanted to work:
        ServiceClient sdkClient = ServiceClient.sdk("aws-service", MockAWSSDK.class)
            .sdkSupplier(() -> new MockAWSSDK()) // ✅ FIXED: sdkSupplier() now works!
            .timeout(Duration.ofSeconds(45))
            .build();

        // Get type-safe client for true type safety
        TypedSdkClient<MockAWSSDK> typedClient = sdkClient.typed();

        // ✅ FIXED: Type-safe SDK operations - no casting required!
        Mono<String> object = typedClient.call(sdk ->
            sdk.s3().getObject("test-request")
        );

        // ✅ FIXED: Async SDK operations for reactive SDKs
        Mono<String> asyncObject = typedClient.callAsync(sdk ->
            sdk.s3().getObjectAsync("test-request")
        );

        // ✅ FIXED: Direct SDK access for complex operations
        MockAWSSDK sdk = typedClient.sdk();
        String directObject = sdk.s3().getObject("test-request");

        // Verify all operations work correctly
        StepVerifier.create(object)
            .expectNext("S3Object from request: test-request")
            .verifyComplete();

        StepVerifier.create(asyncObject)
            .expectNext("Async S3Object from request: test-request")
            .verifyComplete();

        assertEquals("S3Object from request: test-request", directObject);

        // Verify the underlying ServiceClient is accessible
        assertSame(sdkClient, typedClient.getServiceClient());
    }
}
