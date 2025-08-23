package com.catalis.common.domain.config;

import com.catalis.common.domain.events.outbound.DomainEventPublisher;
import com.catalis.common.domain.stepevents.StepEventPublisherBridge;
import com.catalis.transactionalengine.events.ApplicationEventStepEventPublisher;
import com.catalis.transactionalengine.events.StepEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StepEventsAutoConfigurationTest {

    @Test
    void bridgesToDomainEventPublisherWhenPresent() {
        StepEventsAutoConfiguration cfg = new StepEventsAutoConfiguration();
        ApplicationContext ctx = mock(ApplicationContext.class);
        ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
        StepEventsProperties props = new StepEventsProperties();
        @SuppressWarnings("unchecked")
        ObjectProvider<DomainEventPublisher> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(envelope -> reactor.core.publisher.Mono.empty());

        StepEventPublisher bean = cfg.stepEventPublisher(ctx, appPublisher, props, provider);
        assertTrue(bean instanceof StepEventPublisherBridge);
    }

    @Test
    void fallsBackToApplicationEventWhenKafkaNotAvailableInCore() {
        StepEventsAutoConfiguration cfg = new StepEventsAutoConfiguration();
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBeansOfType((Class) any())).thenAnswer(inv -> {
            Class<?> type = inv.getArgument(0);
            if (type.getName().equals("org.springframework.kafka.core.KafkaTemplate")) {
                return Map.of("tpl", mock(org.springframework.kafka.core.KafkaTemplate.class));
            }
            return Map.of();
        });
        ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
        StepEventsProperties props = new StepEventsProperties();
        props.setAdapter(StepEventsProperties.Adapter.AUTO);
        @SuppressWarnings("unchecked")
        ObjectProvider<DomainEventPublisher> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        StepEventPublisher bean = cfg.stepEventPublisher(ctx, appPublisher, props, provider);
        // Core module now falls back to ApplicationEvent when Kafka implementations are in separate modules
        assertTrue(bean instanceof ApplicationEventStepEventPublisher);
    }
}
