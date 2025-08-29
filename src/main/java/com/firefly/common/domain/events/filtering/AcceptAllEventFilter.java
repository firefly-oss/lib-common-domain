package com.firefly.common.domain.events.filtering;

import com.firefly.common.domain.events.DomainEventEnvelope;

/**
 * A filter that accepts all events.
 * This is a singleton implementation for efficiency.
 */
public final class AcceptAllEventFilter implements EventFilter {

    public static final AcceptAllEventFilter INSTANCE = new AcceptAllEventFilter();

    private AcceptAllEventFilter() {
        // Singleton
    }

    @Override
    public boolean accept(DomainEventEnvelope envelope) {
        return true;
    }

    @Override
    public String getDescription() {
        return "AcceptAll";
    }

    @Override
    public EventFilter and(EventFilter other) {
        // AcceptAll AND anything = anything
        return other;
    }

    @Override
    public EventFilter or(EventFilter other) {
        // AcceptAll OR anything = AcceptAll
        return this;
    }

    @Override
    public EventFilter negate() {
        return RejectAllEventFilter.INSTANCE;
    }
}