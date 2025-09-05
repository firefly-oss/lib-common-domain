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

package com.firefly.common.domain.client.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.firefly.common.domain.client.config.RequestConfiguration;
import com.firefly.common.domain.client.config.ResponseConfiguration;
import com.firefly.common.domain.tracing.CorrelationContext;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Default implementation of SdkServiceClient providing SDK lifecycle management,
 * circuit breaker protection, and retry mechanisms.
 * 
 * @param <S> the SDK type
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public class DefaultSdkServiceClient<S> implements SdkServiceClient<S> {

    private final String serviceName;
    private final S sdkInstance;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final CorrelationContext correlationContext;
    private final Duration timeout;
    private final Map<String, Object> sdkConfiguration;
    private final boolean autoShutdown;
    private final String sdkVersion;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    public DefaultSdkServiceClient(String serviceName,
                                  S sdkInstance,
                                  CircuitBreaker circuitBreaker,
                                  Retry retry,
                                  CorrelationContext correlationContext,
                                  Duration timeout,
                                  Map<String, Object> sdkConfiguration,
                                  boolean autoShutdown,
                                  String sdkVersion) {
        this.serviceName = serviceName;
        this.sdkInstance = sdkInstance;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.correlationContext = correlationContext;
        this.timeout = timeout;
        this.sdkConfiguration = Collections.unmodifiableMap(sdkConfiguration);
        this.autoShutdown = autoShutdown;
        this.sdkVersion = sdkVersion;
        
        log.info("Initialized SdkServiceClient for service '{}' with SDK type '{}'", 
                serviceName, sdkInstance.getClass().getSimpleName());
    }

    @Override
    public <R> Mono<R> execute(Function<S, R> operation) {
        if (isShutdown.get()) {
            return Mono.error(new IllegalStateException("SDK client has been shut down"));
        }

        return Mono.fromCallable(() -> {
            // Apply correlation context if available
            if (correlationContext != null) {
                log.debug("Executing SDK operation for service '{}' with correlation ID: {}", 
                         serviceName, correlationContext.getCorrelationId());
            }
            
            // Execute the operation with the SDK
            return operation.apply(sdkInstance);
        })
        .timeout(timeout)
        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
        .transformDeferred(RetryOperator.of(retry))
        .doOnSuccess(result -> log.debug("SDK operation completed successfully for service '{}'", serviceName))
        .doOnError(error -> log.error("SDK operation failed for service '{}': {}", serviceName, error.getMessage()));
    }

    @Override
    public <R> Mono<R> executeAsync(Function<S, Mono<R>> operation) {
        if (isShutdown.get()) {
            return Mono.error(new IllegalStateException("SDK client has been shut down"));
        }

        return Mono.defer(() -> {
            // Apply correlation context if available
            if (correlationContext != null) {
                log.debug("Executing async SDK operation for service '{}' with correlation ID: {}", 
                         serviceName, correlationContext.getCorrelationId());
            }
            
            // Execute the async operation with the SDK
            return operation.apply(sdkInstance);
        })
        .timeout(timeout)
        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
        .transformDeferred(RetryOperator.of(retry))
        .doOnSuccess(result -> log.debug("Async SDK operation completed successfully for service '{}'", serviceName))
        .doOnError(error -> log.error("Async SDK operation failed for service '{}': {}", serviceName, error.getMessage()));
    }

    @Override
    public S getSdk() {
        if (isShutdown.get()) {
            throw new IllegalStateException("SDK client has been shut down");
        }
        return sdkInstance;
    }

    @Override
    public boolean isReady() {
        return !isShutdown.get() && sdkInstance != null;
    }

    @Override
    public String getSdkVersion() {
        return sdkVersion;
    }

    @Override
    public Map<String, Object> getSdkConfiguration() {
        return sdkConfiguration;
    }

    @Override
    public Mono<Void> healthCheck() {
        if (isShutdown.get()) {
            return Mono.error(new IllegalStateException("SDK client has been shut down"));
        }

        return Mono.<Void>fromCallable(() -> {
            // Basic health check - verify SDK instance is available
            if (sdkInstance == null) {
                throw new IllegalStateException("SDK instance is null");
            }
            
            // Additional SDK-specific health checks can be implemented here
            // For example, calling a health endpoint or ping method if available
            
            log.debug("Health check passed for SDK service '{}'", serviceName);
            return null;
        })
        .timeout(Duration.ofSeconds(5))
        .doOnError(error -> log.warn("Health check failed for SDK service '{}': {}", serviceName, error.getMessage()));
    }

    @Override
    public Mono<Void> shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            return Mono.fromRunnable(() -> {
                log.info("Shutting down SdkServiceClient for service '{}'", serviceName);
                
                if (autoShutdown && sdkInstance != null) {
                    try {
                        // Attempt to shutdown the SDK if it has a shutdown method
                        // This is SDK-specific and may need to be customized
                        shutdownSdkInstance();
                    } catch (Exception e) {
                        log.warn("Error during SDK shutdown for service '{}': {}", serviceName, e.getMessage());
                    }
                }
                
                log.info("SdkServiceClient shutdown completed for service '{}'", serviceName);
            });
        }
        
        return Mono.empty();
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String getBaseUrl() {
        throw new UnsupportedOperationException("SDK clients do not use base URLs. Use execute() or executeAsync() methods instead.");
    }

    // HTTP methods are not supported for SDK clients - they should use execute() methods
    @Override
    public <R> Mono<R> get(String endpoint, Class<R> responseType) {
        return Mono.error(new UnsupportedOperationException("HTTP GET not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> get(String endpoint, Map<String, Object> queryParams, Class<R> responseType) {
        return Mono.error(new UnsupportedOperationException("HTTP GET not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> post(String endpoint, Object request, Class<R> responseType) {
        return Mono.error(new UnsupportedOperationException("HTTP POST not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> put(String endpoint, Object request, Class<R> responseType) {
        return Mono.error(new UnsupportedOperationException("HTTP PUT not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> delete(String endpoint, Class<R> responseType) {
        return Mono.error(new UnsupportedOperationException("HTTP DELETE not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> patch(String endpoint, Object request, Class<R> responseType) {
        return Mono.error(new UnsupportedOperationException("HTTP PATCH not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> get(String endpoint, RequestConfiguration requestConfig, Class<R> responseType) {
        return Mono.error(new UnsupportedOperationException("HTTP GET not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> get(String endpoint, Map<String, Object> queryParams, RequestConfiguration requestConfig, Class<R> responseType) {
        return Mono.error(new UnsupportedOperationException("HTTP GET not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> post(String endpoint, Object request, RequestConfiguration requestConfig, Class<R> responseType) {
        return Mono.error(new UnsupportedOperationException("HTTP POST not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> put(String endpoint, Object request, RequestConfiguration requestConfig, Class<R> responseType) {
        return Mono.error(new UnsupportedOperationException("HTTP PUT not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> delete(String endpoint, RequestConfiguration requestConfig, Class<R> responseType) {
        return Mono.error(new UnsupportedOperationException("HTTP DELETE not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> patch(String endpoint, Object request, RequestConfiguration requestConfig, Class<R> responseType) {
        return Mono.error(new UnsupportedOperationException("HTTP PATCH not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> get(String endpoint, ResponseConfiguration<R> responseConfig) {
        return Mono.error(new UnsupportedOperationException("HTTP GET not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> get(String endpoint, Map<String, Object> queryParams, ResponseConfiguration<R> responseConfig) {
        return Mono.error(new UnsupportedOperationException("HTTP GET not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> post(String endpoint, Object request, ResponseConfiguration<R> responseConfig) {
        return Mono.error(new UnsupportedOperationException("HTTP POST not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> put(String endpoint, Object request, ResponseConfiguration<R> responseConfig) {
        return Mono.error(new UnsupportedOperationException("HTTP PUT not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> delete(String endpoint, ResponseConfiguration<R> responseConfig) {
        return Mono.error(new UnsupportedOperationException("HTTP DELETE not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> patch(String endpoint, Object request, ResponseConfiguration<R> responseConfig) {
        return Mono.error(new UnsupportedOperationException("HTTP PATCH not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> get(String endpoint, RequestConfiguration requestConfig, ResponseConfiguration<R> responseConfig) {
        return Mono.error(new UnsupportedOperationException("HTTP GET not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> get(String endpoint, Map<String, Object> queryParams, RequestConfiguration requestConfig, ResponseConfiguration<R> responseConfig) {
        return Mono.error(new UnsupportedOperationException("HTTP GET not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> post(String endpoint, Object request, RequestConfiguration requestConfig, ResponseConfiguration<R> responseConfig) {
        return Mono.error(new UnsupportedOperationException("HTTP POST not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> put(String endpoint, Object request, RequestConfiguration requestConfig, ResponseConfiguration<R> responseConfig) {
        return Mono.error(new UnsupportedOperationException("HTTP PUT not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> delete(String endpoint, RequestConfiguration requestConfig, ResponseConfiguration<R> responseConfig) {
        return Mono.error(new UnsupportedOperationException("HTTP DELETE not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> patch(String endpoint, Object request, RequestConfiguration requestConfig, ResponseConfiguration<R> responseConfig) {
        return Mono.error(new UnsupportedOperationException("HTTP PATCH not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> get(String endpoint, TypeReference<R> typeReference) {
        return Mono.error(new UnsupportedOperationException("HTTP GET not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> get(String endpoint, Map<String, Object> queryParams, TypeReference<R> typeReference) {
        return Mono.error(new UnsupportedOperationException("HTTP GET not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> post(String endpoint, Object request, TypeReference<R> typeReference) {
        return Mono.error(new UnsupportedOperationException("HTTP POST not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> put(String endpoint, Object request, TypeReference<R> typeReference) {
        return Mono.error(new UnsupportedOperationException("HTTP PUT not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> delete(String endpoint, TypeReference<R> typeReference) {
        return Mono.error(new UnsupportedOperationException("HTTP DELETE not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    @Override
    public <R> Mono<R> patch(String endpoint, Object request, TypeReference<R> typeReference) {
        return Mono.error(new UnsupportedOperationException("HTTP PATCH not supported for SDK clients. Use execute() or executeAsync() methods instead."));
    }

    private void shutdownSdkInstance() {
        // Try common shutdown methods using reflection
        try {
            var shutdownMethod = sdkInstance.getClass().getMethod("shutdown");
            shutdownMethod.invoke(sdkInstance);
            log.debug("Called shutdown() method on SDK instance for service '{}'", serviceName);
        } catch (NoSuchMethodException e) {
            // Try close method
            try {
                var closeMethod = sdkInstance.getClass().getMethod("close");
                closeMethod.invoke(sdkInstance);
                log.debug("Called close() method on SDK instance for service '{}'", serviceName);
            } catch (NoSuchMethodException ex) {
                log.debug("No shutdown or close method found on SDK instance for service '{}'", serviceName);
            } catch (Exception ex) {
                log.warn("Error calling close() method on SDK instance for service '{}': {}", serviceName, ex.getMessage());
            }
        } catch (Exception e) {
            log.warn("Error calling shutdown() method on SDK instance for service '{}': {}", serviceName, e.getMessage());
        }
    }
}
