package com.catalis.common.domain.events.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "firefly.events")
public class DomainEventsProperties {

    public enum Adapter { AUTO, APPLICATION_EVENT, KAFKA, RABBIT, SQS, NOOP }

    private boolean enabled = true;
    private Adapter adapter = Adapter.AUTO;

    private final Kafka kafka = new Kafka();
    private final Rabbit rabbit = new Rabbit();
    private final Sqs sqs = new Sqs();
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

        public String getTemplateBeanName() { return templateBeanName; }
        public void setTemplateBeanName(String templateBeanName) { this.templateBeanName = templateBeanName; }
        public boolean isUseMessagingIfAvailable() { return useMessagingIfAvailable; }
        public void setUseMessagingIfAvailable(boolean useMessagingIfAvailable) { this.useMessagingIfAvailable = useMessagingIfAvailable; }
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
        private String queueUrl;
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
        private String typeHeader = "event_type";
        private String keyHeader = "event_key";
        private final KafkaConsumer kafka = new KafkaConsumer();
        private final RabbitConsumer rabbit = new RabbitConsumer();
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
            private String queueUrl;
            private String queueName;
            private Integer waitTimeSeconds = 10;
            private Integer maxMessages = 10;
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
