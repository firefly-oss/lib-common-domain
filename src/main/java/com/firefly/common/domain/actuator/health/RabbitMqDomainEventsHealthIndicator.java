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

package com.firefly.common.domain.actuator.health;

import com.firefly.common.domain.events.properties.DomainEventsProperties;
import com.firefly.common.domain.util.DomainEventAdapterUtils;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.health.Health;
import org.springframework.context.ApplicationContext;

/**
 * Health indicator for RabbitMQ Domain Events adapter.
 * Checks the availability and health of the RabbitTemplate.
 */
public class RabbitMqDomainEventsHealthIndicator extends DomainEventsHealthIndicator {

    private final ApplicationContext applicationContext;

    public RabbitMqDomainEventsHealthIndicator(DomainEventsProperties properties, ApplicationContext applicationContext) {
        super(properties);
        this.applicationContext = applicationContext;
    }

    @Override
    protected void performHealthCheck(Health.Builder builder) throws Exception {
        try {
            // Check if RabbitTemplate is available
            Object template = DomainEventAdapterUtils.resolveBean(
                    applicationContext,
                    properties.getRabbit().getTemplateBeanName(),
                    "org.springframework.amqp.rabbit.core.RabbitTemplate"
            );

            if (template == null) {
                builder.down()
                        .withDetail("status", "RabbitTemplate not available")
                        .withDetail("adapter", "rabbitmq")
                        .withDetail("templateBeanName", properties.getRabbit().getTemplateBeanName());
                return;
            }

            RabbitTemplate rabbitTemplate = (RabbitTemplate) template;

            // Check if exchange and routing key are configured
            String exchange = properties.getRabbit().getExchange();
            String routingKey = properties.getRabbit().getRoutingKey();

            if (exchange == null || exchange.isEmpty()) {
                builder.up()
                        .withDetail("status", "RabbitMQ template available but no exchange configured")
                        .withDetail("adapter", "rabbitmq")
                        .withDetail("templateBeanName", properties.getRabbit().getTemplateBeanName());
                return;
            }

            // Try to test RabbitMQ connectivity by checking the connection factory
            try {
                ConnectionFactory connectionFactory = rabbitTemplate.getConnectionFactory();
                if (connectionFactory != null) {
                    // Try to create a connection to test connectivity
                    try (var connection = connectionFactory.createConnection()) {
                        if (connection != null && connection.isOpen()) {
                            builder.up()
                                    .withDetail("status", "RabbitMQ connection available")
                                    .withDetail("adapter", "rabbitmq")
                                    .withDetail("exchange", exchange)
                                    .withDetail("routingKey", routingKey)
                                    .withDetail("templateBeanName", properties.getRabbit().getTemplateBeanName());
                        } else {
                            builder.down()
                                    .withDetail("status", "RabbitMQ connection is not open")
                                    .withDetail("adapter", "rabbitmq")
                                    .withDetail("exchange", exchange)
                                    .withDetail("routingKey", routingKey);
                        }
                    }
                } else {
                    builder.down()
                            .withDetail("status", "RabbitMQ connection factory not available")
                            .withDetail("adapter", "rabbitmq")
                            .withDetail("templateBeanName", properties.getRabbit().getTemplateBeanName());
                }

            } catch (Exception e) {
                builder.down()
                        .withDetail("status", "Failed to test RabbitMQ connection")
                        .withDetail("adapter", "rabbitmq")
                        .withDetail("exchange", exchange)
                        .withDetail("routingKey", routingKey)
                        .withDetail("error", e.getMessage());
            }

        } catch (Exception e) {
            builder.down()
                    .withDetail("status", "Error checking RabbitMQ health")
                    .withDetail("adapter", "rabbitmq")
                    .withDetail("error", e.getMessage());
        }
    }
}