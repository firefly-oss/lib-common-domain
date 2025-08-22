package com.catalis.common.domain.actuator.health;

import com.catalis.common.domain.events.properties.DomainEventsProperties;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

/**
 * Base health indicator for Domain Events components.
 * Provides common functionality for all domain events health indicators.
 */
public abstract class DomainEventsHealthIndicator extends AbstractHealthIndicator {

    protected final DomainEventsProperties properties;

    protected DomainEventsHealthIndicator(DomainEventsProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        if (!properties.isEnabled()) {
            builder.up()
                    .withDetail("status", "Domain events are disabled")
                    .withDetail("adapter", properties.getAdapter().name());
            return;
        }

        performHealthCheck(builder);
    }

    /**
     * Perform the specific health check for the adapter.
     * @param builder the health builder
     * @throws Exception if health check fails
     */
    protected abstract void performHealthCheck(Health.Builder builder) throws Exception;
}