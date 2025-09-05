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

package com.firefly.common.domain.client.sdk.builder;

import com.firefly.common.domain.client.builder.ServiceClientBuilder;
import com.firefly.common.domain.client.sdk.SdkServiceClient;
import com.firefly.common.domain.client.sdk.DefaultSdkServiceClient;
import com.firefly.common.domain.tracing.CorrelationContext;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Builder for creating SDK-based ServiceClient instances with fluent API.
 * 
 * <p>Provides comprehensive configuration options for SDK service communication
 * including SDK lifecycle management, circuit breakers, retry policies, and
 * SDK-specific configuration.
 * 
 * @param <S> the SDK type
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public class SdkServiceClientBuilder<S> implements ServiceClientBuilder<SdkServiceClient<S>, SdkServiceClientBuilder<S>> {

    private String serviceName;
    private Supplier<S> sdkFactory;
    private S sdkInstance;
    private CircuitBreaker circuitBreaker;
    private Retry retry;
    private CorrelationContext correlationContext;
    private Duration timeout = Duration.ofSeconds(30);
    private Map<String, Object> sdkConfiguration = new HashMap<>();
    private boolean autoShutdown = true;
    private String sdkVersion;

    /**
     * Sets the service name for this SDK client.
     * Used for metrics, logging, and circuit breaker identification.
     * 
     * @param serviceName the service name
     * @return this builder
     */
    @Override
    public SdkServiceClientBuilder<S> serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    /**
     * Sets the SDK factory for creating SDK instances.
     * 
     * <p>The factory will be called to create the SDK instance when the client is built.
     * This allows for lazy initialization and proper dependency injection.
     * 
     * @param sdkFactory the SDK factory
     * @return this builder
     */
    public SdkServiceClientBuilder<S> sdkFactory(Supplier<S> sdkFactory) {
        this.sdkFactory = sdkFactory;
        return this;
    }

    /**
     * Sets a pre-created SDK instance.
     * 
     * <p>Use this when you have an existing SDK instance that should be managed
     * by the ServiceClient. The instance will be used directly without calling
     * the SDK factory.
     * 
     * @param sdkInstance the SDK instance
     * @return this builder
     */
    public SdkServiceClientBuilder<S> sdkInstance(S sdkInstance) {
        this.sdkInstance = sdkInstance;
        return this;
    }

    /**
     * Sets the circuit breaker for this client.
     * 
     * @param circuitBreaker the circuit breaker
     * @return this builder
     */
    @Override
    public SdkServiceClientBuilder<S> circuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
        return this;
    }

    /**
     * Sets the retry mechanism for this client.
     * 
     * @param retry the retry mechanism
     * @return this builder
     */
    @Override
    public SdkServiceClientBuilder<S> retry(Retry retry) {
        this.retry = retry;
        return this;
    }

    /**
     * Sets the correlation context for distributed tracing.
     * 
     * @param correlationContext the correlation context
     * @return this builder
     */
    @Override
    public SdkServiceClientBuilder<S> correlationContext(CorrelationContext correlationContext) {
        this.correlationContext = correlationContext;
        return this;
    }

    /**
     * Sets the timeout for SDK operations.
     * 
     * @param timeout the timeout duration
     * @return this builder
     */
    @Override
    public SdkServiceClientBuilder<S> timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Adds SDK-specific configuration.
     * 
     * @param key the configuration key
     * @param value the configuration value
     * @return this builder
     */
    public SdkServiceClientBuilder<S> sdkConfiguration(String key, Object value) {
        this.sdkConfiguration.put(key, value);
        return this;
    }

    /**
     * Sets multiple SDK configuration properties.
     * 
     * @param configuration the configuration map
     * @return this builder
     */
    public SdkServiceClientBuilder<S> sdkConfiguration(Map<String, Object> configuration) {
        this.sdkConfiguration.putAll(configuration);
        return this;
    }

    /**
     * Sets whether the SDK should be automatically shut down when the client is closed.
     * 
     * @param autoShutdown true to enable auto-shutdown, false otherwise
     * @return this builder
     */
    public SdkServiceClientBuilder<S> autoShutdown(boolean autoShutdown) {
        this.autoShutdown = autoShutdown;
        return this;
    }

    /**
     * Sets the SDK version information.
     * 
     * @param sdkVersion the SDK version
     * @return this builder
     */
    public SdkServiceClientBuilder<S> sdkVersion(String sdkVersion) {
        this.sdkVersion = sdkVersion;
        return this;
    }

    /**
     * Builds the SdkServiceClient instance.
     * 
     * @return the configured SdkServiceClient
     * @throws IllegalStateException if required configuration is missing
     */
    @Override
    public SdkServiceClient<S> build() {
        validateRequiredFields();
        
        S finalSdkInstance = sdkInstance != null ? sdkInstance : createSdkInstance();
        CircuitBreaker finalCircuitBreaker = circuitBreaker != null ? circuitBreaker : createDefaultCircuitBreaker();
        Retry finalRetry = retry != null ? retry : createDefaultRetry();
        
        log.info("Building SdkServiceClient for service '{}' with SDK type '{}'", 
                serviceName, finalSdkInstance.getClass().getSimpleName());
        
        return new DefaultSdkServiceClient<>(
            serviceName,
            finalSdkInstance,
            finalCircuitBreaker,
            finalRetry,
            correlationContext,
            timeout,
            sdkConfiguration,
            autoShutdown,
            sdkVersion
        );
    }

    private void validateRequiredFields() {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalStateException("Service name is required");
        }
        if (sdkInstance == null && sdkFactory == null) {
            throw new IllegalStateException("Either SDK instance or SDK factory must be provided");
        }
    }

    private S createSdkInstance() {
        if (sdkFactory == null) {
            throw new IllegalStateException("SDK factory is required when SDK instance is not provided");
        }
        
        try {
            S instance = sdkFactory.get();
            if (instance == null) {
                throw new IllegalStateException("SDK factory returned null instance");
            }
            return instance;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create SDK instance", e);
        }
    }

    private CircuitBreaker createDefaultCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .build();
        
        return CircuitBreaker.of(serviceName + "-sdk-circuit-breaker", config);
    }

    private Retry createDefaultRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .build();
        
        return Retry.of(serviceName + "-sdk-retry", config);
    }

    /**
     * Creates a new SdkServiceClientBuilder instance.
     * 
     * @param <S> the SDK type
     * @return a new builder instance
     */
    public static <S> SdkServiceClientBuilder<S> create() {
        return new SdkServiceClientBuilder<>();
    }
}
