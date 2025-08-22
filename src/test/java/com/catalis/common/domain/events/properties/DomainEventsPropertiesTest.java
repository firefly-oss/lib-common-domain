package com.catalis.common.domain.events.properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventsPropertiesTest {

    private DomainEventsProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DomainEventsProperties();
    }

    @Test
    void shouldHaveCorrectDefaultValues() {
        // Then
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getAdapter()).isEqualTo(DomainEventsProperties.Adapter.AUTO);
    }

    @Test
    void shouldSetAndGetBasicProperties() {
        // When
        properties.setEnabled(false);
        properties.setAdapter(DomainEventsProperties.Adapter.KAFKA);

        // Then
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getAdapter()).isEqualTo(DomainEventsProperties.Adapter.KAFKA);
    }

    @Test
    void shouldProvideNestedConfigurationObjects() {
        // When
        DomainEventsProperties.Kafka kafka = properties.getKafka();
        DomainEventsProperties.Rabbit rabbit = properties.getRabbit();
        DomainEventsProperties.Sqs sqs = properties.getSqs();
        DomainEventsProperties.Consumer consumer = properties.getConsumer();

        // Then
        assertThat(kafka).isNotNull();
        assertThat(rabbit).isNotNull();
        assertThat(sqs).isNotNull();
        assertThat(consumer).isNotNull();
    }

    @Test
    void kafkaConfigurationShouldHaveCorrectDefaults() {
        // When
        DomainEventsProperties.Kafka kafka = properties.getKafka();

        // Then
        assertThat(kafka.getTemplateBeanName()).isNull();
        assertThat(kafka.isUseMessagingIfAvailable()).isTrue();
    }

    @Test
    void kafkaConfigurationShouldSetAndGetProperties() {
        // Given
        DomainEventsProperties.Kafka kafka = properties.getKafka();

        // When
        kafka.setTemplateBeanName("customKafkaTemplate");
        kafka.setUseMessagingIfAvailable(false);

        // Then
        assertThat(kafka.getTemplateBeanName()).isEqualTo("customKafkaTemplate");
        assertThat(kafka.isUseMessagingIfAvailable()).isFalse();
    }

    @Test
    void rabbitConfigurationShouldHaveCorrectDefaults() {
        // When
        DomainEventsProperties.Rabbit rabbit = properties.getRabbit();

        // Then
        assertThat(rabbit.getTemplateBeanName()).isNull();
        assertThat(rabbit.getExchange()).isEqualTo("${topic}");
        assertThat(rabbit.getRoutingKey()).isEqualTo("${type}");
    }

    @Test
    void rabbitConfigurationShouldSetAndGetProperties() {
        // Given
        DomainEventsProperties.Rabbit rabbit = properties.getRabbit();

        // When
        rabbit.setTemplateBeanName("customRabbitTemplate");
        rabbit.setExchange("test-exchange");
        rabbit.setRoutingKey("test.routing.key");

        // Then
        assertThat(rabbit.getTemplateBeanName()).isEqualTo("customRabbitTemplate");
        assertThat(rabbit.getExchange()).isEqualTo("test-exchange");
        assertThat(rabbit.getRoutingKey()).isEqualTo("test.routing.key");
    }

    @Test
    void sqsConfigurationShouldHaveCorrectDefaults() {
        // When
        DomainEventsProperties.Sqs sqs = properties.getSqs();

        // Then
        assertThat(sqs.getClientBeanName()).isNull();
        assertThat(sqs.getQueueUrl()).isNull();
        assertThat(sqs.getQueueName()).isNull();
    }

    @Test
    void sqsConfigurationShouldSetAndGetProperties() {
        // Given
        DomainEventsProperties.Sqs sqs = properties.getSqs();

        // When
        sqs.setClientBeanName("customSqsClient");
        sqs.setQueueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/test-queue");
        sqs.setQueueName("test-queue");

        // Then
        assertThat(sqs.getClientBeanName()).isEqualTo("customSqsClient");
        assertThat(sqs.getQueueUrl()).isEqualTo("https://sqs.us-east-1.amazonaws.com/123456789012/test-queue");
        assertThat(sqs.getQueueName()).isEqualTo("test-queue");
    }

    @Test
    void consumerConfigurationShouldHaveCorrectDefaults() {
        // When
        DomainEventsProperties.Consumer consumer = properties.getConsumer();

        // Then
        assertThat(consumer.isEnabled()).isFalse();
        assertThat(consumer.getTypeHeader()).isEqualTo("event_type");
        assertThat(consumer.getKeyHeader()).isEqualTo("event_key");
    }

    @Test
    void consumerConfigurationShouldSetAndGetProperties() {
        // Given
        DomainEventsProperties.Consumer consumer = properties.getConsumer();

        // When
        consumer.setEnabled(true);
        consumer.setTypeHeader("X-Event-Type");
        consumer.setKeyHeader("X-Event-Key");

        // Then
        assertThat(consumer.isEnabled()).isTrue();
        assertThat(consumer.getTypeHeader()).isEqualTo("X-Event-Type");
        assertThat(consumer.getKeyHeader()).isEqualTo("X-Event-Key");
    }

    @Test
    void consumerShouldProvideNestedConfigurationObjects() {
        // When
        DomainEventsProperties.Consumer consumer = properties.getConsumer();
        DomainEventsProperties.Consumer.KafkaConsumer kafka = consumer.getKafka();
        DomainEventsProperties.Consumer.RabbitConsumer rabbit = consumer.getRabbit();
        DomainEventsProperties.Consumer.SqsConsumer sqs = consumer.getSqs();

        // Then
        assertThat(kafka).isNotNull();
        assertThat(rabbit).isNotNull();
        assertThat(sqs).isNotNull();
    }

    @Test
    void kafkaConsumerShouldHaveCorrectDefaults() {
        // When
        DomainEventsProperties.Consumer.KafkaConsumer kafka = properties.getConsumer().getKafka();

        // Then
        assertThat(kafka.getTopics()).isEmpty();
        assertThat(kafka.getConsumerFactoryBeanName()).isNull();
        assertThat(kafka.getGroupId()).isNull();
    }

    @Test
    void kafkaConsumerShouldSetAndGetProperties() {
        // Given
        DomainEventsProperties.Consumer.KafkaConsumer kafka = properties.getConsumer().getKafka();

        // When
        kafka.setTopics(List.of("topic1", "topic2"));
        kafka.setConsumerFactoryBeanName("customConsumerFactory");
        kafka.setGroupId("test-group");

        // Then
        assertThat(kafka.getTopics()).containsExactly("topic1", "topic2");
        assertThat(kafka.getConsumerFactoryBeanName()).isEqualTo("customConsumerFactory");
        assertThat(kafka.getGroupId()).isEqualTo("test-group");
    }

    @Test
    void rabbitConsumerShouldHaveCorrectDefaults() {
        // When
        DomainEventsProperties.Consumer.RabbitConsumer rabbit = properties.getConsumer().getRabbit();

        // Then
        assertThat(rabbit.getQueues()).isEmpty();
    }

    @Test
    void rabbitConsumerShouldSetAndGetProperties() {
        // Given
        DomainEventsProperties.Consumer.RabbitConsumer rabbit = properties.getConsumer().getRabbit();

        // When
        rabbit.setQueues(List.of("queue1", "queue2"));

        // Then
        assertThat(rabbit.getQueues()).containsExactly("queue1", "queue2");
    }

    @Test
    void sqsConsumerShouldHaveCorrectDefaults() {
        // When
        DomainEventsProperties.Consumer.SqsConsumer sqs = properties.getConsumer().getSqs();

        // Then
        assertThat(sqs.getQueueUrl()).isNull();
        assertThat(sqs.getQueueName()).isNull();
        assertThat(sqs.getWaitTimeSeconds()).isEqualTo(10);
        assertThat(sqs.getMaxMessages()).isEqualTo(10);
        assertThat(sqs.getPollDelayMillis()).isEqualTo(1000L);
    }

    @Test
    void sqsConsumerShouldSetAndGetProperties() {
        // Given
        DomainEventsProperties.Consumer.SqsConsumer sqs = properties.getConsumer().getSqs();

        // When
        sqs.setQueueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/consumer-queue");
        sqs.setQueueName("consumer-queue");
        sqs.setWaitTimeSeconds(20);
        sqs.setMaxMessages(5);
        sqs.setPollDelayMillis(2000L);

        // Then
        assertThat(sqs.getQueueUrl()).isEqualTo("https://sqs.us-east-1.amazonaws.com/123456789012/consumer-queue");
        assertThat(sqs.getQueueName()).isEqualTo("consumer-queue");
        assertThat(sqs.getWaitTimeSeconds()).isEqualTo(20);
        assertThat(sqs.getMaxMessages()).isEqualTo(5);
        assertThat(sqs.getPollDelayMillis()).isEqualTo(2000L);
    }

    @Test
    void shouldHaveAllAdapterEnumValues() {
        // Then
        assertThat(DomainEventsProperties.Adapter.values()).containsExactly(
                DomainEventsProperties.Adapter.AUTO,
                DomainEventsProperties.Adapter.APPLICATION_EVENT,
                DomainEventsProperties.Adapter.KAFKA,
                DomainEventsProperties.Adapter.RABBIT,
                DomainEventsProperties.Adapter.SQS,
                DomainEventsProperties.Adapter.NOOP
        );
    }

    @Test
    void shouldSetAllAdapterEnumValues() {
        // Test each adapter enum value
        for (DomainEventsProperties.Adapter adapter : DomainEventsProperties.Adapter.values()) {
            // When
            properties.setAdapter(adapter);

            // Then
            assertThat(properties.getAdapter()).isEqualTo(adapter);
        }
    }

    @Test
    void shouldSupportComplexConfigurationScenario() {
        // Given - full configuration setup
        properties.setEnabled(false);
        properties.setAdapter(DomainEventsProperties.Adapter.SQS);
        
        // Configure adapters
        properties.getKafka().setTemplateBeanName("myKafkaTemplate");
        properties.getKafka().setUseMessagingIfAvailable(false);
        
        properties.getRabbit().setTemplateBeanName("myRabbitTemplate");
        properties.getRabbit().setExchange("my-exchange");
        properties.getRabbit().setRoutingKey("my.routing.key");
        
        properties.getSqs().setClientBeanName("mySqsClient");
        properties.getSqs().setQueueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/my-queue");
        properties.getSqs().setQueueName("my-queue");
        
        // Configure consumer
        properties.getConsumer().setEnabled(true);
        properties.getConsumer().setTypeHeader("X-Type");
        properties.getConsumer().setKeyHeader("X-Key");
        
        properties.getConsumer().getKafka().setTopics(List.of("events", "notifications"));
        properties.getConsumer().getKafka().setConsumerFactoryBeanName("myConsumerFactory");
        properties.getConsumer().getKafka().setGroupId("my-group");
        
        properties.getConsumer().getRabbit().setQueues(List.of("event-queue", "notification-queue"));
        
        properties.getConsumer().getSqs().setQueueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/consumer-queue");
        properties.getConsumer().getSqs().setQueueName("consumer-queue");
        properties.getConsumer().getSqs().setWaitTimeSeconds(15);
        properties.getConsumer().getSqs().setMaxMessages(8);
        properties.getConsumer().getSqs().setPollDelayMillis(3000L);

        // Then - verify all properties are set correctly
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getAdapter()).isEqualTo(DomainEventsProperties.Adapter.SQS);
        
        // Verify adapter configurations
        assertThat(properties.getKafka().getTemplateBeanName()).isEqualTo("myKafkaTemplate");
        assertThat(properties.getKafka().isUseMessagingIfAvailable()).isFalse();
        
        assertThat(properties.getRabbit().getTemplateBeanName()).isEqualTo("myRabbitTemplate");
        assertThat(properties.getRabbit().getExchange()).isEqualTo("my-exchange");
        assertThat(properties.getRabbit().getRoutingKey()).isEqualTo("my.routing.key");
        
        assertThat(properties.getSqs().getClientBeanName()).isEqualTo("mySqsClient");
        assertThat(properties.getSqs().getQueueUrl()).isEqualTo("https://sqs.us-east-1.amazonaws.com/123456789012/my-queue");
        assertThat(properties.getSqs().getQueueName()).isEqualTo("my-queue");
        
        // Verify consumer configurations
        assertThat(properties.getConsumer().isEnabled()).isTrue();
        assertThat(properties.getConsumer().getTypeHeader()).isEqualTo("X-Type");
        assertThat(properties.getConsumer().getKeyHeader()).isEqualTo("X-Key");
        
        assertThat(properties.getConsumer().getKafka().getTopics()).containsExactly("events", "notifications");
        assertThat(properties.getConsumer().getKafka().getConsumerFactoryBeanName()).isEqualTo("myConsumerFactory");
        assertThat(properties.getConsumer().getKafka().getGroupId()).isEqualTo("my-group");
        
        assertThat(properties.getConsumer().getRabbit().getQueues()).containsExactly("event-queue", "notification-queue");
        
        assertThat(properties.getConsumer().getSqs().getQueueUrl()).isEqualTo("https://sqs.us-east-1.amazonaws.com/123456789012/consumer-queue");
        assertThat(properties.getConsumer().getSqs().getQueueName()).isEqualTo("consumer-queue");
        assertThat(properties.getConsumer().getSqs().getWaitTimeSeconds()).isEqualTo(15);
        assertThat(properties.getConsumer().getSqs().getMaxMessages()).isEqualTo(8);
        assertThat(properties.getConsumer().getSqs().getPollDelayMillis()).isEqualTo(3000L);
    }
}