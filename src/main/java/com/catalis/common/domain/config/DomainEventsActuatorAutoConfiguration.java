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

    @Bean
    @ConditionalOnEnabledHealthIndicator("domainEventsKafka")
    @ConditionalOnExpression("'${firefly.events.adapter:auto}'=='kafka' or '${firefly.events.adapter:auto}'=='auto'")
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    public KafkaDomainEventsHealthIndicator kafkaDomainEventsHealthIndicator(
            DomainEventsProperties properties, ApplicationContext applicationContext) {
        return new KafkaDomainEventsHealthIndicator(properties, applicationContext);
    }

    @Bean
    @ConditionalOnEnabledHealthIndicator("domainEventsRabbit")
    @ConditionalOnExpression("'${firefly.events.adapter:auto}'=='rabbit' or '${firefly.events.adapter:auto}'=='auto'")
    @ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
    public RabbitDomainEventsHealthIndicator rabbitDomainEventsHealthIndicator(
            DomainEventsProperties properties, ApplicationContext applicationContext) {
        return new RabbitDomainEventsHealthIndicator(properties, applicationContext);
    }

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

        @Bean
        @ConditionalOnEnabledHealthIndicator("domainEventsKafkaAuto")
        @ConditionalOnMissingBean(KafkaDomainEventsHealthIndicator.class)
        public KafkaDomainEventsHealthIndicator kafkaDomainEventsHealthIndicatorAuto(
                DomainEventsProperties properties, ApplicationContext applicationContext) {
            // Only create if Kafka is actually available
            if (isKafkaAvailable(applicationContext)) {
                return new KafkaDomainEventsHealthIndicator(properties, applicationContext);
            }
            return null;
        }

        @Bean
        @ConditionalOnEnabledHealthIndicator("domainEventsRabbitAuto")
        @ConditionalOnMissingBean(RabbitDomainEventsHealthIndicator.class)
        public RabbitDomainEventsHealthIndicator rabbitDomainEventsHealthIndicatorAuto(
                DomainEventsProperties properties, ApplicationContext applicationContext) {
            // Only create if RabbitMQ is actually available
            if (isRabbitAvailable(applicationContext)) {
                return new RabbitDomainEventsHealthIndicator(properties, applicationContext);
            }
            return null;
        }

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

        private boolean isKafkaAvailable(ApplicationContext ctx) {
            return StepEventAdapterUtils.isClassPresent("org.springframework.kafka.core.KafkaTemplate") &&
                    StepEventAdapterUtils.resolveBean(ctx, null, "org.springframework.kafka.core.KafkaTemplate") != null;
        }

        private boolean isRabbitAvailable(ApplicationContext ctx) {
            return StepEventAdapterUtils.isClassPresent("org.springframework.amqp.rabbit.core.RabbitTemplate") &&
                    StepEventAdapterUtils.resolveBean(ctx, null, "org.springframework.amqp.rabbit.core.RabbitTemplate") != null;
        }

        private boolean isSqsAvailable(ApplicationContext ctx) {
            return StepEventAdapterUtils.isClassPresent("software.amazon.awssdk.services.sqs.SqsAsyncClient") &&
                    StepEventAdapterUtils.resolveBean(ctx, null, "software.amazon.awssdk.services.sqs.SqsAsyncClient") != null;
        }
    }
}