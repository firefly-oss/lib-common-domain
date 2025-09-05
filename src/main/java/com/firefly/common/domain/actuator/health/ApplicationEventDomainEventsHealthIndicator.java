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
import org.springframework.boot.actuate.health.Health;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Health indicator for ApplicationEvent Domain Events adapter.
 * Checks the availability of the ApplicationEventPublisher.
 */
public class ApplicationEventDomainEventsHealthIndicator extends DomainEventsHealthIndicator {

    private final ApplicationEventPublisher applicationEventPublisher;

    public ApplicationEventDomainEventsHealthIndicator(DomainEventsProperties properties, 
                                                       ApplicationEventPublisher applicationEventPublisher) {
        super(properties);
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    protected void performHealthCheck(Health.Builder builder) throws Exception {
        try {
            if (applicationEventPublisher == null) {
                builder.down()
                        .withDetail("status", "ApplicationEventPublisher not available")
                        .withDetail("adapter", "application_event");
                return;
            }

            // ApplicationEventPublisher is always healthy if available
            // since it's part of the Spring framework
            builder.up()
                    .withDetail("status", "ApplicationEventPublisher available")
                    .withDetail("adapter", "application_event")
                    .withDetail("publisherClass", applicationEventPublisher.getClass().getSimpleName());

        } catch (Exception e) {
            builder.down()
                    .withDetail("status", "Error checking ApplicationEventPublisher health")
                    .withDetail("adapter", "application_event")
                    .withDetail("error", e.getMessage());
        }
    }
}