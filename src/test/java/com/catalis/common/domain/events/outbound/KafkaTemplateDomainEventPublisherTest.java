package com.catalis.common.domain.events.outbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KafkaTemplateDomainEventPublisherTest {

    @Test
    void sendsUsingTopicKeyPayload() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        KafkaTemplate<Object, Object> template = mock(KafkaTemplate.class);
        when(ctx.getBeansOfType((Class) any())).thenAnswer(inv -> {
            Class<?> type = inv.getArgument(0);
            if (type.getName().equals("org.springframework.kafka.core.KafkaTemplate")) {
                return Map.of("tpl", template);
            }
            return Map.of();
        });
        when(template.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        KafkaTemplateDomainEventPublisher sut = new KafkaTemplateDomainEventPublisher(ctx, true, null);
        DomainEventEnvelope e = DomainEventEnvelope.builder().topic("t").type("ty").key("k").payload("p").build();
        assertDoesNotThrow(() -> sut.publish(e).block());
        verify(template, times(1)).send("t", "k", "p");
    }

    @Test
    void fallsBackToMessagingWhenTopicKeyPayloadNotAvailable() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        KafkaTemplate<Object, Object> template = mock(KafkaTemplate.class);
        when(ctx.getBeansOfType((Class) any())).thenAnswer(inv -> {
            Class<?> type = inv.getArgument(0);
            if (type.getName().equals("org.springframework.kafka.core.KafkaTemplate")) {
                return Map.of("tpl", template);
            }
            return Map.of();
        });
        when(template.send(anyString(), anyString(), any())).thenThrow(new NoSuchMethodError("send(String,String,Object)"));
        when(template.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));

        KafkaTemplateDomainEventPublisher sut = new KafkaTemplateDomainEventPublisher(ctx, true, null);
        DomainEventEnvelope e = DomainEventEnvelope.builder().topic("topic").type("type").key("key").payload("payload").build();
        assertDoesNotThrow(() -> sut.publish(e).block());

        ArgumentCaptor<Message<?>> captor = ArgumentCaptor.forClass(Message.class);
        verify(template, times(1)).send(captor.capture());
        Message<?> msg = captor.getValue();
        assertEquals("payload", msg.getPayload());
        assertEquals("topic", msg.getHeaders().get("kafka_topic"));
        assertEquals("type", msg.getHeaders().get("event_type"));
        assertEquals("key", msg.getHeaders().get("event_key"));
    }

    @Test
    void throwsWhenNoTemplateFound() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        assertThrows(IllegalStateException.class, () -> new KafkaTemplateDomainEventPublisher(ctx, true, null));
    }
}
