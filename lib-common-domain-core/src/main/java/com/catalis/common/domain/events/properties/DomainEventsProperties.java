package com.catalis.common.domain.events.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "firefly.events")
@Validated
public class DomainEventsProperties {

    public enum Adapter { AUTO, APPLICATION_EVENT, KAFKA, RABBIT, SQS, NOOP }

    private boolean enabled = true;
    
    @NotNull(message = "Adapter cannot be null")
    private Adapter adapter = Adapter.AUTO;

    @Valid
    private final Kafka kafka = new Kafka();
    
    @Valid
    private final Rabbit rabbit = new Rabbit();
    
    @Valid
    private final Sqs sqs = new Sqs();
    
    @Valid
    private final Consumer consumer = new Consumer();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Adapter getAdapter() { return adapter; }
    public void setAdapter(Adapter adapter) { this.adapter = adapter; }

    public Kafka getKafka() { return kafka; }
    public Rabbit getRabbit() { return rabbit; }
    public Sqs getSqs() { return sqs; }
    public Consumer getConsumer() { return consumer; }

    public static class Kafka {
        private String templateBeanName;
        private boolean useMessagingIfAvailable = true;
        
        // Kafka-specific properties
        @Size(min = 1, message = "Bootstrap servers cannot be empty")
        private String bootstrapServers;
        
        private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";
        private String valueSerializer = "org.apache.kafka.common.serialization.StringSerializer";
        
        @Min(value = 0, message = "Retries must be 0 or greater")
        private Integer retries;
        
        @Min(value = 1, message = "Batch size must be 1 or greater")
        private Integer batchSize;
        
        @Min(value = 0, message = "Linger ms must be 0 or greater")
        private Integer lingerMs;
        
        @Min(value = 1, message = "Buffer memory must be 1 or greater")
        private Long bufferMemory;
        
        @Pattern(regexp = "^(none|all|1|-1)$", message = "Acks must be 'none', 'all', '1', or '-1'")
        private String acks;
        
        // Additional properties for advanced configuration
        private Map<String, Object> properties = new HashMap<>();

        // Existing getters/setters
        public String getTemplateBeanName() { return templateBeanName; }
        public void setTemplateBeanName(String templateBeanName) { this.templateBeanName = templateBeanName; }
        public boolean isUseMessagingIfAvailable() { return useMessagingIfAvailable; }
        public void setUseMessagingIfAvailable(boolean useMessagingIfAvailable) { this.useMessagingIfAvailable = useMessagingIfAvailable; }
        
        // New getters/setters
        public String getBootstrapServers() { return bootstrapServers; }
        public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }
        
        public String getKeySerializer() { return keySerializer; }
        public void setKeySerializer(String keySerializer) { this.keySerializer = keySerializer; }
        
        public String getValueSerializer() { return valueSerializer; }
        public void setValueSerializer(String valueSerializer) { this.valueSerializer = valueSerializer; }
        
        public Integer getRetries() { return retries; }
        public void setRetries(Integer retries) { this.retries = retries; }
        
        public Integer getBatchSize() { return batchSize; }
        public void setBatchSize(Integer batchSize) { this.batchSize = batchSize; }
        
        public Integer getLingerMs() { return lingerMs; }
        public void setLingerMs(Integer lingerMs) { this.lingerMs = lingerMs; }
        
        public Long getBufferMemory() { return bufferMemory; }
        public void setBufferMemory(Long bufferMemory) { this.bufferMemory = bufferMemory; }
        
        public String getAcks() { return acks; }
        public void setAcks(String acks) { this.acks = acks; }
        
        public Map<String, Object> getProperties() { return properties; }
        public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    }

    public static class Rabbit {
        private String templateBeanName;
        private String exchange = "${topic}";
        private String routingKey = "${type}";

        public String getTemplateBeanName() { return templateBeanName; }
        public void setTemplateBeanName(String templateBeanName) { this.templateBeanName = templateBeanName; }
        public String getExchange() { return exchange; }
        public void setExchange(String exchange) { this.exchange = exchange; }
        public String getRoutingKey() { return routingKey; }
        public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
    }

    public static class Sqs {
        private String clientBeanName;
        
        @Pattern(regexp = "^https://sqs\\.[a-z0-9-]+\\.amazonaws\\.com/\\d+/.+$", 
                message = "Queue URL must be a valid SQS URL format")
        private String queueUrl;
        
        @Size(min = 1, max = 80, message = "Queue name must be between 1 and 80 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+(\\.fifo)?$", 
                message = "Queue name can only contain alphanumeric characters, hyphens, underscores, and optional .fifo suffix")
        private String queueName;

        public String getClientBeanName() { return clientBeanName; }
        public void setClientBeanName(String clientBeanName) { this.clientBeanName = clientBeanName; }
        public String getQueueUrl() { return queueUrl; }
        public void setQueueUrl(String queueUrl) { this.queueUrl = queueUrl; }
        public String getQueueName() { return queueName; }
        public void setQueueName(String queueName) { this.queueName = queueName; }
    }

    public static class Consumer {
        private boolean enabled = false;
        
        @NotBlank(message = "Type header cannot be blank")
        @Size(max = 255, message = "Type header must not exceed 255 characters")
        private String typeHeader = "event_type";
        
        @NotBlank(message = "Key header cannot be blank")
        @Size(max = 255, message = "Key header must not exceed 255 characters")
        private String keyHeader = "event_key";
        
        @Valid
        private final KafkaConsumer kafka = new KafkaConsumer();
        
        @Valid
        private final RabbitConsumer rabbit = new RabbitConsumer();
        
        @Valid
        private final SqsConsumer sqs = new SqsConsumer();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getTypeHeader() { return typeHeader; }
        public void setTypeHeader(String typeHeader) { this.typeHeader = typeHeader; }
        public String getKeyHeader() { return keyHeader; }
        public void setKeyHeader(String keyHeader) { this.keyHeader = keyHeader; }
        public KafkaConsumer getKafka() { return kafka; }
        public RabbitConsumer getRabbit() { return rabbit; }
        public SqsConsumer getSqs() { return sqs; }

        public static class KafkaConsumer {
            private List<String> topics = new ArrayList<>();
            private String consumerFactoryBeanName; // optional override
            private String groupId; // optional

            public List<String> getTopics() { return topics; }
            public void setTopics(List<String> topics) { this.topics = topics; }
            public String getConsumerFactoryBeanName() { return consumerFactoryBeanName; }
            public void setConsumerFactoryBeanName(String consumerFactoryBeanName) { this.consumerFactoryBeanName = consumerFactoryBeanName; }
            public String getGroupId() { return groupId; }
            public void setGroupId(String groupId) { this.groupId = groupId; }
        }

        public static class RabbitConsumer {
            private List<String> queues = new ArrayList<>();

            public List<String> getQueues() { return queues; }
            public void setQueues(List<String> queues) { this.queues = queues; }
        }

        public static class SqsConsumer {
            @Pattern(regexp = "^https://sqs\\.[a-z0-9-]+\\.amazonaws\\.com/\\d+/.+$", 
                    message = "Queue URL must be a valid SQS URL format")
            private String queueUrl;
            
            @Size(min = 1, max = 80, message = "Queue name must be between 1 and 80 characters")
            @Pattern(regexp = "^[a-zA-Z0-9_-]+(\\.fifo)?$", 
                    message = "Queue name can only contain alphanumeric characters, hyphens, underscores, and optional .fifo suffix")
            private String queueName;
            
            @Min(value = 0, message = "Wait time seconds must be between 0 and 20")
            @Max(value = 20, message = "Wait time seconds must be between 0 and 20")
            private Integer waitTimeSeconds = 10;
            
            @Min(value = 1, message = "Max messages must be between 1 and 10")
            @Max(value = 10, message = "Max messages must be between 1 and 10")
            private Integer maxMessages = 10;
            
            @Min(value = 100, message = "Poll delay must be at least 100 milliseconds")
            @Max(value = 300000, message = "Poll delay must not exceed 300000 milliseconds (5 minutes)")
            private Long pollDelayMillis = 1000L;

            public String getQueueUrl() { return queueUrl; }
            public void setQueueUrl(String queueUrl) { this.queueUrl = queueUrl; }
            public String getQueueName() { return queueName; }
            public void setQueueName(String queueName) { this.queueName = queueName; }
            public Integer getWaitTimeSeconds() { return waitTimeSeconds; }
            public void setWaitTimeSeconds(Integer waitTimeSeconds) { this.waitTimeSeconds = waitTimeSeconds; }
            public Integer getMaxMessages() { return maxMessages; }
            public void setMaxMessages(Integer maxMessages) { this.maxMessages = maxMessages; }
            public Long getPollDelayMillis() { return pollDelayMillis; }
            public void setPollDelayMillis(Long pollDelayMillis) { this.pollDelayMillis = pollDelayMillis; }
        }
    }
}
