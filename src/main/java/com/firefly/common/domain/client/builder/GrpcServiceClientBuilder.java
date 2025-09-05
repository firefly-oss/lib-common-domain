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
package com.firefly.common.domain.client.builder;

import com.firefly.common.domain.client.grpc.GrpcServiceClient;
import com.firefly.common.domain.tracing.CorrelationContext;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractAsyncStub;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Builder for creating gRPC ServiceClient instances with fluent API.
 * Provides comprehensive configuration options for gRPC service communication
 * including channel management, timeouts, circuit breakers, and retry policies.
 * 
 * @param <T> the type of gRPC stub
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public class GrpcServiceClientBuilder<T extends AbstractAsyncStub<T>> 
        implements ServiceClientBuilder<GrpcServiceClient<T>, GrpcServiceClientBuilder<T>> {

    private String serviceName;
    private String address;
    private T stub;
    private ManagedChannel channel;
    private CircuitBreaker circuitBreaker;
    private Retry retry;
    private CorrelationContext correlationContext;
    private Duration timeout = Duration.ofSeconds(30);
    
    // gRPC-specific settings
    private Duration keepAliveTime = Duration.ofMinutes(5);
    private Duration keepAliveTimeout = Duration.ofSeconds(30);
    private boolean keepAliveWithoutCalls = true;
    private int maxInboundMessageSize = 4 * 1024 * 1024; // 4MB
    private int maxInboundMetadataSize = 8 * 1024; // 8KB
    private boolean usePlaintext = false;
    private Function<ManagedChannelBuilder<?>, T> stubFactory;

    /**
     * Creates a new GrpcServiceClientBuilder instance.
     *
     * @param <T> the type of gRPC stub
     * @return a new builder instance
     */
    public static <T extends AbstractAsyncStub<T>> GrpcServiceClientBuilder<T> create() {
        return new GrpcServiceClientBuilder<>();
    }

    @Override
    public GrpcServiceClientBuilder<T> serviceName(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }
        this.serviceName = serviceName.trim();
        return this;
    }

    @Override
    public GrpcServiceClientBuilder<T> baseUrl(String address) {
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Address cannot be null or empty");
        }
        this.address = address.trim();
        return this;
    }

    /**
     * Sets the gRPC address (alias for baseUrl for consistency).
     *
     * @param address the gRPC server address
     * @return this builder instance for method chaining
     */
    public GrpcServiceClientBuilder<T> address(String address) {
        return baseUrl(address);
    }

    /**
     * Sets a pre-configured gRPC stub instance.
     *
     * @param stub the gRPC stub instance
     * @return this builder instance for method chaining
     */
    public GrpcServiceClientBuilder<T> stub(T stub) {
        this.stub = stub;
        return this;
    }

    /**
     * Sets a pre-configured ManagedChannel instance.
     *
     * @param channel the ManagedChannel instance
     * @return this builder instance for method chaining
     */
    public GrpcServiceClientBuilder<T> channel(ManagedChannel channel) {
        this.channel = channel;
        return this;
    }

    /**
     * Sets a factory function to create the stub from a ManagedChannelBuilder.
     * This is useful when you need to create the stub from the channel.
     *
     * @param stubFactory function that creates the stub from a channel builder
     * @return this builder instance for method chaining
     */
    public GrpcServiceClientBuilder<T> stubFactory(Function<ManagedChannelBuilder<?>, T> stubFactory) {
        this.stubFactory = stubFactory;
        return this;
    }

    @Override
    public GrpcServiceClientBuilder<T> circuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
        return this;
    }

    @Override
    public GrpcServiceClientBuilder<T> retry(Retry retry) {
        this.retry = retry;
        return this;
    }

    @Override
    public GrpcServiceClientBuilder<T> correlationContext(CorrelationContext correlationContext) {
        this.correlationContext = correlationContext;
        return this;
    }

    @Override
    public GrpcServiceClientBuilder<T> timeout(Duration timeout) {
        if (timeout == null || timeout.isNegative()) {
            throw new IllegalArgumentException("Timeout cannot be null or negative");
        }
        this.timeout = timeout;
        return this;
    }

    /**
     * Sets the keep alive time for gRPC connections.
     *
     * @param keepAliveTime the keep alive time
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if keepAliveTime is null or negative
     */
    public GrpcServiceClientBuilder<T> keepAliveTime(Duration keepAliveTime) {
        if (keepAliveTime == null || keepAliveTime.isNegative()) {
            throw new IllegalArgumentException("Keep alive time cannot be null or negative");
        }
        this.keepAliveTime = keepAliveTime;
        return this;
    }

    /**
     * Sets the keep alive timeout for gRPC connections.
     *
     * @param keepAliveTimeout the keep alive timeout
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if keepAliveTimeout is null or negative
     */
    public GrpcServiceClientBuilder<T> keepAliveTimeout(Duration keepAliveTimeout) {
        if (keepAliveTimeout == null || keepAliveTimeout.isNegative()) {
            throw new IllegalArgumentException("Keep alive timeout cannot be null or negative");
        }
        this.keepAliveTimeout = keepAliveTimeout;
        return this;
    }

    /**
     * Sets whether to keep alive without calls.
     *
     * @param keepAliveWithoutCalls whether to keep alive without calls
     * @return this builder instance for method chaining
     */
    public GrpcServiceClientBuilder<T> keepAliveWithoutCalls(boolean keepAliveWithoutCalls) {
        this.keepAliveWithoutCalls = keepAliveWithoutCalls;
        return this;
    }

    /**
     * Sets the maximum inbound message size.
     *
     * @param maxInboundMessageSize the maximum inbound message size in bytes
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if maxInboundMessageSize is less than 1
     */
    public GrpcServiceClientBuilder<T> maxInboundMessageSize(int maxInboundMessageSize) {
        if (maxInboundMessageSize < 1) {
            throw new IllegalArgumentException("Max inbound message size must be at least 1 byte");
        }
        this.maxInboundMessageSize = maxInboundMessageSize;
        return this;
    }

    /**
     * Sets the maximum inbound metadata size.
     *
     * @param maxInboundMetadataSize the maximum inbound metadata size in bytes
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if maxInboundMetadataSize is less than 1
     */
    public GrpcServiceClientBuilder<T> maxInboundMetadataSize(int maxInboundMetadataSize) {
        if (maxInboundMetadataSize < 1) {
            throw new IllegalArgumentException("Max inbound metadata size must be at least 1 byte");
        }
        this.maxInboundMetadataSize = maxInboundMetadataSize;
        return this;
    }

    /**
     * Sets whether to use plaintext communication (no TLS).
     *
     * @param usePlaintext whether to use plaintext
     * @return this builder instance for method chaining
     */
    public GrpcServiceClientBuilder<T> usePlaintext(boolean usePlaintext) {
        this.usePlaintext = usePlaintext;
        return this;
    }

    /**
     * Enables plaintext communication (no TLS).
     *
     * @return this builder instance for method chaining
     */
    public GrpcServiceClientBuilder<T> usePlaintext() {
        return usePlaintext(true);
    }

    @Override
    public GrpcServiceClient<T> build() {
        validateRequiredFields();
        
        ManagedChannel finalChannel = channel != null ? channel : createDefaultChannel();
        T finalStub = stub != null ? stub : createStubFromChannel(finalChannel);
        CircuitBreaker finalCircuitBreaker = circuitBreaker != null ? circuitBreaker : createDefaultCircuitBreaker();
        Retry finalRetry = retry != null ? retry : createDefaultRetry();
        
        log.info("Building GrpcServiceClient for service '{}' with address '{}'", serviceName, address);
        
        return new GrpcServiceClient<>(
            finalStub,
            serviceName,
            address,
            finalChannel,
            finalCircuitBreaker,
            finalRetry,
            correlationContext
        );
    }

    private void validateRequiredFields() {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalStateException("Service name is required");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalStateException("Address is required");
        }
        if (stub == null && stubFactory == null && channel == null) {
            throw new IllegalStateException("Either stub, stubFactory, or channel must be provided");
        }
    }

    private ManagedChannel createDefaultChannel() {
        log.debug("Creating default ManagedChannel for service '{}'", serviceName);
        
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forTarget(address)
                .keepAliveTime(keepAliveTime.toSeconds(), TimeUnit.SECONDS)
                .keepAliveTimeout(keepAliveTimeout.toSeconds(), TimeUnit.SECONDS)
                .keepAliveWithoutCalls(keepAliveWithoutCalls)
                .maxInboundMessageSize(maxInboundMessageSize)
                .maxInboundMetadataSize(maxInboundMetadataSize);

        if (usePlaintext) {
            channelBuilder.usePlaintext();
        }

        return channelBuilder.build();
    }

    @SuppressWarnings("unchecked")
    private T createStubFromChannel(ManagedChannel channel) {
        if (stubFactory != null) {
            log.debug("Creating stub using provided factory for service '{}'", serviceName);
            ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forTarget(address);
            return stubFactory.apply(builder);
        }
        
        throw new IllegalStateException("Cannot create stub without stubFactory. Please provide either a stub instance or a stubFactory function.");
    }

    private CircuitBreaker createDefaultCircuitBreaker() {
        log.debug("Creating default CircuitBreaker for service '{}'", serviceName);
        
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        return CircuitBreakerRegistry.of(config).circuitBreaker(serviceName);
    }

    private Retry createDefaultRetry() {
        log.debug("Creating default Retry for service '{}'", serviceName);
        
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryOnResult(result -> false)
                .retryExceptions(
                    java.net.ConnectException.class,
                    java.net.SocketTimeoutException.class,
                    java.util.concurrent.TimeoutException.class
                )
                .build();

        return RetryRegistry.of(config).retry(serviceName);
    }
}
