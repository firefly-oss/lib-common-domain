package com.catalis.common.domain.config;

import com.catalis.common.domain.events.outbound.DomainEventPublisher;
import com.catalis.common.domain.stepevents.StepEventPublisherBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class StepBridgeConfiguration {
    @Bean
    @Primary
    public StepEventPublisherBridge stepEventDomainPublisher(DomainEventPublisher domainEventPublisher) {
        return new StepEventPublisherBridge(domainEventPublisher); 
    }
}