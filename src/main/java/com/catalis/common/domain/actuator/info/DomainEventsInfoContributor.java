package com.catalis.common.domain.actuator.info;

import com.catalis.common.domain.events.properties.DomainEventsProperties;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Info contributor that exposes Domain Events configuration details.
 * Provides visibility into the current adapter configuration and settings.
 */
@Component
public class DomainEventsInfoContributor implements InfoContributor {

    private final DomainEventsProperties properties;

    public DomainEventsInfoContributor(DomainEventsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> domainEventsInfo = new HashMap<>();
        
        // Basic configuration
        domainEventsInfo.put("enabled", properties.isEnabled());
        domainEventsInfo.put("adapter", properties.getAdapter().name());
        
        // Adapter-specific configuration
        if (properties.getAdapter() != DomainEventsProperties.Adapter.NOOP) {
            Map<String, Object> adaptersInfo = new HashMap<>();
            
            // Kafka configuration
            Map<String, Object> kafkaInfo = new HashMap<>();
            kafkaInfo.put("templateBeanName", properties.getKafka().getTemplateBeanName());
            kafkaInfo.put("useMessagingIfAvailable", properties.getKafka().isUseMessagingIfAvailable());
            adaptersInfo.put("kafka", kafkaInfo);
            
            // RabbitMQ configuration
            Map<String, Object> rabbitInfo = new HashMap<>();
            rabbitInfo.put("templateBeanName", properties.getRabbit().getTemplateBeanName());
            rabbitInfo.put("exchange", properties.getRabbit().getExchange());
            rabbitInfo.put("routingKey", properties.getRabbit().getRoutingKey());
            adaptersInfo.put("rabbit", rabbitInfo);
            
            // SQS configuration
            Map<String, Object> sqsInfo = new HashMap<>();
            sqsInfo.put("clientBeanName", properties.getSqs().getClientBeanName());
            sqsInfo.put("queueUrl", properties.getSqs().getQueueUrl());
            sqsInfo.put("queueName", properties.getSqs().getQueueName());
            adaptersInfo.put("sqs", sqsInfo);
            
            domainEventsInfo.put("adapters", adaptersInfo);
        }
        
        // Consumer configuration
        Map<String, Object> consumerInfo = new HashMap<>();
        consumerInfo.put("enabled", properties.getConsumer().isEnabled());
        consumerInfo.put("typeHeader", properties.getConsumer().getTypeHeader());
        consumerInfo.put("keyHeader", properties.getConsumer().getKeyHeader());
        
        if (properties.getConsumer().isEnabled()) {
            Map<String, Object> consumerAdaptersInfo = new HashMap<>();
            
            // Kafka consumer
            Map<String, Object> kafkaConsumerInfo = new HashMap<>();
            kafkaConsumerInfo.put("topics", properties.getConsumer().getKafka().getTopics());
            kafkaConsumerInfo.put("consumerFactoryBeanName", properties.getConsumer().getKafka().getConsumerFactoryBeanName());
            kafkaConsumerInfo.put("groupId", properties.getConsumer().getKafka().getGroupId());
            consumerAdaptersInfo.put("kafka", kafkaConsumerInfo);
            
            // RabbitMQ consumer
            Map<String, Object> rabbitConsumerInfo = new HashMap<>();
            rabbitConsumerInfo.put("queues", properties.getConsumer().getRabbit().getQueues());
            consumerAdaptersInfo.put("rabbit", rabbitConsumerInfo);
            
            // SQS consumer
            Map<String, Object> sqsConsumerInfo = new HashMap<>();
            sqsConsumerInfo.put("queueUrl", properties.getConsumer().getSqs().getQueueUrl());
            sqsConsumerInfo.put("queueName", properties.getConsumer().getSqs().getQueueName());
            sqsConsumerInfo.put("waitTimeSeconds", properties.getConsumer().getSqs().getWaitTimeSeconds());
            sqsConsumerInfo.put("maxMessages", properties.getConsumer().getSqs().getMaxMessages());
            sqsConsumerInfo.put("pollDelayMillis", properties.getConsumer().getSqs().getPollDelayMillis());
            consumerAdaptersInfo.put("sqs", sqsConsumerInfo);
            
            consumerInfo.put("adapters", consumerAdaptersInfo);
        }
        
        domainEventsInfo.put("consumer", consumerInfo);
        
        // Add to info endpoint under 'domainEvents' key
        builder.withDetail("domainEvents", domainEventsInfo);
    }
}