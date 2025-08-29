package com.firefly.common.domain.events.outbound;

import java.lang.annotation.*;

/**
 * Annotate a method to emit a domain event after successful execution.
 * Supports SpEL expressions for topic/type/key/payload.
 *
 * Defaults:
 * - payload: #result (the return value)
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventPublisher {
    String topic();
    String type() default "";
    String key() default "";
    /** SpEL for payload; default is '#result' (method return). */
    String payload() default "#result";
}