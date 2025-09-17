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

import com.firefly.common.domain.cqrs.command.CommandBus;
import com.firefly.common.domain.cqrs.query.QueryBus;
import com.firefly.common.domain.events.outbound.DomainEventPublisher;
import com.firefly.common.domain.events.properties.DomainEventsProperties;
import com.firefly.common.domain.resilience.CircuitBreakerManager;
import com.firefly.common.domain.stepevents.StepEventPublisherBridge;
import com.firefly.common.domain.tracing.CorrelationContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Comprehensive test suite for Auto-Configuration classes in the Firefly Common Domain library.
 * Tests cover conditional bean creation, property binding, and integration scenarios
 * for banking domain microservices auto-configuration.
 */
@DisplayName("Banking Domain Auto-Configuration - Component Initialization")
class AutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            CqrsAutoConfiguration.class,
            DomainEventsAutoConfiguration.class,
            ServiceClientAutoConfiguration.class,
            StepBridgeConfiguration.class
        ))
        .withBean(CorrelationContext.class);

    @Test
    @DisplayName("Should auto-configure CQRS framework when enabled")
    void shouldAutoConfigureCqrsFrameworkWhenEnabled() {
        contextRunner
            .withPropertyValues(
                "firefly.cqrs.enabled=true"
            )
            .run(context -> {
                // Then: CQRS components should be available
                assertThat(context).hasSingleBean(CommandBus.class);
                assertThat(context).hasSingleBean(QueryBus.class);
                assertThat(context).hasSingleBean(CacheManager.class);
                assertThat(context).hasSingleBean(CorrelationContext.class);
                
                // Verify beans are properly configured
                CommandBus commandBus = context.getBean(CommandBus.class);
                assertThat(commandBus).isNotNull();
                
                QueryBus queryBus = context.getBean(QueryBus.class);
                assertThat(queryBus).isNotNull();
            });
    }

    @Test
    @DisplayName("Should not auto-configure CQRS framework when disabled")
    void shouldNotAutoConfigureCqrsFrameworkWhenDisabled() {
        contextRunner
            .withPropertyValues(
                "firefly.cqrs.enabled=false"
            )
            .run(context -> {
                // Then: CQRS components should not be available
                assertThat(context).doesNotHaveBean(CommandBus.class);
                assertThat(context).doesNotHaveBean(QueryBus.class);
            });
    }

    @Test
    @DisplayName("Should auto-configure Domain Events with APPLICATION_EVENT adapter")
    void shouldAutoConfigureDomainEventsWithApplicationEventAdapter() {
        contextRunner
            .withBean(ApplicationEventPublisher.class, () -> mock(ApplicationEventPublisher.class))
            .withPropertyValues(
                "firefly.events.enabled=true",
                "firefly.events.adapter=APPLICATION_EVENT"
            )
            .run(context -> {
                // Then: Domain Events components should be available
                assertThat(context).hasSingleBean(DomainEventPublisher.class);
                assertThat(context).hasSingleBean(ApplicationEventPublisher.class);

                // Verify publisher is properly configured
                DomainEventPublisher publisher = context.getBean(DomainEventPublisher.class);
                assertThat(publisher).isNotNull();
                assertThat(publisher.getClass().getSimpleName())
                    .isEqualTo("ApplicationEventDomainEventPublisher");
            });
    }

    @Test
    @DisplayName("Should auto-configure Domain Events with AUTO adapter selection")
    void shouldAutoConfigureDomainEventsWithAutoAdapterSelection() {
        contextRunner
            .withPropertyValues(
                "firefly.events.enabled=true",
                "firefly.events.adapter=AUTO"
            )
            .run(context -> {
                // Then: Domain Events should auto-detect available adapter
                assertThat(context).hasSingleBean(DomainEventPublisher.class);

                DomainEventPublisher publisher = context.getBean(DomainEventPublisher.class);
                assertThat(publisher).isNotNull();
                // In test environment with RabbitMQ dependencies, RabbitMQ is auto-detected
                assertThat(publisher.getClass().getSimpleName())
                    .isEqualTo("RabbitMqDomainEventPublisher");
            });
    }

    @Test
    @DisplayName("Should auto-configure StepEvents bridge when enabled")
    void shouldAutoConfigureStepEventsBridgeWhenEnabled() {
        contextRunner
            .withPropertyValues(
                "firefly.stepevents.enabled=true",
                "firefly.events.enabled=true",
                "firefly.events.adapter=APPLICATION_EVENT",
                "domain.topic=banking-step-events"
            )
            .run(context -> {
                // Then: StepEvents bridge should be available
                assertThat(context).hasSingleBean(StepEventPublisherBridge.class);
                assertThat(context).hasSingleBean(DomainEventPublisher.class);
                
                // Verify bridge is properly configured
                StepEventPublisherBridge bridge = context.getBean(StepEventPublisherBridge.class);
                assertThat(bridge).isNotNull();
            });
    }

    @Test
    @DisplayName("Should auto-configure ServiceClient framework when enabled")
    void shouldAutoConfigureServiceClientFrameworkWhenEnabled() {
        contextRunner
            .withPropertyValues(
                "firefly.service-client.enabled=true"
            )
            .run(context -> {
                // Then: ServiceClient components should be available
                assertThat(context).hasSingleBean(WebClient.Builder.class);
                assertThat(context).hasSingleBean(CircuitBreakerManager.class);

                // Verify components are properly configured
                WebClient.Builder webClientBuilder = context.getBean(WebClient.Builder.class);
                assertThat(webClientBuilder).isNotNull();

                CircuitBreakerManager circuitBreakerManager = context.getBean(CircuitBreakerManager.class);
                assertThat(circuitBreakerManager).isNotNull();
            });
    }

    @Test
    @DisplayName("Should not auto-configure ServiceClient framework when disabled")
    void shouldNotAutoConfigureServiceClientFrameworkWhenDisabled() {
        contextRunner
            .withPropertyValues(
                "firefly.service-client.enabled=false"
            )
            .run(context -> {
                // Then: ServiceClient components should not be available
                assertThat(context).doesNotHaveBean(WebClient.Builder.class);
                assertThat(context).doesNotHaveBean(CircuitBreakerManager.class);
            });
    }

    @Test
    @DisplayName("Should configure complete banking microservice stack")
    void shouldConfigureCompleteBankingMicroserviceStack() {
        contextRunner
            .withPropertyValues(
                // Enable all components
                "firefly.cqrs.enabled=true",
                "firefly.events.enabled=true",
                "firefly.events.adapter=APPLICATION_EVENT",
                "firefly.stepevents.enabled=true",
                "firefly.service-client.enabled=true",
                
                // Banking-specific configuration
                "domain.topic=banking-domain-events",
                "spring.application.name=banking-service",
                
                // CQRS configuration
                "firefly.cqrs.query.cache.enabled=true",
                "firefly.cqrs.query.cache.default-ttl=300",
                
                // ServiceClient configuration
                "firefly.service-client.rest.max-connections=100",
                "firefly.service-client.rest.response-timeout=30s",
                "firefly.service-client.circuit-breaker.failure-rate-threshold=50",
                "firefly.service-client.retry.max-attempts=3"
            )
            .run(context -> {
                // Then: All banking microservice components should be available
                
                // CQRS Framework
                assertThat(context).hasSingleBean(CommandBus.class);
                assertThat(context).hasSingleBean(QueryBus.class);
                assertThat(context).hasBean("cqrsCacheManager");
                
                // Domain Events
                assertThat(context).hasSingleBean(DomainEventPublisher.class);
                
                // StepEvents Bridge
                assertThat(context).hasSingleBean(StepEventPublisherBridge.class);
                
                // ServiceClient Framework
                assertThat(context).hasSingleBean(WebClient.Builder.class);
                assertThat(context).hasSingleBean(CircuitBreakerManager.class);
                
                // Correlation Context
                assertThat(context).hasSingleBean(CorrelationContext.class);
                
                // Verify integration between components
                StepEventPublisherBridge bridge = context.getBean(StepEventPublisherBridge.class);
                DomainEventPublisher publisher = context.getBean(DomainEventPublisher.class);
                assertThat(bridge).isNotNull();
                assertThat(publisher).isNotNull();
            });
    }

    @Test
    @DisplayName("Should fail when explicitly configured adapter is not available")
    void shouldFailWhenExplicitlyConfiguredAdapterIsNotAvailable() {
        contextRunner
            .withPropertyValues(
                "firefly.events.enabled=true",
                "firefly.events.adapter=KAFKA" // Kafka not available in test
            )
            .run(context -> {
                // Then: Should fail with appropriate error message
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasMessageContaining("Kafka adapter selected but KafkaTemplate bean was not found");
            });
    }

    @Test
    @DisplayName("Should bind configuration properties correctly")
    void shouldBindConfigurationPropertiesCorrectly() {
        contextRunner
            .withPropertyValues(
                "firefly.cqrs.enabled=true",
                "firefly.events.enabled=true",
                "firefly.events.adapter=APPLICATION_EVENT",
                "firefly.service-client.enabled=true",
                "firefly.service-client.rest.max-connections=200",
                "firefly.service-client.rest.response-timeout=45s",
                "firefly.service-client.circuit-breaker.failure-rate-threshold=60",
                "firefly.service-client.retry.max-attempts=5"
            )
            .run(context -> {
                // Then: Configuration properties should be properly bound
                assertThat(context).hasSingleBean(CqrsProperties.class);
                assertThat(context).hasSingleBean(DomainEventsProperties.class);
                assertThat(context).hasSingleBean(ServiceClientProperties.class);
                
                // Verify property values
                DomainEventsProperties eventsProps = context.getBean(DomainEventsProperties.class);
                assertThat(eventsProps.isEnabled()).isTrue();
                assertThat(eventsProps.getAdapter()).isEqualTo(DomainEventsProperties.Adapter.APPLICATION_EVENT);
                
                ServiceClientProperties clientProps = context.getBean(ServiceClientProperties.class);
                assertThat(clientProps.isEnabled()).isTrue();
                assertThat(clientProps.getRest().getMaxConnections()).isEqualTo(200);
                assertThat(clientProps.getCircuitBreaker().getFailureRateThreshold()).isEqualTo(60);
                assertThat(clientProps.getRetry().getMaxAttempts()).isEqualTo(5);
            });
    }

    @Test
    @DisplayName("Should support custom bean overrides")
    void shouldSupportCustomBeanOverrides() {
        contextRunner
            .withPropertyValues(
                "firefly.cqrs.enabled=true"
            )
            .withBean("cqrsCacheManager", CacheManager.class, () -> {
                // Custom cache manager implementation
                return new org.springframework.cache.concurrent.ConcurrentMapCacheManager("custom-cache");
            })
            .run(context -> {
                // Then: Custom bean should be used instead of auto-configured one
                assertThat(context).hasBean("cqrsCacheManager");

                CacheManager cacheManager = context.getBean("cqrsCacheManager", CacheManager.class);
                assertThat(cacheManager).isInstanceOf(org.springframework.cache.concurrent.ConcurrentMapCacheManager.class);
                assertThat(cacheManager.getCacheNames()).contains("custom-cache");
            });
    }
}
