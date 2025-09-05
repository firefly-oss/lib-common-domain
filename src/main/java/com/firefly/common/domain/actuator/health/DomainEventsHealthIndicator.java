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

package com.firefly.common.domain.actuator.health;

import com.firefly.common.domain.events.properties.DomainEventsProperties;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

/**
 * Base health indicator for Domain Events components.
 * Provides common functionality for all domain events health indicators.
 */
public abstract class DomainEventsHealthIndicator extends AbstractHealthIndicator {

    protected final DomainEventsProperties properties;

    protected DomainEventsHealthIndicator(DomainEventsProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        if (!properties.isEnabled()) {
            builder.up()
                    .withDetail("status", "Domain events are disabled")
                    .withDetail("adapter", properties.getAdapter().name());
            return;
        }

        performHealthCheck(builder);
    }

    /**
     * Perform the specific health check for the adapter.
     * @param builder the health builder
     * @throws Exception if health check fails
     */
    protected abstract void performHealthCheck(Health.Builder builder) throws Exception;
}