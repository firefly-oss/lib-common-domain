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
package com.firefly.common.domain.client.grpc;

import com.firefly.common.domain.client.ServiceClient;
import com.firefly.common.domain.client.builder.GrpcServiceClientBuilder;
import com.firefly.common.domain.client.exception.*;
import com.firefly.common.domain.tracing.CorrelationContext;
import com.google.protobuf.Message;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.MetadataUtils;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * gRPC implementation of ServiceClient with circuit breaker and retry mechanisms
 * for resilient gRPC service communication.
 *
 * <p>This implementation provides:
 * <ul>
 *   <li>Reactive gRPC client operations with unary and streaming support</li>
 *   <li>Circuit breaker pattern for fault tolerance</li>
 *   <li>Retry mechanisms with exponential backoff</li>
 *   <li>Correlation context propagation via gRPC metadata</li>
 *   <li>Comprehensive error handling and status code mapping</li>
 * </ul>
 *
 * <p>Example usage with builder pattern:
 * <pre>{@code
 * GrpcServiceClient<PaymentServiceGrpc.PaymentServiceStub> client =
 *     GrpcServiceClient.<PaymentServiceGrpc.PaymentServiceStub>builder()
 *         .serviceName("payment-service")
 *         .address("payment-service:9090")
 *         .usePlaintext()
 *         .stubFactory(channel -> PaymentServiceGrpc.newStub(channel))
 *         .build();
 * }</pre>
 *
 * @param <T> The gRPC stub type extending AbstractAsyncStub
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public class GrpcServiceClient<T extends AbstractAsyncStub<T>> implements ServiceClient<Message> {

    private final T stub;
    private final String serviceName;
    private final String address;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final CorrelationContext correlationContext;
    private final ManagedChannel channel;

    /**
     * Constructs a new GrpcServiceClient with the specified configuration.
     *
     * @param stub the gRPC stub instance for making calls
     * @param serviceName the name of the service for identification and metrics
     * @param address the address of the gRPC service
     * @param channel the managed channel for gRPC communication
     * @param circuitBreaker the circuit breaker for fault tolerance
     * @param retry the retry configuration for resilience
     * @param correlationContext the correlation context for tracing
     * @throws IllegalArgumentException if any required parameter is null or empty
     */
    public GrpcServiceClient(T stub,
                           String serviceName,
                           String address,
                           ManagedChannel channel,
                           CircuitBreaker circuitBreaker,
                           Retry retry,
                           CorrelationContext correlationContext) {
        if (stub == null) {
            throw new IllegalArgumentException("Stub cannot be null");
        }
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Address cannot be null or empty");
        }
        if (channel == null) {
            throw new IllegalArgumentException("Channel cannot be null");
        }
        if (circuitBreaker == null) {
            throw new IllegalArgumentException("CircuitBreaker cannot be null");
        }
        if (retry == null) {
            throw new IllegalArgumentException("Retry cannot be null");
        }

        this.stub = stub;
        this.serviceName = serviceName.trim();
        this.address = address.trim();
        this.channel = channel;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.correlationContext = correlationContext;
    }

    /**
     * Creates a new GrpcServiceClientBuilder for fluent configuration.
     *
     * @param <T> the type of gRPC stub
     * @return a new builder instance
     */
    public static <T extends AbstractAsyncStub<T>> GrpcServiceClientBuilder<T> builder() {
        return GrpcServiceClientBuilder.create();
    }

    /**
     * Executes a gRPC call with the provided request and response handler.
     * 
     * @param request the gRPC request message
     * @param callFunction function that executes the gRPC call
     * @param <R> the response type
     * @return a Mono containing the response
     */
    public <R> Mono<R> call(Message request, Function<T, CompletableFuture<R>> callFunction) {
        log.debug("Making gRPC call to service {} at {}", serviceName, address);
        
        return Mono.fromFuture(() -> {
                    // Create stub with metadata
                    T stubWithMetadata = addMetadataToStub(stub);
                    return callFunction.apply(stubWithMetadata);
                })
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .timeout(Duration.ofSeconds(30))
                .doOnSuccess(response -> log.debug("gRPC call to service {} completed successfully", serviceName))
                .doOnError(error -> log.error("gRPC call to service {} failed: {}", serviceName, error.getMessage()))
                .onErrorMap(this::mapGrpcException);
    }

    /**
     * Executes a unary gRPC call.
     * 
     * @param request the request message
     * @param callFunction function that executes the unary call
     * @param <REQ> the request type
     * @param <RES> the response type
     * @return a Mono containing the response
     */
    public <REQ extends Message, RES> Mono<RES> unaryCall(REQ request, 
                                                         Function<T, Function<REQ, CompletableFuture<RES>>> callFunction) {
        return call(request, stub -> callFunction.apply(stub).apply(request));
    }

    @Override
    public <R> Mono<R> get(String endpoint, Class<R> responseType) {
        throw new UnsupportedOperationException("GET operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> get(String endpoint, Map<String, Object> queryParams, Class<R> responseType) {
        throw new UnsupportedOperationException("GET operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> post(String endpoint, Object request, Class<R> responseType) {
        throw new UnsupportedOperationException("POST operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> put(String endpoint, Object request, Class<R> responseType) {
        throw new UnsupportedOperationException("PUT operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> delete(String endpoint, Class<R> responseType) {
        throw new UnsupportedOperationException("DELETE operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> patch(String endpoint, Object request, Class<R> responseType) {
        throw new UnsupportedOperationException("PATCH operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public String getBaseUrl() {
        return address;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public Mono<Void> healthCheck() {
        return Mono.fromRunnable(() -> {
                    // Check if channel is ready
                    if (channel.isShutdown() || channel.isTerminated()) {
                        throw new ServiceUnavailableException("gRPC channel is not available for service: " + serviceName);
                    }
                })
                .then()
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(v -> log.debug("Health check successful for gRPC service: {}", serviceName))
                .doOnError(error -> log.warn("Health check failed for gRPC service: {}", serviceName, error));
    }

    /**
     * Gets the gRPC stub for direct usage when needed.
     * 
     * @return the gRPC stub
     */
    public T getStub() {
        return addMetadataToStub(stub);
    }

    /**
     * Gets the managed channel for advanced usage.
     * 
     * @return the managed channel
     */
    public ManagedChannel getChannel() {
        return channel;
    }

    private T addMetadataToStub(T originalStub) {
        Metadata metadata = new Metadata();
        
        // Add correlation ID if available
        String correlationId = correlationContext.getCorrelationId();
        if (correlationId != null) {
            Metadata.Key<String> correlationKey = Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);
            metadata.put(correlationKey, correlationId);
        }
        
        // Add trace ID if available
        String traceId = correlationContext.getTraceId();
        if (traceId != null) {
            Metadata.Key<String> traceKey = Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER);
            metadata.put(traceKey, traceId);
        }
        
        return originalStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
    }

    private Throwable mapGrpcException(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException) {
            StatusRuntimeException grpcError = (StatusRuntimeException) throwable;
            Status.Code code = grpcError.getStatus().getCode();
            
            return switch (code) {
                case NOT_FOUND -> new ServiceNotFoundException(
                    "gRPC service not found: " + grpcError.getMessage(), throwable);
                case UNAUTHENTICATED, PERMISSION_DENIED -> new ServiceAuthenticationException(
                    "gRPC authentication failed: " + grpcError.getMessage(), throwable);
                case INVALID_ARGUMENT -> new ServiceValidationException(
                    "gRPC invalid argument: " + grpcError.getMessage(), throwable);
                case UNAVAILABLE, DEADLINE_EXCEEDED, RESOURCE_EXHAUSTED -> new ServiceUnavailableException(
                    "gRPC service unavailable: " + grpcError.getMessage(), throwable);
                case INTERNAL -> new ServiceClientException(
                    "gRPC internal error: " + grpcError.getMessage(), throwable);
                default -> new ServiceClientException(
                    "gRPC call failed: " + grpcError.getMessage(), throwable);
            };
        }
        
        return new ServiceClientException("gRPC call failed: " + throwable.getMessage(), throwable);
    }

    @Override
    public <R> Mono<R> patch(String endpoint, Object request, com.fasterxml.jackson.core.type.TypeReference<R> typeReference) {
        throw new UnsupportedOperationException("PATCH operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> delete(String endpoint, com.fasterxml.jackson.core.type.TypeReference<R> typeReference) {
        throw new UnsupportedOperationException("DELETE operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> put(String endpoint, Object request, com.fasterxml.jackson.core.type.TypeReference<R> typeReference) {
        throw new UnsupportedOperationException("PUT operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> post(String endpoint, Object request, com.fasterxml.jackson.core.type.TypeReference<R> typeReference) {
        throw new UnsupportedOperationException("POST operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> get(String endpoint, Map<String, Object> queryParams, com.fasterxml.jackson.core.type.TypeReference<R> typeReference) {
        throw new UnsupportedOperationException("GET operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> get(String endpoint, com.fasterxml.jackson.core.type.TypeReference<R> typeReference) {
        throw new UnsupportedOperationException("GET operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> patch(String endpoint, Object request,
                           com.firefly.common.domain.client.config.RequestConfiguration requestConfig,
                           com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("PATCH operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> delete(String endpoint,
                            com.firefly.common.domain.client.config.RequestConfiguration requestConfig,
                            com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("DELETE operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> put(String endpoint, Object request,
                         com.firefly.common.domain.client.config.RequestConfiguration requestConfig,
                         com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("PUT operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> post(String endpoint, Object request,
                          com.firefly.common.domain.client.config.RequestConfiguration requestConfig,
                          com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("POST operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    // Additional methods with RequestConfiguration only
    @Override
    public <R> Mono<R> get(String endpoint, com.firefly.common.domain.client.config.RequestConfiguration requestConfig, Class<R> responseType) {
        throw new UnsupportedOperationException("GET operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> get(String endpoint, Map<String, Object> queryParams,
                         com.firefly.common.domain.client.config.RequestConfiguration requestConfig, Class<R> responseType) {
        throw new UnsupportedOperationException("GET operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> post(String endpoint, Object request,
                          com.firefly.common.domain.client.config.RequestConfiguration requestConfig, Class<R> responseType) {
        throw new UnsupportedOperationException("POST operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> put(String endpoint, Object request,
                         com.firefly.common.domain.client.config.RequestConfiguration requestConfig, Class<R> responseType) {
        throw new UnsupportedOperationException("PUT operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> delete(String endpoint, com.firefly.common.domain.client.config.RequestConfiguration requestConfig, Class<R> responseType) {
        throw new UnsupportedOperationException("DELETE operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> patch(String endpoint, Object request,
                           com.firefly.common.domain.client.config.RequestConfiguration requestConfig, Class<R> responseType) {
        throw new UnsupportedOperationException("PATCH operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    // Additional methods with both RequestConfiguration and ResponseConfiguration
    @Override
    public <R> Mono<R> get(String endpoint,
                         com.firefly.common.domain.client.config.RequestConfiguration requestConfig,
                         com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("GET operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> get(String endpoint, Map<String, Object> queryParams,
                         com.firefly.common.domain.client.config.RequestConfiguration requestConfig,
                         com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("GET operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    // Methods with ResponseConfiguration only
    @Override
    public <R> Mono<R> get(String endpoint, com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("GET operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> get(String endpoint, Map<String, Object> queryParams,
                         com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("GET operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> post(String endpoint, Object request,
                          com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("POST operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> put(String endpoint, Object request,
                         com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("PUT operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> delete(String endpoint, com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("DELETE operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

    @Override
    public <R> Mono<R> patch(String endpoint, Object request,
                           com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("PATCH operation not supported for gRPC. Use call() or unaryCall() methods instead.");
    }

}