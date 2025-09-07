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

import com.firefly.common.domain.client.ServiceClient;
import com.firefly.common.domain.client.impl.SdkServiceClientImpl;
import com.firefly.common.domain.client.interceptor.ServiceClientInterceptor;
import com.firefly.common.domain.client.interceptor.LoggingInterceptor;
import com.firefly.common.domain.client.interceptor.MetricsInterceptor;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Simplified builder for SDK service clients.
 * 
 * <p>This builder provides a fluent API for creating SDK service clients that wrap
 * third-party SDKs with the unified ServiceClient interface.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Simple SDK client
 * ServiceClient client = ServiceClient.sdk("payment-service", PaymentSDK.class)
 *     .sdkFactory(() -> new PaymentSDK(apiKey, environment))
 *     .build();
 *
 * // SDK client with custom configuration
 * ServiceClient client = ServiceClient.sdk("aws-service", AWSSDK.class)
 *     .sdkFactory(() -> AWSSDK.builder()
 *         .region(Region.US_EAST_1)
 *         .credentialsProvider(credentialsProvider)
 *         .build())
 *     .timeout(Duration.ofSeconds(45))
 *     .autoShutdown(true)
 *     .build();
 * }</pre>
 *
 * @param <S> the SDK type
 * @author Firefly Software Solutions Inc
 * @since 2.0.0
 */
@Slf4j
public class SdkClientBuilder<S> implements ServiceClient.SdkClientBuilder<S> {

    private final String serviceName;
    private final Class<S> sdkType;
    private Function<Void, S> sdkFactory;
    private Duration timeout = Duration.ofSeconds(30);
    private boolean autoShutdown = true;
    private CircuitBreaker circuitBreaker;
    private Retry retry;
    private final List<ServiceClientInterceptor> interceptors = new ArrayList<>();

    /**
     * Creates a new SDK client builder.
     *
     * @param serviceName the service name
     * @param sdkType the SDK type
     */
    public SdkClientBuilder(String serviceName, Class<S> sdkType) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }
        if (sdkType == null) {
            throw new IllegalArgumentException("SDK type cannot be null");
        }
        
        this.serviceName = serviceName.trim();
        this.sdkType = sdkType;
        
        log.debug("Created SDK client builder for service '{}' with SDK type '{}'", 
                this.serviceName, sdkType.getSimpleName());
    }

    @Override
    public SdkClientBuilder<S> sdkFactory(Function<Void, S> sdkFactory) {
        if (sdkFactory == null) {
            throw new IllegalArgumentException("SDK factory cannot be null");
        }
        this.sdkFactory = sdkFactory;
        return this;
    }

    @Override
    public SdkClientBuilder<S> sdkSupplier(Supplier<S> sdkSupplier) {
        if (sdkSupplier == null) {
            throw new IllegalArgumentException("SDK supplier cannot be null");
        }
        // Convert Supplier<S> to Function<Void, S>
        this.sdkFactory = unused -> sdkSupplier.get();
        return this;
    }

    /**
     * Convenience method for using an existing SDK instance.
     *
     * <p>This method is useful when you already have a configured SDK instance
     * and want to wrap it with ServiceClient capabilities.
     *
     * @param sdkInstance the pre-configured SDK instance
     * @return this builder
     */
    public SdkClientBuilder<S> sdkInstance(S sdkInstance) {
        if (sdkInstance == null) {
            throw new IllegalArgumentException("SDK instance cannot be null");
        }
        this.sdkFactory = unused -> sdkInstance;
        return this;
    }

    @Override
    public SdkClientBuilder<S> timeout(Duration timeout) {
        if (timeout == null || timeout.isNegative()) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        this.timeout = timeout;
        return this;
    }

    @Override
    public SdkClientBuilder<S> autoShutdown(boolean autoShutdown) {
        this.autoShutdown = autoShutdown;
        return this;
    }

    /**
     * Sets a custom circuit breaker.
     *
     * @param circuitBreaker the circuit breaker
     * @return this builder
     */
    public SdkClientBuilder<S> circuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
        return this;
    }

    /**
     * Sets a custom retry policy.
     *
     * @param retry the retry policy
     * @return this builder
     */
    public SdkClientBuilder<S> retry(Retry retry) {
        this.retry = retry;
        return this;
    }

    /**
     * Adds an interceptor to the SDK client.
     *
     * <p>Interceptors can be used for logging, metrics collection, authentication,
     * and other cross-cutting concerns.
     *
     * @param interceptor the interceptor to add
     * @return this builder
     */
    public SdkClientBuilder<S> addInterceptor(ServiceClientInterceptor interceptor) {
        if (interceptor != null) {
            this.interceptors.add(interceptor);
        }
        return this;
    }

    /**
     * Convenience method to add multiple interceptors at once.
     *
     * @param interceptors the interceptors to add
     * @return this builder
     */
    public SdkClientBuilder<S> addInterceptors(List<ServiceClientInterceptor> interceptors) {
        if (interceptors != null) {
            this.interceptors.addAll(interceptors);
        }
        return this;
    }

    /**
     * Convenience method to enable logging with default settings.
     *
     * @return this builder
     */
    public SdkClientBuilder<S> withLogging() {
        return addInterceptor(LoggingInterceptor.builder().build());
    }

    /**
     * Convenience method to enable logging with custom settings.
     *
     * @param loggingInterceptor the configured logging interceptor
     * @return this builder
     */
    public SdkClientBuilder<S> withLogging(LoggingInterceptor loggingInterceptor) {
        return addInterceptor(loggingInterceptor);
    }

    /**
     * Convenience method to enable metrics collection with default settings.
     *
     * @return this builder
     */
    public SdkClientBuilder<S> withMetrics() {
        MetricsInterceptor.InMemoryMetricsCollector collector = new MetricsInterceptor.InMemoryMetricsCollector();
        return addInterceptor(new MetricsInterceptor(collector, false, false));
    }

    /**
     * Convenience method to enable metrics collection with custom settings.
     *
     * @param metricsInterceptor the configured metrics interceptor
     * @return this builder
     */
    public SdkClientBuilder<S> withMetrics(MetricsInterceptor metricsInterceptor) {
        return addInterceptor(metricsInterceptor);
    }

    /**
     * Convenience method to enable both logging and metrics with default settings.
     *
     * @return this builder
     */
    public SdkClientBuilder<S> withObservability() {
        return withLogging().withMetrics();
    }

    /**
     * Convenience method to enable circuit breaker with default settings.
     *
     * <p>Default settings:
     * <ul>
     *   <li>Failure rate threshold: 50%</li>
     *   <li>Wait duration in open state: 60 seconds</li>
     *   <li>Sliding window size: 10 calls</li>
     *   <li>Minimum number of calls: 5</li>
     * </ul>
     *
     * @return this builder
     */
    public SdkClientBuilder<S> withCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .build();

        this.circuitBreaker = CircuitBreaker.of(serviceName + "-circuit-breaker", config);
        return this;
    }

    /**
     * Convenience method to enable retry with default settings.
     *
     * <p>Default settings:
     * <ul>
     *   <li>Max attempts: 3</li>
     *   <li>Wait duration: 1 second</li>
     * </ul>
     *
     * @return this builder
     */
    public SdkClientBuilder<S> withRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(1))
            .build();

        this.retry = Retry.of(serviceName + "-retry", config);
        return this;
    }

    /**
     * Convenience method to enable both circuit breaker and retry with default settings.
     *
     * @return this builder
     */
    public SdkClientBuilder<S> withResilience() {
        return withCircuitBreaker().withRetry();
    }

    /**
     * Convenience method to enable comprehensive resilience and observability.
     *
     * <p>This enables:
     * <ul>
     *   <li>Circuit breaker with default settings</li>
     *   <li>Retry with default settings</li>
     *   <li>Logging with default settings</li>
     *   <li>Metrics with default settings</li>
     * </ul>
     *
     * @return this builder
     */
    public SdkClientBuilder<S> withDefaults() {
        return withResilience().withObservability();
    }





    @Override
    public ServiceClient build() {
        validateConfiguration();

        log.info("Building SDK service client for service '{}' with SDK type '{}'",
                serviceName, sdkType.getSimpleName());

        S sdkInstance = createSdkInstance();

        return new SdkServiceClientImpl<>(
            serviceName,
            sdkType,
            sdkInstance,
            timeout,
            autoShutdown,
            circuitBreaker,
            retry,
            interceptors
        );
    }



    private void validateConfiguration() {
        if (sdkFactory == null) {
            throw new IllegalStateException("SDK factory must be configured for SDK clients");
        }
    }

    private S createSdkInstance() {
        try {
            S instance = sdkFactory.apply(null);
            if (instance == null) {
                throw new IllegalStateException("SDK factory returned null instance");
            }
            return instance;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create SDK instance", e);
        }
    }
}
