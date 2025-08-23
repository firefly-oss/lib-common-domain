package com.catalis.common.domain.events.outbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import com.catalis.common.domain.events.DomainSpringEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationEventDomainEventPublisherTest {

    static class StubPublisher implements ApplicationEventPublisher {
        Object last;
        @Override public void publishEvent(Object event) { last = event; }
        @Override public void publishEvent(org.springframework.context.ApplicationEvent event) { last = event; }
    }

    @Test
    void publishesSpringEventWithEnvelope() {
        StubPublisher publisher = new StubPublisher();
        ApplicationEventDomainEventPublisher sut = new ApplicationEventDomainEventPublisher(publisher);

        DomainEventEnvelope e = DomainEventEnvelope.builder().topic("orders").type("created").payload("p").build();
        sut.publish(e).block();

        assertNotNull(publisher.last);
        assertTrue(publisher.last instanceof DomainSpringEvent);
        assertSame(e, ((DomainSpringEvent) publisher.last).getEnvelope());
    }
}
