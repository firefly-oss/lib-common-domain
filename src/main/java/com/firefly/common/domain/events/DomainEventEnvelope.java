package com.firefly.common.domain.events;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Generic domain event envelope for publishing and consuming events outside StepEvents.
 */
@Builder
@Data
public final class DomainEventEnvelope {

    @Value("${domain.topic:domain-events}")
    private String topic;

    private String type;
    private String key;
    private Object payload;
    private Instant timestamp;
    private Map<String, Object> headers;
    private Map<String, Object> metadata;

    public DomainEventEnvelope(String topic, String type, String key, Object payload, Instant timestamp,
                               Map<String, Object> headers, Map<String, Object> metadata) {
        this.topic = topic;
        this.type = type;
        this.key = key;
        this.payload = payload;
        this.timestamp = timestamp;
        this.headers = headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers);
        this.metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
    }

    public DomainEventEnvelope(String type, String key, Object payload, Instant timestamp,
                               Map<String, Object> headers, Map<String, Object> metadata) {
        new DomainEventEnvelope(this.topic, type, key, payload, timestamp, headers, metadata);
    }
    
    // Custom builder class to handle default values and validation
    public static class DomainEventEnvelopeBuilder {
        private Map<String, Object> headers = Collections.emptyMap();
        private Map<String, Object> metadata = Collections.emptyMap();
        
        public DomainEventEnvelope build() {
            return new DomainEventEnvelope(topic, type, key, payload, timestamp, headers, metadata);
        }
    }
}
