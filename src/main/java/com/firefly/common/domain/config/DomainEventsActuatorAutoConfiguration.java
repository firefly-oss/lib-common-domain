/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.common.domain.config;

import com.firefly.common.domain.actuator.health.*;
import com.firefly.common.domain.actuator.info.DomainEventsInfoContributor;
import com.firefly.common.domain.actuator.metrics.ApplicationStartupMetrics;
import com.firefly.common.domain.actuator.metrics.DomainEventsMetrics;
import com.firefly.common.domain.actuator.metrics.HttpClientMetrics;
import com.firefly.common.domain.actuator.metrics.JvmMetrics;
import com.firefly.common.domain.events.properties.DomainEventsProperties;
import com.firefly.common.domain.util.DomainEventAdapterUtils;
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

    @Bean
    @ConditionalOnEnabledHealthIndicator("domainEventsKafka")
    @ConditionalOnExpression("'${firefly.events.adapter:auto}'=='kafka' or '${firefly.events.adapter:auto}'=='auto'")
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    public KafkaDomainEventsHealthIndicator kafkaDomainEventsHealthIndicator(
            DomainEventsProperties properties, ApplicationContext applicationContext) {
        return new KafkaDomainEventsHealthIndicator(properties, applicationContext);
    }

    @Bean
    @ConditionalOnEnabledHealthIndicator("domainEventsKinesis")
    @ConditionalOnExpression("'${firefly.events.adapter:auto}'=='kinesis' or '${firefly.events.adapter:auto}'=='auto'")
    @ConditionalOnClass(name = "software.amazon.awssdk.services.kinesis.KinesisAsyncClient")
    public KinesisDomainEventsHealthIndicator kinesisDomainEventsHealthIndicator(
            DomainEventsProperties properties, ApplicationContext applicationContext) {
        return new KinesisDomainEventsHealthIndicator(properties, applicationContext);
    }

    @Bean
    @ConditionalOnEnabledHealthIndicator("domainEventsRabbit")
    @ConditionalOnExpression("'${firefly.events.adapter:auto}'=='rabbit' or '${firefly.events.adapter:auto}'=='auto'")
    @ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
    public RabbitMqDomainEventsHealthIndicator rabbitMqDomainEventsHealthIndicator(
            DomainEventsProperties properties, ApplicationContext applicationContext) {
        return new RabbitMqDomainEventsHealthIndicator(properties, applicationContext);
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

    // Enhanced Observability Components

    @Bean
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnProperty(prefix = "firefly.observability.jvm", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JvmMetrics jvmMetrics(MeterRegistry meterRegistry) {
        return new JvmMetrics(meterRegistry);
    }

    @Bean
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnProperty(prefix = "firefly.observability.http-client", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HttpClientMetrics httpClientMetrics(MeterRegistry meterRegistry) {
        return new HttpClientMetrics(meterRegistry);
    }

    @Bean
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnProperty(prefix = "firefly.observability.startup", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ApplicationStartupMetrics applicationStartupMetrics(MeterRegistry meterRegistry) {
        return new ApplicationStartupMetrics(meterRegistry);
    }

    @Bean
    @ConditionalOnEnabledHealthIndicator("threadPool")
    @ConditionalOnProperty(prefix = "firefly.observability.thread-pool", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ThreadPoolHealthIndicator threadPoolHealthIndicator(ApplicationContext applicationContext) {
        return new ThreadPoolHealthIndicator(applicationContext);
    }

    @Bean
    @ConditionalOnEnabledHealthIndicator("httpClient")
    @ConditionalOnProperty(prefix = "firefly.observability.http-client", name = "health-enabled", havingValue = "true", matchIfMissing = true)
    public HttpClientHealthIndicator httpClientHealthIndicator() {
        return new HttpClientHealthIndicator();
    }

    @Bean
    @ConditionalOnEnabledHealthIndicator("cache")
    @ConditionalOnProperty(prefix = "firefly.observability.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CacheHealthIndicator cacheHealthIndicator(ApplicationContext applicationContext) {
        return new CacheHealthIndicator(applicationContext);
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
            return DomainEventAdapterUtils.isClassPresent("software.amazon.awssdk.services.sqs.SqsAsyncClient") &&
                    DomainEventAdapterUtils.resolveBean(ctx, null, "software.amazon.awssdk.services.sqs.SqsAsyncClient") != null;
        }
    }
}