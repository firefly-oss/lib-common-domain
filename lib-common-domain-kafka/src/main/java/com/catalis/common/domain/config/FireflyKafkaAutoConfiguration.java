package com.catalis.common.domain.config;

import com.catalis.common.domain.events.properties.DomainEventsProperties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Auto-configuration for Kafka domain events using firefly properties.
 * This configuration creates KafkaTemplate beans when bootstrap servers are configured
 * through firefly.events.kafka.bootstrap-servers property.
 */
@AutoConfiguration(before = KafkaAutoConfiguration.class)
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(DomainEventsProperties.class)
@ConditionalOnProperty(prefix = "firefly.events.kafka", name = "bootstrap-servers")
public class FireflyKafkaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "fireflyKafkaProducerFactory")
    public ProducerFactory<Object, Object> fireflyKafkaProducerFactory(DomainEventsProperties properties) {
        DomainEventsProperties.Kafka kafkaProps = properties.getKafka();
        
        Map<String, Object> props = new HashMap<>();
        
        // Required bootstrap servers
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProps.getBootstrapServers());
        
        // Serializers with defaults
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
                  StringUtils.hasText(kafkaProps.getKeySerializer()) ? 
                  kafkaProps.getKeySerializer() : StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
                  StringUtils.hasText(kafkaProps.getValueSerializer()) ? 
                  kafkaProps.getValueSerializer() : StringSerializer.class.getName());
        
        // Optional properties
        if (kafkaProps.getRetries() != null) {
            props.put(ProducerConfig.RETRIES_CONFIG, kafkaProps.getRetries());
        }
        if (kafkaProps.getBatchSize() != null) {
            props.put(ProducerConfig.BATCH_SIZE_CONFIG, kafkaProps.getBatchSize());
        }
        if (kafkaProps.getLingerMs() != null) {
            props.put(ProducerConfig.LINGER_MS_CONFIG, kafkaProps.getLingerMs());
        }
        if (kafkaProps.getBufferMemory() != null) {
            props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, kafkaProps.getBufferMemory());
        }
        if (StringUtils.hasText(kafkaProps.getAcks())) {
            props.put(ProducerConfig.ACKS_CONFIG, kafkaProps.getAcks());
        }
        
        // Add any additional properties from the properties map
        if (kafkaProps.getProperties() != null && !kafkaProps.getProperties().isEmpty()) {
            props.putAll(kafkaProps.getProperties());
        }
        
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    @ConditionalOnMissingBean(name = "fireflyKafkaTemplate")
    public KafkaTemplate<Object, Object> fireflyKafkaTemplate(
            ProducerFactory<Object, Object> fireflyKafkaProducerFactory) {
        return new KafkaTemplate<>(fireflyKafkaProducerFactory);
    }

    @Bean(name = "kafkaTemplate")
    @ConditionalOnMissingBean(name = "kafkaTemplate")
    public KafkaTemplate<Object, Object> kafkaTemplate(
            KafkaTemplate<Object, Object> fireflyKafkaTemplate) {
        // Provide the default kafkaTemplate bean if none exists
        return fireflyKafkaTemplate;
    }
}