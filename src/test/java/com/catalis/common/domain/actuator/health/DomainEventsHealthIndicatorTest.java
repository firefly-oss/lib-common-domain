package com.catalis.common.domain.actuator.health;

import com.catalis.common.domain.events.properties.DomainEventsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventsHealthIndicatorTest {

    private DomainEventsProperties properties;
    private TestDomainEventsHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        properties = new DomainEventsProperties();
        healthIndicator = new TestDomainEventsHealthIndicator(properties);
    }

    @Test
    void shouldReturnUpWhenDomainEventsDisabled() {
        // Given
        properties.setEnabled(false);
        properties.setAdapter(DomainEventsProperties.Adapter.KAFKA);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("status", "Domain events are disabled");
        assertThat(health.getDetails()).containsEntry("adapter", "KAFKA");
    }

    @Test
    void shouldCallPerformHealthCheckWhenEnabled() {
        // Given
        properties.setEnabled(true);
        properties.setAdapter(DomainEventsProperties.Adapter.APPLICATION_EVENT);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("status", "Perform health check called");
        assertThat(health.getDetails()).containsEntry("adapter", "test");
        assertThat(healthIndicator.performHealthCheckCalled).isTrue();
    }

    @Test
    void shouldHandleExceptionInPerformHealthCheck() {
        // Given
        properties.setEnabled(true);
        healthIndicator.shouldThrowException = true;

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", "java.lang.RuntimeException: Test exception");
    }

    // Test implementation of the abstract class
    private static class TestDomainEventsHealthIndicator extends DomainEventsHealthIndicator {
        
        boolean performHealthCheckCalled = false;
        boolean shouldThrowException = false;

        protected TestDomainEventsHealthIndicator(DomainEventsProperties properties) {
            super(properties);
        }

        @Override
        protected void performHealthCheck(Health.Builder builder) throws Exception {
            performHealthCheckCalled = true;
            
            if (shouldThrowException) {
                throw new RuntimeException("Test exception");
            }
            
            builder.up()
                    .withDetail("status", "Perform health check called")
                    .withDetail("adapter", "test");
        }
    }
}