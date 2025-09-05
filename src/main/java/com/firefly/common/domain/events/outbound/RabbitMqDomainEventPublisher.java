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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.retry.support.RetryTemplate;
import reactor.core.publisher.Mono;

/**
 * RabbitMQ adapter for DomainEventPublisher using Spring AMQP RabbitTemplate.
 */
public class RabbitMqDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqDomainEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final ApplicationContext ctx;
    private final DomainEventsProperties.Rabbit props;
    private final RetryTemplate retryTemplate;

    public RabbitMqDomainEventPublisher(ApplicationContext ctx, DomainEventsProperties.Rabbit props) {
        this.ctx = ctx;
        this.props = props;
        Object template = DomainEventAdapterUtils.resolveBean(
                ctx,
                props.getTemplateBeanName(),
                "org.springframework.amqp.rabbit.core.RabbitTemplate"
        );
        if (template == null) {
            throw new IllegalStateException("RabbitMQ adapter selected but RabbitTemplate bean was not found.");
        }
        this.rabbitTemplate = (RabbitTemplate) template;
        
        // Get retry template if available, otherwise use null (no retry)
        RetryTemplate retryTemplateBean = null;
        try {
            retryTemplateBean = ctx.getBean("domainEventsRetryTemplate", RetryTemplate.class);
            log.debug("RabbitMQ publisher configured with retry template");
        } catch (Exception e) {
            log.debug("RabbitMQ publisher configured without retry template");
        }
        this.retryTemplate = retryTemplateBean;
    }

    @Override
    public Mono<Void> publish(DomainEventEnvelope e) {
        if (retryTemplate != null) {
            // Use retry template for resilient publishing
            return Mono.fromCallable(() -> retryTemplate.execute(retryContext -> {
                retryContext.setAttribute("operationName", "rabbitmq-event-publish:" + e.getTopic() + ":" + e.getType());
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
                        log.error("Failed to publish event to RabbitMQ: topic={}, type={}, key={}: {}", 
                                e.getTopic(), e.getType(), e.getKey(), ex.getMessage());
                        return ex;
                    });
        }
    }

    private Void trySendBlocking(DomainEventEnvelope e) {
        try {
            String exchange = resolveExchange(e);
            String routingKey = resolveRoutingKey(e);
            String messageBody = serializePayload(e);
            
            log.debug("Attempting to send event to RabbitMQ: topic={}, type={}, key={}, exchange={}, routingKey={}", 
                     e.getTopic(), e.getType(), e.getKey(), exchange, routingKey);
            
            MessageProperties messageProperties = new MessageProperties();
            
            // Add headers if available
            if (e.getHeaders() != null && !e.getHeaders().isEmpty()) {
                e.getHeaders().forEach((key, value) -> {
                    if (value != null) {
                        messageProperties.setHeader(key, value);
                    }
                });
            }
            
            // Add event metadata as headers
            if (e.getType() != null) {
                messageProperties.setHeader("event-type", e.getType());
            }
            if (e.getTopic() != null) {
                messageProperties.setHeader("event-topic", e.getTopic());
            }
            if (e.getKey() != null) {
                messageProperties.setHeader("event-key", e.getKey());
            }
            
            Message message = new Message(messageBody.getBytes(), messageProperties);
            
            rabbitTemplate.send(exchange, routingKey, message);
            
            log.debug("Successfully sent event to RabbitMQ: topic={}, type={}, exchange={}, routingKey={}", 
                     e.getTopic(), e.getType(), exchange, routingKey);
            
            return null;
        } catch (Exception ex) {
            log.error("Failed to send event to RabbitMQ: topic={}, type={}, key={}: {}", 
                     e.getTopic(), e.getType(), e.getKey(), ex.getMessage());
            throw new RuntimeException("RabbitMQ send failed", ex);
        }
    }

    private Mono<Void> trySendReactive(DomainEventEnvelope e) {
        return Mono.fromCallable(() -> {
            String exchange = resolveExchange(e);
            String routingKey = resolveRoutingKey(e);
            String messageBody = serializePayload(e);
            
            log.debug("Attempting to send event to RabbitMQ: topic={}, type={}, key={}, exchange={}, routingKey={}", 
                     e.getTopic(), e.getType(), e.getKey(), exchange, routingKey);
            
            MessageProperties messageProperties = new MessageProperties();
            
            // Add headers if available
            if (e.getHeaders() != null && !e.getHeaders().isEmpty()) {
                e.getHeaders().forEach((key, value) -> {
                    if (value != null) {
                        messageProperties.setHeader(key, value);
                    }
                });
            }
            
            // Add event metadata as headers
            if (e.getType() != null) {
                messageProperties.setHeader("event-type", e.getType());
            }
            if (e.getTopic() != null) {
                messageProperties.setHeader("event-topic", e.getTopic());
            }
            if (e.getKey() != null) {
                messageProperties.setHeader("event-key", e.getKey());
            }
            
            Message message = new Message(messageBody.getBytes(), messageProperties);
            
            rabbitTemplate.send(exchange, routingKey, message);
            
            log.debug("Successfully sent event to RabbitMQ: topic={}, type={}, exchange={}, routingKey={}", 
                     e.getTopic(), e.getType(), exchange, routingKey);
            
            return null;
        })
        .then();
    }

    private String resolveExchange(DomainEventEnvelope e) {
        String configuredExchange = props.getExchange();
        if (configuredExchange != null && !configuredExchange.isEmpty()) {
            return configuredExchange;
        }
        // Use topic as exchange name if no specific exchange configured
        return e.getTopic();
    }

    private String resolveRoutingKey(DomainEventEnvelope e) {
        String configuredRoutingKey = props.getRoutingKey();
        if (configuredRoutingKey != null && !configuredRoutingKey.isEmpty()) {
            return configuredRoutingKey;
        }
        // Use event type as routing key if no specific routing key configured
        return e.getType() != null ? e.getType() : "";
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