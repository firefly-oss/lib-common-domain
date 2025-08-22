package com.catalis.common.domain.events.outbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import com.catalis.common.domain.events.DomainSpringEvent;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;

/**
 * In-process DomainEventPublisher using Spring's ApplicationEventPublisher.
 */
public class ApplicationEventDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    public ApplicationEventDomainEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public Mono<Void> publish(DomainEventEnvelope envelope) {
        publisher.publishEvent(new DomainSpringEvent(envelope));
        return Mono.empty();
    }
}
