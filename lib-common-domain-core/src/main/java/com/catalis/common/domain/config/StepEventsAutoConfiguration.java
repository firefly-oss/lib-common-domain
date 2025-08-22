package com.catalis.common.domain.config;

import com.catalis.common.domain.events.outbound.DomainEventPublisher;
import com.catalis.common.domain.stepevents.NoopStepEventPublisher;
import com.catalis.common.domain.stepevents.SqsAsyncClientStepEventPublisher;
import com.catalis.common.domain.stepevents.StepEventAdapterUtils;
import com.catalis.common.domain.stepevents.StepEventPublisherBridge;
import com.catalis.transactionalengine.events.ApplicationEventStepEventPublisher;
import com.catalis.transactionalengine.events.StepEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configures a StepEventPublisher adapter based on configuration and available beans.
 *
 * Hexagonal guidance:
 * - The port is StepEventPublisher (from lib-transactional-engine).
 * - Adapters (Kafka/Rabbit/SQS/ApplicationEvent/Noop) are provided here; selection via properties.
 * - Microservices only add dependencies and set application.yaml.
 */
@AutoConfiguration
@EnableConfigurationProperties(StepEventsProperties.class)
@ConditionalOnClass({StepEventPublisher.class, ApplicationEventPublisher.class})
@ConditionalOnProperty(prefix = "firefly.stepevents", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StepEventsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(StepEventPublisher.class)
    public StepEventPublisher stepEventPublisher(ApplicationContext ctx,
                                                 ApplicationEventPublisher applicationEventPublisher,
                                                 StepEventsProperties props,
                                                 ObjectProvider<DomainEventPublisher> domainPublisherProvider) {
        DomainEventPublisher domainPublisher = domainPublisherProvider.getIfAvailable();
        if (domainPublisher != null) {
            return new StepEventPublisherBridge(domainPublisher);
        }
        StepEventsProperties.Adapter adapter = props.getAdapter();
        if (adapter == StepEventsProperties.Adapter.NOOP) {
            return new NoopStepEventPublisher();
        }
        if (adapter == StepEventsProperties.Adapter.APPLICATION_EVENT) {
            return new ApplicationEventStepEventPublisher(applicationEventPublisher);
        }
        if (adapter == StepEventsProperties.Adapter.SQS) {
            return new SqsAsyncClientStepEventPublisher(ctx, props);
        }
        // For KAFKA and RABBIT adapters, fall back to ApplicationEvent if specific implementations not available
        if (adapter == StepEventsProperties.Adapter.KAFKA || adapter == StepEventsProperties.Adapter.RABBIT) {
            return new ApplicationEventStepEventPublisher(applicationEventPublisher);
        }
        // AUTO detection order: SQS -> ApplicationEvent (Kafka and Rabbit handled by their own modules)
        if (isSqsAvailable(ctx)) {
            return new SqsAsyncClientStepEventPublisher(ctx, props);
        }
        return new ApplicationEventStepEventPublisher(applicationEventPublisher);
    }

    private boolean isSqsAvailable(ApplicationContext ctx) {
        return StepEventAdapterUtils.isClassPresent("software.amazon.awssdk.services.sqs.SqsAsyncClient") &&
                StepEventAdapterUtils.resolveBean(ctx, null, "software.amazon.awssdk.services.sqs.SqsAsyncClient") != null;
    }
}
