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

package com.firefly.common.domain.events.inbound;

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
public @interface EventListener {
    String topic() default "*";
    String type() default "*";
}