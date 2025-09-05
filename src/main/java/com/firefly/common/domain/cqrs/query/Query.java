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

package com.firefly.common.domain.cqrs.query;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base interface for all queries in the CQRS architecture.
 * Queries represent requests for data and should be idempotent read operations.
 * 
 * @param <R> The type of result returned by this query
 */
public interface Query<R> {

    /**
     * Unique identifier for this query instance.
     * @return the query ID
     */
    default String getQueryId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Timestamp when the query was created.
     * @return the creation timestamp
     */
    default Instant getTimestamp() {
        return Instant.now();
    }

    /**
     * Correlation ID for tracing across system boundaries.
     * @return the correlation ID, or null if not set
     */
    default String getCorrelationId() {
        return null;
    }

    /**
     * User or system identifier that initiated this query.
     * @return the initiator ID, or null if not set
     */
    default String getInitiatedBy() {
        return null;
    }

    /**
     * Additional metadata associated with this query.
     * Used for filtering, pagination, sorting, etc.
     * @return metadata map, or null if no metadata
     */
    default Map<String, Object> getMetadata() {
        return null;
    }

    /**
     * Expected result type for this query.
     * @return the result type class
     */
    @SuppressWarnings("unchecked")
    default Class<R> getResultType() {
        return (Class<R>) Object.class;
    }

    /**
     * Indicates if this query supports caching.
     * @return true if the result can be cached, false otherwise
     */
    default boolean isCacheable() {
        return true;
    }

    /**
     * Cache key for this query if caching is enabled.
     * Default implementation uses query class name and metadata hash.
     * @return cache key, or null to disable caching for this instance
     */
    default String getCacheKey() {
        if (!isCacheable()) {
            return null;
        }
        
        String baseKey = this.getClass().getSimpleName();
        Map<String, Object> metadata = getMetadata();
        
        if (metadata != null && !metadata.isEmpty()) {
            return baseKey + "_" + metadata.hashCode();
        }
        
        return baseKey;
    }
}