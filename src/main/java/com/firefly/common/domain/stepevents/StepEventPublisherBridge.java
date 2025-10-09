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

import com.firefly.common.eda.publisher.EventPublisher;
import com.firefly.transactional.events.StepEventEnvelope;
import com.firefly.transactional.events.StepEventPublisher;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Bridges StepEvents from lib-transactional-engine to lib-common-eda's EventPublisher.
 * <p>
 * This bridge allows SAGA step events to be published through the unified EDA infrastructure,
 * enabling step events to leverage all EDA features including:
 * <ul>
 *   <li>Multi-platform support (Kafka, RabbitMQ, etc.)</li>
 *   <li>Resilience patterns (circuit breaker, retry, rate limiting)</li>
 *   <li>Metrics and monitoring</li>
 *   <li>Health checks</li>
 * </ul>
 */
public class StepEventPublisherBridge implements StepEventPublisher {

    private final EventPublisher edaPublisher;
    private final String defaultTopic;

    /**
     * Creates a new StepEventPublisherBridge.
     *
     * @param defaultTopic the default topic/destination for step events
     * @param edaPublisher the EDA event publisher to delegate to
     */
    public StepEventPublisherBridge(String defaultTopic, EventPublisher edaPublisher) {
        this.edaPublisher = edaPublisher;
        this.defaultTopic = defaultTopic;
    }

    @Override
    public Mono<Void> publish(StepEventEnvelope stepEvent) {
        // Prepare headers from step event
        Map<String, Object> headers = new HashMap<>();
        if (stepEvent.getHeaders() != null && !stepEvent.getHeaders().isEmpty()) {
            headers.putAll(stepEvent.getHeaders());
        }

        // Add step event metadata as headers for traceability
        headers.put("step.saga_name", stepEvent.getSagaName());
        headers.put("step.saga_id", stepEvent.getSagaId());
        headers.put("step.type", stepEvent.getType());
        headers.put("step.attempts", stepEvent.getAttempts());
        headers.put("step.latency_ms", stepEvent.getLatencyMs());
        headers.put("step.started_at", stepEvent.getStartedAt());
        headers.put("step.completed_at", stepEvent.getCompletedAt());
        headers.put("step.result_type", stepEvent.getResultType());
        headers.put("step.timestamp", stepEvent.getTimestamp());

        // Set routing key if not already set
        if (stepEvent.getKey() == null || stepEvent.getKey().isEmpty()) {
            stepEvent.setKey(stepEvent.getSagaName() + ":" + stepEvent.getSagaId());
            headers.put("routing_key", stepEvent.getKey());
        } else {
            headers.put("routing_key", stepEvent.getKey());
        }

        // Set event type header
        headers.put("event_type", stepEvent.getType());

        // Determine destination topic
        String destination = (stepEvent.getTopic() != null && !stepEvent.getTopic().isEmpty())
                ? stepEvent.getTopic()
                : defaultTopic;

        // Publish through EDA infrastructure
        return edaPublisher.publish(stepEvent, destination, headers);
    }
}