package com.catalis.common.domain.events;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Generic domain event envelope for publishing and consuming events outside StepEvents.
 */
public final class DomainEventEnvelope {
    public final String topic;
    public final String type;
    public final String key;
    public final Object payload;
    public final Map<String, Object> headers;

    public DomainEventEnvelope(String topic, String type, String key, Object payload, Map<String, Object> headers) {
        this.topic = topic;
        this.type = type;
        this.key = key;
        this.payload = payload;
        this.headers = headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String topic;
        private String type;
        private String key;
        private Object payload;
        private Map<String, Object> headers = Collections.emptyMap();

        public Builder topic(String topic) { this.topic = topic; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder key(String key) { this.key = key; return this; }
        public Builder payload(Object payload) { this.payload = payload; return this; }
        public Builder headers(Map<String, Object> headers) { this.headers = headers; return this; }

        public DomainEventEnvelope build() {
            Objects.requireNonNull(topic, "topic is required");
            return new DomainEventEnvelope(topic, type, key, payload, headers);
        }
    }
}
