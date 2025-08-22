package com.catalis.common.domain.actuator.health;

import com.catalis.common.domain.events.properties.DomainEventsProperties;
import com.catalis.common.domain.stepevents.StepEventAdapterUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class RabbitDomainEventsHealthIndicatorTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private Connection connection;

    private DomainEventsProperties properties;
    private RabbitDomainEventsHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        properties = new DomainEventsProperties();
        properties.setEnabled(true);
        properties.setAdapter(DomainEventsProperties.Adapter.RABBIT);
        properties.getRabbit().setTemplateBeanName("rabbitTemplate");
        properties.getRabbit().setExchange("test-exchange");
        properties.getRabbit().setRoutingKey("test.routing.key");
        healthIndicator = new RabbitDomainEventsHealthIndicator(properties, applicationContext);
    }

    @Test
    void shouldReturnUpWhenDomainEventsDisabled() {
        // Given
        properties.setEnabled(false);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("status", "Domain events are disabled");
        assertThat(health.getDetails()).containsEntry("adapter", "RABBIT");
    }

    @Test
    void shouldReturnDownWhenRabbitTemplateNotAvailable() {
        // Given
        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(null);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("status", "RabbitTemplate not available");
            assertThat(health.getDetails()).containsEntry("adapter", "rabbit");
            assertThat(health.getDetails()).containsEntry("templateBeanName", "rabbitTemplate");
        }
    }

    @Test
    void shouldReturnDownWhenConnectionFactoryNotAvailable() {
        // Given
        when(rabbitTemplate.getConnectionFactory()).thenReturn(null);

        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(rabbitTemplate);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("status", "RabbitMQ connection factory not available");
            assertThat(health.getDetails()).containsEntry("adapter", "rabbit");
        }
    }

    @Test
    void shouldReturnUpWhenConnectionIsHealthy() {
        // Given
        when(rabbitTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.createConnection()).thenReturn(connection);
        when(connection.isOpen()).thenReturn(true);

        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(rabbitTemplate);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsEntry("status", "RabbitMQ connection healthy");
            assertThat(health.getDetails()).containsEntry("adapter", "rabbit");
            assertThat(health.getDetails()).containsEntry("templateBeanName", "rabbitTemplate");
            assertThat(health.getDetails()).containsEntry("exchange", "test-exchange");
            assertThat(health.getDetails()).containsEntry("routingKey", "test.routing.key");
            assertThat(health.getDetails()).containsKey("connectionFactory");
        }
    }

    @Test
    void shouldReturnDownWhenConnectionIsNotOpen() {
        // Given
        when(rabbitTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.createConnection()).thenReturn(connection);
        when(connection.isOpen()).thenReturn(false);

        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(rabbitTemplate);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("status", "RabbitMQ connection not available or closed");
            assertThat(health.getDetails()).containsEntry("adapter", "rabbit");
        }
    }

    @Test
    void shouldReturnDownWhenConnectionIsNull() {
        // Given
        when(rabbitTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.createConnection()).thenReturn(null);

        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(rabbitTemplate);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("status", "RabbitMQ connection not available or closed");
            assertThat(health.getDetails()).containsEntry("adapter", "rabbit");
        }
    }

    @Test
    void shouldReturnDownWhenConnectionCreationFails() {
        // Given
        when(rabbitTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.createConnection()).thenThrow(new RuntimeException("Connection failed"));

        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(rabbitTemplate);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("status", "Failed to connect to RabbitMQ");
            assertThat(health.getDetails()).containsEntry("adapter", "rabbit");
            assertThat(health.getDetails()).containsEntry("error", "Connection failed");
        }
    }

    @Test
    void shouldReturnDownWhenExceptionThrown() {
        // Given
        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("General error"));

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("status", "Error checking RabbitMQ health");
            assertThat(health.getDetails()).containsEntry("adapter", "rabbit");
            assertThat(health.getDetails()).containsEntry("error", "General error");
        }
    }

    @Test
    void shouldUseCustomTemplateBeanName() {
        // Given
        properties.getRabbit().setTemplateBeanName("customRabbitTemplate");

        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(null);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("templateBeanName", "customRabbitTemplate");
        }
    }
}