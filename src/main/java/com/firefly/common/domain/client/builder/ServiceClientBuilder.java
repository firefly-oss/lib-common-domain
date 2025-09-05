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
import com.firefly.common.domain.client.config.AuthenticationConfiguration;
import com.firefly.common.domain.tracing.CorrelationContext;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

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

    // Enhanced configuration methods

    /**
     * Sets the authentication configuration for this client.
     *
     * @param authConfig the authentication configuration
     * @return this builder instance for method chaining
     */
    B authentication(AuthenticationConfiguration authConfig);

    /**
     * Adds a default header that will be included in all requests.
     *
     * @param name the header name
     * @param value the header value
     * @return this builder instance for method chaining
     */
    B defaultHeader(String name, String value);

    /**
     * Adds multiple default headers that will be included in all requests.
     *
     * @param headers map of headers to add
     * @return this builder instance for method chaining
     */
    B defaultHeaders(Map<String, String> headers);

    /**
     * Adds a default query parameter that will be included in all requests.
     *
     * @param name the parameter name
     * @param value the parameter value
     * @return this builder instance for method chaining
     */
    B defaultQueryParam(String name, Object value);

    /**
     * Adds multiple default query parameters that will be included in all requests.
     *
     * @param queryParams map of query parameters to add
     * @return this builder instance for method chaining
     */
    B defaultQueryParams(Map<String, Object> queryParams);

    /**
     * Sets the default content type for requests.
     *
     * @param contentType the content type
     * @return this builder instance for method chaining
     */
    B defaultContentType(String contentType);

    /**
     * Sets the default accept type for requests.
     *
     * @param acceptType the accept type
     * @return this builder instance for method chaining
     */
    B defaultAcceptType(String acceptType);

    /**
     * Sets both default content type and accept type to application/json.
     *
     * @return this builder instance for method chaining
     */
    B defaultJsonContentType();

    /**
     * Sets a custom response deserializer for the specified type.
     *
     * @param responseType the response type class
     * @param deserializer function to deserialize response string to object
     * @param <R> the response type
     * @return this builder instance for method chaining
     */
    <R> B customDeserializer(Class<R> responseType, Function<String, R> deserializer);

    /**
     * Enables response validation for the specified type.
     *
     * @param responseType the response type class
     * @param validator function to validate responses
     * @param <R> the response type
     * @return this builder instance for method chaining
     */
    <R> B responseValidator(Class<R> responseType, Function<R, Boolean> validator);

    /**
     * Sets a response transformer for the specified type.
     *
     * @param responseType the response type class
     * @param transformer function to transform responses
     * @param <R> the response type
     * @return this builder instance for method chaining
     */
    <R> B responseTransformer(Class<R> responseType, Function<R, R> transformer);

    // Convenience authentication methods

    /**
     * Configures Bearer token authentication.
     *
     * @param token the bearer token
     * @return this builder instance for method chaining
     */
    default B bearerToken(String token) {
        return authentication(AuthenticationConfiguration.bearerToken(token));
    }

    /**
     * Configures Basic authentication.
     *
     * @param username the username
     * @param password the password
     * @return this builder instance for method chaining
     */
    default B basicAuth(String username, String password) {
        return authentication(AuthenticationConfiguration.basicAuth(username, password));
    }

    /**
     * Configures API key authentication.
     *
     * @param headerName the header name for the API key
     * @param apiKey the API key value
     * @return this builder instance for method chaining
     */
    default B apiKey(String headerName, String apiKey) {
        return authentication(AuthenticationConfiguration.apiKey(headerName, apiKey));
    }
}
