package com.catalis.common.domain.config;

import com.catalis.common.domain.events.outbound.*;
import com.catalis.common.domain.events.properties.DomainEventsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DomainEventsAutoConfigurationTest {

    @Test
    void applicationEventAdapterSelected() {
        DomainEventsAutoConfiguration cfg = new DomainEventsAutoConfiguration();
        ApplicationContext ctx = mock(ApplicationContext.class);
        ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
        DomainEventsProperties props = new DomainEventsProperties();
        props.setAdapter(DomainEventsProperties.Adapter.APPLICATION_EVENT);

        DomainEventPublisher dep = cfg.domainEventPublisher(ctx, appPublisher, props);
        assertTrue(dep instanceof ApplicationEventDomainEventPublisher);
        assertNotNull(cfg.emitEventAspect(dep));
        assertNotNull(cfg.onDomainEventDispatcher());
    }

    @Test
    void kafkaAdapterThrowsExceptionWhenNotProperlyConfigured() {
        DomainEventsAutoConfiguration cfg = new DomainEventsAutoConfiguration();
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBeansOfType((Class) any())).thenAnswer(inv -> {
            Class<?> type = inv.getArgument(0);
            if (type.getName().equals("org.springframework.kafka.core.KafkaTemplate")) {
                return Map.of("tpl", mock(org.springframework.kafka.core.KafkaTemplate.class));
            }
            return Map.of();
        });
        ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
        DomainEventsProperties props = new DomainEventsProperties();
        props.setAdapter(DomainEventsProperties.Adapter.KAFKA);

        // Core module now throws exception when Kafka adapter is selected but module is not properly configured
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> cfg.domainEventPublisher(ctx, appPublisher, props));
        assertTrue(exception.getMessage().contains("Kafka adapter selected but no KafkaTemplateDomainEventPublisher found"));
    }

    @Test
    void autoDetectionFallsBackToApplicationEventInCore() {
        DomainEventsAutoConfiguration cfg = new DomainEventsAutoConfiguration();
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBeansOfType((Class) any())).thenAnswer(inv -> {
            Class<?> type = inv.getArgument(0);
            if (type.getName().equals("org.springframework.amqp.rabbit.core.RabbitTemplate")) {
                return Map.of("tpl", mock(org.springframework.amqp.rabbit.core.RabbitTemplate.class));
            }
            return Map.of();
        });
        ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
        DomainEventsProperties props = new DomainEventsProperties();
        props.setAdapter(DomainEventsProperties.Adapter.AUTO);

        DomainEventPublisher dep = cfg.domainEventPublisher(ctx, appPublisher, props);
        // Core module now falls back to ApplicationEvent when Rabbit implementations are in separate modules
        assertTrue(dep instanceof ApplicationEventDomainEventPublisher);
    }

    @Test
    void noopSelectedWhenConfigured() {
        DomainEventsAutoConfiguration cfg = new DomainEventsAutoConfiguration();
        ApplicationContext ctx = mock(ApplicationContext.class);
        ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
        DomainEventsProperties props = new DomainEventsProperties();
        props.setAdapter(DomainEventsProperties.Adapter.NOOP);

        DomainEventPublisher dep = cfg.domainEventPublisher(ctx, appPublisher, props);
        assertTrue(dep instanceof NoopDomainEventPublisher);
    }

    @Test
    void kafkaInboundSubscriberReturnsNoopInCore() {
        DomainEventsAutoConfiguration cfg = new DomainEventsAutoConfiguration();
        org.springframework.kafka.core.ConsumerFactory<Object,Object> cf = mock(org.springframework.kafka.core.ConsumerFactory.class);
        DomainEventsProperties props = new DomainEventsProperties();
        props.getConsumer().setEnabled(true);
        props.setAdapter(DomainEventsProperties.Adapter.KAFKA);
        props.getConsumer().getKafka().setTopics(List.of("orders"));
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

        SmartLifecycle lifecycle = cfg.domainEventsKafkaSubscriber(cf, props, events);
        // Core module now returns noop lifecycle since Kafka subscriber implementation moved to kafka module
        assertFalse(lifecycle.getClass().getName().contains("KafkaDomainEventsSubscriber"));
    }

    @Test
    void kafkaInboundSubscriberAlwaysReturnsNoopInCore() {
        DomainEventsAutoConfiguration cfg = new DomainEventsAutoConfiguration();
        org.springframework.kafka.core.ConsumerFactory<Object,Object> cf = mock(org.springframework.kafka.core.ConsumerFactory.class);
        DomainEventsProperties props = new DomainEventsProperties();
        props.getConsumer().setEnabled(true);
        props.setAdapter(DomainEventsProperties.Adapter.KAFKA);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

        SmartLifecycle lifecycle = cfg.domainEventsKafkaSubscriber(cf, props, events);
        // Core module always returns noop lifecycle since Kafka subscriber implementation moved to kafka module
        assertFalse(lifecycle.getClass().getName().contains("KafkaDomainEventsSubscriber"));
    }
}
