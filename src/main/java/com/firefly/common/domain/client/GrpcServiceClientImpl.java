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

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * gRPC implementation of ServiceClient.
 * 
 * <p>This implementation provides a unified interface for gRPC service communication
 * while maintaining protocol-specific optimizations for Protocol Buffer serialization
 * and streaming operations.
 *
 * @param <T> the gRPC stub type
 * @author Firefly Software Solutions Inc
 * @since 2.0.0
 */
@Slf4j
public class GrpcServiceClientImpl<T> implements ServiceClient {

    private final String serviceName;
    private final Class<T> stubType;
    private final String address;
    private final Duration timeout;
    private final ManagedChannel channel;
    private final T stub;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    /**
     * Creates a new gRPC service client implementation.
     */
    public GrpcServiceClientImpl(String serviceName,
                                Class<T> stubType,
                                String address,
                                Duration timeout,
                                ManagedChannel channel,
                                T stub,
                                CircuitBreaker circuitBreaker,
                                Retry retry) {
        this.serviceName = serviceName;
        this.stubType = stubType;
        this.address = address;
        this.timeout = timeout;
        this.channel = channel;
        this.stub = stub;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        
        log.info("Initialized gRPC service client for service '{}' with address '{}'", 
                serviceName, address);
    }

    // ========================================
    // Request Builder Methods (Not Supported for gRPC)
    // ========================================

    @Override
    public <R> RequestBuilder<R> get(String endpoint, Class<R> responseType) {
        return new UnsupportedRequestBuilder<>("gRPC clients do not support HTTP GET operations. Use execute() methods with gRPC stubs instead.");
    }

    @Override
    public <R> RequestBuilder<R> get(String endpoint, TypeReference<R> typeReference) {
        return new UnsupportedRequestBuilder<>("gRPC clients do not support HTTP GET operations. Use execute() methods with gRPC stubs instead.");
    }

    @Override
    public <R> RequestBuilder<R> post(String endpoint, Class<R> responseType) {
        return new UnsupportedRequestBuilder<>("gRPC clients do not support HTTP POST operations. Use execute() methods with gRPC stubs instead.");
    }

    @Override
    public <R> RequestBuilder<R> post(String endpoint, TypeReference<R> typeReference) {
        return new UnsupportedRequestBuilder<>("gRPC clients do not support HTTP POST operations. Use execute() methods with gRPC stubs instead.");
    }

    @Override
    public <R> RequestBuilder<R> put(String endpoint, Class<R> responseType) {
        return new UnsupportedRequestBuilder<>("gRPC clients do not support HTTP PUT operations. Use execute() methods with gRPC stubs instead.");
    }

    @Override
    public <R> RequestBuilder<R> put(String endpoint, TypeReference<R> typeReference) {
        return new UnsupportedRequestBuilder<>("gRPC clients do not support HTTP PUT operations. Use execute() methods with gRPC stubs instead.");
    }

    @Override
    public <R> RequestBuilder<R> delete(String endpoint, Class<R> responseType) {
        return new UnsupportedRequestBuilder<>("gRPC clients do not support HTTP DELETE operations. Use execute() methods with gRPC stubs instead.");
    }

    @Override
    public <R> RequestBuilder<R> delete(String endpoint, TypeReference<R> typeReference) {
        return new UnsupportedRequestBuilder<>("gRPC clients do not support HTTP DELETE operations. Use execute() methods with gRPC stubs instead.");
    }

    @Override
    public <R> RequestBuilder<R> patch(String endpoint, Class<R> responseType) {
        return new UnsupportedRequestBuilder<>("gRPC clients do not support HTTP PATCH operations. Use execute() methods with gRPC stubs instead.");
    }

    @Override
    public <R> RequestBuilder<R> patch(String endpoint, TypeReference<R> typeReference) {
        return new UnsupportedRequestBuilder<>("gRPC clients do not support HTTP PATCH operations. Use execute() methods with gRPC stubs instead.");
    }

    // ========================================
    // gRPC-specific Methods
    // ========================================

    @Override
    @SuppressWarnings("unchecked")
    public <R> Mono<R> execute(Function<Object, R> operation) {
        if (isShutdown.get()) {
            return Mono.error(new IllegalStateException("Client has been shut down"));
        }

        return Mono.fromCallable(() -> operation.apply(stub))
            .timeout(timeout)
            .doOnSubscribe(subscription -> 
                log.debug("Executing gRPC operation for service '{}'", serviceName))
            .doOnSuccess(result -> 
                log.debug("Successfully completed gRPC operation for service '{}'", serviceName))
            .doOnError(error -> 
                log.error("Failed gRPC operation for service '{}': {}", serviceName, error.getMessage()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Mono<R> executeAsync(Function<Object, Mono<R>> operation) {
        if (isShutdown.get()) {
            return Mono.error(new IllegalStateException("Client has been shut down"));
        }

        return operation.apply(stub)
            .timeout(timeout)
            .doOnSubscribe(subscription -> 
                log.debug("Executing async gRPC operation for service '{}'", serviceName))
            .doOnSuccess(result -> 
                log.debug("Successfully completed async gRPC operation for service '{}'", serviceName))
            .doOnError(error -> 
                log.error("Failed async gRPC operation for service '{}': {}", serviceName, error.getMessage()));
    }

    // ========================================
    // Streaming Methods
    // ========================================

    @Override
    public <R> Flux<R> stream(String endpoint, Class<R> responseType) {
        return Flux.error(new UnsupportedOperationException(
            "gRPC streaming should be handled through the stub directly. Use execute() or executeAsync() methods."));
    }

    @Override
    public <R> Flux<R> stream(String endpoint, TypeReference<R> typeReference) {
        return Flux.error(new UnsupportedOperationException(
            "gRPC streaming should be handled through the stub directly. Use execute() or executeAsync() methods."));
    }

    // ========================================
    // Client Metadata and Lifecycle
    // ========================================

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String getBaseUrl() {
        return address; // For gRPC, we return the address instead of base URL
    }

    @Override
    public boolean isReady() {
        return !isShutdown.get() && !channel.isShutdown();
    }

    @Override
    public Mono<Void> healthCheck() {
        if (isShutdown.get()) {
            return Mono.error(new IllegalStateException("Client has been shut down"));
        }
        
        // For gRPC, we check if the channel is ready
        return Mono.fromCallable(() -> {
            if (channel.isShutdown() || channel.isTerminated()) {
                throw new RuntimeException("gRPC channel is not available");
            }
            return null;
        })
        .timeout(Duration.ofSeconds(5))
        .onErrorMap(throwable -> new RuntimeException("Health check failed for gRPC service: " + serviceName, throwable));
    }

    @Override
    public ClientType getClientType() {
        return ClientType.GRPC;
    }

    @Override
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            log.info("Shutting down gRPC service client for service '{}'", serviceName);
            if (!channel.isShutdown()) {
                channel.shutdown();
            }
        }
    }

    /**
     * Gets the gRPC stub for direct access.
     *
     * @return the gRPC stub
     */
    public T getStub() {
        if (isShutdown.get()) {
            throw new IllegalStateException("Client has been shut down");
        }
        return stub;
    }

    // ========================================
    // Helper Classes
    // ========================================

    private static class UnsupportedRequestBuilder<R> implements RequestBuilder<R> {
        private final String errorMessage;

        public UnsupportedRequestBuilder(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public RequestBuilder<R> withBody(Object body) { return this; }

        @Override
        public RequestBuilder<R> withPathParam(String name, Object value) { return this; }

        @Override
        public RequestBuilder<R> withPathParams(Map<String, Object> pathParams) { return this; }

        @Override
        public RequestBuilder<R> withQueryParam(String name, Object value) { return this; }

        @Override
        public RequestBuilder<R> withQueryParams(Map<String, Object> queryParams) { return this; }

        @Override
        public RequestBuilder<R> withHeader(String name, String value) { return this; }

        @Override
        public RequestBuilder<R> withHeaders(Map<String, String> headers) { return this; }

        @Override
        public RequestBuilder<R> withTimeout(Duration timeout) { return this; }

        @Override
        public Mono<R> execute() {
            return Mono.error(new UnsupportedOperationException(errorMessage));
        }

        @Override
        public Flux<R> stream() {
            return Flux.error(new UnsupportedOperationException(errorMessage));
        }
    }
}
