package com.firefly.common.domain.actuator.info;

import com.firefly.common.domain.events.properties.DomainEventsProperties;
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
            kafkaInfo.put("bootstrapServers", properties.getKafka().getBootstrapServers());
            kafkaInfo.put("keySerializer", properties.getKafka().getKeySerializer());
            kafkaInfo.put("valueSerializer", properties.getKafka().getValueSerializer());
            kafkaInfo.put("retries", properties.getKafka().getRetries());
            kafkaInfo.put("batchSize", properties.getKafka().getBatchSize());
            kafkaInfo.put("lingerMs", properties.getKafka().getLingerMs());
            kafkaInfo.put("bufferMemory", properties.getKafka().getBufferMemory());
            kafkaInfo.put("acks", properties.getKafka().getAcks());
            kafkaInfo.put("properties", properties.getKafka().getProperties());
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
            
            // Kinesis configuration
            Map<String, Object> kinesisInfo = new HashMap<>();
            kinesisInfo.put("clientBeanName", properties.getKinesis().getClientBeanName());
            kinesisInfo.put("streamName", properties.getKinesis().getStreamName());
            kinesisInfo.put("partitionKey", properties.getKinesis().getPartitionKey());
            adaptersInfo.put("kinesis", kinesisInfo);
            
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
            
            // Kinesis consumer
            Map<String, Object> kinesisConsumerInfo = new HashMap<>();
            kinesisConsumerInfo.put("streamName", properties.getConsumer().getKinesis().getStreamName());
            kinesisConsumerInfo.put("applicationName", properties.getConsumer().getKinesis().getApplicationName());
            kinesisConsumerInfo.put("pollDelayMillis", properties.getConsumer().getKinesis().getPollDelayMillis());
            consumerAdaptersInfo.put("kinesis", kinesisConsumerInfo);
            
            consumerInfo.put("adapters", consumerAdaptersInfo);
        }
        
        domainEventsInfo.put("consumer", consumerInfo);
        
        // Add to info endpoint under 'domainEvents' key
        builder.withDetail("domainEvents", domainEventsInfo);
    }
}