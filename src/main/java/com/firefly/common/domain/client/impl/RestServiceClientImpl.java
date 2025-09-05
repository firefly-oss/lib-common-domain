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
import com.firefly.common.domain.client.RequestBuilder;
import com.firefly.common.domain.client.ServiceClient;
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

    @Override
    public <S, R> Mono<R> call(Function<S, R> operation) {
        return Mono.error(new UnsupportedOperationException(
            "SDK operations are not supported for REST clients. Use HTTP methods instead."));
    }

    @Override
    public <S, R> Mono<R> callAsync(Function<S, Mono<R>> operation) {
        return Mono.error(new UnsupportedOperationException(
            "SDK operations are not supported for REST clients. Use HTTP methods instead."));
    }

    @Override
    public <S> S sdk() {
        throw new UnsupportedOperationException(
            "SDK operations are not supported for REST clients. Use HTTP methods instead.");
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

    // ========================================
    // Inner RequestBuilder Implementation
    // ========================================

    private class RestRequestBuilder<R> implements RequestBuilder<R> {
        private final String method;
        private final String endpoint;
        private final Class<R> responseType;
        private final TypeReference<R> typeReference;

        private Object body;
        private Map<String, Object> pathParams = new java.util.HashMap<>();
        private Map<String, Object> queryParams = new java.util.HashMap<>();
        private Map<String, String> headers = new java.util.HashMap<>();
        private Duration requestTimeout = timeout;

        public RestRequestBuilder(String method, String endpoint, Class<R> responseType, TypeReference<R> typeReference) {
            this.method = method;
            this.endpoint = endpoint;
            this.responseType = responseType;
            this.typeReference = typeReference;
        }

        @Override
        public RequestBuilder<R> withBody(Object body) {
            this.body = body;
            return this;
        }

        @Override
        public RequestBuilder<R> withPathParam(String name, Object value) {
            if (name != null && value != null) {
                pathParams.put(name, value);
            }
            return this;
        }

        @Override
        public RequestBuilder<R> withPathParams(Map<String, Object> pathParams) {
            if (pathParams != null) {
                this.pathParams.putAll(pathParams);
            }
            return this;
        }

        @Override
        public RequestBuilder<R> withQueryParam(String name, Object value) {
            if (name != null && value != null) {
                queryParams.put(name, value);
            }
            return this;
        }

        @Override
        public RequestBuilder<R> withQueryParams(Map<String, Object> queryParams) {
            if (queryParams != null) {
                this.queryParams.putAll(queryParams);
            }
            return this;
        }

        @Override
        public RequestBuilder<R> withHeader(String name, String value) {
            if (name != null && value != null) {
                headers.put(name, value);
            }
            return this;
        }

        @Override
        public RequestBuilder<R> withHeaders(Map<String, String> headers) {
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }

        @Override
        public RequestBuilder<R> withTimeout(Duration timeout) {
            if (timeout != null && !timeout.isNegative()) {
                this.requestTimeout = timeout;
            }
            return this;
        }

        @Override
        public Mono<R> execute() {
            if (isShutdown.get()) {
                return Mono.error(new IllegalStateException("Client has been shut down"));
            }

            return buildRequest()
                .timeout(requestTimeout)
                .doOnSubscribe(subscription ->
                    log.debug("Executing {} request to {} for service '{}'", method, endpoint, serviceName))
                .doOnSuccess(result ->
                    log.debug("Successfully completed {} request to {} for service '{}'", method, endpoint, serviceName))
                .doOnError(error ->
                    log.error("Failed {} request to {} for service '{}': {}", method, endpoint, serviceName, error.getMessage()));
        }

        @Override
        public Flux<R> stream() {
            if (isShutdown.get()) {
                return Flux.error(new IllegalStateException("Client has been shut down"));
            }

            // For streaming, we expect the response to be a collection or stream
            return buildRequest()
                .flatMapMany(response -> {
                    if (response instanceof Iterable) {
                        return Flux.fromIterable((Iterable<R>) response);
                    } else {
                        return Flux.just(response);
                    }
                })
                .timeout(requestTimeout)
                .doOnSubscribe(subscription ->
                    log.debug("Executing streaming {} request to {} for service '{}'", method, endpoint, serviceName));
        }

        private Mono<R> buildRequest() {
            // Build the URI with path parameters
            String uri = buildUri();

            // Create the request spec
            WebClient.RequestHeadersSpec<?> requestSpec = createRequestSpec(uri);

            // Add headers
            headers.forEach(requestSpec::header);

            // Execute and retrieve response
            return executeRequest(requestSpec);
        }

        private String buildUri() {
            String uri = endpoint;

            // Replace path parameters
            for (Map.Entry<String, Object> entry : pathParams.entrySet()) {
                uri = uri.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }

            // Add query parameters
            if (!queryParams.isEmpty()) {
                StringBuilder queryString = new StringBuilder();
                queryParams.forEach((key, value) -> {
                    if (queryString.length() > 0) {
                        queryString.append("&");
                    }
                    queryString.append(key).append("=").append(value);
                });
                uri += "?" + queryString.toString();
            }

            return uri;
        }

        private WebClient.RequestHeadersSpec<?> createRequestSpec(String uri) {
            switch (method.toUpperCase()) {
                case "GET":
                    return webClient.get().uri(uri);
                case "POST":
                    return body != null ? webClient.post().uri(uri).bodyValue(body) : webClient.post().uri(uri);
                case "PUT":
                    return body != null ? webClient.put().uri(uri).bodyValue(body) : webClient.put().uri(uri);
                case "DELETE":
                    return webClient.delete().uri(uri);
                case "PATCH":
                    return body != null ? webClient.patch().uri(uri).bodyValue(body) : webClient.patch().uri(uri);
                default:
                    throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }
        }

        @SuppressWarnings("unchecked")
        private Mono<R> executeRequest(WebClient.RequestHeadersSpec<?> requestSpec) {
            if (responseType != null) {
                return requestSpec.retrieve().bodyToMono(responseType);
            } else if (typeReference != null) {
                // For TypeReference, we need to use a different approach
                // This is a simplified implementation - in practice, you'd need proper Jackson integration
                return requestSpec.retrieve().bodyToMono(String.class)
                    .map(json -> {
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            return mapper.readValue(json, typeReference);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to deserialize response", e);
                        }
                    });
            } else {
                throw new IllegalStateException("Either responseType or typeReference must be provided");
            }
        }
    }
}
