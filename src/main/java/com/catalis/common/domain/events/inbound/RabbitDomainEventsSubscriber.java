package com.catalis.common.domain.events.inbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import com.catalis.common.domain.events.properties.DomainEventsProperties;
import com.catalis.common.domain.events.DomainSpringEvent;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;

import java.util.HashMap;
import java.util.Map;

/**
 * Inbound RabbitMQ subscriber that listens to configured queues and republishes as DomainSpringEvent.
 */
public class RabbitDomainEventsSubscriber implements SmartLifecycle, MessageListener {

    private final SimpleMessageListenerContainer container;
    private final ApplicationEventPublisher events;
    private final DomainEventsProperties props;
    private volatile boolean running = false;

    public RabbitDomainEventsSubscriber(ConnectionFactory connectionFactory,
                                        DomainEventsProperties props,
                                        ApplicationEventPublisher events) {
        this.props = props;
        this.events = events;
        this.container = new SimpleMessageListenerContainer(connectionFactory);
        var queues = props.getConsumer().getRabbit().getQueues();
        this.container.setQueueNames(queues.toArray(new String[0]));
        this.container.setMessageListener(this);
    }

    @Override
    public void onMessage(Message message) {
        var propsMsg = message.getMessageProperties();
        String topic = propsMsg.getReceivedExchange() != null ? propsMsg.getReceivedExchange() : propsMsg.getConsumerQueue();
        Map<String, Object> headers = new HashMap<>(propsMsg.getHeaders());
        String type = (String) headers.getOrDefault(props.getConsumer().getTypeHeader(), null);
        Object keyObj = headers.get(props.getConsumer().getKeyHeader());
        String key = keyObj == null ? null : String.valueOf(keyObj);
        Object payload = message.getBody();
        DomainEventEnvelope env = DomainEventEnvelope.builder()
                .topic(topic).type(type).key(key).payload(payload).headers(headers).build();
        events.publishEvent(new DomainSpringEvent(env));
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
