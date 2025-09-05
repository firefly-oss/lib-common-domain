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

package com.firefly.common.domain.client.interceptor;

import reactor.core.publisher.Mono;

/**
 * Base interface for ServiceClient interceptors.
 * 
 * <p>Interceptors provide a way to add cross-cutting concerns to service client operations
 * such as logging, metrics collection, authentication, request/response transformation,
 * and custom business logic.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class LoggingInterceptor implements ServiceClientInterceptor {
 *     @Override
 *     public Mono<InterceptorResponse> intercept(InterceptorRequest request, InterceptorChain chain) {
 *         log.info("Executing request to {}", request.getEndpoint());
 *         return chain.proceed(request)
 *             .doOnSuccess(response -> log.info("Request completed with status: {}", response.getStatusCode()))
 *             .doOnError(error -> log.error("Request failed: {}", error.getMessage()));
 *     }
 * }
 * }</pre>
 *
 * @author Firefly Software Solutions Inc
 * @since 2.0.0
 */
public interface ServiceClientInterceptor {

    /**
     * Intercepts a service client request and optionally modifies the request or response.
     * 
     * <p>Implementations should call {@code chain.proceed(request)} to continue the chain
     * or return a custom response to short-circuit the execution.
     *
     * @param request the interceptor request containing request details
     * @param chain the interceptor chain to continue execution
     * @return a Mono containing the interceptor response
     */
    Mono<InterceptorResponse> intercept(InterceptorRequest request, InterceptorChain chain);

    /**
     * Returns the order of this interceptor in the chain.
     * Lower values have higher priority and execute first.
     *
     * @return the order value (default: 0)
     */
    default int getOrder() {
        return 0;
    }

    /**
     * Returns whether this interceptor should be applied to the given request.
     * 
     * <p>This allows for conditional interceptor application based on service name,
     * endpoint, client type, or other request characteristics.
     *
     * @param request the interceptor request
     * @return true if this interceptor should be applied, false otherwise
     */
    default boolean shouldIntercept(InterceptorRequest request) {
        return true;
    }

    /**
     * Called when the interceptor is registered with a client.
     * Can be used for initialization or validation.
     *
     * @param clientType the type of client this interceptor is registered with
     */
    default void onRegistration(String clientType) {
        // Default implementation does nothing
    }

    /**
     * Called when the client is shutting down.
     * Can be used for cleanup or resource release.
     */
    default void onShutdown() {
        // Default implementation does nothing
    }
}

/**
 * Represents a request in the interceptor chain.
 */
interface InterceptorRequest {
    /**
     * Gets the service name.
     */
    String getServiceName();

    /**
     * Gets the endpoint being called.
     */
    String getEndpoint();

    /**
     * Gets the HTTP method (for REST clients).
     */
    String getMethod();

    /**
     * Gets the request body.
     */
    Object getBody();

    /**
     * Gets request headers.
     */
    java.util.Map<String, String> getHeaders();

    /**
     * Gets query parameters.
     */
    java.util.Map<String, Object> getQueryParams();

    /**
     * Gets path parameters.
     */
    java.util.Map<String, Object> getPathParams();

    /**
     * Gets the client type.
     */
    String getClientType();

    /**
     * Gets the request timeout.
     */
    java.time.Duration getTimeout();

    /**
     * Gets additional request attributes.
     */
    java.util.Map<String, Object> getAttributes();

    /**
     * Creates a modified copy of this request.
     */
    InterceptorRequest withHeader(String name, String value);
    InterceptorRequest withBody(Object body);
    InterceptorRequest withTimeout(java.time.Duration timeout);
    InterceptorRequest withAttribute(String name, Object value);
}

/**
 * Represents a response in the interceptor chain.
 */
interface InterceptorResponse {
    /**
     * Gets the response body.
     */
    Object getBody();

    /**
     * Gets the HTTP status code (for REST clients).
     */
    int getStatusCode();

    /**
     * Gets response headers.
     */
    java.util.Map<String, String> getHeaders();

    /**
     * Gets the response time in milliseconds.
     */
    long getResponseTimeMs();

    /**
     * Gets whether the response was successful.
     */
    boolean isSuccessful();

    /**
     * Gets any error that occurred.
     */
    Throwable getError();

    /**
     * Gets additional response attributes.
     */
    java.util.Map<String, Object> getAttributes();

    /**
     * Creates a modified copy of this response.
     */
    InterceptorResponse withBody(Object body);
    InterceptorResponse withHeader(String name, String value);
    InterceptorResponse withAttribute(String name, Object value);
}

/**
 * Represents the interceptor chain for continuing execution.
 */
interface InterceptorChain {
    /**
     * Proceeds with the request to the next interceptor or the actual service call.
     *
     * @param request the request to proceed with
     * @return a Mono containing the response
     */
    Mono<InterceptorResponse> proceed(InterceptorRequest request);

    /**
     * Gets the remaining interceptors in the chain.
     */
    java.util.List<ServiceClientInterceptor> getRemainingInterceptors();

    /**
     * Gets the current interceptor index.
     */
    int getCurrentIndex();
}
