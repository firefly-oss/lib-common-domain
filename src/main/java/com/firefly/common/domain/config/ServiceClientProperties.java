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

package com.firefly.common.domain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for ServiceClient framework.
 */
@ConfigurationProperties(prefix = "firefly.service-client")
@Data
public class ServiceClientProperties {

    /**
     * Whether ServiceClient framework is enabled.
     */
    private boolean enabled = true;

    /**
     * REST client configuration.
     */
    private Rest rest = new Rest();

    /**
     * gRPC client configuration.
     */
    private Grpc grpc = new Grpc();

    /**
     * Circuit breaker configuration.
     */
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    /**
     * Retry configuration.
     */
    private Retry retry = new Retry();

    @Data
    public static class Rest {
        /**
         * Maximum number of connections in the pool.
         */
        private int maxConnections = 100;

        /**
         * Maximum idle time for connections.
         */
        private Duration maxIdleTime = Duration.ofMinutes(5);

        /**
         * Maximum lifetime for connections.
         */
        private Duration maxLifeTime = Duration.ofMinutes(30);

        /**
         * Timeout for acquiring a connection from the pool.
         */
        private Duration pendingAcquireTimeout = Duration.ofSeconds(10);

        /**
         * Response timeout for HTTP requests.
         */
        private Duration responseTimeout = Duration.ofSeconds(30);

        /**
         * Maximum in-memory size for response bodies.
         */
        private int maxInMemorySize = 1024 * 1024; // 1MB

        /**
         * Default connect timeout.
         */
        private Duration connectTimeout = Duration.ofSeconds(10);

        /**
         * Default read timeout.
         */
        private Duration readTimeout = Duration.ofSeconds(30);
    }

    @Data
    public static class Grpc {
        /**
         * Keep alive time for gRPC connections.
         */
        private Duration keepAliveTime = Duration.ofMinutes(5);

        /**
         * Keep alive timeout for gRPC connections.
         */
        private Duration keepAliveTimeout = Duration.ofSeconds(30);

        /**
         * Whether to keep alive without calls.
         */
        private boolean keepAliveWithoutCalls = true;

        /**
         * Maximum inbound message size.
         */
        private int maxInboundMessageSize = 4 * 1024 * 1024; // 4MB

        /**
         * Maximum inbound metadata size.
         */
        private int maxInboundMetadataSize = 8 * 1024; // 8KB

        /**
         * Default deadline for gRPC calls.
         */
        private Duration callTimeout = Duration.ofSeconds(30);

        /**
         * Whether to enable gRPC retry.
         */
        private boolean retryEnabled = true;
    }

    @Data
    public static class CircuitBreaker {
        /**
         * Failure rate threshold (percentage).
         */
        private float failureRateThreshold = 50.0f;

        /**
         * Wait duration in open state.
         */
        private Duration waitDurationInOpenState = Duration.ofSeconds(60);

        /**
         * Sliding window size for failure rate calculation.
         */
        private int slidingWindowSize = 10;

        /**
         * Minimum number of calls before circuit breaker can calculate failure rate.
         */
        private int minimumNumberOfCalls = 5;

        /**
         * Slow call rate threshold (percentage).
         */
        private float slowCallRateThreshold = 100.0f;

        /**
         * Slow call duration threshold.
         */
        private Duration slowCallDurationThreshold = Duration.ofSeconds(60);

        /**
         * Number of permitted calls in half-open state.
         */
        private int permittedNumberOfCallsInHalfOpenState = 3;

        /**
         * Maximum wait duration in half-open state.
         */
        private Duration maxWaitDurationInHalfOpenState = Duration.ofSeconds(0);
    }

    @Data
    public static class Retry {
        /**
         * Maximum number of retry attempts.
         */
        private int maxAttempts = 3;

        /**
         * Wait duration between retries.
         */
        private Duration waitDuration = Duration.ofMillis(500);

        /**
         * Exponential backoff multiplier.
         */
        private double exponentialBackoffMultiplier = 2.0;

        /**
         * Maximum wait duration for exponential backoff.
         */
        private Duration maxWaitDuration = Duration.ofSeconds(10);

        /**
         * Whether to use jitter in wait duration.
         */
        private boolean jitterEnabled = true;
    }
}