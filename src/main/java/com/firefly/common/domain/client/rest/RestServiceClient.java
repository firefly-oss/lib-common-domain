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
package com.firefly.common.domain.client.rest;

import com.firefly.common.domain.client.ServiceClient;
import com.firefly.common.domain.client.builder.RestServiceClientBuilder;
import com.firefly.common.domain.client.exception.*;
import com.firefly.common.domain.tracing.CorrelationContext;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * REST implementation of ServiceClient using WebClient with circuit breaker
 * and retry mechanisms for resilient service communication.
 *
 * <p>This implementation provides:
 * <ul>
 *   <li>Reactive HTTP client operations (GET, POST, PUT, DELETE, PATCH)</li>
 *   <li>Circuit breaker pattern for fault tolerance</li>
 *   <li>Retry mechanisms with exponential backoff</li>
 *   <li>Correlation context propagation for distributed tracing</li>
 *   <li>Comprehensive error handling and mapping</li>
 * </ul>
 *
 * <p>Example usage with builder pattern:
 * <pre>{@code
 * RestServiceClient client = RestServiceClient.builder()
 *     .serviceName("user-service")
 *     .baseUrl("http://user-service:8080")
 *     .timeout(Duration.ofSeconds(30))
 *     .maxConnections(50)
 *     .build();
 * }</pre>
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public class RestServiceClient implements ServiceClient<Object> {

    private final WebClient webClient;
    private final String serviceName;
    private final String baseUrl;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final CorrelationContext correlationContext;

    /**
     * Constructs a new RestServiceClient with the specified configuration.
     *
     * @param serviceName the name of the service for identification and metrics
     * @param baseUrl the base URL of the REST service
     * @param webClient the configured WebClient instance
     * @param circuitBreaker the circuit breaker for fault tolerance
     * @param retry the retry configuration for resilience
     * @param correlationContext the correlation context for tracing
     * @throws IllegalArgumentException if any required parameter is null or empty
     */
    public RestServiceClient(String serviceName,
                           String baseUrl,
                           WebClient webClient,
                           CircuitBreaker circuitBreaker,
                           Retry retry,
                           CorrelationContext correlationContext) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL cannot be null or empty");
        }
        if (webClient == null) {
            throw new IllegalArgumentException("WebClient cannot be null");
        }
        if (circuitBreaker == null) {
            throw new IllegalArgumentException("CircuitBreaker cannot be null");
        }
        if (retry == null) {
            throw new IllegalArgumentException("Retry cannot be null");
        }

        this.serviceName = serviceName.trim();
        this.baseUrl = baseUrl.trim();
        this.webClient = webClient;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.correlationContext = correlationContext;
    }

    /**
     * Creates a new RestServiceClientBuilder for fluent configuration.
     *
     * @return a new builder instance
     */
    public static RestServiceClientBuilder builder() {
        return RestServiceClientBuilder.create();
    }

    @Override
    public <R> Mono<R> get(String endpoint, Class<R> responseType) {
        return get(endpoint, (Map<String, Object>) null, responseType);
    }

    @Override
    public <R> Mono<R> get(String endpoint, Map<String, Object> queryParams, Class<R> responseType) {
        log.debug("Making GET request to {}/{} for service {}", baseUrl, endpoint, serviceName);
        
        WebClient.RequestHeadersUriSpec<?> requestSpec = webClient.get();
        WebClient.RequestHeadersSpec<?> uriSpec = requestSpec.uri(uriBuilder -> {
            uriBuilder.path(endpoint);
            if (queryParams != null) {
                queryParams.forEach(uriBuilder::queryParam);
            }
            return uriBuilder.build();
        });

        return executeRequest(uriSpec, responseType, "GET", endpoint);
    }

    @Override
    public <R> Mono<R> post(String endpoint, Object request, Class<R> responseType) {
        log.debug("Making POST request to {}/{} for service {}", baseUrl, endpoint, serviceName);

        WebClient.RequestHeadersSpec<?> requestSpec = webClient.post().uri(endpoint);
        if (request != null) {
            requestSpec = webClient.post().uri(endpoint).bodyValue(request);
        }

        return executeRequest(requestSpec, responseType, "POST", endpoint);
    }

    @Override
    public <R> Mono<R> put(String endpoint, Object request, Class<R> responseType) {
        log.debug("Making PUT request to {}/{} for service {}", baseUrl, endpoint, serviceName);

        WebClient.RequestHeadersSpec<?> requestSpec = webClient.put().uri(endpoint);
        if (request != null) {
            requestSpec = webClient.put().uri(endpoint).bodyValue(request);
        }

        return executeRequest(requestSpec, responseType, "PUT", endpoint);
    }

    @Override
    public <R> Mono<R> delete(String endpoint, Class<R> responseType) {
        log.debug("Making DELETE request to {}/{} for service {}", baseUrl, endpoint, serviceName);
        
        WebClient.RequestHeadersSpec<?> requestSpec = webClient.delete().uri(endpoint);

        return executeRequest(requestSpec, responseType, "DELETE", endpoint);
    }

    @Override
    public <R> Mono<R> patch(String endpoint, Object request, Class<R> responseType) {
        log.debug("Making PATCH request to {}/{} for service {}", baseUrl, endpoint, serviceName);

        WebClient.RequestHeadersSpec<?> requestSpec = webClient.patch().uri(endpoint);
        if (request != null) {
            requestSpec = webClient.patch().uri(endpoint).bodyValue(request);
        }

        return executeRequest(requestSpec, responseType, "PATCH", endpoint);
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public Mono<Void> healthCheck() {
        return webClient.get()
                .uri("/health")
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return Mono.empty();
                    } else {
                        return Mono.error(new ServiceUnavailableException(
                            "Health check failed for service: " + serviceName +
                            " with status: " + response.statusCode()));
                    }
                })
                .then()
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(v -> log.debug("Health check successful for service: {}", serviceName))
                .doOnError(error -> log.warn("Health check failed for service: {}", serviceName, error));
    }

    private <R> Mono<R> executeRequest(WebClient.RequestHeadersSpec<?> requestSpec, 
                                      Class<R> responseType, 
                                      String method, 
                                      String endpoint) {
        return requestSpec
                .headers(headers -> {
                    // Add correlation ID if available
                    String correlationId = correlationContext.getCorrelationId();
                    if (correlationId != null) {
                        headers.add("X-Correlation-ID", correlationId);
                    }
                    
                    // Add trace ID if available
                    String traceId = correlationContext.getTraceId();
                    if (traceId != null) {
                        headers.add("X-Trace-ID", traceId);
                    }
                })
                .retrieve()
                .bodyToMono(responseType)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .timeout(Duration.ofSeconds(30))
                .doOnSuccess(response -> log.debug("{} request to {}/{} completed successfully", 
                    method, baseUrl, endpoint))
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException webError = (WebClientResponseException) error;
                        log.error("{} request to {}/{} failed with status {}: {}", 
                            method, baseUrl, endpoint, webError.getStatusCode(), webError.getMessage());
                    } else {
                        log.error("{} request to {}/{} failed: {}", 
                            method, baseUrl, endpoint, error.getMessage());
                    }
                })
                .onErrorMap(this::mapException);
    }

    private Throwable mapException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException webError = (WebClientResponseException) throwable;
            HttpStatus status = (HttpStatus) webError.getStatusCode();
            
            return switch (status) {
                case NOT_FOUND -> new ServiceNotFoundException(
                    "Service endpoint not found: " + webError.getMessage(), throwable);
                case UNAUTHORIZED, FORBIDDEN -> new ServiceAuthenticationException(
                    "Authentication failed: " + webError.getMessage(), throwable);
                case BAD_REQUEST -> new ServiceValidationException(
                    "Bad request: " + webError.getMessage(), throwable);
                case INTERNAL_SERVER_ERROR, BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT -> 
                    new ServiceUnavailableException(
                        "Service unavailable: " + webError.getMessage(), throwable);
                default -> new ServiceClientException(
                    "Service call failed: " + webError.getMessage(), throwable);
            };
        }
        
        return new ServiceClientException("Service call failed: " + throwable.getMessage(), throwable);
    }

    @Override
    public <R> Mono<R> patch(String endpoint, Object request, com.fasterxml.jackson.core.type.TypeReference<R> typeReference) {
        // For now, delegate to the Class-based method
        // TODO: Implement proper TypeReference support when needed
        throw new UnsupportedOperationException("PATCH with TypeReference not yet implemented. Use patch(endpoint, request, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> delete(String endpoint, com.fasterxml.jackson.core.type.TypeReference<R> typeReference) {
        // For now, delegate to the Class-based method
        // TODO: Implement proper TypeReference support when needed
        throw new UnsupportedOperationException("DELETE with TypeReference not yet implemented. Use delete(endpoint, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> put(String endpoint, Object request, com.fasterxml.jackson.core.type.TypeReference<R> typeReference) {
        // For now, delegate to the Class-based method
        // TODO: Implement proper TypeReference support when needed
        throw new UnsupportedOperationException("PUT with TypeReference not yet implemented. Use put(endpoint, request, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> post(String endpoint, Object request, com.fasterxml.jackson.core.type.TypeReference<R> typeReference) {
        // For now, delegate to the Class-based method
        // TODO: Implement proper TypeReference support when needed
        throw new UnsupportedOperationException("POST with TypeReference not yet implemented. Use post(endpoint, request, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> get(String endpoint, Map<String, Object> queryParams, com.fasterxml.jackson.core.type.TypeReference<R> typeReference) {
        // For now, delegate to the Class-based method
        // TODO: Implement proper TypeReference support when needed
        throw new UnsupportedOperationException("GET with TypeReference not yet implemented. Use get(endpoint, queryParams, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> get(String endpoint, com.fasterxml.jackson.core.type.TypeReference<R> typeReference) {
        // For now, delegate to the Class-based method
        // TODO: Implement proper TypeReference support when needed
        throw new UnsupportedOperationException("GET with TypeReference not yet implemented. Use get(endpoint, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> patch(String endpoint, Object request,
                           com.firefly.common.domain.client.config.RequestConfiguration requestConfig,
                           com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        // For now, delegate to the simple method
        // TODO: Implement proper RequestConfiguration and ResponseConfiguration support when needed
        throw new UnsupportedOperationException("PATCH with RequestConfiguration and ResponseConfiguration not yet implemented. Use patch(endpoint, request, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> delete(String endpoint,
                            com.firefly.common.domain.client.config.RequestConfiguration requestConfig,
                            com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        // For now, delegate to the simple method
        // TODO: Implement proper RequestConfiguration and ResponseConfiguration support when needed
        throw new UnsupportedOperationException("DELETE with RequestConfiguration and ResponseConfiguration not yet implemented. Use delete(endpoint, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> put(String endpoint, Object request,
                         com.firefly.common.domain.client.config.RequestConfiguration requestConfig,
                         com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        // For now, delegate to the simple method
        // TODO: Implement proper RequestConfiguration and ResponseConfiguration support when needed
        throw new UnsupportedOperationException("PUT with RequestConfiguration and ResponseConfiguration not yet implemented. Use put(endpoint, request, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> post(String endpoint, Object request,
                          com.firefly.common.domain.client.config.RequestConfiguration requestConfig,
                          com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        // For now, delegate to the simple method
        // TODO: Implement proper RequestConfiguration and ResponseConfiguration support when needed
        throw new UnsupportedOperationException("POST with RequestConfiguration and ResponseConfiguration not yet implemented. Use post(endpoint, request, Class<R>) instead.");
    }

    // Additional methods with RequestConfiguration only
    @Override
    public <R> Mono<R> get(String endpoint, com.firefly.common.domain.client.config.RequestConfiguration requestConfig, Class<R> responseType) {
        throw new UnsupportedOperationException("GET with RequestConfiguration not yet implemented. Use get(endpoint, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> get(String endpoint, Map<String, Object> queryParams,
                         com.firefly.common.domain.client.config.RequestConfiguration requestConfig, Class<R> responseType) {
        throw new UnsupportedOperationException("GET with query params and RequestConfiguration not yet implemented. Use get(endpoint, queryParams, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> post(String endpoint, Object request,
                          com.firefly.common.domain.client.config.RequestConfiguration requestConfig, Class<R> responseType) {
        throw new UnsupportedOperationException("POST with RequestConfiguration not yet implemented. Use post(endpoint, request, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> put(String endpoint, Object request,
                         com.firefly.common.domain.client.config.RequestConfiguration requestConfig, Class<R> responseType) {
        throw new UnsupportedOperationException("PUT with RequestConfiguration not yet implemented. Use put(endpoint, request, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> delete(String endpoint, com.firefly.common.domain.client.config.RequestConfiguration requestConfig, Class<R> responseType) {
        throw new UnsupportedOperationException("DELETE with RequestConfiguration not yet implemented. Use delete(endpoint, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> patch(String endpoint, Object request,
                           com.firefly.common.domain.client.config.RequestConfiguration requestConfig, Class<R> responseType) {
        throw new UnsupportedOperationException("PATCH with RequestConfiguration not yet implemented. Use patch(endpoint, request, Class<R>) instead.");
    }

    // Additional methods with both RequestConfiguration and ResponseConfiguration
    @Override
    public <R> Mono<R> get(String endpoint,
                         com.firefly.common.domain.client.config.RequestConfiguration requestConfig,
                         com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("GET with RequestConfiguration and ResponseConfiguration not yet implemented. Use get(endpoint, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> get(String endpoint, Map<String, Object> queryParams,
                         com.firefly.common.domain.client.config.RequestConfiguration requestConfig,
                         com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("GET with query params, RequestConfiguration and ResponseConfiguration not yet implemented. Use get(endpoint, queryParams, Class<R>) instead.");
    }

    // Methods with ResponseConfiguration only
    @Override
    public <R> Mono<R> get(String endpoint, com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("GET with ResponseConfiguration not yet implemented. Use get(endpoint, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> get(String endpoint, Map<String, Object> queryParams,
                         com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("GET with query params and ResponseConfiguration not yet implemented. Use get(endpoint, queryParams, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> post(String endpoint, Object request,
                          com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("POST with ResponseConfiguration not yet implemented. Use post(endpoint, request, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> put(String endpoint, Object request,
                         com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("PUT with ResponseConfiguration not yet implemented. Use put(endpoint, request, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> delete(String endpoint, com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("DELETE with ResponseConfiguration not yet implemented. Use delete(endpoint, Class<R>) instead.");
    }

    @Override
    public <R> Mono<R> patch(String endpoint, Object request,
                           com.firefly.common.domain.client.config.ResponseConfiguration<R> responseConfig) {
        throw new UnsupportedOperationException("PATCH with ResponseConfiguration not yet implemented. Use patch(endpoint, request, Class<R>) instead.");
    }

}