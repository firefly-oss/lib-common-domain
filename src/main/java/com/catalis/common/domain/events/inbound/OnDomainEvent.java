package com.catalis.common.domain.events.inbound;

import java.lang.annotation.*;

/**
 * Marks a method to receive domain events published via the generic DomainEventPublisher.
 *
 * Matching rules:
 * - If topic is set, only events with the same topic are delivered. Use "*" to accept any.
 * - If type is set, only events with the same type are delivered. Use "*" to accept any.
 *
 * Supported handler signatures:
 * - void handle(DomainEventEnvelope e)
 * - void handle(PayloadType payload)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OnDomainEvent {
    String topic() default "*";
    String type() default "*";
}
