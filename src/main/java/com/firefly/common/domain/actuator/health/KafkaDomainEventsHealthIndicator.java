package com.firefly.common.domain.actuator.health;

import com.firefly.common.domain.events.properties.DomainEventsProperties;
import com.firefly.common.domain.util.DomainEventAdapterUtils;
import org.springframework.boot.actuate.health.Health;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Health indicator for Kafka Domain Events adapter.
 * Checks the availability and health of the KafkaTemplate.
 */
public class KafkaDomainEventsHealthIndicator extends DomainEventsHealthIndicator {

    private final ApplicationContext applicationContext;

    public KafkaDomainEventsHealthIndicator(DomainEventsProperties properties, ApplicationContext applicationContext) {
        super(properties);
        this.applicationContext = applicationContext;
    }

    @Override
    protected void performHealthCheck(Health.Builder builder) throws Exception {
        try {
            // Check if KafkaTemplate is available
            Object template = DomainEventAdapterUtils.resolveBean(
                    applicationContext,
                    properties.getKafka().getTemplateBeanName(),
                    "org.springframework.kafka.core.KafkaTemplate"
            );

            if (template == null) {
                builder.down()
                        .withDetail("status", "KafkaTemplate not available")
                        .withDetail("adapter", "kafka")
                        .withDetail("templateBeanName", properties.getKafka().getTemplateBeanName());
                return;
            }

            @SuppressWarnings("unchecked")
            KafkaTemplate<String, String> kafkaTemplate = (KafkaTemplate<String, String>) template;

            // Check if bootstrap servers are configured
            String bootstrapServers = properties.getKafka().getBootstrapServers();

            if (bootstrapServers == null || bootstrapServers.isEmpty()) {
                builder.up()
                        .withDetail("status", "Kafka template available but no bootstrap servers configured")
                        .withDetail("adapter", "kafka")
                        .withDetail("templateBeanName", properties.getKafka().getTemplateBeanName());
                return;
            }

            // Try to check Kafka connectivity by attempting to get metadata
            try {
                // This is a lightweight check - we don't send actual data
                // Just verify the template is properly configured
                var producerFactory = kafkaTemplate.getProducerFactory();
                if (producerFactory != null) {
                    builder.up()
                            .withDetail("status", "Kafka template available and configured")
                            .withDetail("adapter", "kafka")
                            .withDetail("bootstrapServers", bootstrapServers)
                            .withDetail("templateBeanName", properties.getKafka().getTemplateBeanName());
                } else {
                    builder.down()
                            .withDetail("status", "Kafka template producer factory not available")
                            .withDetail("adapter", "kafka")
                            .withDetail("templateBeanName", properties.getKafka().getTemplateBeanName());
                }

            } catch (Exception e) {
                builder.down()
                        .withDetail("status", "Failed to access Kafka template")
                        .withDetail("adapter", "kafka")
                        .withDetail("bootstrapServers", bootstrapServers)
                        .withDetail("error", e.getMessage());
            }

        } catch (Exception e) {
            builder.down()
                    .withDetail("status", "Error checking Kafka health")
                    .withDetail("adapter", "kafka")
                    .withDetail("error", e.getMessage());
        }
    }
}