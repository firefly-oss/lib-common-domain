package com.firefly.common.domain.events.inbound;

import com.firefly.common.domain.events.DomainEventEnvelope;
import com.firefly.common.domain.events.DomainSpringEvent;
import com.firefly.common.domain.events.properties.DomainEventsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Inbound RabbitMQ subscriber that consumes messages and republishes as DomainSpringEvent.
 */
public class RabbitMqDomainEventsSubscriber implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqDomainEventsSubscriber.class);
    private final ApplicationContext ctx;
    private final DomainEventsProperties props;
    private final ApplicationEventPublisher events;
    private volatile boolean running = false;
    private SimpleMessageListenerContainer container;

    public RabbitMqDomainEventsSubscriber(ApplicationContext ctx,
                                          DomainEventsProperties props,
                                          ApplicationEventPublisher events) {
        this.ctx = ctx;
        this.props = props;
        this.events = events;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        
        try {
            // Get connection factory from application context
            ConnectionFactory connectionFactory = ctx.getBean(ConnectionFactory.class);
            
            // Get queues to listen to
            List<String> queues = getQueuesToSubscribe();
            if (queues.isEmpty()) {
                log.warn("No RabbitMQ queues configured for domain events subscription");
                return;
            }
            
            // Create and configure message listener container
            container = new SimpleMessageListenerContainer();
            container.setConnectionFactory(connectionFactory);
            container.setQueueNames(queues.toArray(new String[0]));
            container.setMessageListener(new DomainEventMessageListener());
            container.setAutoStartup(true);
            container.setConcurrentConsumers(1);
            container.setMaxConcurrentConsumers(1);
            
            // Start the container
            container.start();
            running = true;
            
            log.info("RabbitMQ domain events subscriber started, listening to queues: {}", queues);
            
        } catch (Exception e) {
            log.error("Error starting RabbitMQ domain events subscriber", e);
        }
    }

    private List<String> getQueuesToSubscribe() {
        DomainEventsProperties.Consumer.RabbitConsumer rabbitConsumer = this.props.getConsumer().getRabbit();
        if (rabbitConsumer != null && rabbitConsumer.getQueues() != null) {
            return rabbitConsumer.getQueues();
        }
        return List.of();
    }

    @Override
    public void stop() {
        if (running && container != null) {
            try {
                container.stop();
                log.info("Stopped RabbitMQ domain events subscriber");
            } catch (Exception e) {
                log.warn("Error stopping RabbitMQ domain events subscriber", e);
            } finally {
                running = false;
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Message listener that processes incoming RabbitMQ messages and converts them to domain events.
     */
    private class DomainEventMessageListener implements MessageListener {
        
        @Override
        public void onMessage(Message message) {
            try {
                processMessage(message);
            } catch (Exception e) {
                log.error("Error processing RabbitMQ message", e);
            }
        }
        
        private void processMessage(Message message) {
            Map<String, Object> headers = new HashMap<>();
            
            // Extract headers from message properties
            if (message.getMessageProperties() != null && message.getMessageProperties().getHeaders() != null) {
                headers.putAll(message.getMessageProperties().getHeaders());
            }
            
            // Extract event metadata from headers
            String type = getHeaderValue(headers, "event-type");
            String topic = getHeaderValue(headers, "event-topic");
            String key = getHeaderValue(headers, "event-key");
            
            // Use routing key as topic if not found in headers
            if (topic == null || topic.isEmpty()) {
                topic = message.getMessageProperties() != null ? 
                       message.getMessageProperties().getReceivedRoutingKey() : "unknown";
            }
            
            // Convert message body to string
            String messageBody = new String(message.getBody());
            
            DomainEventEnvelope envelope = DomainEventEnvelope.builder()
                    .topic(topic)
                    .type(type)
                    .key(key)
                    .payload(messageBody)
                    .headers(headers)
                    .build();
                    
            events.publishEvent(new DomainSpringEvent(envelope));
            
            log.debug("Processed RabbitMQ message: topic={}, type={}, key={}, routingKey={}", 
                     topic, type, key, message.getMessageProperties().getReceivedRoutingKey());
        }
        
        private String getHeaderValue(Map<String, Object> headers, String key) {
            Object value = headers.get(key);
            return value != null ? String.valueOf(value) : null;
        }
    }
}