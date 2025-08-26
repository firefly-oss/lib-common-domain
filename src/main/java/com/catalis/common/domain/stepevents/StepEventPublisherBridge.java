package com.catalis.common.domain.stepevents;

import com.catalis.common.domain.events.DomainEventEnvelope;
import com.catalis.common.domain.events.outbound.DomainEventPublisher;
import com.catalis.transactionalengine.events.StepEventEnvelope;
import com.catalis.transactionalengine.events.StepEventPublisher;
import reactor.core.publisher.Mono;

/**
 * Bridges StepEvents to the generic DomainEventPublisher so both share adapters and configuration.
 */
public class StepEventPublisherBridge implements StepEventPublisher {

    private final DomainEventPublisher delegate;

    public StepEventPublisherBridge(DomainEventPublisher delegate) {
        this.delegate = delegate;
    }

    @Override
    public Mono<Void> publish(StepEventEnvelope e) {
        java.util.Map<String, Object> hdrs = null;
        if (e.headers != null && !e.headers.isEmpty()) {
            hdrs = new java.util.HashMap<>(e.headers);
        }

        DomainEventEnvelope env = DomainEventEnvelope.builder()
                .topic(e.topic)
                .type(e.type)
                .key(e.key)
                .payload(e.payload)
                .headers(hdrs)
                .build();
        return delegate.publish(env);
    }
}