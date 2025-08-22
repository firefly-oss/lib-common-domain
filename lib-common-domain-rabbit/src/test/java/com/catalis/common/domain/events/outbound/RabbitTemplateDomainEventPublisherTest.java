package com.catalis.common.domain.events.outbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import com.catalis.common.domain.events.properties.DomainEventsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RabbitTemplateDomainEventPublisherTest {

    @Test
    void convertsAndSendsWithTemplatedExchangeAndRoutingKey() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        RabbitTemplate template = mock(RabbitTemplate.class);
        when(ctx.getBeansOfType((Class) any())).thenAnswer(inv -> {
            Class<?> type = inv.getArgument(0);
            if (type.getName().equals("org.springframework.amqp.rabbit.core.RabbitTemplate")) {
                return Map.of("tpl", template);
            }
            return Map.of();
        });
        DomainEventsProperties.Rabbit props = new DomainEventsProperties.Rabbit();
        props.setExchange("ex.${topic}");
        props.setRoutingKey("rk.${type}.${key}");

        RabbitTemplateDomainEventPublisher sut = new RabbitTemplateDomainEventPublisher(ctx, props);
        DomainEventEnvelope e = DomainEventEnvelope.builder().topic("orders").type("created").key("42").payload("data").build();
        assertDoesNotThrow(() -> sut.publish(e).block());

        verify(template, times(1)).convertAndSend("ex.orders", "rk.created.42", "data");
    }

    @Test
    void throwsWhenNoTemplateFound() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        DomainEventsProperties.Rabbit props = new DomainEventsProperties.Rabbit();
        assertThrows(IllegalStateException.class, () -> new RabbitTemplateDomainEventPublisher(ctx, props));
    }
}
