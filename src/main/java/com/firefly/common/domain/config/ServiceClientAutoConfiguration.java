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

package com.firefly.common.domain.config;

import com.firefly.common.domain.tracing.CorrelationContext;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * Auto-configuration for ServiceClient framework components.
 * Provides automatic setup of WebClient, gRPC channels, circuit breakers, and retry mechanisms.
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(ServiceClientProperties.class)
@ConditionalOnProperty(prefix = "firefly.service-client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ServiceClientAutoConfiguration {

    private final ServiceClientProperties properties;

    public ServiceClientAutoConfiguration(ServiceClientProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public WebClient.Builder webClientBuilder() {
        log.info("Configuring WebClient builder for REST service clients");
        
        // Configure connection pool
        ConnectionProvider connectionProvider = ConnectionProvider.builder("rest-service-client")
                .maxConnections(properties.getRest().getMaxConnections())
                .maxIdleTime(properties.getRest().getMaxIdleTime())
                .maxLifeTime(properties.getRest().getMaxLifeTime())
                .pendingAcquireTimeout(properties.getRest().getPendingAcquireTimeout())
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .responseTimeout(properties.getRest().getResponseTimeout());

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(properties.getRest().getMaxInMemorySize());
                });
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        log.info("Configuring circuit breaker registry");
        
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.getCircuitBreaker().getFailureRateThreshold())
                .waitDurationInOpenState(properties.getCircuitBreaker().getWaitDurationInOpenState())
                .slidingWindowSize(properties.getCircuitBreaker().getSlidingWindowSize())
                .minimumNumberOfCalls(properties.getCircuitBreaker().getMinimumNumberOfCalls())
                .slowCallRateThreshold(properties.getCircuitBreaker().getSlowCallRateThreshold())
                .slowCallDurationThreshold(properties.getCircuitBreaker().getSlowCallDurationThreshold())
                .permittedNumberOfCallsInHalfOpenState(properties.getCircuitBreaker().getPermittedNumberOfCallsInHalfOpenState())
                .maxWaitDurationInHalfOpenState(properties.getCircuitBreaker().getMaxWaitDurationInHalfOpenState())
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public RetryRegistry retryRegistry() {
        log.info("Configuring retry registry");
        
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(properties.getRetry().getMaxAttempts())
                .waitDuration(properties.getRetry().getWaitDuration())
                .retryOnResult(result -> false) // Don't retry on successful results
                .retryExceptions(
                    java.net.ConnectException.class,
                    java.net.SocketTimeoutException.class,
                    java.util.concurrent.TimeoutException.class
                )
                .build();

        return RetryRegistry.of(config);
    }

    @Bean
    @ConditionalOnMissingBean(name = "defaultCircuitBreaker")
    public CircuitBreaker defaultCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker("default");
    }

    @Bean
    @ConditionalOnMissingBean(name = "defaultRetry")
    public Retry defaultRetry(RetryRegistry retryRegistry) {
        return retryRegistry.retry("default");
    }

    /**
     * Factory method to create circuit breakers for specific services.
     * 
     * @param serviceName the service name
     * @param circuitBreakerRegistry the circuit breaker registry
     * @return a circuit breaker for the service
     */
    public static CircuitBreaker createCircuitBreaker(String serviceName, CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker(serviceName);
    }

    /**
     * Factory method to create retry instances for specific services.
     * 
     * @param serviceName the service name
     * @param retryRegistry the retry registry
     * @return a retry instance for the service
     */
    public static Retry createRetry(String serviceName, RetryRegistry retryRegistry) {
        return retryRegistry.retry(serviceName);
    }
}