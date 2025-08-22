package com.catalis.common.domain.events.inbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import com.catalis.common.domain.events.DomainSpringEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class OnDomainEventDispatcherTest {

    static class Payload { public int id; }

    static class Handler {
        final AtomicReference<String> strSeen = new AtomicReference<>();
        final AtomicReference<Payload> payloadSeen = new AtomicReference<>();

        @OnDomainEvent(topic = "orders", type = "created")
        public void onString(String body) { strSeen.set(body); }

        @OnDomainEvent(topic = "orders", type = "mapped")
        public void onTyped(Payload p) { payloadSeen.set(p); }
    }

    @Configuration
    static class Cfg {
        @Bean OnDomainEventDispatcher dispatcher() { return new OnDomainEventDispatcher(); }
        @Bean Handler handler() { return new Handler(); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
    }

    @Test
    void dispatchesToMatchingHandlersAndConvertsPayload() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Cfg.class);
        try {
            DomainEventEnvelope e1 = DomainEventEnvelope.builder().topic("orders").type("created").payload("hi").build();
            ctx.publishEvent(new DomainSpringEvent(e1));

            Handler h = ctx.getBean(Handler.class);
            assertEquals("hi", h.strSeen.get());

            DomainEventEnvelope e2 = DomainEventEnvelope.builder().topic("orders").type("mapped").payload("{\"id\":5}").build();
            ctx.publishEvent(new DomainSpringEvent(e2));
            assertNotNull(h.payloadSeen.get());
            assertEquals(5, h.payloadSeen.get().id);
        } finally {
            ctx.close();
        }
    }
}
