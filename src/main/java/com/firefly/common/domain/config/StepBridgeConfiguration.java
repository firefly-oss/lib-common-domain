package com.firefly.common.domain.config;

import com.firefly.common.domain.events.outbound.DomainEventPublisher;
import com.firefly.common.domain.stepevents.StepEventPublisherBridge;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class StepBridgeConfiguration {
    @Value("${domain.topic:domain-events}")
    private String defaultTopic;

    @Bean
    @Primary
    public StepEventPublisherBridge stepEventDomainPublisher(DomainEventPublisher domainEventPublisher) {
        return new StepEventPublisherBridge(defaultTopic, domainEventPublisher);
    }
}