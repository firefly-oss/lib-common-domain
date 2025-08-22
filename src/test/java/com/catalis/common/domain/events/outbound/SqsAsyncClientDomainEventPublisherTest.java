package com.catalis.common.domain.events.outbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import com.catalis.common.domain.events.properties.DomainEventsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SqsAsyncClientDomainEventPublisherTest {

    @Test
    void usesDirectQueueUrlWhenProvided() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        SqsAsyncClient client = mock(SqsAsyncClient.class);
        when(ctx.getBeansOfType((Class) any())).thenAnswer(inv -> {
            Class<?> type = inv.getArgument(0);
            if (type.getName().equals("software.amazon.awssdk.services.sqs.SqsAsyncClient")) {
                return Map.of("c", client);
            }
            return Map.of();
        });
        DomainEventsProperties.Sqs props = new DomainEventsProperties.Sqs();
        props.setQueueUrl("http://queue-url");

        when(client.sendMessage(any(SendMessageRequest.class))).thenReturn(CompletableFuture.completedFuture(null));

        SqsAsyncClientDomainEventPublisher sut = new SqsAsyncClientDomainEventPublisher(ctx, props);
        DomainEventEnvelope e = DomainEventEnvelope.builder().topic("t").payload("body").build();
        assertDoesNotThrow(() -> sut.publish(e).block());

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(captor.capture());
        assertEquals("http://queue-url", captor.getValue().queueUrl());
        assertEquals("body", captor.getValue().messageBody());
    }

    @Test
    void resolvesQueueUrlByNameAndSerializesWithObjectMapperIfPresent() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        SqsAsyncClient client = mock(SqsAsyncClient.class);
        ObjectMapper mapper = new ObjectMapper();
        when(ctx.getBeansOfType((Class) any())).thenAnswer(inv -> {
            Class<?> type = inv.getArgument(0);
            if (type.getName().equals("software.amazon.awssdk.services.sqs.SqsAsyncClient")) {
                return Map.of("c", client);
            }
            if (type.getName().equals("com.fasterxml.jackson.databind.ObjectMapper")) {
                return Map.of("m", mapper);
            }
            return Map.of();
        });
        DomainEventsProperties.Sqs props = new DomainEventsProperties.Sqs();
        props.setQueueName("orders");

        when(client.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl("http://resolved").build()));
        when(client.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        SqsAsyncClientDomainEventPublisher sut = new SqsAsyncClientDomainEventPublisher(ctx, props);
        Map<String, Object> payload = Map.of("id", 1);
        DomainEventEnvelope e = DomainEventEnvelope.builder().topic("ignored").payload(payload).build();
        assertDoesNotThrow(() -> sut.publish(e).block());

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(captor.capture());
        assertEquals("http://resolved", captor.getValue().queueUrl());
        assertEquals("{\"id\":1}", captor.getValue().messageBody());
    }

    @Test
    void throwsWhenNoClientFound() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        DomainEventsProperties.Sqs props = new DomainEventsProperties.Sqs();
        assertThrows(IllegalStateException.class, () -> new SqsAsyncClientDomainEventPublisher(ctx, props));
    }
}
