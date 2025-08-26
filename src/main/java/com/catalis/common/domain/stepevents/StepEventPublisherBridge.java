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

        // Add StepEventEnvelope metadata fields to provide context about the step event origin
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("step.attempts", e.attempts);
        metadata.put("step.latency_ms", e.latencyMs);
        metadata.put("step.started_at", e.startedAt);
        metadata.put("step.completed_at", e.completedAt);
        metadata.put("step.result_type", e.resultType);
        
        DomainEventEnvelope env = DomainEventEnvelope.builder()
                .topic(e.topic)
                .type(e.type)
                .key(e.key)
                .payload(e.payload)
                .headers(hdrs)
                .metadata(metadata)
                .build();
        return delegate.publish(env);
    }
}
