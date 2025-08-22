package com.catalis.common.domain.stepevents;

import com.catalis.transactionalengine.events.StepEventEnvelope;
import com.catalis.transactionalengine.events.StepEventPublisher;
import reactor.core.publisher.Mono;

/**
 * No-op publisher for disabling publications.
 */
public class NoopStepEventPublisher implements StepEventPublisher {
    @Override
    public Mono<Void> publish(StepEventEnvelope envelope) {
        return Mono.empty();
    }
}
