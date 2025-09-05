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

package com.firefly.common.domain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for CQRS framework.
 */
@ConfigurationProperties(prefix = "firefly.cqrs")
@Data
public class CqrsProperties {

    /**
     * Whether CQRS framework is enabled.
     */
    private boolean enabled = true;

    /**
     * Command processing configuration.
     */
    private Command command = new Command();

    /**
     * Query processing configuration.
     */
    private Query query = new Query();

    @Data
    public static class Command {
        /**
         * Default timeout for command processing.
         */
        private Duration timeout = Duration.ofSeconds(30);

        /**
         * Whether to enable command metrics.
         */
        private boolean metricsEnabled = true;

        /**
         * Whether to enable command tracing.
         */
        private boolean tracingEnabled = true;
    }

    @Data
    public static class Query {
        /**
         * Default timeout for query processing.
         */
        private Duration timeout = Duration.ofSeconds(15);

        /**
         * Whether to enable query caching by default.
         */
        private boolean cachingEnabled = true;

        /**
         * Default cache TTL for queries.
         */
        private Duration cacheTtl = Duration.ofMinutes(15);

        /**
         * Whether to enable query metrics.
         */
        private boolean metricsEnabled = true;

        /**
         * Whether to enable query tracing.
         */
        private boolean tracingEnabled = true;
    }
}