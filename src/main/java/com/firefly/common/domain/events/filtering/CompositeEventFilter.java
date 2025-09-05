/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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