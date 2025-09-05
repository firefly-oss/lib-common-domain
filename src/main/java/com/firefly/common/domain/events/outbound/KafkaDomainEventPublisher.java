/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.common.domain.events.outbound;

import com.firefly.common.domain.events.DomainEventEnvelope;
import com.firefly.common.domain.events.properties.DomainEventsProperties;
import com.firefly.common.domain.util.DomainEventAdapterUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.support.RetryTemplate;
import reactor.core.publisher.Mono;

/**
 * Apache Kafka adapter for DomainEventPublisher using Spring Kafka KafkaTemplate.
 */
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaDomainEventPublisher.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ApplicationContext ctx;
    private final DomainEventsProperties.Kafka props;
    private final RetryTemplate retryTemplate;

    @SuppressWarnings("unchecked")
    public KafkaDomainEventPublisher(ApplicationContext ctx, DomainEventsProperties.Kafka props) {
        this.ctx = ctx;
        this.props = props;
        Object template = DomainEventAdapterUtils.resolveBean(
                ctx,
                props.getTemplateBeanName(),
                "org.springframework.kafka.core.KafkaTemplate"
        );
        if (template == null) {
            throw new IllegalStateException("Kafka adapter selected but KafkaTemplate bean was not found.");
        }
        this.kafkaTemplate = (KafkaTemplate<String, String>) template;
        
        // Get retry template if available, otherwise use null (no retry)
        RetryTemplate retryTemplateBean = null;
        try {
            retryTemplateBean = ctx.getBean("domainEventsRetryTemplate", RetryTemplate.class);
            log.debug("Kafka publisher configured with retry template");
        } catch (Exception e) {
            log.debug("Kafka publisher configured without retry template");
        }
        this.retryTemplate = retryTemplateBean;
    }

    @Override
    public Mono<Void> publish(DomainEventEnvelope e) {
        if (retryTemplate != null) {
            // Use retry template for resilient publishing
            return Mono.fromCallable(() -> retryTemplate.execute(retryContext -> {
                retryContext.setAttribute("operationName", "kafka-event-publish:" + e.getTopic() + ":" + e.getType());
                return trySendBlocking(e);
            }))
            .then()
            .onErrorMap(throwable -> {
                log.error("Failed to publish event after retries: topic={}, type={}, key={}", 
                         e.getTopic(), e.getType(), e.getKey(), throwable);
                return throwable;
            });
        } else {
            // Fallback to reactive logic without retry
            return trySendReactive(e)
                    .onErrorMap(ex -> {
                        log.error("Failed to publish event to Kafka: topic={}, type={}, key={}: {}", 
                                e.getTopic(), e.getType(), e.getKey(), ex.getMessage());
                        return ex;
                    });
        }
    }

    private Void trySendBlocking(DomainEventEnvelope e) {
        try {
            String topic = resolveTopic(e);
            String messageValue = serializePayload(e);
            String messageKey = e.getKey();
            
            log.debug("Attempting to send event to Kafka: topic={}, type={}, key={}", 
                     e.getTopic(), e.getType(), e.getKey());
            
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, messageKey, messageValue);
            
            // Add headers if available
            if (e.getHeaders() != null && !e.getHeaders().isEmpty()) {
                e.getHeaders().forEach((key, value) -> {
                    if (value != null) {
                        record.headers().add(key, String.valueOf(value).getBytes());
                    }
                });
            }
            
            // Add event type as header
            if (e.getType() != null) {
                record.headers().add("event-type", e.getType().getBytes());
            }
            
            SendResult<String, String> result = kafkaTemplate.send(record).get(); // Blocking call for retry template
            
            log.debug("Successfully sent event to Kafka: topic={}, type={}, partition={}, offset={}", 
                     e.getTopic(), e.getType(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            
            return null;
        } catch (Exception ex) {
            log.error("Failed to send event to Kafka: topic={}, type={}, key={}: {}", 
                     e.getTopic(), e.getType(), e.getKey(), ex.getMessage());
            throw new RuntimeException("Kafka send failed", ex);
        }
    }

    private Mono<Void> trySendReactive(DomainEventEnvelope e) {
        String topic = resolveTopic(e);
        String messageValue = serializePayload(e);
        String messageKey = e.getKey();
        
        log.debug("Attempting to send event to Kafka: topic={}, type={}, key={}", 
                 e.getTopic(), e.getType(), e.getKey());
        
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, messageKey, messageValue);
        
        // Add headers if available
        if (e.getHeaders() != null && !e.getHeaders().isEmpty()) {
            e.getHeaders().forEach((key, value) -> {
                if (value != null) {
                    record.headers().add(key, String.valueOf(value).getBytes());
                }
            });
        }
        
        // Add event type as header
        if (e.getType() != null) {
            record.headers().add("event-type", e.getType().getBytes());
        }
        
        return Mono.fromFuture(kafkaTemplate.send(record))
                .doOnSuccess(result -> log.debug("Successfully sent event to Kafka: topic={}, type={}, partition={}, offset={}", 
                                               e.getTopic(), e.getType(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset()))
                .then();
    }

    private String resolveTopic(DomainEventEnvelope e) {
        // Use the event's topic directly for Kafka
        return e.getTopic();
    }

    private String serializePayload(DomainEventEnvelope e) {
        try {
            Object mapperObj = DomainEventAdapterUtils.resolveBean(ctx, null,
                    "com.fasterxml.jackson.databind.ObjectMapper");
            if (mapperObj instanceof ObjectMapper mapper) {
                return mapper.writeValueAsString(e.getPayload());
            }
        } catch (Exception ex) {
            log.warn("Failed to serialize payload using ObjectMapper, falling back to String.valueOf: {}", ex.getMessage());
        }
        return String.valueOf(e.getPayload());
    }
}