package com.catalis.common.domain.util;

import java.time.Instant;
import java.util.UUID;

/**
 * Base record for domain events in business logic orchestration.
 */
public record DomainEvent(
    String eventId,
    String eventType,
    Instant occurredAt,
    Object payload
) {
    
    public DomainEvent(String eventType, Object payload) {
        this(UUID.randomUUID().toString(), eventType, Instant.now(), payload);
    }
}