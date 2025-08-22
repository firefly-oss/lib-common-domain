package com.catalis.common.domain.config;

import com.catalis.common.domain.actuator.health.*;
import com.catalis.common.domain.actuator.info.DomainEventsInfoContributor;
import com.catalis.common.domain.actuator.metrics.DomainEventsMetrics;
import com.catalis.common.domain.events.properties.DomainEventsProperties;
import com.catalis.common.domain.stepevents.StepEventAdapterUtils;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.info.ConditionalOnEnabledInfoContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Domain Events actuator components.
 * Provides health indicators, metrics, and info contributors for domain events.
 */
@AutoConfiguration(after = DomainEventsAutoConfiguration.class)
@EnableConfigurationProperties(DomainEventsProperties.class)
@ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
@ConditionalOnProperty(prefix = "firefly.events", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DomainEventsActuatorAutoConfiguration {

    // Health Indicators

    // Kafka and Rabbit health indicators moved to their respective technology modules

    @Bean
    @ConditionalOnEnabledHealthIndicator("domainEventsSqs")
    @ConditionalOnExpression("'${firefly.events.adapter:auto}'=='sqs' or '${firefly.events.adapter:auto}'=='auto'")
    @ConditionalOnClass(name = "software.amazon.awssdk.services.sqs.SqsAsyncClient")
    public SqsDomainEventsHealthIndicator sqsDomainEventsHealthIndicator(
            DomainEventsProperties properties, ApplicationContext applicationContext) {
        return new SqsDomainEventsHealthIndicator(properties, applicationContext);
    }

    @Bean
    @ConditionalOnEnabledHealthIndicator("domainEventsApplicationEvent")
    @ConditionalOnExpression("'${firefly.events.adapter:auto}'=='application_event' or '${firefly.events.adapter:auto}'=='auto'")
    public ApplicationEventDomainEventsHealthIndicator applicationEventDomainEventsHealthIndicator(
            DomainEventsProperties properties, ApplicationEventPublisher applicationEventPublisher) {
        return new ApplicationEventDomainEventsHealthIndicator(properties, applicationEventPublisher);
    }

    // Metrics

    @Bean
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    @ConditionalOnBean(MeterRegistry.class)
    public DomainEventsMetrics domainEventsMetrics(MeterRegistry meterRegistry) {
        return new DomainEventsMetrics(meterRegistry);
    }

    // Info Contributor

    @Bean
    @ConditionalOnEnabledInfoContributor("domainEvents")
    public DomainEventsInfoContributor domainEventsInfoContributor(DomainEventsProperties properties) {
        return new DomainEventsInfoContributor(properties);
    }

    /**
     * Configuration for additional health indicators when AUTO adapter is used.
     */
    @ConditionalOnExpression("'${firefly.events.adapter:auto}'=='auto'")
    static class AutoAdapterHealthConfiguration {

        // Kafka and Rabbit health indicator auto-configuration moved to their respective technology modules

        @Bean
        @ConditionalOnEnabledHealthIndicator("domainEventsSqsAuto")
        @ConditionalOnMissingBean(SqsDomainEventsHealthIndicator.class)
        public SqsDomainEventsHealthIndicator sqsDomainEventsHealthIndicatorAuto(
                DomainEventsProperties properties, ApplicationContext applicationContext) {
            // Only create if SQS is actually available
            if (isSqsAvailable(applicationContext)) {
                return new SqsDomainEventsHealthIndicator(properties, applicationContext);
            }
            return null;
        }


        private boolean isSqsAvailable(ApplicationContext ctx) {
            return StepEventAdapterUtils.isClassPresent("software.amazon.awssdk.services.sqs.SqsAsyncClient") &&
                    StepEventAdapterUtils.resolveBean(ctx, null, "software.amazon.awssdk.services.sqs.SqsAsyncClient") != null;
        }
    }
}