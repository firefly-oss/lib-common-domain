package com.catalis.common.domain.actuator.health;

import com.catalis.common.domain.events.properties.DomainEventsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ApplicationEventDomainEventsHealthIndicatorTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private DomainEventsProperties properties;
    private ApplicationEventDomainEventsHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        properties = new DomainEventsProperties();
        properties.setEnabled(true);
        properties.setAdapter(DomainEventsProperties.Adapter.APPLICATION_EVENT);
        healthIndicator = new ApplicationEventDomainEventsHealthIndicator(properties, applicationEventPublisher);
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
        assertThat(health.getDetails()).containsEntry("adapter", "APPLICATION_EVENT");
    }

    @Test
    void shouldReturnDownWhenApplicationEventPublisherNotAvailable() {
        // Given
        healthIndicator = new ApplicationEventDomainEventsHealthIndicator(properties, null);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("status", "ApplicationEventPublisher not available");
        assertThat(health.getDetails()).containsEntry("adapter", "application_event");
    }

    @Test
    void shouldReturnUpWhenApplicationEventPublisherAvailable() {
        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("status", "ApplicationEventPublisher available");
        assertThat(health.getDetails()).containsEntry("adapter", "application_event");
        assertThat(health.getDetails()).containsKey("publisherClass");
    }
}