package com.catalis.common.domain.events;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DomainEventEnvelopeTest {

    @Test
    void builderRequiresTopic() {
        DomainEventEnvelope.Builder b = DomainEventEnvelope.builder()
                .type("t")
                .key("k")
                .payload("p");
        assertThrows(NullPointerException.class, b::build);
    }

    @Test
    void headersAreUnmodifiableAndNullBecomesEmpty() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("a", 1);
        DomainEventEnvelope e = DomainEventEnvelope.builder()
                .topic("topic")
                .headers(headers)
                .build();
        assertEquals(1, e.headers.get("a"));
        assertThrows(UnsupportedOperationException.class, () -> e.headers.put("b", 2));

        DomainEventEnvelope e2 = new DomainEventEnvelope("topic", null, null, null, null);
        assertNotNull(e2.headers);
        assertTrue(e2.headers.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> e2.headers.put("x", "y"));
    }

    @Test
    void fieldsAreAssigned() {
        Map<String, Object> headers = Map.of("h", "v");
        Object payload = Map.of("id", 123);
        DomainEventEnvelope e = new DomainEventEnvelope("topic", "type", "key", payload, headers);
        assertEquals("topic", e.topic);
        assertEquals("type", e.type);
        assertEquals("key", e.key);
        assertSame(payload, e.payload);
        assertEquals("v", e.headers.get("h"));
    }
}
