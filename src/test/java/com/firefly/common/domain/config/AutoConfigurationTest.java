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

import com.firefly.common.cqrs.command.CommandBus;
import com.firefly.common.cqrs.config.CqrsAutoConfiguration;
import com.firefly.common.cqrs.config.CqrsProperties;
import com.firefly.common.cqrs.query.QueryBus;
import com.firefly.common.cqrs.tracing.CorrelationContext;
import com.firefly.common.domain.stepevents.StepEventPublisherBridge;
import com.firefly.common.eda.publisher.EventPublisher;
import com.firefly.common.eda.publisher.EventPublisherFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Comprehensive test suite for Auto-Configuration classes in the Firefly Common Domain library.
 * Tests cover conditional bean creation, property binding, and integration scenarios
 * for banking domain microservices auto-configuration.
 */
@DisplayName("Common Domain Auto-Configuration - Component Initialization")
class AutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            CqrsAutoConfiguration.class,
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
    @DisplayName("Should auto-configure StepEvents bridge when EventPublisherFactory is available")
    void shouldAutoConfigureStepEventsBridgeWhenEnabled() {
        // Create mock EventPublisher and EventPublisherFactory
        EventPublisher mockPublisher = mock(EventPublisher.class);
        EventPublisherFactory mockFactory = mock(EventPublisherFactory.class);
        when(mockFactory.getDefaultPublisher()).thenReturn(mockPublisher);

        contextRunner
            .withBean(EventPublisherFactory.class, () -> mockFactory)
            .withPropertyValues(
                "firefly.stepevents.enabled=true",
                "firefly.stepevents.topic=step-events"
            )
            .run(context -> {
                // Then: StepEvents bridge should be available
                assertThat(context).hasSingleBean(StepEventPublisherBridge.class);
                assertThat(context).hasSingleBean(EventPublisherFactory.class);

                // Verify bridge is properly configured
                StepEventPublisherBridge bridge = context.getBean(StepEventPublisherBridge.class);
                assertThat(bridge).isNotNull();
            });
    }

    @Test
    @DisplayName("Should not auto-configure StepEvents bridge when EventPublisherFactory is missing")
    void shouldNotAutoConfigureStepEventsBridgeWhenEdaMissing() {
        contextRunner
            .withPropertyValues(
                "firefly.stepevents.enabled=true",
                "firefly.stepevents.topic=step-events"
            )
            .run(context -> {
                // Then: StepEvents bridge should not be available without EDA
                assertThat(context).doesNotHaveBean(StepEventPublisherBridge.class);
            });
    }


    @Test
    @DisplayName("Should configure complete banking microservice stack")
    void shouldConfigureCompleteBankingMicroserviceStack() {
        // Create mock EventPublisher and EventPublisherFactory
        EventPublisher mockPublisher = mock(EventPublisher.class);
        EventPublisherFactory mockFactory = mock(EventPublisherFactory.class);
        when(mockFactory.getDefaultPublisher()).thenReturn(mockPublisher);

        contextRunner
            .withBean(EventPublisherFactory.class, () -> mockFactory)
            .withPropertyValues(
                // Enable all components
                "firefly.cqrs.enabled=true",
                "firefly.stepevents.enabled=true",
                "firefly.stepevents.topic=step-events",

                // Banking-specific configuration
                "spring.application.name=banking-service"
            )
            .run(context -> {
                // Then: All banking microservice components should be available

                // CQRS Framework
                assertThat(context).hasSingleBean(CommandBus.class);
                assertThat(context).hasSingleBean(QueryBus.class);

                // EDA Framework (mocked)
                assertThat(context).hasSingleBean(EventPublisherFactory.class);

                // StepEvents Bridge
                assertThat(context).hasSingleBean(StepEventPublisherBridge.class);

                // Correlation Context
                assertThat(context).hasSingleBean(CorrelationContext.class);

                // Verify integration between components
                StepEventPublisherBridge bridge = context.getBean(StepEventPublisherBridge.class);
                EventPublisherFactory factory = context.getBean(EventPublisherFactory.class);
                assertThat(bridge).isNotNull();
                assertThat(factory).isNotNull();
            });
    }

    @Test
    @DisplayName("Should bind configuration properties correctly")
    void shouldBindConfigurationPropertiesCorrectly() {
        // Create mock EventPublisher and EventPublisherFactory
        EventPublisher mockPublisher = mock(EventPublisher.class);
        EventPublisherFactory mockFactory = mock(EventPublisherFactory.class);
        when(mockFactory.getDefaultPublisher()).thenReturn(mockPublisher);

        contextRunner
            .withBean(EventPublisherFactory.class, () -> mockFactory)
            .withPropertyValues(
                "firefly.cqrs.enabled=true",
                "firefly.stepevents.enabled=true",
                "firefly.stepevents.topic=custom-step-events"
            )
            .run(context -> {
                // Then: Configuration properties should be properly bound
                assertThat(context).hasSingleBean(CqrsProperties.class);
                assertThat(context).hasSingleBean(StepEventsProperties.class);

                // Verify property values
                StepEventsProperties stepProps = context.getBean(StepEventsProperties.class);
                assertThat(stepProps.isEnabled()).isTrue();
                assertThat(stepProps.getTopic()).isEqualTo("custom-step-events");
            });
    }

    @Test
    @DisplayName("Should support custom bean overrides")
    void shouldSupportCustomBeanOverrides() {
        contextRunner
            .withPropertyValues(
                "firefly.cqrs.enabled=true"
            )
            .withBean("customCommandBus", CommandBus.class, () -> {
                // Custom command bus implementation (mock for testing)
                return mock(CommandBus.class);
            })
            .run(context -> {
                // Then: Custom bean should be used instead of auto-configured one
                assertThat(context).hasBean("customCommandBus");

                CommandBus customCommandBus = context.getBean("customCommandBus", CommandBus.class);
                assertThat(customCommandBus).isNotNull();
            });
    }
}
