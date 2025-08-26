package com.catalis.common.domain.events;

import lombok.Builder;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Generic domain event envelope for publishing and consuming events outside StepEvents.
 */
@Builder
public final class DomainEventEnvelope {
    public final String topic;
    public final String type;
    public final String key;
    public final Object payload;
    public final Map<String, Object> headers;
    public final Map<String, Object> metadata;

    public DomainEventEnvelope(String topic, String type, String key, Object payload, 
                              Map<String, Object> headers, Map<String, Object> metadata) {
        this.topic = topic;
        this.type = type;
        this.key = key;
        this.payload = payload;
        this.headers = headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers);
        this.metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
        
        // Validate required fields
        Objects.requireNonNull(topic, "topic is required");
    }
    
    // Custom builder class to handle default values and validation
    public static class DomainEventEnvelopeBuilder {
        private Map<String, Object> headers = Collections.emptyMap();
        private Map<String, Object> metadata = Collections.emptyMap();
        
        public DomainEventEnvelope build() {
            Objects.requireNonNull(topic, "topic is required");
            return new DomainEventEnvelope(topic, type, key, payload, headers, metadata);
        }
    }
}
