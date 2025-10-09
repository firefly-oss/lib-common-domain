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

import com.firefly.common.domain.stepevents.StepEventPublisherBridge;
import com.firefly.common.eda.publisher.EventPublisherFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Auto-configuration for SAGA Step Event Bridge.
 * <p>
 * This configuration creates a bridge between lib-transactional-engine's StepEventPublisher
 * and lib-common-eda's EventPublisher, allowing SAGA step events to be published through
 * the unified EDA infrastructure.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "firefly.stepevents", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(EventPublisherFactory.class)
@EnableConfigurationProperties(StepEventsProperties.class)
public class StepBridgeConfiguration {

    /**
     * Creates the StepEventPublisherBridge bean.
     * <p>
     * This bridge uses the default EDA publisher configured in lib-common-eda to publish
     * step events. The destination topic is configured via firefly.stepevents.topic property.
     *
     * @param publisherFactory the EDA event publisher factory
     * @param properties the step events configuration properties
     * @return the configured StepEventPublisherBridge
     */
    @Bean
    @Primary
    public StepEventPublisherBridge stepEventPublisherBridge(
            EventPublisherFactory publisherFactory,
            StepEventsProperties properties) {

        String topic = properties.getTopic();
        log.info("Configuring StepEventPublisherBridge with topic: {}", topic);

        return new StepEventPublisherBridge(topic, publisherFactory.getDefaultPublisher());
    }
}