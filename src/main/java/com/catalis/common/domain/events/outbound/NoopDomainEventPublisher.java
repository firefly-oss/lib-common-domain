package com.catalis.common.domain.events.outbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import reactor.core.publisher.Mono;

/**
 * No-op domain event publisher to disable publishing.
 */
public class NoopDomainEventPublisher implements DomainEventPublisher {
    @Override
    public Mono<Void> publish(DomainEventEnvelope envelope) {
        return Mono.empty();
    }
}
