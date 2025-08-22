package com.catalis.common.domain.events.outbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import com.catalis.common.domain.stepevents.StepEventAdapterUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka adapter for DomainEventPublisher using Spring Kafka KafkaTemplate.
 */
public class KafkaTemplateDomainEventPublisher implements DomainEventPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final boolean tryMessaging;

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
    }

    @Override
    public Mono<Void> publish(DomainEventEnvelope e) {
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

    private Object trySend(DomainEventEnvelope e) {
        // Prefer send(topic, key, payload)
        try {
            return kafkaTemplate.send(e.topic, e.key, e.payload);
        } catch (Throwable ignored) {}

        if (tryMessaging) {
            try {
                Message<Object> message = MessageBuilder.withPayload(e.payload)
                        .setHeader("kafka_topic", e.topic)
                        .setHeader("event_type", e.type)
                        .setHeader("event_key", e.key)
                        .copyHeadersIfAbsent(e.headers == null ? java.util.Map.of() : e.headers)
                        .build();
                return kafkaTemplate.send(message);
            } catch (Throwable ignored) {}
        }

        try {
            return kafkaTemplate.send(e.topic, e.payload);
        } catch (Throwable ignored) {}

        throw new IllegalStateException("Could not find a suitable KafkaTemplate#send(...) method to publish events.");
    }
}
