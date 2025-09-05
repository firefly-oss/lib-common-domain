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
import com.firefly.common.domain.client.config.RequestConfiguration;
import com.firefly.common.domain.client.config.ResponseConfiguration;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Base interface for service clients in the common domain architecture.
 *
 * <p>Provides a unified, reactive abstraction for both REST and gRPC service communication
 * with built-in resilience patterns, authentication, and advanced configuration options.
 *
 * <p>Key features:
 * <ul>
 *   <li>Reactive programming model with Mono return types</li>
 *   <li>Built-in circuit breaker and retry mechanisms</li>
 *   <li>Automatic authentication and authorization</li>
 *   <li>Request/response configuration and transformation</li>
 *   <li>Correlation context propagation for distributed tracing</li>
 *   <li>Health check capabilities</li>
 * </ul>
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link com.firefly.common.domain.client.rest.RestServiceClient} - HTTP/REST communication</li>
 *   <li>{@link com.firefly.common.domain.client.grpc.GrpcServiceClient} - gRPC communication</li>
 *   <li>{@link com.firefly.common.domain.client.sdk.SdkServiceClient} - SDK-based communication</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // REST client
 * RestServiceClient client = RestServiceClient.builder()
 *     .serviceName("user-service")
 *     .baseUrl("http://user-service:8080")
 *     .timeout(Duration.ofSeconds(30))
 *     .build();
 *
 * // Make a request
 * Mono<User> user = client.get("/users/{id}", User.class, Map.of("id", "123"));
 *
 * // SDK client
 * SdkServiceClient<PaymentSDK> sdkClient = SdkServiceClient.<PaymentSDK>builder()
 *     .serviceName("payment-service")
 *     .sdkFactory(() -> new PaymentSDK(apiKey, environment))
 *     .timeout(Duration.ofSeconds(30))
 *     .build();
 *
 * // Execute SDK operation
 * Mono<PaymentResult> result = sdkClient.execute(sdk ->
 *     sdk.processPayment(paymentRequest));
 * }</pre>
 *
 * @param <T> The type of request/response data this client handles
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 * @see com.firefly.common.domain.client.rest.RestServiceClient
 * @see com.firefly.common.domain.client.grpc.GrpcServiceClient
 * @see com.firefly.common.domain.client.builder.RestServiceClientBuilder
 * @see com.firefly.common.domain.client.builder.GrpcServiceClientBuilder
 */
public interface ServiceClient<T> {

    /**
     * Executes a GET request to retrieve data.
     * 
     * @param endpoint the endpoint path
     * @param responseType the expected response type
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> get(String endpoint, Class<R> responseType);

    /**
     * Executes a GET request with query parameters.
     * 
     * @param endpoint the endpoint path
     * @param queryParams query parameters
     * @param responseType the expected response type
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> get(String endpoint, Map<String, Object> queryParams, Class<R> responseType);

    /**
     * Executes a POST request with a request body.
     * 
     * @param endpoint the endpoint path
     * @param request the request body
     * @param responseType the expected response type
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> post(String endpoint, Object request, Class<R> responseType);

    /**
     * Executes a PUT request with a request body.
     * 
     * @param endpoint the endpoint path
     * @param request the request body
     * @param responseType the expected response type
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> put(String endpoint, Object request, Class<R> responseType);

    /**
     * Executes a DELETE request.
     * 
     * @param endpoint the endpoint path
     * @param responseType the expected response type
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> delete(String endpoint, Class<R> responseType);

    /**
     * Executes a PATCH request with a request body.
     * 
     * @param endpoint the endpoint path
     * @param request the request body
     * @param responseType the expected response type
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> patch(String endpoint, Object request, Class<R> responseType);

    /**
     * Returns the base URL for this service client.
     * 
     * @return the base URL
     */
    String getBaseUrl();

    /**
     * Returns the service name for this client.
     * Used for metrics, logging, and circuit breaker identification.
     * 
     * @return the service name
     */
    String getServiceName();

    /**
     * Health check method to verify service connectivity.
     *
     * @return a Mono that completes successfully if the service is healthy
     */
    Mono<Void> healthCheck();

    // Enhanced methods with RequestConfiguration support

    /**
     * Executes a GET request with request configuration.
     *
     * @param endpoint the endpoint path
     * @param requestConfig request configuration for headers, query params, etc.
     * @param responseType the expected response type
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> get(String endpoint, RequestConfiguration requestConfig, Class<R> responseType);

    /**
     * Executes a GET request with query parameters and request configuration.
     *
     * @param endpoint the endpoint path
     * @param queryParams query parameters (merged with requestConfig query params)
     * @param requestConfig request configuration for headers, timeouts, etc.
     * @param responseType the expected response type
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> get(String endpoint, Map<String, Object> queryParams,
                    RequestConfiguration requestConfig, Class<R> responseType);

    /**
     * Executes a POST request with request configuration.
     *
     * @param endpoint the endpoint path
     * @param request the request body
     * @param requestConfig request configuration for headers, content type, etc.
     * @param responseType the expected response type
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> post(String endpoint, Object request, RequestConfiguration requestConfig,
                     Class<R> responseType);

    /**
     * Executes a PUT request with request configuration.
     *
     * @param endpoint the endpoint path
     * @param request the request body
     * @param requestConfig request configuration for headers, content type, etc.
     * @param responseType the expected response type
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> put(String endpoint, Object request, RequestConfiguration requestConfig,
                    Class<R> responseType);

    /**
     * Executes a DELETE request with request configuration.
     *
     * @param endpoint the endpoint path
     * @param requestConfig request configuration for headers, timeouts, etc.
     * @param responseType the expected response type
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> delete(String endpoint, RequestConfiguration requestConfig, Class<R> responseType);

    /**
     * Executes a PATCH request with request configuration.
     *
     * @param endpoint the endpoint path
     * @param request the request body
     * @param requestConfig request configuration for headers, content type, etc.
     * @param responseType the expected response type
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> patch(String endpoint, Object request, RequestConfiguration requestConfig,
                      Class<R> responseType);

    // Enhanced methods with ResponseConfiguration support

    /**
     * Executes a GET request with response configuration for complex types.
     *
     * @param endpoint the endpoint path
     * @param responseConfig response configuration with type reference and processing
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> get(String endpoint, ResponseConfiguration<R> responseConfig);

    /**
     * Executes a GET request with query parameters and response configuration.
     *
     * @param endpoint the endpoint path
     * @param queryParams query parameters
     * @param responseConfig response configuration with type reference and processing
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> get(String endpoint, Map<String, Object> queryParams,
                    ResponseConfiguration<R> responseConfig);

    /**
     * Executes a POST request with response configuration.
     *
     * @param endpoint the endpoint path
     * @param request the request body
     * @param responseConfig response configuration with type reference and processing
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> post(String endpoint, Object request, ResponseConfiguration<R> responseConfig);

    /**
     * Executes a PUT request with response configuration.
     *
     * @param endpoint the endpoint path
     * @param request the request body
     * @param responseConfig response configuration with type reference and processing
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> put(String endpoint, Object request, ResponseConfiguration<R> responseConfig);

    /**
     * Executes a DELETE request with response configuration.
     *
     * @param endpoint the endpoint path
     * @param responseConfig response configuration with type reference and processing
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> delete(String endpoint, ResponseConfiguration<R> responseConfig);

    /**
     * Executes a PATCH request with response configuration.
     *
     * @param endpoint the endpoint path
     * @param request the request body
     * @param responseConfig response configuration with type reference and processing
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> patch(String endpoint, Object request, ResponseConfiguration<R> responseConfig);

    // Enhanced methods with both RequestConfiguration and ResponseConfiguration

    /**
     * Executes a GET request with full configuration support.
     *
     * @param endpoint the endpoint path
     * @param requestConfig request configuration for headers, query params, etc.
     * @param responseConfig response configuration with type reference and processing
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> get(String endpoint, RequestConfiguration requestConfig,
                    ResponseConfiguration<R> responseConfig);

    /**
     * Executes a GET request with query parameters and full configuration support.
     *
     * @param endpoint the endpoint path
     * @param queryParams query parameters (merged with requestConfig query params)
     * @param requestConfig request configuration for headers, timeouts, etc.
     * @param responseConfig response configuration with type reference and processing
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> get(String endpoint, Map<String, Object> queryParams,
                    RequestConfiguration requestConfig, ResponseConfiguration<R> responseConfig);

    /**
     * Executes a POST request with full configuration support.
     *
     * @param endpoint the endpoint path
     * @param request the request body
     * @param requestConfig request configuration for headers, content type, etc.
     * @param responseConfig response configuration with type reference and processing
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> post(String endpoint, Object request, RequestConfiguration requestConfig,
                     ResponseConfiguration<R> responseConfig);

    /**
     * Executes a PUT request with full configuration support.
     *
     * @param endpoint the endpoint path
     * @param request the request body
     * @param requestConfig request configuration for headers, content type, etc.
     * @param responseConfig response configuration with type reference and processing
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> put(String endpoint, Object request, RequestConfiguration requestConfig,
                    ResponseConfiguration<R> responseConfig);

    /**
     * Executes a DELETE request with full configuration support.
     *
     * @param endpoint the endpoint path
     * @param requestConfig request configuration for headers, timeouts, etc.
     * @param responseConfig response configuration with type reference and processing
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> delete(String endpoint, RequestConfiguration requestConfig,
                       ResponseConfiguration<R> responseConfig);

    /**
     * Executes a PATCH request with full configuration support.
     *
     * @param endpoint the endpoint path
     * @param request the request body
     * @param requestConfig request configuration for headers, content type, etc.
     * @param responseConfig response configuration with type reference and processing
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> patch(String endpoint, Object request, RequestConfiguration requestConfig,
                      ResponseConfiguration<R> responseConfig);

    // Convenience methods for TypeReference support

    /**
     * Executes a GET request with TypeReference for generic types.
     *
     * @param endpoint the endpoint path
     * @param typeReference type reference for generic response types
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> get(String endpoint, TypeReference<R> typeReference);

    /**
     * Executes a GET request with query parameters and TypeReference.
     *
     * @param endpoint the endpoint path
     * @param queryParams query parameters
     * @param typeReference type reference for generic response types
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> get(String endpoint, Map<String, Object> queryParams, TypeReference<R> typeReference);

    /**
     * Executes a POST request with TypeReference for generic types.
     *
     * @param endpoint the endpoint path
     * @param request the request body
     * @param typeReference type reference for generic response types
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> post(String endpoint, Object request, TypeReference<R> typeReference);

    /**
     * Executes a PUT request with TypeReference for generic types.
     *
     * @param endpoint the endpoint path
     * @param request the request body
     * @param typeReference type reference for generic response types
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> put(String endpoint, Object request, TypeReference<R> typeReference);

    /**
     * Executes a DELETE request with TypeReference for generic types.
     *
     * @param endpoint the endpoint path
     * @param typeReference type reference for generic response types
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> delete(String endpoint, TypeReference<R> typeReference);

    /**
     * Executes a PATCH request with TypeReference for generic types.
     *
     * @param endpoint the endpoint path
     * @param request the request body
     * @param typeReference type reference for generic response types
     * @param <R> the response type
     * @return a Mono containing the response
     */
    <R> Mono<R> patch(String endpoint, Object request, TypeReference<R> typeReference);
}