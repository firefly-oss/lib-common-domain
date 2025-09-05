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

import com.firefly.common.domain.client.rest.RestServiceClient;
import com.firefly.common.domain.tracing.CorrelationContext;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Builder for creating REST ServiceClient instances with fluent API.
 * Provides comprehensive configuration options for REST service communication
 * including connection pooling, timeouts, circuit breakers, and retry policies.
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public class RestServiceClientBuilder implements ServiceClientBuilder<RestServiceClient, RestServiceClientBuilder> {

    private String serviceName;
    private String baseUrl;
    private WebClient webClient;
    private CircuitBreaker circuitBreaker;
    private Retry retry;
    private CorrelationContext correlationContext;
    private Duration timeout = Duration.ofSeconds(30);
    
    // Connection pool settings
    private int maxConnections = 100;
    private Duration maxIdleTime = Duration.ofMinutes(5);
    private Duration maxLifeTime = Duration.ofMinutes(30);
    private Duration pendingAcquireTimeout = Duration.ofSeconds(10);
    private int maxInMemorySize = 1024 * 1024; // 1MB
    private Duration connectTimeout = Duration.ofSeconds(10);

    /**
     * Creates a new RestServiceClientBuilder instance.
     *
     * @return a new builder instance
     */
    public static RestServiceClientBuilder create() {
        return new RestServiceClientBuilder();
    }

    @Override
    public RestServiceClientBuilder serviceName(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }
        this.serviceName = serviceName.trim();
        return this;
    }

    @Override
    public RestServiceClientBuilder baseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL cannot be null or empty");
        }
        this.baseUrl = baseUrl.trim();
        return this;
    }

    /**
     * Sets a pre-configured WebClient instance.
     *
     * @param webClient the WebClient instance
     * @return this builder instance for method chaining
     */
    public RestServiceClientBuilder webClient(WebClient webClient) {
        this.webClient = webClient;
        return this;
    }

    @Override
    public RestServiceClientBuilder circuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
        return this;
    }

    @Override
    public RestServiceClientBuilder retry(Retry retry) {
        this.retry = retry;
        return this;
    }

    @Override
    public RestServiceClientBuilder correlationContext(CorrelationContext correlationContext) {
        this.correlationContext = correlationContext;
        return this;
    }

    @Override
    public RestServiceClientBuilder timeout(Duration timeout) {
        if (timeout == null || timeout.isNegative()) {
            throw new IllegalArgumentException("Timeout cannot be null or negative");
        }
        this.timeout = timeout;
        return this;
    }

    /**
     * Sets the maximum number of connections in the connection pool.
     *
     * @param maxConnections the maximum number of connections
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if maxConnections is less than 1
     */
    public RestServiceClientBuilder maxConnections(int maxConnections) {
        if (maxConnections < 1) {
            throw new IllegalArgumentException("Max connections must be at least 1");
        }
        this.maxConnections = maxConnections;
        return this;
    }

    /**
     * Sets the maximum idle time for connections in the pool.
     *
     * @param maxIdleTime the maximum idle time
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if maxIdleTime is null or negative
     */
    public RestServiceClientBuilder maxIdleTime(Duration maxIdleTime) {
        if (maxIdleTime == null || maxIdleTime.isNegative()) {
            throw new IllegalArgumentException("Max idle time cannot be null or negative");
        }
        this.maxIdleTime = maxIdleTime;
        return this;
    }

    /**
     * Sets the maximum lifetime for connections in the pool.
     *
     * @param maxLifeTime the maximum lifetime
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if maxLifeTime is null or negative
     */
    public RestServiceClientBuilder maxLifeTime(Duration maxLifeTime) {
        if (maxLifeTime == null || maxLifeTime.isNegative()) {
            throw new IllegalArgumentException("Max life time cannot be null or negative");
        }
        this.maxLifeTime = maxLifeTime;
        return this;
    }

    /**
     * Sets the timeout for acquiring a connection from the pool.
     *
     * @param pendingAcquireTimeout the pending acquire timeout
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if pendingAcquireTimeout is null or negative
     */
    public RestServiceClientBuilder pendingAcquireTimeout(Duration pendingAcquireTimeout) {
        if (pendingAcquireTimeout == null || pendingAcquireTimeout.isNegative()) {
            throw new IllegalArgumentException("Pending acquire timeout cannot be null or negative");
        }
        this.pendingAcquireTimeout = pendingAcquireTimeout;
        return this;
    }

    /**
     * Sets the maximum in-memory size for response bodies.
     *
     * @param maxInMemorySize the maximum in-memory size in bytes
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if maxInMemorySize is less than 1
     */
    public RestServiceClientBuilder maxInMemorySize(int maxInMemorySize) {
        if (maxInMemorySize < 1) {
            throw new IllegalArgumentException("Max in-memory size must be at least 1 byte");
        }
        this.maxInMemorySize = maxInMemorySize;
        return this;
    }

    /**
     * Sets the connection timeout.
     *
     * @param connectTimeout the connection timeout
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if connectTimeout is null or negative
     */
    public RestServiceClientBuilder connectTimeout(Duration connectTimeout) {
        if (connectTimeout == null || connectTimeout.isNegative()) {
            throw new IllegalArgumentException("Connect timeout cannot be null or negative");
        }
        this.connectTimeout = connectTimeout;
        return this;
    }

    @Override
    public RestServiceClient build() {
        validateRequiredFields();
        
        WebClient finalWebClient = webClient != null ? webClient : createDefaultWebClient();
        CircuitBreaker finalCircuitBreaker = circuitBreaker != null ? circuitBreaker : createDefaultCircuitBreaker();
        Retry finalRetry = retry != null ? retry : createDefaultRetry();
        
        log.info("Building RestServiceClient for service '{}' with baseUrl '{}'", serviceName, baseUrl);
        
        return new RestServiceClient(
            serviceName,
            baseUrl,
            finalWebClient,
            finalCircuitBreaker,
            finalRetry,
            correlationContext
        );
    }

    private void validateRequiredFields() {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalStateException("Service name is required");
        }
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalStateException("Base URL is required");
        }
    }

    private WebClient createDefaultWebClient() {
        log.debug("Creating default WebClient for service '{}'", serviceName);
        
        ConnectionProvider connectionProvider = ConnectionProvider.builder(serviceName + "-connection-pool")
                .maxConnections(maxConnections)
                .maxIdleTime(maxIdleTime)
                .maxLifeTime(maxLifeTime)
                .pendingAcquireTimeout(pendingAcquireTimeout)
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
                .responseTimeout(timeout)
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(maxInMemorySize);
                })
                .build();
    }

    private CircuitBreaker createDefaultCircuitBreaker() {
        log.debug("Creating default CircuitBreaker for service '{}'", serviceName);
        
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        return CircuitBreakerRegistry.of(config).circuitBreaker(serviceName);
    }

    private Retry createDefaultRetry() {
        log.debug("Creating default Retry for service '{}'", serviceName);
        
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryOnResult(result -> false)
                .retryExceptions(
                    java.net.ConnectException.class,
                    java.net.SocketTimeoutException.class,
                    java.util.concurrent.TimeoutException.class
                )
                .build();

        return RetryRegistry.of(config).retry(serviceName);
    }
}
