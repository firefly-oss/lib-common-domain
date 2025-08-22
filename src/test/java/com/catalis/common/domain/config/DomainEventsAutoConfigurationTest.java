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
    void kafkaAdapterSelectedWhenTemplateBeanPresent() {
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

        DomainEventPublisher dep = cfg.domainEventPublisher(ctx, appPublisher, props);
        assertTrue(dep instanceof KafkaTemplateDomainEventPublisher);
    }

    @Test
    void autoDetectionPrefersRabbitWhenOnlyRabbitBeanPresent() {
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
        assertTrue(dep instanceof RabbitTemplateDomainEventPublisher);
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
    void kafkaInboundSubscriberCreatedWhenConsumerEnabledAndTopicsPresent() {
        DomainEventsAutoConfiguration cfg = new DomainEventsAutoConfiguration();
        org.springframework.kafka.core.ConsumerFactory<Object,Object> cf = mock(org.springframework.kafka.core.ConsumerFactory.class);
        DomainEventsProperties props = new DomainEventsProperties();
        props.getConsumer().setEnabled(true);
        props.setAdapter(DomainEventsProperties.Adapter.KAFKA);
        props.getConsumer().getKafka().setTopics(List.of("orders"));
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

        SmartLifecycle lifecycle = cfg.domainEventsKafkaSubscriber(cf, props, events);
        assertTrue(lifecycle.getClass().getName().contains("KafkaDomainEventsSubscriber"));
    }

    @Test
    void kafkaInboundSubscriberNoopWhenNoTopics() {
        DomainEventsAutoConfiguration cfg = new DomainEventsAutoConfiguration();
        org.springframework.kafka.core.ConsumerFactory<Object,Object> cf = mock(org.springframework.kafka.core.ConsumerFactory.class);
        DomainEventsProperties props = new DomainEventsProperties();
        props.getConsumer().setEnabled(true);
        props.setAdapter(DomainEventsProperties.Adapter.KAFKA);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

        SmartLifecycle lifecycle = cfg.domainEventsKafkaSubscriber(cf, props, events);
        assertFalse(lifecycle.getClass().getName().contains("KafkaDomainEventsSubscriber"));
    }
}
