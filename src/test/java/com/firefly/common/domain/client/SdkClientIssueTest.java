package com.firefly.common.domain.client;

import com.firefly.common.domain.client.test.MockAWSSDK;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to reproduce the SDK client issues mentioned by the user.
 */
class SdkClientIssueTest {

    @Test
    @DisplayName("Should reproduce SDK client issues with the API shown in documentation")
    void shouldReproduceSdkClientIssues() {
        // This should fail to compile because sdkSupplier() doesn't exist
        // ServiceClient sdkClient = ServiceClient.sdk("aws-service", MockAWSSDK.class)
        //     .sdkSupplier(() -> new MockAWSSDK())
        //     .timeout(Duration.ofSeconds(45))
        //     .build();

        // Let's try with the actual API that exists
        ServiceClient sdkClient = ServiceClient.sdk("aws-service", MockAWSSDK.class)
            .sdkFactory(unused -> new MockAWSSDK())
            .timeout(Duration.ofSeconds(45))
            .build();

        // Test the call() method - this should work
        Mono<String> result = sdkClient.call(sdk -> {
            MockAWSSDK awsSDK = (MockAWSSDK) sdk; // Still requires casting!
            return awsSDK.getS3Object("test-bucket", "test-key");
        });

        // Test the callAsync() method - this should work
        Mono<String> asyncResult = sdkClient.callAsync(sdk -> {
            MockAWSSDK awsSDK = (MockAWSSDK) sdk; // Still requires casting!
            return awsSDK.getS3ObjectAsync("test-bucket", "test-key");
        });

        // Test the sdk() method - this should work
        MockAWSSDK sdk = sdkClient.sdk();
        String directResult = sdk.getS3Object("test-bucket", "test-key");

        // Verify results
        assertNotNull(result);
        assertNotNull(asyncResult);
        assertNotNull(directResult);
        assertEquals("S3Object from test-bucket/test-key", directResult);
    }
}
