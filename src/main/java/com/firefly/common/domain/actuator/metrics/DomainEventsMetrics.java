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

package com.firefly.common.domain.actuator.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Metrics collector for Domain Events operations.
 * Tracks publishing, consumption, and error metrics using Micrometer.
 */
@Component
public class DomainEventsMetrics {

    private final MeterRegistry meterRegistry;
    
    // Cache for counters and timers to avoid recreation
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    public DomainEventsMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Records a successful event publication.
     */
    public void recordEventPublished(String adapter, String topic, String type) {
        getOrCreateCounter("domain_events_published_total", 
                "adapter", adapter,
                "topic", topic,
                "type", type,
                "result", "success")
                .increment();
    }

    /**
     * Records a failed event publication.
     */
    public void recordEventPublishFailed(String adapter, String topic, String type, String errorType) {
        getOrCreateCounter("domain_events_published_total",
                "adapter", adapter,
                "topic", topic,
                "type", type,
                "result", "error",
                "error_type", errorType)
                .increment();
    }

    /**
     * Records the duration of an event publication.
     */
    public void recordEventPublishDuration(String adapter, String topic, String type, Duration duration) {
        getOrCreateTimer("domain_events_publish_duration",
                "adapter", adapter,
                "topic", topic,
                "type", type)
                .record(duration);
    }

    /**
     * Records a successful event consumption.
     */
    public void recordEventConsumed(String adapter, String topic, String type) {
        getOrCreateCounter("domain_events_consumed_total",
                "adapter", adapter,
                "topic", topic,
                "type", type,
                "result", "success")
                .increment();
    }

    /**
     * Records a failed event consumption.
     */
    public void recordEventConsumeFailed(String adapter, String topic, String type, String errorType) {
        getOrCreateCounter("domain_events_consumed_total",
                "adapter", adapter,
                "topic", topic,
                "type", type,
                "result", "error",
                "error_type", errorType)
                .increment();
    }

    /**
     * Records the duration of an event consumption.
     */
    public void recordEventConsumeDuration(String adapter, String topic, String type, Duration duration) {
        getOrCreateTimer("domain_events_consume_duration",
                "adapter", adapter,
                "topic", topic,
                "type", type)
                .record(duration);
    }

    /**
     * Records adapter availability checks.
     */
    public void recordAdapterHealthCheck(String adapter, boolean healthy) {
        getOrCreateCounter("domain_events_health_checks_total",
                "adapter", adapter,
                "result", healthy ? "healthy" : "unhealthy")
                .increment();
    }

    private Counter getOrCreateCounter(String name, String... tags) {
        String key = buildKey(name, tags);
        return counters.computeIfAbsent(key, k -> 
                Counter.builder(name)
                        .description("Domain Events metric: " + name)
                        .tags(tags)
                        .register(meterRegistry)
        );
    }

    private Timer getOrCreateTimer(String name, String... tags) {
        String key = buildKey(name, tags);
        return timers.computeIfAbsent(key, k ->
                Timer.builder(name)
                        .description("Domain Events metric: " + name)
                        .tags(tags)
                        .register(meterRegistry)
        );
    }

    private String buildKey(String name, String... tags) {
        StringBuilder sb = new StringBuilder(name);
        for (int i = 0; i < tags.length; i += 2) {
            if (i + 1 < tags.length) {
                sb.append(":").append(tags[i]).append("=").append(tags[i + 1]);
            }
        }
        return sb.toString();
    }
}