package com.firefly.common.domain.events;

import org.springframework.context.ApplicationEvent;

/**
 * In-process Spring event that carries a DomainEventEnvelope for local dispatching.
 */
public class DomainSpringEvent extends ApplicationEvent {
    public DomainSpringEvent(DomainEventEnvelope envelope) {
        super(envelope);
    }
    public DomainEventEnvelope getEnvelope() {
        return (DomainEventEnvelope) getSource();
    }
}
