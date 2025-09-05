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
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.function.Function;

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
     * Convenience method for creating SDK factory from supplier.
     * 
     * <p>This method allows using a simple supplier instead of a function,
     * making the API more intuitive for most use cases.
     *
     * @param sdkSupplier the SDK supplier
     * @return this builder
     */
    public SdkClientBuilder<S> sdkSupplier(java.util.function.Supplier<S> sdkSupplier) {
        if (sdkSupplier == null) {
            throw new IllegalArgumentException("SDK supplier cannot be null");
        }
        this.sdkFactory = ignored -> sdkSupplier.get();
        return this;
    }

    /**
     * Convenience method for creating SDK factory from instance.
     * 
     * <p>This method allows using a pre-created SDK instance. Note that
     * the same instance will be reused for all operations.
     *
     * @param sdkInstance the SDK instance
     * @return this builder
     */
    public SdkClientBuilder<S> sdkInstance(S sdkInstance) {
        if (sdkInstance == null) {
            throw new IllegalArgumentException("SDK instance cannot be null");
        }
        this.sdkFactory = ignored -> sdkInstance;
        return this;
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
            retry
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
