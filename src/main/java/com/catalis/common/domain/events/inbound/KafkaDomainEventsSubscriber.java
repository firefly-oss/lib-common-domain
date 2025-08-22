package com.catalis.common.domain.events.inbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import com.catalis.common.domain.events.properties.DomainEventsProperties;
import com.catalis.common.domain.events.DomainSpringEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Inbound Kafka subscriber that listens to configured topics and republishes as DomainSpringEvent.
 */
public class KafkaDomainEventsSubscriber implements SmartLifecycle {

    private final KafkaMessageListenerContainer<Object, Object> container;
    private final ApplicationEventPublisher events;
    private final DomainEventsProperties props;
    private volatile boolean running = false;

    public KafkaDomainEventsSubscriber(ConsumerFactory<Object, Object> consumerFactory,
                                       DomainEventsProperties props,
                                       ApplicationEventPublisher events) {
        this.props = props;
        this.events = events;
        ContainerProperties cp = new ContainerProperties(props.getConsumer().getKafka().getTopics().toArray(new String[0]));
        cp.setMessageListener((org.springframework.kafka.listener.MessageListener<Object, Object>) this::onMessage);
        this.container = new KafkaMessageListenerContainer<>(consumerFactory, cp);
    }

    private void onMessage(ConsumerRecord<Object, Object> record) {
        String topic = record.topic();
        String type = headerAsString(record, props.getConsumer().getTypeHeader());
        String key = record.key() == null ? headerAsString(record, props.getConsumer().getKeyHeader()) : String.valueOf(record.key());
        Object payload = record.value();
        Map<String, Object> headers = new HashMap<>();
        record.headers().forEach(h -> headers.put(h.key(), new String(h.value() == null ? new byte[0] : h.value(), StandardCharsets.UTF_8)));
        DomainEventEnvelope env = DomainEventEnvelope.builder()
                .topic(topic).type(type).key(key).payload(payload).headers(headers).build();
        events.publishEvent(new DomainSpringEvent(env));
    }

    private String headerAsString(ConsumerRecord<?, ?> record, String name) {
        if (name == null) return null;
        var header = record.headers().lastHeader(name);
        if (header == null || header.value() == null) return null;
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    @Override
    public void start() {
        container.start();
        running = true;
    }

    @Override
    public void stop() {
        container.stop();
        running = false;
    }

    @Override
    public boolean isRunning() { return running; }
}
