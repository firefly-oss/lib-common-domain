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

package com.firefly.common.domain.cqrs.fluent;

import com.firefly.common.domain.cqrs.query.Query;
import com.firefly.common.domain.cqrs.query.QueryBus;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fluent builder for creating and executing queries with reduced boilerplate.
 *
 * <p>This builder provides a fluent API for query creation that eliminates
 * common boilerplate and provides smart defaults for metadata, correlation,
 * and caching.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Simple query creation and execution
 * QueryBuilder.create(GetAccountBalanceQuery.class)
 *     .withAccountNumber("ACC-123")
 *     .withCurrency("USD")
 *     .correlatedBy("REQ-456")
 *     .cached(true)
 *     .executeWith(queryBus)
 *     .subscribe(balance -> log.info("Balance: {}", balance));
 *
 * // Advanced usage with custom cache key and metadata
 * QueryBuilder.create(GetTransactionHistoryQuery.class)
 *     .withAccountNumber("ACC-123")
 *     .withFromDate(LocalDate.now().minusDays(30))
 *     .withToDate(LocalDate.now())
 *     .withPageSize(50)
 *     .withMetadata("priority", "HIGH")
 *     .cached(true)
 *     .withCacheKey("transactions:ACC-123:30days")
 *     .executeWith(queryBus);
 * }</pre>
 *
 * @param <Q> the query type
 * @param <R> the result type
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public class QueryBuilder<Q extends Query<R>, R> {

    private final Class<Q> queryType;
    private final Map<String, Object> properties = new HashMap<>();
    private final Map<String, Object> metadata = new HashMap<>();
    private String queryId;
    private String correlationId;
    private Instant timestamp;
    private boolean cacheable = true;
    private String cacheKey;
    private Long cacheTtlSeconds;

    private QueryBuilder(Class<Q> queryType) {
        this.queryType = queryType;
        this.queryId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    /**
     * Creates a new query builder for the specified query type.
     *
     * @param queryType the query class
     * @param <Q> the query type
     * @param <R> the result type
     * @return a new query builder
     */
    public static <Q extends Query<R>, R> QueryBuilder<Q, R> create(Class<Q> queryType) {
        return new QueryBuilder<>(queryType);
    }

    /**
     * Sets a property value using fluent method naming.
     * This method uses reflection to set the property on the query.
     *
     * @param propertyName the property name
     * @param value the property value
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> with(String propertyName, Object value) {
        properties.put(propertyName, value);
        return this;
    }

    /**
     * Sets the query ID.
     *
     * @param queryId the query ID
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> withId(String queryId) {
        this.queryId = queryId;
        return this;
    }

    /**
     * Sets the correlation ID for distributed tracing.
     *
     * @param correlationId the correlation ID
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> correlatedBy(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    /**
     * Sets the query timestamp.
     *
     * @param timestamp the timestamp
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> at(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Adds metadata to the query.
     *
     * @param key the metadata key
     * @param value the metadata value
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> withMetadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    /**
     * Adds multiple metadata entries.
     *
     * @param metadata the metadata map
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> withMetadata(Map<String, Object> metadata) {
        this.metadata.putAll(metadata);
        return this;
    }

    /**
     * Sets whether the query result should be cached.
     *
     * @param cacheable true to enable caching
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> cached(boolean cacheable) {
        this.cacheable = cacheable;
        return this;
    }

    /**
     * Sets a custom cache key for the query.
     *
     * @param cacheKey the cache key
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> withCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
        return this;
    }

    /**
     * Sets the cache TTL in seconds.
     *
     * @param ttlSeconds the TTL in seconds
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> withCacheTtl(long ttlSeconds) {
        this.cacheTtlSeconds = ttlSeconds;
        return this;
    }

    /**
     * Builds the query instance.
     *
     * @return the built query
     */
    public Q build() {
        try {
            // Create query instance using reflection or factory
            Q query = createQueryInstance();
            return query;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build query: " + e.getMessage(), e);
        }
    }

    /**
     * Builds and executes the query using the provided query bus.
     *
     * @param queryBus the query bus to use for execution
     * @return a Mono containing the query result
     */
    public Mono<R> executeWith(QueryBus queryBus) {
        Q query = build();
        return queryBus.query(query);
    }

    /**
     * Creates a query instance using reflection and builder pattern.
     */
    @SuppressWarnings("unchecked")
    private Q createQueryInstance() throws Exception {
        // Try to find a builder method first
        try {
            var builderMethod = queryType.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            
            // Set properties on the builder
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String methodName = entry.getKey();
                Object value = entry.getValue();
                
                try {
                    var method = builder.getClass().getMethod(methodName, value.getClass());
                    method.invoke(builder, value);
                } catch (NoSuchMethodException e) {
                    // Try with different parameter types
                    for (var method : builder.getClass().getMethods()) {
                        if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                            method.invoke(builder, value);
                            break;
                        }
                    }
                }
            }
            
            // Build the query
            var buildMethod = builder.getClass().getMethod("build");
            Q query = (Q) buildMethod.invoke(builder);
            
            // Set metadata and other properties if the query supports it
            setQueryMetadata(query);
            
            return query;
        } catch (NoSuchMethodException e) {
            // Fall back to constructor-based creation
            return createQueryWithConstructor();
        }
    }

    /**
     * Creates query using constructor (fallback method).
     */
    private Q createQueryWithConstructor() throws Exception {
        // This is a simplified implementation
        // In a real implementation, you'd use more sophisticated reflection
        // or code generation to handle various constructor patterns
        throw new UnsupportedOperationException(
            "Constructor-based query creation not yet implemented. " +
            "Please ensure your query class has a builder() method."
        );
    }

    /**
     * Sets metadata and other properties on the query if supported.
     */
    private void setQueryMetadata(Q query) {
        // Use reflection to set metadata if the query supports it
        try {
            if (correlationId != null) {
                setFieldIfExists(query, "correlationId", correlationId);
            }
            if (queryId != null) {
                setFieldIfExists(query, "queryId", queryId);
            }
            if (timestamp != null) {
                setFieldIfExists(query, "timestamp", timestamp);
            }
            if (!metadata.isEmpty()) {
                setFieldIfExists(query, "metadata", metadata);
            }
            if (cacheKey != null) {
                setFieldIfExists(query, "cacheKey", cacheKey);
            }
            setFieldIfExists(query, "cacheable", cacheable);
            if (cacheTtlSeconds != null) {
                setFieldIfExists(query, "cacheTtlSeconds", cacheTtlSeconds);
            }
        } catch (Exception e) {
            // Ignore metadata setting errors - not all queries may support all metadata
        }
    }

    /**
     * Sets a field value if the field exists.
     */
    private void setFieldIfExists(Q query, String fieldName, Object value) {
        try {
            var field = query.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(query, value);
        } catch (Exception e) {
            // Field doesn't exist or can't be set - ignore
        }
    }
}
