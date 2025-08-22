package com.catalis.common.domain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "catalis.stepevents")
public class StepEventsProperties {

    public enum Adapter { AUTO, APPLICATION_EVENT, KAFKA, RABBIT, SQS, NOOP }

    private boolean enabled = true;
    private Adapter adapter = Adapter.AUTO;

    private final Kafka kafka = new Kafka();
    private final Rabbit rabbit = new Rabbit();
    private final Sqs sqs = new Sqs();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Adapter getAdapter() { return adapter; }
    public void setAdapter(Adapter adapter) { this.adapter = adapter; }

    public Kafka getKafka() { return kafka; }
    public Rabbit getRabbit() { return rabbit; }
    public Sqs getSqs() { return sqs; }

    public static class Kafka {
        /** Optional bean name of KafkaTemplate to use. If empty, will resolve by type. */
        private String templateBeanName;
        /** If true and supported, use Spring Messaging Message with headers; otherwise falls back to send(topic,key,payload). */
        private boolean useMessagingIfAvailable = true;

        public String getTemplateBeanName() { return templateBeanName; }
        public void setTemplateBeanName(String templateBeanName) { this.templateBeanName = templateBeanName; }

        public boolean isUseMessagingIfAvailable() { return useMessagingIfAvailable; }
        public void setUseMessagingIfAvailable(boolean useMessagingIfAvailable) { this.useMessagingIfAvailable = useMessagingIfAvailable; }
    }

    public static class Rabbit {
        /** Optional bean name of RabbitTemplate to use. If empty, will resolve by type. */
        private String templateBeanName;
        /** Exchange to publish to. Supports placeholders ${topic}, ${type}, ${key}. Default: ${topic}. */
        private String exchange = "${topic}";
        /** Routing key to use. Supports placeholders ${topic}, ${type}, ${key}. Default: ${type}. */
        private String routingKey = "${type}";

        public String getTemplateBeanName() { return templateBeanName; }
        public void setTemplateBeanName(String templateBeanName) { this.templateBeanName = templateBeanName; }

        public String getExchange() { return exchange; }
        public void setExchange(String exchange) { this.exchange = exchange; }

        public String getRoutingKey() { return routingKey; }
        public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
    }

    public static class Sqs {
        /** Optional bean name of SqsAsyncClient to use. If empty, will resolve by type. */
        private String clientBeanName;
        /** Direct queue URL. If not set, will try to resolve using queueName via GetQueueUrl. */
        private String queueUrl;
        /** Queue name. Default: use envelope.topic */
        private String queueName;

        public String getClientBeanName() { return clientBeanName; }
        public void setClientBeanName(String clientBeanName) { this.clientBeanName = clientBeanName; }

        public String getQueueUrl() { return queueUrl; }
        public void setQueueUrl(String queueUrl) { this.queueUrl = queueUrl; }

        public String getQueueName() { return queueName; }
        public void setQueueName(String queueName) { this.queueName = queueName; }
    }
}
