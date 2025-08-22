package com.catalis.common.domain.actuator.health;

import com.catalis.common.domain.events.properties.DomainEventsProperties;
import com.catalis.common.domain.stepevents.StepEventAdapterUtils;
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
            Object template = StepEventAdapterUtils.resolveBean(
                    applicationContext,
                    properties.getKafka().getTemplateBeanName(),
                    "org.springframework.kafka.core.KafkaTemplate"
            );

            if (template == null) {
                Health.Builder downBuilder = builder.down()
                        .withDetail("status", "KafkaTemplate not available")
                        .withDetail("adapter", "kafka");
                
                // Only add templateBeanName if it's not null
                String templateBeanName = properties.getKafka().getTemplateBeanName();
                if (templateBeanName != null) {
                    downBuilder.withDetail("templateBeanName", templateBeanName);
                }
                return;
            }

            @SuppressWarnings("unchecked")
            KafkaTemplate<Object, Object> kafkaTemplate = (KafkaTemplate<Object, Object>) template;

            // Try to get producer metrics to verify connectivity
            try {
                var producerFactory = kafkaTemplate.getProducerFactory();
                if (producerFactory != null) {
                    // If we can access the producer factory, Kafka is likely healthy
                    String producerFactoryName;
                    try {
                        Class<?> clazz = producerFactory.getClass();
                        producerFactoryName = clazz != null ? clazz.getSimpleName() : "ProducerFactory";
                        if (producerFactoryName == null || producerFactoryName.isEmpty()) {
                            producerFactoryName = "ProducerFactory";
                        }
                    } catch (Exception e) {
                        producerFactoryName = "ProducerFactory";
                    }
                    
                    Health.Builder healthBuilder = builder.up()
                            .withDetail("status", "Kafka template available")
                            .withDetail("adapter", "kafka")
                            .withDetail("useMessagingIfAvailable", properties.getKafka().isUseMessagingIfAvailable())
                            .withDetail("producerFactory", producerFactoryName);
                    
                    // Only add templateBeanName if it's not null
                    String templateBeanName = properties.getKafka().getTemplateBeanName();
                    if (templateBeanName != null) {
                        healthBuilder.withDetail("templateBeanName", templateBeanName);
                    }
                } else {
                    builder.down()
                            .withDetail("status", "KafkaTemplate producer factory not available")
                            .withDetail("adapter", "kafka");
                }
            } catch (Exception e) {
                builder.down()
                        .withDetail("status", "Error accessing Kafka producer factory")
                        .withDetail("adapter", "kafka")
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