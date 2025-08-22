package com.catalis.common.domain.actuator.health;

import com.catalis.common.domain.events.properties.DomainEventsProperties;
import com.catalis.common.domain.stepevents.StepEventAdapterUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import java.util.Map;
import java.util.HashMap;

@ExtendWith(MockitoExtension.class)
class KafkaDomainEventsHealthIndicatorTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    private ProducerFactory<Object, Object> producerFactory;

    private DomainEventsProperties properties;
    private KafkaDomainEventsHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        properties = new DomainEventsProperties();
        properties.setEnabled(true);
        properties.setAdapter(DomainEventsProperties.Adapter.KAFKA);
        healthIndicator = new KafkaDomainEventsHealthIndicator(properties, applicationContext);
        
        // Create a spy of a concrete ProducerFactory implementation
        producerFactory = spy(new DefaultKafkaProducerFactory<>(new HashMap<>()));
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
        assertThat(health.getDetails()).containsEntry("adapter", "KAFKA");
    }

    @Test
    void shouldReturnDownWhenKafkaTemplateNotAvailable() {
        // Given
        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), any(), anyString())).thenReturn(null);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("status", "KafkaTemplate not available");
            assertThat(health.getDetails()).containsEntry("adapter", "kafka");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnUpWhenKafkaTemplateAvailable() throws Exception {
        // Given
        when(kafkaTemplate.getProducerFactory()).thenReturn(producerFactory);

        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), any(), anyString())).thenReturn(kafkaTemplate);

            // When
            Health health = healthIndicator.health();

            // Debug output
            System.out.println("[DEBUG_LOG] Health status: " + health.getStatus());
            System.out.println("[DEBUG_LOG] Health details: " + health.getDetails());

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsEntry("status", "Kafka template available");
            assertThat(health.getDetails()).containsEntry("adapter", "kafka");
            assertThat(health.getDetails()).containsEntry("useMessagingIfAvailable", true);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnDownWhenProducerFactoryNotAvailable() throws Exception {
        // Given
        when(kafkaTemplate.getProducerFactory()).thenReturn(null);

        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), any(), anyString())).thenReturn(kafkaTemplate);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("status", "KafkaTemplate producer factory not available");
            assertThat(health.getDetails()).containsEntry("adapter", "kafka");
        }
    }

    @Test
    void shouldReturnDownWhenExceptionThrown() throws Exception {
        // Given
        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), any(), anyString()))
                    .thenThrow(new RuntimeException("Connection failed"));

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("status", "Error checking Kafka health");
            assertThat(health.getDetails()).containsEntry("adapter", "kafka");
            assertThat(health.getDetails()).containsEntry("error", "Connection failed");
        }
    }

    @Test
    void shouldUseCustomTemplateBeanName() throws Exception {
        // Given
        properties.getKafka().setTemplateBeanName("customKafkaTemplate");
        when(kafkaTemplate.getProducerFactory()).thenReturn(producerFactory);

        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), eq("customKafkaTemplate"), anyString()))
                    .thenReturn(kafkaTemplate);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsEntry("templateBeanName", "customKafkaTemplate");
        }
    }
}