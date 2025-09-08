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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

/**
 * Unified interface for service clients providing reactive communication patterns.
 *
 * <p>This redesigned interface provides a simplified, consistent API for all service communication
 * types (REST, gRPC) while maintaining protocol-specific optimizations under the hood.
 *
 * <p>Key improvements:
 * <ul>
 *   <li>Simplified method signatures with consistent parameter patterns</li>
 *   <li>Better type safety with improved generic handling</li>
 *   <li>Unified error handling across all implementations</li>
 *   <li>Built-in support for streaming operations</li>
 *   <li>Consistent request/response transformation</li>
 *   <li>Simplified configuration and builder patterns</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // REST client - simplified creation
 * ServiceClient client = ServiceClient.rest("user-service")
 *     .baseUrl("http://user-service:8080")
 *     .build();
 *
 * // Simple GET request
 * Mono<User> user = client.get("/users/{id}", User.class)
 *     .withPathParam("id", "123")
 *     .execute();
 *
 * // POST with request body
 * Mono<User> created = client.post("/users", User.class)
 *     .withBody(newUser)
 *     .execute();
 * }</pre>
 *
 * @author Firefly Software Solutions Inc
 * @since 2.0.0
 */
public interface ServiceClient {

    // ========================================
    // Static Factory Methods
    // ========================================

    /**
     * Creates a REST service client builder.
     *
     * @param serviceName the name of the service
     * @return a REST client builder
     */
    static com.firefly.common.domain.client.builder.RestClientBuilder rest(String serviceName) {
        return new com.firefly.common.domain.client.builder.RestClientBuilder(serviceName);
    }

    /**
     * Creates a gRPC service client builder.
     *
     * @param serviceName the name of the service
     * @param stubType the gRPC stub type
     * @param <T> the stub type
     * @return a gRPC client builder
     */
    static <T> com.firefly.common.domain.client.builder.GrpcClientBuilder<T> grpc(String serviceName, Class<T> stubType) {
        return new com.firefly.common.domain.client.builder.GrpcClientBuilder<>(serviceName, stubType);
    }


    // ========================================
    // Request Builder Methods
    // ========================================

    /**
     * Creates a GET request builder.
     *
     * @param endpoint the endpoint path
     * @param responseType the expected response type
     * @param <R> the response type
     * @return a request builder for GET operations
     */
    <R> RequestBuilder<R> get(String endpoint, Class<R> responseType);

    /**
     * Creates a GET request builder with TypeReference for generic types.
     *
     * @param endpoint the endpoint path
     * @param typeReference the type reference for generic response types
     * @param <R> the response type
     * @return a request builder for GET operations
     */
    <R> RequestBuilder<R> get(String endpoint, TypeReference<R> typeReference);

    /**
     * Creates a POST request builder.
     *
     * @param endpoint the endpoint path
     * @param responseType the expected response type
     * @param <R> the response type
     * @return a request builder for POST operations
     */
    <R> RequestBuilder<R> post(String endpoint, Class<R> responseType);

    /**
     * Creates a POST request builder with TypeReference for generic types.
     *
     * @param endpoint the endpoint path
     * @param typeReference the type reference for generic response types
     * @param <R> the response type
     * @return a request builder for POST operations
     */
    <R> RequestBuilder<R> post(String endpoint, TypeReference<R> typeReference);

    /**
     * Creates a PUT request builder.
     *
     * @param endpoint the endpoint path
     * @param responseType the expected response type
     * @param <R> the response type
     * @return a request builder for PUT operations
     */
    <R> RequestBuilder<R> put(String endpoint, Class<R> responseType);

    /**
     * Creates a PUT request builder with TypeReference for generic types.
     *
     * @param endpoint the endpoint path
     * @param typeReference the type reference for generic response types
     * @param <R> the response type
     * @return a request builder for PUT operations
     */
    <R> RequestBuilder<R> put(String endpoint, TypeReference<R> typeReference);

    /**
     * Creates a DELETE request builder.
     *
     * @param endpoint the endpoint path
     * @param responseType the expected response type
     * @param <R> the response type
     * @return a request builder for DELETE operations
     */
    <R> RequestBuilder<R> delete(String endpoint, Class<R> responseType);

    /**
     * Creates a DELETE request builder with TypeReference for generic types.
     *
     * @param endpoint the endpoint path
     * @param typeReference the type reference for generic response types
     * @param <R> the response type
     * @return a request builder for DELETE operations
     */
    <R> RequestBuilder<R> delete(String endpoint, TypeReference<R> typeReference);

    /**
     * Creates a PATCH request builder.
     *
     * @param endpoint the endpoint path
     * @param responseType the expected response type
     * @param <R> the response type
     * @return a request builder for PATCH operations
     */
    <R> RequestBuilder<R> patch(String endpoint, Class<R> responseType);

    /**
     * Creates a PATCH request builder with TypeReference for generic types.
     *
     * @param endpoint the endpoint path
     * @param typeReference the type reference for generic response types
     * @param <R> the response type
     * @return a request builder for PATCH operations
     */
    <R> RequestBuilder<R> patch(String endpoint, TypeReference<R> typeReference);



    // ========================================
    // Streaming Methods
    // ========================================

    /**
     * Creates a streaming request for server-sent events or similar streaming responses.
     *
     * @param endpoint the endpoint path
     * @param responseType the expected response type for each stream element
     * @param <R> the response type
     * @return a Flux containing the streaming response
     */
    <R> Flux<R> stream(String endpoint, Class<R> responseType);

    /**
     * Creates a streaming request with TypeReference for generic types.
     *
     * @param endpoint the endpoint path
     * @param typeReference the type reference for generic response types
     * @param <R> the response type
     * @return a Flux containing the streaming response
     */
    <R> Flux<R> stream(String endpoint, TypeReference<R> typeReference);

    // ========================================
    // Client Metadata and Lifecycle
    // ========================================

    /**
     * Returns the service name for this client.
     *
     * @return the service name
     */
    String getServiceName();

    /**
     * Returns the base URL for this service client (REST clients only).
     *
     * @return the base URL, or null for non-REST clients
     */
    String getBaseUrl();

    /**
     * Checks if the service client is ready to handle requests.
     *
     * @return true if the client is ready, false otherwise
     */
    boolean isReady();

    /**
     * Performs a health check on the service.
     *
     * @return a Mono that completes successfully if the service is healthy
     */
    Mono<Void> healthCheck();

    /**
     * Returns the client type (REST, GRPC, SDK).
     *
     * @return the client type
     */
    ClientType getClientType();

    /**
     * Shuts down the service client and releases resources.
     */
    void shutdown();

    // ========================================
    // Nested Interfaces and Enums
    // ========================================

    // ClientType is defined as a separate class

    /**
     * Request builder interface for fluent API.
     *
     * @param <R> the response type
     */
    interface RequestBuilder<R> {
        /**
         * Sets the request body.
         *
         * @param body the request body
         * @return this builder
         */
        RequestBuilder<R> withBody(Object body);

        /**
         * Sets a path parameter.
         *
         * @param name the parameter name
         * @param value the parameter value
         * @return this builder
         */
        RequestBuilder<R> withPathParam(String name, Object value);

        /**
         * Sets multiple path parameters.
         *
         * @param pathParams the path parameters
         * @return this builder
         */
        RequestBuilder<R> withPathParams(Map<String, Object> pathParams);

        /**
         * Sets a query parameter.
         *
         * @param name the parameter name
         * @param value the parameter value
         * @return this builder
         */
        RequestBuilder<R> withQueryParam(String name, Object value);

        /**
         * Sets multiple query parameters.
         *
         * @param queryParams the query parameters
         * @return this builder
         */
        RequestBuilder<R> withQueryParams(Map<String, Object> queryParams);

        /**
         * Sets a header.
         *
         * @param name the header name
         * @param value the header value
         * @return this builder
         */
        RequestBuilder<R> withHeader(String name, String value);

        /**
         * Sets multiple headers.
         *
         * @param headers the headers
         * @return this builder
         */
        RequestBuilder<R> withHeaders(Map<String, String> headers);

        /**
         * Sets the request timeout.
         *
         * @param timeout the timeout duration
         * @return this builder
         */
        RequestBuilder<R> withTimeout(Duration timeout);

        /**
         * Executes the request.
         *
         * @return a Mono containing the response
         */
        Mono<R> execute();

        /**
         * Executes the request as a stream.
         *
         * @return a Flux containing the streaming response
         */
        Flux<R> stream();
    }

    /**
     * Client builder interfaces for different client types.
     */
    interface RestClientBuilder {
        RestClientBuilder baseUrl(String baseUrl);
        RestClientBuilder timeout(Duration timeout);
        RestClientBuilder maxConnections(int maxConnections);
        RestClientBuilder defaultHeader(String name, String value);
        ServiceClient build();
    }

    interface GrpcClientBuilder<T> {
        GrpcClientBuilder<T> address(String address);
        GrpcClientBuilder<T> timeout(Duration timeout);
        GrpcClientBuilder<T> usePlaintext();
        GrpcClientBuilder<T> useTransportSecurity();
        GrpcClientBuilder<T> stubFactory(Function<Object, T> stubFactory);
        ServiceClient build();
    }

}