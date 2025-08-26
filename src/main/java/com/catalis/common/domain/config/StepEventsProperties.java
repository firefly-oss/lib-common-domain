package com.catalis.common.domain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Step Events.
 * 
 * Step Events always use the Domain Events infrastructure for publishing via the bridge pattern.
 * This properties class only controls whether Step Events are enabled or disabled.
 */
@ConfigurationProperties(prefix = "firefly.stepevents")
@Data
public class StepEventsProperties {

    /**
     * Whether Step Events are enabled. When enabled, Step Events will use the Domain Events 
     * infrastructure for publishing via the StepEventPublisherBridge.
     */
    private boolean enabled = true;
}
