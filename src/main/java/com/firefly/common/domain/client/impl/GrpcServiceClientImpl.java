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
import com.firefly.common.domain.client.ServiceClient;
import com.firefly.common.domain.resilience.CircuitBreakerManager;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * gRPC implementation of ServiceClient.
 * 
 * <p>This implementation provides a unified interface for gRPC service communication
 * while maintaining protocol-specific optimizations for Protocol Buffer serialization
 * and streaming operations.
 *
 * @param <T> the gRPC stub type
 * @author Firefly Software Solutions Inc
 * @since 2.0.0
 */
@Slf4j
public class GrpcServiceClientImpl<T> implements ServiceClient {

    private final String serviceName;
    private final Class<T> stubType;
    private final String address;
    private final Duration timeout;
    private final ManagedChannel channel;
    private final T stub;
    private final CircuitBreakerManager circuitBreakerManager;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    /**
     * Creates a new gRPC service client implementation.
     */
    public GrpcServiceClientImpl(String serviceName,
                                Class<T> stubType,
                                String address,
                                Duration timeout,
                                ManagedChannel channel,
                                T stub,
                                CircuitBreakerManager circuitBreakerManager) {
        this.serviceName = serviceName;
        this.stubType = stubType;
        this.address = address;
        this.timeout = timeout;
        this.channel = channel;
        this.stub = stub;
        this.circuitBreakerManager = circuitBreakerManager;

        log.info("Initialized gRPC service client for service '{}' with enhanced circuit breaker and address '{}'",
                serviceName, address);
    }

    // ========================================
    // Request Builder Methods (gRPC-specific Implementation)
    // ========================================

    @Override
    public <R> RequestBuilder<R> get(String endpoint, Class<R> responseType) {
        return new GrpcRequestBuilder<>(endpoint, responseType, "GET");
    }

    @Override
    public <R> RequestBuilder<R> get(String endpoint, TypeReference<R> typeReference) {
        return new GrpcRequestBuilder<>(endpoint, typeReference, "GET");
    }

    @Override
    public <R> RequestBuilder<R> post(String endpoint, Class<R> responseType) {
        return new GrpcRequestBuilder<>(endpoint, responseType, "POST");
    }

    @Override
    public <R> RequestBuilder<R> post(String endpoint, TypeReference<R> typeReference) {
        return new GrpcRequestBuilder<>(endpoint, typeReference, "POST");
    }

    @Override
    public <R> RequestBuilder<R> put(String endpoint, Class<R> responseType) {
        return new GrpcRequestBuilder<>(endpoint, responseType, "PUT");
    }

    @Override
    public <R> RequestBuilder<R> put(String endpoint, TypeReference<R> typeReference) {
        return new GrpcRequestBuilder<>(endpoint, typeReference, "PUT");
    }

    @Override
    public <R> RequestBuilder<R> delete(String endpoint, Class<R> responseType) {
        return new GrpcRequestBuilder<>(endpoint, responseType, "DELETE");
    }

    @Override
    public <R> RequestBuilder<R> delete(String endpoint, TypeReference<R> typeReference) {
        return new GrpcRequestBuilder<>(endpoint, typeReference, "DELETE");
    }

    @Override
    public <R> RequestBuilder<R> patch(String endpoint, Class<R> responseType) {
        return new GrpcRequestBuilder<>(endpoint, responseType, "PATCH");
    }

    @Override
    public <R> RequestBuilder<R> patch(String endpoint, TypeReference<R> typeReference) {
        return new GrpcRequestBuilder<>(endpoint, typeReference, "PATCH");
    }


    // ========================================
    // Streaming Methods
    // ========================================

    @Override
    public <R> Flux<R> stream(String endpoint, Class<R> responseType) {
        return Flux.error(new UnsupportedOperationException(
            "gRPC streaming should be handled through the stub directly. Use execute() or executeAsync() methods."));
    }

    @Override
    public <R> Flux<R> stream(String endpoint, TypeReference<R> typeReference) {
        return Flux.error(new UnsupportedOperationException(
            "gRPC streaming should be handled through the stub directly. Use execute() or executeAsync() methods."));
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
        return address; // For gRPC, we return the address instead of base URL
    }

    @Override
    public boolean isReady() {
        return !isShutdown.get() && !channel.isShutdown();
    }

    @Override
    public Mono<Void> healthCheck() {
        if (isShutdown.get()) {
            return Mono.error(new IllegalStateException("Client has been shut down"));
        }

        // For gRPC, we check if the channel is ready with circuit breaker protection
        Mono<Void> healthCheckOperation = Mono.<Void>fromCallable(() -> {
            if (channel.isShutdown() || channel.isTerminated()) {
                throw new RuntimeException("gRPC channel is not available");
            }
            return null;
        })
        .timeout(Duration.ofSeconds(5))
        .onErrorMap(throwable -> new RuntimeException("Health check failed for gRPC service: " + serviceName, throwable));

        return applyCircuitBreakerProtection(healthCheckOperation);
    }

    @Override
    public ClientType getClientType() {
        return ClientType.GRPC;
    }

    @Override
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            log.info("Shutting down gRPC service client for service '{}'", serviceName);
            if (!channel.isShutdown()) {
                channel.shutdown();
            }
        }
    }

    /**
     * Gets the gRPC stub for direct access.
     *
     * @return the gRPC stub
     */
    public T getStub() {
        if (isShutdown.get()) {
            throw new IllegalStateException("Client has been shut down");
        }
        return stub;
    }

    /**
     * Executes a gRPC operation with circuit breaker protection.
     *
     * @param operation the gRPC operation to execute
     * @param <R> the response type
     * @return the result wrapped in a Mono with circuit breaker protection
     */
    public <R> Mono<R> executeWithCircuitBreaker(Mono<R> operation) {
        return applyCircuitBreakerProtection(operation);
    }

    /**
     * Executes a streaming gRPC operation with circuit breaker protection.
     *
     * @param operation the gRPC streaming operation to execute
     * @param <R> the response type
     * @return the result wrapped in a Flux with circuit breaker protection
     */
    public <R> Flux<R> executeStreamWithCircuitBreaker(Flux<R> operation) {
        return applyCircuitBreakerProtectionFlux(operation);
    }

    // ========================================
    // Circuit Breaker Protection
    // ========================================

    private <R> Mono<R> applyCircuitBreakerProtection(Mono<R> operation) {
        // Use enhanced circuit breaker
        if (circuitBreakerManager != null) {
            return circuitBreakerManager.executeWithCircuitBreaker(serviceName, () -> operation)
                .doOnError(error -> log.warn("Circuit breaker detected failure for gRPC service '{}': {}",
                    serviceName, error.getMessage()))
                .doOnSuccess(result -> log.debug("Circuit breaker allowed successful gRPC request for service '{}'",
                    serviceName));
        } else {
            // No circuit breaker protection (should not happen with auto-configuration)
            log.warn("No circuit breaker configured for gRPC service '{}'", serviceName);
            return operation;
        }
    }

    private <R> Flux<R> applyCircuitBreakerProtectionFlux(Flux<R> operation) {
        // For streaming operations, we apply circuit breaker protection to the entire stream
        if (circuitBreakerManager != null) {
            return circuitBreakerManager.executeWithCircuitBreaker(serviceName, () -> operation.collectList())
                .flatMapMany(Flux::fromIterable)
                .doOnError(error -> log.warn("Circuit breaker detected failure for gRPC streaming service '{}': {}",
                    serviceName, error.getMessage()))
                .doOnComplete(() -> log.debug("Circuit breaker allowed successful gRPC streaming request for service '{}'",
                    serviceName));
        } else {
            // No circuit breaker protection (should not happen with auto-configuration)
            log.warn("No circuit breaker configured for gRPC streaming service '{}'", serviceName);
            return operation;
        }
    }

    // ========================================
    // Helper Classes
    // ========================================

    /**
     * gRPC-specific request builder that provides meaningful gRPC operations.
     *
     * <p>This implementation maps HTTP-style method calls to appropriate gRPC operations
     * based on the method name and endpoint pattern. It provides a bridge between the
     * unified ServiceClient interface and gRPC-specific operations.
     */
    private class GrpcRequestBuilder<R> implements RequestBuilder<R> {
        private final String endpoint;
        private final Class<R> responseType;
        private final TypeReference<R> typeReference;
        private final String method;
        private Object body;
        private final Map<String, Object> pathParams = new HashMap<>();
        private final Map<String, Object> queryParams = new HashMap<>();
        private final Map<String, String> headers = new HashMap<>();
        private Duration requestTimeout = timeout;

        public GrpcRequestBuilder(String endpoint, Class<R> responseType, String method) {
            this.endpoint = endpoint;
            this.responseType = responseType;
            this.typeReference = null;
            this.method = method;
        }

        public GrpcRequestBuilder(String endpoint, TypeReference<R> typeReference, String method) {
            this.endpoint = endpoint;
            this.responseType = null;
            this.typeReference = typeReference;
            this.method = method;
        }

        @Override
        public RequestBuilder<R> withBody(Object body) {
            this.body = body;
            return this;
        }

        @Override
        public RequestBuilder<R> withPathParam(String name, Object value) {
            pathParams.put(name, value);
            return this;
        }

        @Override
        public RequestBuilder<R> withPathParams(Map<String, Object> pathParams) {
            this.pathParams.putAll(pathParams);
            return this;
        }

        @Override
        public RequestBuilder<R> withQueryParam(String name, Object value) {
            queryParams.put(name, value);
            return this;
        }

        @Override
        public RequestBuilder<R> withQueryParams(Map<String, Object> queryParams) {
            this.queryParams.putAll(queryParams);
            return this;
        }

        @Override
        public RequestBuilder<R> withHeader(String name, String value) {
            headers.put(name, value);
            return this;
        }

        @Override
        public RequestBuilder<R> withHeaders(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        @Override
        public RequestBuilder<R> withTimeout(Duration timeout) {
            this.requestTimeout = timeout;
            return this;
        }

        @Override
        public Mono<R> execute() {
            return executeGrpcOperation();
        }

        @Override
        public Flux<R> stream() {
            return streamGrpcOperation();
        }

        /**
         * Executes a gRPC operation based on the method and endpoint.
         */
        @SuppressWarnings("unchecked")
        private Mono<R> executeGrpcOperation() {
            return Mono.fromCallable(() -> {
                String grpcMethodName = deriveGrpcMethodName(endpoint, method);
                log.debug("Executing gRPC operation: {} on service: {}", grpcMethodName, serviceName);

                try {
                    // Use reflection to find and invoke the gRPC method
                    Method grpcMethod = findGrpcMethod(grpcMethodName);
                    if (grpcMethod == null) {
                        throw new IllegalArgumentException(
                            String.format("No gRPC method found for endpoint '%s' and HTTP method '%s'. " +
                                "Expected method name: %s", endpoint, method, grpcMethodName));
                    }

                    // Prepare the request object
                    Object request = prepareGrpcRequest(grpcMethod, body, pathParams, queryParams);

                    // Invoke the gRPC method
                    Object result = grpcMethod.invoke(stub, request);

                    // Handle the response
                    return (R) handleGrpcResponse(result);

                } catch (Exception e) {
                    throw new RuntimeException(
                        String.format("Failed to execute gRPC operation %s: %s",
                            grpcMethodName, e.getMessage()), e);
                }
            })
            .timeout(requestTimeout)
            .transform(this::applyCircuitBreakerProtection);
        }

        /**
         * Executes a streaming gRPC operation.
         */
        private Flux<R> streamGrpcOperation() {
            return Flux.defer(() -> {
                String grpcMethodName = deriveGrpcMethodName(endpoint, method);
                log.debug("Executing streaming gRPC operation: {} on service: {}", grpcMethodName, serviceName);

                try {
                    // Find streaming method
                    Method streamingMethod = findStreamingGrpcMethod(grpcMethodName);
                    if (streamingMethod == null) {
                        return Flux.error(new IllegalArgumentException(
                            String.format("No streaming gRPC method found for endpoint '%s'. " +
                                "Expected method name: %s or %sStream", endpoint, grpcMethodName, grpcMethodName)));
                    }

                    // Prepare the request
                    Object request = prepareGrpcRequest(streamingMethod, body, pathParams, queryParams);

                    // This would need to be implemented based on your specific gRPC streaming setup
                    // For now, return an error indicating streaming needs specific implementation
                    return Flux.error(new UnsupportedOperationException(
                        String.format("gRPC streaming requires specific implementation for method: %s. " +
                            "Please use the gRPC stub directly for streaming operations: client.getStub().%s(request)",
                            grpcMethodName, grpcMethodName)));

                } catch (Exception e) {
                    return Flux.error(new RuntimeException(
                        String.format("Failed to execute streaming gRPC operation %s: %s",
                            grpcMethodName, e.getMessage()), e));
                }
            });
        }

        /**
         * Derives a gRPC method name from HTTP endpoint and method.
         */
        private String deriveGrpcMethodName(String endpoint, String method) {
            // Convert REST-style endpoint to gRPC method name
            // Example: GET /users/{id} -> getUser
            // Example: POST /users -> createUser
            // Example: PUT /users/{id} -> updateUser
            // Example: DELETE /users/{id} -> deleteUser

            String cleanEndpoint = endpoint.replaceAll("\\{[^}]+\\}", "").replaceAll("/+", "/");
            if (cleanEndpoint.startsWith("/")) {
                cleanEndpoint = cleanEndpoint.substring(1);
            }
            if (cleanEndpoint.endsWith("/")) {
                cleanEndpoint = cleanEndpoint.substring(0, cleanEndpoint.length() - 1);
            }

            String[] parts = cleanEndpoint.split("/");
            StringBuilder methodName = new StringBuilder();

            switch (method.toUpperCase()) {
                case "GET":
                    methodName.append("get");
                    break;
                case "POST":
                    methodName.append("create");
                    break;
                case "PUT":
                    methodName.append("update");
                    break;
                case "DELETE":
                    methodName.append("delete");
                    break;
                case "PATCH":
                    methodName.append("patch");
                    break;
                default:
                    methodName.append(method.toLowerCase());
            }

            for (String part : parts) {
                if (!part.isEmpty()) {
                    methodName.append(Character.toUpperCase(part.charAt(0)))
                             .append(part.substring(1));
                }
            }

            return methodName.toString();
        }

        /**
         * Finds a gRPC method on the stub by name.
         */
        private Method findGrpcMethod(String methodName) {
            try {
                Method[] methods = stub.getClass().getMethods();
                for (Method method : methods) {
                    if (method.getName().equals(methodName)) {
                        return method;
                    }
                }
                return null;
            } catch (Exception e) {
                log.warn("Error finding gRPC method {}: {}", methodName, e.getMessage());
                return null;
            }
        }

        /**
         * Finds a streaming gRPC method on the stub by name.
         */
        private Method findStreamingGrpcMethod(String methodName) {
            // Look for methods that return streaming types
            try {
                Method[] methods = stub.getClass().getMethods();
                for (Method method : methods) {
                    if (method.getName().equals(methodName) ||
                        method.getName().equals(methodName + "Stream") ||
                        method.getName().equals("stream" + Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1))) {
                        // Check if return type suggests streaming
                        Class<?> returnType = method.getReturnType();
                        if (returnType.getName().contains("Stream") ||
                            returnType.getName().contains("Iterator") ||
                            returnType.getName().contains("Observable")) {
                            return method;
                        }
                    }
                }
                return null;
            } catch (Exception e) {
                log.warn("Error finding streaming gRPC method {}: {}", methodName, e.getMessage());
                return null;
            }
        }

        /**
         * Prepares a gRPC request object from the HTTP-style parameters.
         */
        private Object prepareGrpcRequest(Method grpcMethod, Object body,
                                        Map<String, Object> pathParams,
                                        Map<String, Object> queryParams) {
            // This is a simplified implementation
            // In practice, you would need to:
            // 1. Determine the expected request type from the method signature
            // 2. Create an instance of that type
            // 3. Map HTTP parameters to protobuf fields

            if (body != null) {
                return body;
            }

            // For now, return a simple map that can be used for basic operations
            Map<String, Object> request = new HashMap<>();
            request.putAll(pathParams);
            request.putAll(queryParams);
            return request;
        }

        /**
         * Handles the gRPC response and converts it to the expected type.
         */
        private Object handleGrpcResponse(Object grpcResponse) {
            // This is a simplified implementation
            // In practice, you would need to handle protobuf to POJO conversion
            return grpcResponse;
        }

        /**
         * Applies circuit breaker protection to the operation.
         */
        private <T> Mono<T> applyCircuitBreakerProtection(Mono<T> operation) {
            if (circuitBreakerManager != null) {
                return circuitBreakerManager.executeWithCircuitBreaker(serviceName, () -> operation);
            }
            return operation;
        }
    }
}
