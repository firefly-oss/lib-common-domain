package com.firefly.common.domain.events.inbound;

import com.firefly.common.domain.events.DomainEventEnvelope;
import com.firefly.common.domain.events.DomainSpringEvent;
import com.firefly.common.domain.events.properties.DomainEventsProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Inbound Kafka subscriber that consumes messages and republishes as DomainSpringEvent.
 */
public class KafkaDomainEventsSubscriber implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(KafkaDomainEventsSubscriber.class);
    private final ApplicationContext ctx;
    private final DomainEventsProperties props;
    private final ApplicationEventPublisher events;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "kafka-events-subscriber");
        t.setDaemon(true);
        return t;
    });
    private volatile KafkaConsumer<String, String> consumer;

    public KafkaDomainEventsSubscriber(ApplicationContext ctx,
                                       DomainEventsProperties props,
                                       ApplicationEventPublisher events) {
        this.ctx = ctx;
        this.props = props;
        this.events = events;
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        
        executor.submit(() -> {
            try {
                // Create consumer with properties from application context
                Properties consumerProps = getConsumerProperties();
                consumer = new KafkaConsumer<>(consumerProps);
                
                // Subscribe to topics
                List<String> topics = getTopicsToSubscribe();
                if (topics.isEmpty()) {
                    log.warn("No Kafka topics configured for domain events subscription");
                    return;
                }
                
                consumer.subscribe(topics);
                log.info("Kafka domain events subscriber started, subscribed to topics: {}", topics);
                
                while (running.get()) {
                    try {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                        for (ConsumerRecord<String, String> record : records) {
                            processRecord(record);
                        }
                        consumer.commitSync();
                    } catch (Exception e) {
                        if (running.get()) {
                            log.error("Error polling Kafka messages", e);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error starting Kafka domain events subscriber", e);
            } finally {
                if (consumer != null) {
                    try {
                        consumer.close();
                    } catch (Exception e) {
                        log.warn("Error closing Kafka consumer", e);
                    }
                }
            }
        });
    }

    private void processRecord(ConsumerRecord<String, String> record) {
        try {
            Map<String, Object> headers = new HashMap<>();
            
            // Extract headers
            record.headers().forEach(header -> {
                headers.put(header.key(), new String(header.value()));
            });
            
            // Extract event metadata from headers
            String type = getHeaderValue(headers, "event-type");
            String key = record.key();
            String topic = record.topic();
            
            DomainEventEnvelope envelope = DomainEventEnvelope.builder()
                    .topic(topic)
                    .type(type)
                    .key(key)
                    .payload(record.value())
                    .headers(headers)
                    .build();
                    
            events.publishEvent(new DomainSpringEvent(envelope));
            
            log.debug("Processed Kafka message: topic={}, partition={}, offset={}, type={}, key={}", 
                     record.topic(), record.partition(), record.offset(), type, key);
                     
        } catch (Exception e) {
            log.error("Error processing Kafka record: topic={}, partition={}, offset={}", 
                     record.topic(), record.partition(), record.offset(), e);
        }
    }
    
    private String getHeaderValue(Map<String, Object> headers, String key) {
        Object value = headers.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private Properties getConsumerProperties() {
        Properties props = new Properties();
        
        // Default consumer properties
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "earliest");
        props.put("enable.auto.commit", "false");
        props.put("group.id", getConsumerGroupId());
        
        // Get bootstrap servers from consumer factory if available
        try {
            Object consumerFactory = ctx.getBean("consumerFactory");
            if (consumerFactory != null) {
                // Try to extract properties from existing consumer factory
                @SuppressWarnings("unchecked")
                Map<String, Object> factoryProps = (Map<String, Object>) consumerFactory.getClass()
                        .getMethod("getConfigurationProperties")
                        .invoke(consumerFactory);
                        
                if (factoryProps != null) {
                    factoryProps.forEach((key, value) -> {
                        if (key.equals("bootstrap.servers") || key.equals("group.id")) {
                            props.put(key, value);
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract properties from consumer factory, using defaults");
        }
        
        // Override with specific consumer properties if configured
        DomainEventsProperties.Consumer.KafkaConsumer kafkaConsumer = this.props.getConsumer().getKafka();
        if (kafkaConsumer != null) {
            if (kafkaConsumer.getGroupId() != null) {
                props.put("group.id", kafkaConsumer.getGroupId());
            }
        }
        
        return props;
    }
    
    private String getConsumerGroupId() {
        DomainEventsProperties.Consumer.KafkaConsumer kafkaConsumer = this.props.getConsumer().getKafka();
        if (kafkaConsumer != null && kafkaConsumer.getGroupId() != null) {
            return kafkaConsumer.getGroupId();
        }
        return "domain-events-consumer";
    }
    
    private List<String> getTopicsToSubscribe() {
        List<String> topics = new ArrayList<>();
        
        DomainEventsProperties.Consumer.KafkaConsumer kafkaConsumer = this.props.getConsumer().getKafka();
        if (kafkaConsumer != null && kafkaConsumer.getTopics() != null) {
            topics.addAll(kafkaConsumer.getTopics());
        }
        
        // If no specific topics configured, use a default pattern or empty list
        if (topics.isEmpty()) {
            log.debug("No specific Kafka topics configured for domain events consumer");
        }
        
        return topics;
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("Stopping Kafka domain events subscriber");
            executor.shutdown();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}