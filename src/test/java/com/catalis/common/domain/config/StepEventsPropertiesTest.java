package com.catalis.common.domain.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StepEventsPropertiesTest {

    private StepEventsProperties properties;

    @BeforeEach
    void setUp() {
        properties = new StepEventsProperties();
    }

    @Test
    void shouldHaveCorrectDefaultValues() {
        // Then
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getAdapter()).isEqualTo(StepEventsProperties.Adapter.AUTO);
    }

    @Test
    void shouldSetAndGetBasicProperties() {
        // When
        properties.setEnabled(false);
        properties.setAdapter(StepEventsProperties.Adapter.KAFKA);

        // Then
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getAdapter()).isEqualTo(StepEventsProperties.Adapter.KAFKA);
    }

    @Test
    void shouldProvideNestedConfigurationObjects() {
        // When
        StepEventsProperties.Kafka kafka = properties.getKafka();
        StepEventsProperties.Rabbit rabbit = properties.getRabbit();
        StepEventsProperties.Sqs sqs = properties.getSqs();

        // Then
        assertThat(kafka).isNotNull();
        assertThat(rabbit).isNotNull();
        assertThat(sqs).isNotNull();
    }

    @Test
    void shouldReturnSameNestedInstancesOnMultipleCalls() {
        // When
        StepEventsProperties.Kafka kafka1 = properties.getKafka();
        StepEventsProperties.Kafka kafka2 = properties.getKafka();

        // Then
        assertThat(kafka1).isSameAs(kafka2);
    }

    @Test
    void kafkaConfigurationShouldHaveCorrectDefaults() {
        // When
        StepEventsProperties.Kafka kafka = properties.getKafka();

        // Then
        assertThat(kafka.getTemplateBeanName()).isNull();
        assertThat(kafka.isUseMessagingIfAvailable()).isTrue();
    }

    @Test
    void kafkaConfigurationShouldSetAndGetProperties() {
        // Given
        StepEventsProperties.Kafka kafka = properties.getKafka();

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
        StepEventsProperties.Rabbit rabbit = properties.getRabbit();

        // Then
        assertThat(rabbit.getTemplateBeanName()).isNull();
        assertThat(rabbit.getExchange()).isEqualTo("${topic}");
        assertThat(rabbit.getRoutingKey()).isEqualTo("${type}");
    }

    @Test
    void rabbitConfigurationShouldSetAndGetProperties() {
        // Given
        StepEventsProperties.Rabbit rabbit = properties.getRabbit();

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
        StepEventsProperties.Sqs sqs = properties.getSqs();

        // Then
        assertThat(sqs.getClientBeanName()).isNull();
        assertThat(sqs.getQueueUrl()).isNull();
        assertThat(sqs.getQueueName()).isNull();
    }

    @Test
    void sqsConfigurationShouldSetAndGetProperties() {
        // Given
        StepEventsProperties.Sqs sqs = properties.getSqs();

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
    void shouldHaveAllAdapterEnumValues() {
        // Then
        assertThat(StepEventsProperties.Adapter.values()).containsExactly(
                StepEventsProperties.Adapter.AUTO,
                StepEventsProperties.Adapter.APPLICATION_EVENT,
                StepEventsProperties.Adapter.KAFKA,
                StepEventsProperties.Adapter.RABBIT,
                StepEventsProperties.Adapter.SQS,
                StepEventsProperties.Adapter.NOOP
        );
    }

    @Test
    void shouldSetAllAdapterEnumValues() {
        // Test each adapter enum value
        for (StepEventsProperties.Adapter adapter : StepEventsProperties.Adapter.values()) {
            // When
            properties.setAdapter(adapter);

            // Then
            assertThat(properties.getAdapter()).isEqualTo(adapter);
        }
    }

    @Test
    void shouldSupportComplexConfigurationScenario() {
        // Given
        properties.setEnabled(false);
        properties.setAdapter(StepEventsProperties.Adapter.RABBIT);
        
        // Configure Kafka
        properties.getKafka().setTemplateBeanName("myKafkaTemplate");
        properties.getKafka().setUseMessagingIfAvailable(false);
        
        // Configure Rabbit  
        properties.getRabbit().setTemplateBeanName("myRabbitTemplate");
        properties.getRabbit().setExchange("my-exchange");
        properties.getRabbit().setRoutingKey("my.routing.key");
        
        // Configure SQS
        properties.getSqs().setClientBeanName("mySqsClient");
        properties.getSqs().setQueueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/my-queue");
        properties.getSqs().setQueueName("my-queue");

        // Then - verify all properties are set correctly
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getAdapter()).isEqualTo(StepEventsProperties.Adapter.RABBIT);
        
        assertThat(properties.getKafka().getTemplateBeanName()).isEqualTo("myKafkaTemplate");
        assertThat(properties.getKafka().isUseMessagingIfAvailable()).isFalse();
        
        assertThat(properties.getRabbit().getTemplateBeanName()).isEqualTo("myRabbitTemplate");
        assertThat(properties.getRabbit().getExchange()).isEqualTo("my-exchange");
        assertThat(properties.getRabbit().getRoutingKey()).isEqualTo("my.routing.key");
        
        assertThat(properties.getSqs().getClientBeanName()).isEqualTo("mySqsClient");
        assertThat(properties.getSqs().getQueueUrl()).isEqualTo("https://sqs.us-east-1.amazonaws.com/123456789012/my-queue");
        assertThat(properties.getSqs().getQueueName()).isEqualTo("my-queue");
    }
}