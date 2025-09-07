package com.firefly.common.domain.client.test;

import reactor.core.publisher.Mono;

/**
 * Mock AWS SDK for testing purposes.
 */
public class MockAWSSDK {

    public String getS3Object(String bucket, String key) {
        return "S3Object from " + bucket + "/" + key;
    }

    public Mono<String> getS3ObjectAsync(String bucket, String key) {
        return Mono.just("Async S3Object from " + bucket + "/" + key);
    }

    public MockS3Service s3() {
        return new MockS3Service();
    }

    public static class MockS3Service {
        public String getObject(String request) {
            return "S3Object from request: " + request;
        }

        public Mono<String> getObjectAsync(String request) {
            return Mono.just("Async S3Object from request: " + request);
        }
    }
}
