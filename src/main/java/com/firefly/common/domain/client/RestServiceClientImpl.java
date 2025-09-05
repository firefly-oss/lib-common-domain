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
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * REST implementation of ServiceClient using WebClient.
 * 
 * <p>This implementation provides a simplified, unified interface for REST service
 * communication while maintaining all the power and flexibility of WebClient under the hood.
 *
 * <p>Key features:
 * <ul>
 *   <li>Fluent request builder API</li>
 *   <li>Built-in circuit breaker and retry mechanisms</li>
 *   <li>Automatic error handling and mapping</li>
 *   <li>Support for streaming responses</li>
 *   <li>Path parameter substitution</li>
 *   <li>Query parameter handling</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc
 * @since 2.0.0
 */
@Slf4j
public class RestServiceClientImpl implements ServiceClient {

    private final String serviceName;
    private final String baseUrl;
    private final Duration timeout;
    private final int maxConnections;
    private final Map<String, String> defaultHeaders;
    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    /**
     * Creates a new REST service client implementation.
     */
    public RestServiceClientImpl(String serviceName,
                                String baseUrl,
                                Duration timeout,
                                int maxConnections,
                                Map<String, String> defaultHeaders,
                                WebClient webClient,
                                CircuitBreaker circuitBreaker,
                                Retry retry) {
        this.serviceName = serviceName;
        this.baseUrl = baseUrl;
        this.timeout = timeout;
        this.maxConnections = maxConnections;
        this.defaultHeaders = Map.copyOf(defaultHeaders);
        this.webClient = webClient != null ? webClient : createDefaultWebClient();
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        
        log.info("Initialized REST service client for service '{}' with base URL '{}'", 
                serviceName, baseUrl);
    }

    // ========================================
    // Request Builder Methods
    // ========================================

    @Override
    public <R> RequestBuilder<R> get(String endpoint, Class<R> responseType) {
        return new RestRequestBuilder<>("GET", endpoint, responseType, null);
    }

    @Override
    public <R> RequestBuilder<R> get(String endpoint, TypeReference<R> typeReference) {
        return new RestRequestBuilder<>("GET", endpoint, null, typeReference);
    }

    @Override
    public <R> RequestBuilder<R> post(String endpoint, Class<R> responseType) {
        return new RestRequestBuilder<>("POST", endpoint, responseType, null);
    }

    @Override
    public <R> RequestBuilder<R> post(String endpoint, TypeReference<R> typeReference) {
        return new RestRequestBuilder<>("POST", endpoint, null, typeReference);
    }

    @Override
    public <R> RequestBuilder<R> put(String endpoint, Class<R> responseType) {
        return new RestRequestBuilder<>("PUT", endpoint, responseType, null);
    }

    @Override
    public <R> RequestBuilder<R> put(String endpoint, TypeReference<R> typeReference) {
        return new RestRequestBuilder<>("PUT", endpoint, null, typeReference);
    }

    @Override
    public <R> RequestBuilder<R> delete(String endpoint, Class<R> responseType) {
        return new RestRequestBuilder<>("DELETE", endpoint, responseType, null);
    }

    @Override
    public <R> RequestBuilder<R> delete(String endpoint, TypeReference<R> typeReference) {
        return new RestRequestBuilder<>("DELETE", endpoint, null, typeReference);
    }

    @Override
    public <R> RequestBuilder<R> patch(String endpoint, Class<R> responseType) {
        return new RestRequestBuilder<>("PATCH", endpoint, responseType, null);
    }

    @Override
    public <R> RequestBuilder<R> patch(String endpoint, TypeReference<R> typeReference) {
        return new RestRequestBuilder<>("PATCH", endpoint, null, typeReference);
    }

    // ========================================
    // SDK-specific Methods (Not Supported)
    // ========================================

    @Override
    public <R> Mono<R> execute(Function<Object, R> operation) {
        return Mono.error(new UnsupportedOperationException(
            "SDK operations are not supported for REST clients. Use HTTP methods instead."));
    }

    @Override
    public <R> Mono<R> executeAsync(Function<Object, Mono<R>> operation) {
        return Mono.error(new UnsupportedOperationException(
            "SDK operations are not supported for REST clients. Use HTTP methods instead."));
    }

    // ========================================
    // Streaming Methods
    // ========================================

    @Override
    public <R> Flux<R> stream(String endpoint, Class<R> responseType) {
        return get(endpoint, responseType).stream();
    }

    @Override
    public <R> Flux<R> stream(String endpoint, TypeReference<R> typeReference) {
        return get(endpoint, typeReference).stream();
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
        return baseUrl;
    }

    @Override
    public boolean isReady() {
        return !isShutdown.get();
    }

    @Override
    public Mono<Void> healthCheck() {
        if (isShutdown.get()) {
            return Mono.error(new IllegalStateException("Client has been shut down"));
        }
        
        return webClient.get()
            .uri(baseUrl + "/health")
            .retrieve()
            .toBodilessEntity()
            .then()
            .timeout(Duration.ofSeconds(5))
            .onErrorMap(throwable -> new RuntimeException("Health check failed for service: " + serviceName, throwable));
    }

    @Override
    public ClientType getClientType() {
        return ClientType.REST;
    }

    @Override
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            log.info("Shutting down REST service client for service '{}'", serviceName);
            // WebClient doesn't require explicit shutdown, but we mark as shutdown
        }
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    private WebClient createDefaultWebClient() {
        WebClient.Builder builder = WebClient.builder()
            .baseUrl(baseUrl);
        
        // Add default headers
        defaultHeaders.forEach(builder::defaultHeader);
        
        return builder.build();
    }
}
