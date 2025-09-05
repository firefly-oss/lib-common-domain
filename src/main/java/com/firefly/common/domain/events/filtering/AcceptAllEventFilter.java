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