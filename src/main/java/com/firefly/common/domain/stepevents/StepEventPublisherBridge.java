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

package com.firefly.common.domain.stepevents;

import com.firefly.common.domain.events.DomainEventEnvelope;
import com.firefly.common.domain.events.outbound.DomainEventPublisher;
import com.firefly.transactionalengine.events.StepEventEnvelope;
import com.firefly.transactionalengine.events.StepEventPublisher;
import reactor.core.publisher.Mono;

/**
 * Bridges StepEvents to the generic DomainEventPublisher so both share adapters and configuration.
 */
public class StepEventPublisherBridge implements StepEventPublisher {

    private final DomainEventPublisher delegate;
    private final String defaultTopic;

    public StepEventPublisherBridge(String defaultTopic, DomainEventPublisher delegate) {
        this.delegate = delegate;
        this.defaultTopic = defaultTopic;
    }

    @Override
    public Mono<Void> publish(StepEventEnvelope e) {
        java.util.Map<String, Object> hdrs = null;
        if (e.getHeaders() != null && !e.getHeaders().isEmpty()) {
            hdrs = new java.util.HashMap<>(e.getHeaders());
        }

        // Add StepEventEnvelope metadata fields to provide context about the step event origin
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("step.attempts", e.getAttempts());
        metadata.put("step.latency_ms", e.getLatencyMs());
        metadata.put("step.started_at", e.getStartedAt());
        metadata.put("step.completed_at", e.getCompletedAt());
        metadata.put("step.result_type", e.getResultType());

        if(e.getKey() == null || e.getKey().isEmpty()){
            e.setKey(e.getSagaName().concat(":").concat(e.getSagaId()));
        }
        if(e.getTopic() == null || e.getTopic().isEmpty()){
            e.setTopic(defaultTopic);
        }

        DomainEventEnvelope env = new DomainEventEnvelope(e.getTopic(), e.getType(), e.getKey(), e, e.getTimestamp(), hdrs, metadata);

        return delegate.publish(env);
    }
}