package com.catalis.common.domain.config;

import com.catalis.common.domain.events.outbound.DomainEventPublisher;
import com.catalis.common.domain.stepevents.StepEventPublisherBridge;
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