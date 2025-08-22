package com.catalis.common.domain.stepevents;

import com.catalis.common.domain.config.StepEventsProperties;
import com.catalis.transactionalengine.events.StepEventEnvelope;
import com.catalis.transactionalengine.events.StepEventPublisher;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

/**
 * Kafka adapter using Spring Kafka KafkaTemplate.
 */
public class KafkaTemplateStepEventPublisher implements StepEventPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final boolean tryMessaging;

    @SuppressWarnings("unchecked")
    public KafkaTemplateStepEventPublisher(ApplicationContext ctx, StepEventsProperties props) {
        Object template = StepEventAdapterUtils.resolveBean(
                ctx,
                props.getKafka().getTemplateBeanName(),
                "org.springframework.kafka.core.KafkaTemplate"
        );
        if (template == null) {
            throw new IllegalStateException("Kafka adapter selected but KafkaTemplate bean was not found. Add spring-kafka dependency and define a KafkaTemplate bean or set catalis.stepevents.kafka.templateBeanName.");
        }
        this.kafkaTemplate = (KafkaTemplate<Object, Object>) template;
        this.tryMessaging = props.getKafka().isUseMessagingIfAvailable();
    }

    @Override
    public Mono<Void> publish(StepEventEnvelope e) {
        try {
            Object res = trySend(e);
            if (res instanceof java.util.concurrent.CompletableFuture<?> cf) {
                return Mono.fromFuture(cf).then();
            }
            if (res instanceof org.springframework.util.concurrent.ListenableFuture<?> lf) {
                return Mono.create(sink -> lf.addCallback(r -> sink.success(), sink::error));
            }
        } catch (Exception ex) {
            return Mono.error(ex);
        }
        return Mono.empty();
    }

    private Object trySend(StepEventEnvelope e) {
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
