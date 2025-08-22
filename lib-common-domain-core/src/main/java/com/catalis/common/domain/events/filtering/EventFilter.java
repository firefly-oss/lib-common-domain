package com.catalis.common.domain.events.filtering;

import com.catalis.common.domain.events.DomainEventEnvelope;

/**
 * Interface for filtering domain events before publishing or after consuming.
 * Filters can be chained and applied in sequence to provide complex filtering logic.
 */
public interface EventFilter {

    /**
     * Tests whether the given event should be processed.
     * 
     * @param envelope the domain event envelope to test
     * @return true if the event should be processed, false otherwise
     */
    boolean accept(DomainEventEnvelope envelope);

    /**
     * Returns a description of this filter for debugging and logging purposes.
     * 
     * @return a human-readable description of the filter
     */
    default String getDescription() {
        return this.getClass().getSimpleName();
    }

    /**
     * Combines this filter with another filter using AND logic.
     * 
     * @param other the other filter to combine with
     * @return a new filter that returns true only if both filters return true
     */
    default EventFilter and(EventFilter other) {
        return new CompositeEventFilter.AndFilter(this, other);
    }

    /**
     * Combines this filter with another filter using OR logic.
     * 
     * @param other the other filter to combine with
     * @return a new filter that returns true if either filter returns true
     */
    default EventFilter or(EventFilter other) {
        return new CompositeEventFilter.OrFilter(this, other);
    }

    /**
     * Returns a negated version of this filter.
     * 
     * @return a new filter that returns the opposite of this filter
     */
    default EventFilter negate() {
        return new CompositeEventFilter.NotFilter(this);
    }

    /**
     * Creates a filter that always accepts events.
     * 
     * @return a filter that accepts all events
     */
    static EventFilter acceptAll() {
        return AcceptAllEventFilter.INSTANCE;
    }

    /**
     * Creates a filter that never accepts events.
     * 
     * @return a filter that rejects all events
     */
    static EventFilter rejectAll() {
        return RejectAllEventFilter.INSTANCE;
    }
}