package com.catalis.common.domain.events.outbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NoopDomainEventPublisherTest {

    @Test
    void returnsEmptyMonoAndDoesNotThrow() {
        NoopDomainEventPublisher sut = new NoopDomainEventPublisher();
        DomainEventEnvelope e = DomainEventEnvelope.builder().topic("t").build();
        assertDoesNotThrow(() -> sut.publish(e).block());
    }
}
