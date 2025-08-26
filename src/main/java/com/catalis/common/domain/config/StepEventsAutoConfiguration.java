package com.catalis.common.domain.config;

import com.catalis.common.domain.events.outbound.DomainEventPublisher;
import com.catalis.common.domain.stepevents.StepEventPublisherBridge;
import com.catalis.transactionalengine.events.ApplicationEventStepEventPublisher;
import com.catalis.transactionalengine.events.StepEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * Simplified auto-configuration for Step Events that always uses Domain Events infrastructure.
 * 
 * This configuration follows the bridge pattern where:
 * - Step Events always use the existing DomainEventPublisher for publishing
 * - No separate messaging configuration is needed for Step Events
 * - All messaging adapters (Kafka, RabbitMQ, SQS) are handled by DomainEventsAutoConfiguration
 * - Only fallback to ApplicationEvent when no DomainEventPublisher is available
 */
@AutoConfiguration
@EnableConfigurationProperties(StepEventsProperties.class)
@ConditionalOnClass({StepEventPublisher.class, ApplicationEventPublisher.class})
@ConditionalOnProperty(prefix = "firefly.stepevents", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StepEventsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(StepEventPublisher.class)
    public StepEventPublisher stepEventPublisher(ApplicationEventPublisher applicationEventPublisher,
                                                 ObjectProvider<DomainEventPublisher> domainPublisherProvider) {
        DomainEventPublisher domainPublisher = domainPublisherProvider.getIfAvailable();
        if (domainPublisher != null) {
            return new StepEventPublisherBridge(domainPublisher);
        }
        // If no DomainEventPublisher is available, fallback to ApplicationEvent
        // All messaging adapters are handled by the DomainEventPublisher configuration
        return new ApplicationEventStepEventPublisher(applicationEventPublisher);
    }

}
