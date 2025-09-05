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

import reactor.core.publisher.Mono;

/**
 * Interface for handling queries in the CQRS architecture.
 * Query handlers are responsible for processing queries and returning data.
 * Query operations should be idempotent and not cause side effects.
 * 
 * @param <Q> The query type this handler processes
 * @param <R> The result type returned by this handler
 */
public interface QueryHandler<Q extends Query<R>, R> {

    /**
     * Handles the given query asynchronously.
     * 
     * @param query the query to handle
     * @return a Mono containing the result of query processing
     */
    Mono<R> handle(Q query);

    /**
     * Returns the query type this handler can process.
     * Used for automatic handler registration and routing.
     * 
     * @return the query class
     */
    Class<Q> getQueryType();

    /**
     * Returns the result type this handler produces.
     * Used for type safety and result processing.
     * 
     * @return the result class
     */
    default Class<R> getResultType() {
        return null; // Can be overridden for explicit type information
    }

    /**
     * Validates whether this handler can process the given query.
     * Default implementation checks query type compatibility.
     * 
     * @param query the query to validate
     * @return true if this handler can process the query
     */
    default boolean canHandle(Query<?> query) {
        return getQueryType().isInstance(query);
    }

    /**
     * Indicates if this handler supports caching of results.
     * When true, the QueryBus may cache results based on query cache keys.
     * 
     * @return true if results can be cached, false otherwise
     */
    default boolean supportsCaching() {
        return true;
    }

    /**
     * Returns the cache TTL (Time To Live) in seconds for results from this handler.
     * Only used when supportsCaching() returns true.
     * 
     * @return cache TTL in seconds, or null for default TTL
     */
    default Long getCacheTtlSeconds() {
        return null; // Use system default
    }
}