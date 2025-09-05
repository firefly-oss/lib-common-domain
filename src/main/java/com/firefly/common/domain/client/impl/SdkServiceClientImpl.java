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

package com.firefly.common.domain.client.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.firefly.common.domain.client.ClientType;
import com.firefly.common.domain.client.RequestBuilder;
import com.firefly.common.domain.client.ServiceClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * SDK implementation of ServiceClient.
 * 
 * <p>This implementation provides a unified interface for SDK-based service communication
 * while wrapping third-party SDKs and maintaining their specific optimizations and features.
 *
 * @param <S> the SDK type
 * @author Firefly Software Solutions Inc
 * @since 2.0.0
 */
@Slf4j
public class SdkServiceClientImpl<S> implements ServiceClient {

    private final String serviceName;
    private final Class<S> sdkType;
    private final S sdkInstance;
    private final Duration timeout;
    private final boolean autoShutdown;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    /**
     * Creates a new SDK service client implementation.
     */
    public SdkServiceClientImpl(String serviceName,
                               Class<S> sdkType,
                               S sdkInstance,
                               Duration timeout,
                               boolean autoShutdown,
                               CircuitBreaker circuitBreaker,
                               Retry retry) {
        this.serviceName = serviceName;
        this.sdkType = sdkType;
        this.sdkInstance = sdkInstance;
        this.timeout = timeout;
        this.autoShutdown = autoShutdown;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        
        log.info("Initialized SDK service client for service '{}' with SDK type '{}'", 
                serviceName, sdkType.getSimpleName());
    }

    // ========================================
    // Request Builder Methods (Not Supported for SDK)
    // ========================================

    @Override
    public <R> RequestBuilder<R> get(String endpoint, Class<R> responseType) {
        return new UnsupportedRequestBuilder<>("SDK clients do not support HTTP operations. Use execute() methods with SDK instances instead.");
    }

    @Override
    public <R> RequestBuilder<R> get(String endpoint, TypeReference<R> typeReference) {
        return new UnsupportedRequestBuilder<>("SDK clients do not support HTTP operations. Use execute() methods with SDK instances instead.");
    }

    @Override
    public <R> RequestBuilder<R> post(String endpoint, Class<R> responseType) {
        return new UnsupportedRequestBuilder<>("SDK clients do not support HTTP operations. Use execute() methods with SDK instances instead.");
    }

    @Override
    public <R> RequestBuilder<R> post(String endpoint, TypeReference<R> typeReference) {
        return new UnsupportedRequestBuilder<>("SDK clients do not support HTTP operations. Use execute() methods with SDK instances instead.");
    }

    @Override
    public <R> RequestBuilder<R> put(String endpoint, Class<R> responseType) {
        return new UnsupportedRequestBuilder<>("SDK clients do not support HTTP operations. Use execute() methods with SDK instances instead.");
    }

    @Override
    public <R> RequestBuilder<R> put(String endpoint, TypeReference<R> typeReference) {
        return new UnsupportedRequestBuilder<>("SDK clients do not support HTTP operations. Use execute() methods with SDK instances instead.");
    }

    @Override
    public <R> RequestBuilder<R> delete(String endpoint, Class<R> responseType) {
        return new UnsupportedRequestBuilder<>("SDK clients do not support HTTP operations. Use execute() methods with SDK instances instead.");
    }

    @Override
    public <R> RequestBuilder<R> delete(String endpoint, TypeReference<R> typeReference) {
        return new UnsupportedRequestBuilder<>("SDK clients do not support HTTP operations. Use execute() methods with SDK instances instead.");
    }

    @Override
    public <R> RequestBuilder<R> patch(String endpoint, Class<R> responseType) {
        return new UnsupportedRequestBuilder<>("SDK clients do not support HTTP operations. Use execute() methods with SDK instances instead.");
    }

    @Override
    public <R> RequestBuilder<R> patch(String endpoint, TypeReference<R> typeReference) {
        return new UnsupportedRequestBuilder<>("SDK clients do not support HTTP operations. Use execute() methods with SDK instances instead.");
    }

    // ========================================
    // SDK-specific Methods - Simplified API
    // ========================================

    /**
     * Execute a synchronous operation with the SDK instance.
     *
     * <p>Example usage:
     * <pre>{@code
     * // Simple and type-safe - no casting required!
     * Mono<PaymentResult> result = client.call(sdk -> sdk.processPayment(request));
     * }</pre>
     *
     * @param operation the operation to execute with the SDK
     * @param <R> the return type
     * @return a Mono containing the result
     */
    @Override
    @SuppressWarnings("unchecked")
    public <S1, R> Mono<R> call(Function<S1, R> operation) {
        if (isShutdown.get()) {
            return Mono.error(new IllegalStateException("Client has been shut down"));
        }

        return Mono.fromCallable(() -> operation.apply((S1) sdkInstance))
            .timeout(timeout)
            .doOnSubscribe(subscription ->
                log.debug("Executing SDK operation for service '{}'", serviceName))
            .doOnSuccess(result ->
                log.debug("Successfully completed SDK operation for service '{}'", serviceName))
            .doOnError(error ->
                log.error("Failed SDK operation for service '{}': {}", serviceName, error.getMessage()));
    }

    /**
     * Execute an asynchronous operation with the SDK instance.
     *
     * <p>Example usage:
     * <pre>{@code
     * // For SDKs that return Mono/Future - no casting required!
     * Mono<PaymentResult> result = client.callAsync(sdk -> sdk.processPaymentAsync(request));
     * }</pre>
     *
     * @param operation the async operation to execute with the SDK
     * @param <R> the return type
     * @return a Mono containing the result
     */
    @Override
    @SuppressWarnings("unchecked")
    public <S1, R> Mono<R> callAsync(Function<S1, Mono<R>> operation) {
        if (isShutdown.get()) {
            return Mono.error(new IllegalStateException("Client has been shut down"));
        }

        return operation.apply((S1) sdkInstance)
            .timeout(timeout)
            .doOnSubscribe(subscription ->
                log.debug("Executing async SDK operation for service '{}'", serviceName))
            .doOnSuccess(result ->
                log.debug("Successfully completed async SDK operation for service '{}'", serviceName))
            .doOnError(error ->
                log.error("Failed async SDK operation for service '{}': {}", serviceName, error.getMessage()));
    }

    /**
     * Get direct access to the SDK instance for complex operations.
     *
     * <p>Example usage:
     * <pre>{@code
     * // Direct access when you need full control
     * PaymentSDK sdk = client.sdk();
     * PaymentResult result = sdk.processPayment(request);
     * }</pre>
     *
     * @return the SDK instance
     */
    @Override
    @SuppressWarnings("unchecked")
    public <S1> S1 sdk() {
        if (isShutdown.get()) {
            throw new IllegalStateException("Client has been shut down");
        }
        return (S1) sdkInstance;
    }

    // ========================================
    // Legacy Methods (Deprecated but maintained for compatibility)
    // ========================================

    @Override
    @Deprecated
    @SuppressWarnings("unchecked")
    public <R> Mono<R> execute(Function<Object, R> operation) {
        log.warn("execute(Function<Object, R>) is deprecated. Use call(Function<S, R>) for better type safety.");
        return call(sdk -> operation.apply(sdk));
    }

    @Override
    @Deprecated
    @SuppressWarnings("unchecked")
    public <R> Mono<R> executeAsync(Function<Object, Mono<R>> operation) {
        log.warn("executeAsync(Function<Object, Mono<R>>) is deprecated. Use callAsync(Function<S, Mono<R>>) for better type safety.");
        return callAsync(sdk -> operation.apply(sdk));
    }

    // ========================================
    // Streaming Methods (Not Supported for SDK)
    // ========================================

    @Override
    public <R> Flux<R> stream(String endpoint, Class<R> responseType) {
        return Flux.error(new UnsupportedOperationException(
            "SDK clients do not support streaming operations. Use execute() or executeAsync() methods."));
    }

    @Override
    public <R> Flux<R> stream(String endpoint, TypeReference<R> typeReference) {
        return Flux.error(new UnsupportedOperationException(
            "SDK clients do not support streaming operations. Use execute() or executeAsync() methods."));
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
        return null; // SDK clients don't have base URLs
    }

    @Override
    public boolean isReady() {
        return !isShutdown.get() && sdkInstance != null;
    }

    @Override
    public Mono<Void> healthCheck() {
        if (isShutdown.get()) {
            return Mono.error(new IllegalStateException("Client has been shut down"));
        }
        
        // For SDK clients, we just check if the instance is available
        return Mono.<Void>fromCallable(() -> {
            if (sdkInstance == null) {
                throw new RuntimeException("SDK instance is not available");
            }
            return null;
        })
        .timeout(Duration.ofSeconds(5))
        .onErrorMap(throwable -> new RuntimeException(
                "Health check failed for SDK service: " + serviceName, throwable
                )
        ).then();
    }

    @Override
    public ClientType getClientType() {
        return ClientType.SDK;
    }

    @Override
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            log.info("Shutting down SDK service client for service '{}'", serviceName);
            
            if (autoShutdown && sdkInstance != null) {
                // Try to shutdown the SDK if it has a shutdown method
                try {
                    java.lang.reflect.Method shutdownMethod = sdkInstance.getClass().getMethod("shutdown");
                    shutdownMethod.invoke(sdkInstance);
                    log.debug("Successfully shut down SDK instance for service '{}'", serviceName);
                } catch (Exception e) {
                    log.debug("SDK instance for service '{}' does not have a shutdown method or shutdown failed: {}", 
                             serviceName, e.getMessage());
                }
            }
        }
    }

    // getSdk() method replaced by sdk() method above for better naming

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
