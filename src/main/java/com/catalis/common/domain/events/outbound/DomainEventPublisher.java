package com.catalis.common.domain.events.outbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import reactor.core.publisher.Mono;

/**
 * Port for publishing generic domain events.
 */
public interface DomainEventPublisher {
    Mono<Void> publish(DomainEventEnvelope envelope);
}
