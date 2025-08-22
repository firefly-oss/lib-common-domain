package com.catalis.common.domain.actuator.info;

import com.catalis.common.domain.events.properties.DomainEventsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventsInfoContributorTest {

    private DomainEventsProperties properties;
    private DomainEventsInfoContributor infoContributor;
    private Info.Builder infoBuilder;

    @BeforeEach
    void setUp() {
        properties = new DomainEventsProperties();
        infoContributor = new DomainEventsInfoContributor(properties);
        infoBuilder = new Info.Builder();
    }

    @Test
    void shouldContributeBasicInformation() {
        // Given
        properties.setEnabled(true);
        properties.setAdapter(DomainEventsProperties.Adapter.KAFKA);

        // When
        infoContributor.contribute(infoBuilder);
        Info info = infoBuilder.build();

        // Then
        Map<String, Object> domainEventsInfo = (Map<String, Object>) info.getDetails().get("domainEvents");
        assertThat(domainEventsInfo).containsEntry("enabled", true);
        assertThat(domainEventsInfo).containsEntry("adapter", "KAFKA");
    }

    @Test
    void shouldContributeDisabledConfiguration() {
        // Given
        properties.setEnabled(false);
        properties.setAdapter(DomainEventsProperties.Adapter.NOOP);

        // When
        infoContributor.contribute(infoBuilder);
        Info info = infoBuilder.build();

        // Then
        Map<String, Object> domainEventsInfo = (Map<String, Object>) info.getDetails().get("domainEvents");
        assertThat(domainEventsInfo).containsEntry("enabled", false);
        assertThat(domainEventsInfo).containsEntry("adapter", "NOOP");
    }

    @Test
    void shouldNotIncludeAdapterDetailsForNoopAdapter() {
        // Given
        properties.setEnabled(true);
        properties.setAdapter(DomainEventsProperties.Adapter.NOOP);

        // When
        infoContributor.contribute(infoBuilder);
        Info info = infoBuilder.build();

        // Then
        Map<String, Object> domainEventsInfo = (Map<String, Object>) info.getDetails().get("domainEvents");
        assertThat(domainEventsInfo).doesNotContainKey("adapters");
    }

    @Test
    void shouldContributeKafkaAdapterConfiguration() {
        // Given
        properties.setEnabled(true);
        properties.setAdapter(DomainEventsProperties.Adapter.KAFKA);
        properties.getKafka().setTemplateBeanName("customKafkaTemplate");
        properties.getKafka().setUseMessagingIfAvailable(false);

        // When
        infoContributor.contribute(infoBuilder);
        Info info = infoBuilder.build();

        // Then
        Map<String, Object> domainEventsInfo = (Map<String, Object>) info.getDetails().get("domainEvents");
        Map<String, Object> adapters = (Map<String, Object>) domainEventsInfo.get("adapters");
        Map<String, Object> kafkaInfo = (Map<String, Object>) adapters.get("kafka");
        
        assertThat(kafkaInfo).containsEntry("templateBeanName", "customKafkaTemplate");
        assertThat(kafkaInfo).containsEntry("useMessagingIfAvailable", false);
    }

    @Test
    void shouldContributeRabbitAdapterConfiguration() {
        // Given
        properties.setEnabled(true);
        properties.setAdapter(DomainEventsProperties.Adapter.RABBIT);
        properties.getRabbit().setTemplateBeanName("customRabbitTemplate");
        properties.getRabbit().setExchange("test-exchange");
        properties.getRabbit().setRoutingKey("test.routing.key");

        // When
        infoContributor.contribute(infoBuilder);
        Info info = infoBuilder.build();

        // Then
        Map<String, Object> domainEventsInfo = (Map<String, Object>) info.getDetails().get("domainEvents");
        Map<String, Object> adapters = (Map<String, Object>) domainEventsInfo.get("adapters");
        Map<String, Object> rabbitInfo = (Map<String, Object>) adapters.get("rabbit");
        
        assertThat(rabbitInfo).containsEntry("templateBeanName", "customRabbitTemplate");
        assertThat(rabbitInfo).containsEntry("exchange", "test-exchange");
        assertThat(rabbitInfo).containsEntry("routingKey", "test.routing.key");
    }

    @Test
    void shouldContributeSqsAdapterConfiguration() {
        // Given
        properties.setEnabled(true);
        properties.setAdapter(DomainEventsProperties.Adapter.SQS);
        properties.getSqs().setClientBeanName("customSqsClient");
        properties.getSqs().setQueueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/test-queue");
        properties.getSqs().setQueueName("test-queue");

        // When
        infoContributor.contribute(infoBuilder);
        Info info = infoBuilder.build();

        // Then
        Map<String, Object> domainEventsInfo = (Map<String, Object>) info.getDetails().get("domainEvents");
        Map<String, Object> adapters = (Map<String, Object>) domainEventsInfo.get("adapters");
        Map<String, Object> sqsInfo = (Map<String, Object>) adapters.get("sqs");
        
        assertThat(sqsInfo).containsEntry("clientBeanName", "customSqsClient");
        assertThat(sqsInfo).containsEntry("queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789012/test-queue");
        assertThat(sqsInfo).containsEntry("queueName", "test-queue");
    }

    @Test
    void shouldContributeConsumerConfigurationWhenDisabled() {
        // Given
        properties.setEnabled(true);
        properties.setAdapter(DomainEventsProperties.Adapter.KAFKA);
        properties.getConsumer().setEnabled(false);
        properties.getConsumer().setTypeHeader("X-Event-Type");
        properties.getConsumer().setKeyHeader("X-Event-Key");

        // When
        infoContributor.contribute(infoBuilder);
        Info info = infoBuilder.build();

        // Then
        Map<String, Object> domainEventsInfo = (Map<String, Object>) info.getDetails().get("domainEvents");
        Map<String, Object> consumerInfo = (Map<String, Object>) domainEventsInfo.get("consumer");
        
        assertThat(consumerInfo).containsEntry("enabled", false);
        assertThat(consumerInfo).containsEntry("typeHeader", "X-Event-Type");
        assertThat(consumerInfo).containsEntry("keyHeader", "X-Event-Key");
        assertThat(consumerInfo).doesNotContainKey("adapters");
    }

    @Test
    void shouldContributeConsumerConfigurationWhenEnabled() {
        // Given
        properties.setEnabled(true);
        properties.setAdapter(DomainEventsProperties.Adapter.KAFKA);
        properties.getConsumer().setEnabled(true);
        
        // Kafka consumer config
        properties.getConsumer().getKafka().setTopics(List.of("topic1", "topic2"));
        properties.getConsumer().getKafka().setConsumerFactoryBeanName("customConsumerFactory");
        properties.getConsumer().getKafka().setGroupId("test-group");
        
        // Rabbit consumer config
        properties.getConsumer().getRabbit().setQueues(List.of("queue1", "queue2"));
        
        // SQS consumer config
        properties.getConsumer().getSqs().setQueueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/consumer-queue");
        properties.getConsumer().getSqs().setQueueName("consumer-queue");
        properties.getConsumer().getSqs().setWaitTimeSeconds(20);
        properties.getConsumer().getSqs().setMaxMessages(5);
        properties.getConsumer().getSqs().setPollDelayMillis(2000L);

        // When
        infoContributor.contribute(infoBuilder);
        Info info = infoBuilder.build();

        // Then
        Map<String, Object> domainEventsInfo = (Map<String, Object>) info.getDetails().get("domainEvents");
        Map<String, Object> consumerInfo = (Map<String, Object>) domainEventsInfo.get("consumer");
        Map<String, Object> consumerAdapters = (Map<String, Object>) consumerInfo.get("adapters");
        
        // Kafka consumer assertions
        Map<String, Object> kafkaConsumerInfo = (Map<String, Object>) consumerAdapters.get("kafka");
        assertThat(kafkaConsumerInfo).containsEntry("topics", List.of("topic1", "topic2"));
        assertThat(kafkaConsumerInfo).containsEntry("consumerFactoryBeanName", "customConsumerFactory");
        assertThat(kafkaConsumerInfo).containsEntry("groupId", "test-group");
        
        // Rabbit consumer assertions
        Map<String, Object> rabbitConsumerInfo = (Map<String, Object>) consumerAdapters.get("rabbit");
        assertThat(rabbitConsumerInfo).containsEntry("queues", List.of("queue1", "queue2"));
        
        // SQS consumer assertions
        Map<String, Object> sqsConsumerInfo = (Map<String, Object>) consumerAdapters.get("sqs");
        assertThat(sqsConsumerInfo).containsEntry("queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789012/consumer-queue");
        assertThat(sqsConsumerInfo).containsEntry("queueName", "consumer-queue");
        assertThat(sqsConsumerInfo).containsEntry("waitTimeSeconds", 20);
        assertThat(sqsConsumerInfo).containsEntry("maxMessages", 5);
        assertThat(sqsConsumerInfo).containsEntry("pollDelayMillis", 2000L);
    }

    @Test
    void shouldContributeAllAdapterConfigurationsRegardlessOfSelectedAdapter() {
        // Given - using APPLICATION_EVENT adapter but all adapters should still be included in info
        properties.setEnabled(true);
        properties.setAdapter(DomainEventsProperties.Adapter.APPLICATION_EVENT);

        // When
        infoContributor.contribute(infoBuilder);
        Info info = infoBuilder.build();

        // Then
        Map<String, Object> domainEventsInfo = (Map<String, Object>) info.getDetails().get("domainEvents");
        Map<String, Object> adapters = (Map<String, Object>) domainEventsInfo.get("adapters");
        
        assertThat(adapters).containsKeys("kafka", "rabbit", "sqs");
    }
}