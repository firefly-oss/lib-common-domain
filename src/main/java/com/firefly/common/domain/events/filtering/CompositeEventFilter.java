package com.firefly.common.domain.events.filtering;

import com.firefly.common.domain.events.DomainEventEnvelope;

/**
 * Composite filters that combine multiple filters using logical operations.
 */
public final class CompositeEventFilter {

    private CompositeEventFilter() {
        // Utility class
    }

    /**
     * Filter that combines two filters using AND logic.
     */
    public static class AndFilter implements EventFilter {
        private final EventFilter first;
        private final EventFilter second;

        public AndFilter(EventFilter first, EventFilter second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean accept(DomainEventEnvelope envelope) {
            return first.accept(envelope) && second.accept(envelope);
        }

        @Override
        public String getDescription() {
            return "(" + first.getDescription() + " AND " + second.getDescription() + ")";
        }
    }

    /**
     * Filter that combines two filters using OR logic.
     */
    public static class OrFilter implements EventFilter {
        private final EventFilter first;
        private final EventFilter second;

        public OrFilter(EventFilter first, EventFilter second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean accept(DomainEventEnvelope envelope) {
            return first.accept(envelope) || second.accept(envelope);
        }

        @Override
        public String getDescription() {
            return "(" + first.getDescription() + " OR " + second.getDescription() + ")";
        }
    }

    /**
     * Filter that negates another filter using NOT logic.
     */
    public static class NotFilter implements EventFilter {
        private final EventFilter filter;

        public NotFilter(EventFilter filter) {
            this.filter = filter;
        }

        @Override
        public boolean accept(DomainEventEnvelope envelope) {
            return !filter.accept(envelope);
        }

        @Override
        public String getDescription() {
            return "NOT(" + filter.getDescription() + ")";
        }
    }
}