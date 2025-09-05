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
import com.firefly.common.domain.tracing.CorrelationContext;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

import java.time.Duration;

/**
 * Base interface for ServiceClient builders providing fluent API for configuration.
 * This interface defines common configuration options available for all ServiceClient types.
 * 
 * @param <T> the type of ServiceClient being built
 * @param <B> the type of builder (for fluent interface)
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public interface ServiceClientBuilder<T extends ServiceClient<?>, B extends ServiceClientBuilder<T, B>> {

    /**
     * Sets the service name for this client.
     * Used for metrics, logging, and circuit breaker identification.
     *
     * @param serviceName the service name
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if serviceName is null or empty
     */
    B serviceName(String serviceName);

    /**
     * Sets the base URL or address for the service.
     *
     * @param baseUrl the base URL or address
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if baseUrl is null or empty
     */
    B baseUrl(String baseUrl);

    /**
     * Sets the circuit breaker instance to use for resilience.
     *
     * @param circuitBreaker the circuit breaker instance
     * @return this builder instance for method chaining
     */
    B circuitBreaker(CircuitBreaker circuitBreaker);

    /**
     * Sets the retry instance to use for resilience.
     *
     * @param retry the retry instance
     * @return this builder instance for method chaining
     */
    B retry(Retry retry);

    /**
     * Sets the correlation context for tracing.
     *
     * @param correlationContext the correlation context
     * @return this builder instance for method chaining
     */
    B correlationContext(CorrelationContext correlationContext);

    /**
     * Sets the request timeout duration.
     *
     * @param timeout the timeout duration
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if timeout is null or negative
     */
    B timeout(Duration timeout);

    /**
     * Builds and returns the configured ServiceClient instance.
     *
     * @return the configured ServiceClient
     * @throws IllegalStateException if required configuration is missing
     */
    T build();
}
