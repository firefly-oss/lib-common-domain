package com.catalis.common.domain.events.outbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import com.catalis.common.domain.stepevents.StepEventAdapterUtils;
import com.catalis.common.domain.tracing.CorrelationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.support.RetryTemplate;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka adapter for DomainEventPublisher using Spring Kafka KafkaTemplate.
 */
public class KafkaTemplateDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaTemplateDomainEventPublisher.class);
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final boolean tryMessaging;
    private final RetryTemplate retryTemplate;
    private final CorrelationContext correlationContext;

    @SuppressWarnings("unchecked")
    public KafkaTemplateDomainEventPublisher(ApplicationContext ctx, boolean useMessagingIfAvailable, String templateBeanName) {
        Object template = StepEventAdapterUtils.resolveBean(
                ctx,
                templateBeanName,
                "org.springframework.kafka.core.KafkaTemplate"
        );
        if (template == null) {
            throw new IllegalStateException("Kafka adapter selected but KafkaTemplate bean was not found.");
        }
        this.kafkaTemplate = (KafkaTemplate<Object, Object>) template;
        this.tryMessaging = useMessagingIfAvailable;
        
        // Get retry template if available, otherwise use null (no retry)
        RetryTemplate retryTemplateBean = null;
        try {
            retryTemplateBean = ctx.getBean("domainEventsRetryTemplate", RetryTemplate.class);
            log.debug("Kafka publisher configured with retry template");
        } catch (Exception e) {
            log.debug("Kafka publisher configured without retry template");
        }
        this.retryTemplate = retryTemplateBean;
        
        // Get correlation context if available, otherwise use null (no tracing)
        CorrelationContext correlationContextBean = null;
        try {
            correlationContextBean = ctx.getBean(CorrelationContext.class);
            log.debug("Kafka publisher configured with correlation context");
        } catch (Exception e) {
            log.debug("Kafka publisher configured without correlation context");
        }
        this.correlationContext = correlationContextBean;
    }

    @Override
    public Mono<Void> publish(DomainEventEnvelope e) {
        if (retryTemplate != null) {
            // Use retry template for resilient publishing
            return Mono.fromCallable(() -> retryTemplate.execute(retryContext -> {
                retryContext.setAttribute("operationName", "kafka-event-publish:" + e.topic + ":" + e.type);
                return trySend(e);
            }))
            .flatMap(result -> {
                if (result instanceof java.util.concurrent.CompletableFuture<?> cf) {
                    return Mono.fromFuture(cf).then();
                }
                if (result instanceof org.springframework.util.concurrent.ListenableFuture<?> lf) {
                    return Mono.create(sink -> lf.addCallback(r -> sink.success(), sink::error));
                }
                return Mono.empty();
            })
            .onErrorMap(throwable -> {
                log.error("Failed to publish event after retries: topic={}, type={}, key={}", 
                         e.topic, e.type, e.key, throwable);
                return throwable;
            });
        } else {
            // Fallback to original logic without retry
            try {
                Object result = trySend(e);
                if (result instanceof java.util.concurrent.CompletableFuture<?> cf) {
                    return Mono.fromFuture(cf).then();
                }
                if (result instanceof org.springframework.util.concurrent.ListenableFuture<?> lf) {
                    return Mono.create(sink -> lf.addCallback(r -> sink.success(), sink::error));
                }
            } catch (Exception ex) {
                return Mono.error(ex);
            }
            return Mono.empty();
        }
    }

    private Object trySend(DomainEventEnvelope e) {
        // Add correlation context headers
        Map<String, Object> enhancedHeaders = new HashMap<>();
        if (e.headers != null) {
            enhancedHeaders.putAll(e.headers);
        }
        
        // Add correlation headers if correlation context is available
        if (correlationContext != null) {
            Map<String, Object> contextHeaders = correlationContext.createContextHeaders();
            enhancedHeaders.putAll(contextHeaders);
        }
        
        String correlationId = correlationContext != null ? correlationContext.getOrCreateCorrelationId() : null;
        
        // Prefer send(topic, key, payload)
        try {
            log.debug("Attempting to send event with topic={}, key={}, type={}, correlationId={}", 
                     e.topic, e.key, e.type, correlationId);
            return kafkaTemplate.send(e.topic, e.key, e.payload);
        } catch (UnsupportedOperationException ex) {
            log.debug("KafkaTemplate.send(topic, key, payload) not supported: {}", ex.getMessage());
        } catch (Exception ex) {
            log.warn("Failed to send event using KafkaTemplate.send(topic, key, payload) for topic={}, key={}, correlationId={}: {}", 
                    e.topic, e.key, correlationId, ex.getMessage());
        }

        if (tryMessaging) {
            try {
                log.debug("Attempting to send event using Spring Messaging for topic={}, type={}, correlationId={}", 
                         e.topic, e.type, correlationId);
                Message<Object> message = MessageBuilder.withPayload(e.payload)
                        .setHeader("kafka_topic", e.topic)
                        .setHeader("event_type", e.type)
                        .setHeader("event_key", e.key)
                        .copyHeadersIfAbsent(enhancedHeaders)
                        .build();
                return kafkaTemplate.send(message);
            } catch (UnsupportedOperationException ex) {
                log.debug("KafkaTemplate.send(message) not supported: {}", ex.getMessage());
            } catch (Exception ex) {
                log.warn("Failed to send event using KafkaTemplate.send(message) for topic={}, type={}, correlationId={}: {}", 
                        e.topic, e.type, correlationId, ex.getMessage());
            }
        }

        try {
            log.debug("Attempting to send event with topic and payload only for topic={}, correlationId={}", 
                     e.topic, correlationId);
            return kafkaTemplate.send(e.topic, e.payload);
        } catch (UnsupportedOperationException ex) {
            log.debug("KafkaTemplate.send(topic, payload) not supported: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to send event using KafkaTemplate.send(topic, payload) for topic={}, correlationId={}: {}", 
                    e.topic, correlationId, ex.getMessage());
        }

        throw new IllegalStateException("Could not find a suitable KafkaTemplate#send(...) method to publish events for topic: " + e.topic + ", correlationId: " + correlationId);
    }
}