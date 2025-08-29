package com.firefly.common.domain.events.filtering;

import com.firefly.common.domain.events.DomainEventEnvelope;

/**
 * A filter that rejects all events.
 * This is a singleton implementation for efficiency.
 */
public final class RejectAllEventFilter implements EventFilter {

    public static final RejectAllEventFilter INSTANCE = new RejectAllEventFilter();

    private RejectAllEventFilter() {
        // Singleton
    }

    @Override
    public boolean accept(DomainEventEnvelope envelope) {
        return false;
    }

    @Override
    public String getDescription() {
        return "RejectAll";
    }

    @Override
    public EventFilter and(EventFilter other) {
        // RejectAll AND anything = RejectAll
        return this;
    }

    @Override
    public EventFilter or(EventFilter other) {
        // RejectAll OR anything = anything
        return other;
    }

    @Override
    public EventFilter negate() {
        return AcceptAllEventFilter.INSTANCE;
    }
}