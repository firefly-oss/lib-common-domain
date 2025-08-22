package com.catalis.common.domain.actuator.health;

import com.catalis.common.domain.events.properties.DomainEventsProperties;
import com.catalis.common.domain.stepevents.StepEventAdapterUtils;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.health.Health;
import org.springframework.context.ApplicationContext;

/**
 * Health indicator for RabbitMQ Domain Events adapter.
 * Checks the availability and health of the RabbitTemplate and connection factory.
 */
public class RabbitDomainEventsHealthIndicator extends DomainEventsHealthIndicator {

    private final ApplicationContext applicationContext;

    public RabbitDomainEventsHealthIndicator(DomainEventsProperties properties, ApplicationContext applicationContext) {
        super(properties);
        this.applicationContext = applicationContext;
    }

    @Override
    protected void performHealthCheck(Health.Builder builder) throws Exception {
        try {
            // Check if RabbitTemplate is available
            Object template = StepEventAdapterUtils.resolveBean(
                    applicationContext,
                    properties.getRabbit().getTemplateBeanName(),
                    "org.springframework.amqp.rabbit.core.RabbitTemplate"
            );

            if (template == null) {
                builder.down()
                        .withDetail("status", "RabbitTemplate not available")
                        .withDetail("adapter", "rabbit")
                        .withDetail("templateBeanName", properties.getRabbit().getTemplateBeanName());
                return;
            }

            RabbitTemplate rabbitTemplate = (RabbitTemplate) template;

            // Check connection factory availability
            ConnectionFactory connectionFactory = rabbitTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                builder.down()
                        .withDetail("status", "RabbitMQ connection factory not available")
                        .withDetail("adapter", "rabbit");
                return;
            }

            // Try to test the connection
            try {
                var connection = connectionFactory.createConnection();
                if (connection != null && connection.isOpen()) {
                    connection.close();
                    builder.up()
                            .withDetail("status", "RabbitMQ connection healthy")
                            .withDetail("adapter", "rabbit")
                            .withDetail("templateBeanName", properties.getRabbit().getTemplateBeanName())
                            .withDetail("exchange", properties.getRabbit().getExchange())
                            .withDetail("routingKey", properties.getRabbit().getRoutingKey())
                            .withDetail("connectionFactory", connectionFactory.getClass().getSimpleName());
                } else {
                    builder.down()
                            .withDetail("status", "RabbitMQ connection not available or closed")
                            .withDetail("adapter", "rabbit");
                }
            } catch (Exception e) {
                builder.down()
                        .withDetail("status", "Failed to connect to RabbitMQ")
                        .withDetail("adapter", "rabbit")
                        .withDetail("error", e.getMessage());
            }

        } catch (Exception e) {
            builder.down()
                    .withDetail("status", "Error checking RabbitMQ health")
                    .withDetail("adapter", "rabbit")
                    .withDetail("error", e.getMessage());
        }
    }
}