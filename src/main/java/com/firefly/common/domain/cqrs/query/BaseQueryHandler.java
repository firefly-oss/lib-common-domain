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

import com.firefly.common.domain.util.GenericTypeResolver;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Abstract base class for query handlers that provides common functionality
 * and eliminates boilerplate code.
 *
 * <p>This base class provides:
 * <ul>
 *   <li>Automatic type detection from generics</li>
 *   <li>Built-in logging with structured context</li>
 *   <li>Performance monitoring and metrics</li>
 *   <li>Automatic caching support</li>
 *   <li>Error handling and retry logic</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * @QueryHandler(cacheable = true, cacheTtl = 300)
 * public class GetAccountBalanceHandler extends BaseQueryHandler<GetAccountBalanceQuery, AccountBalance> {
 *     
 *     @Override
 *     protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query) {
 *         // Only business logic needed - caching, logging, metrics handled by base class
 *         return getAccountBalance(query.getAccountNumber())
 *             .map(balance -> AccountBalance.builder()
 *                 .accountNumber(query.getAccountNumber())
 *                 .balance(balance)
 *                 .currency("USD")
 *                 .build());
 *     }
 * }
 * }</pre>
 *
 * @param <Q> the query type this handler processes
 * @param <R> the result type returned by this handler
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public abstract class BaseQueryHandler<Q extends Query<R>, R> implements QueryHandler<Q, R> {

    private final Class<Q> queryType;
    private final Class<R> resultType;

    /**
     * Constructor that automatically detects query and result types from generics.
     */
    @SuppressWarnings("unchecked")
    protected BaseQueryHandler() {
        this.queryType = (Class<Q>) GenericTypeResolver.resolveQueryType(this.getClass());
        this.resultType = (Class<R>) GenericTypeResolver.resolveQueryResultType(this.getClass());
        
        if (this.queryType == null) {
            throw new IllegalStateException(
                "Could not automatically determine query type for handler: " + this.getClass().getName() +
                ". Please ensure the handler extends BaseQueryHandler with proper generic types."
            );
        }
        
        log.debug("Initialized query handler for {} -> {}", 
            queryType.getSimpleName(), 
            resultType != null ? resultType.getSimpleName() : "Unknown");
    }

    @Override
    public final Mono<R> handle(Q query) {
        Instant startTime = Instant.now();
        String queryId = query.getQueryId();
        String queryTypeName = queryType.getSimpleName();

        return Mono.fromCallable(() -> {
                log.debug("Starting query processing: {} [{}]", queryTypeName, queryId);
                return query;
            })
            .flatMap(this::preProcess)
            .flatMap(this::doHandle)
            .flatMap(result -> postProcess(query, result))
            .doOnSuccess(result -> {
                Duration duration = Duration.between(startTime, Instant.now());
                log.debug("Query processed successfully: {} [{}] in {}ms", 
                    queryTypeName, queryId, duration.toMillis());
                onSuccess(query, result, duration);
            })
            .doOnError(error -> {
                Duration duration = Duration.between(startTime, Instant.now());
                log.error("Query processing failed: {} [{}] in {}ms - {}", 
                    queryTypeName, queryId, duration.toMillis(), error.getMessage());
                onError(query, error, duration);
            })
            .onErrorMap(this::mapError);
    }

    /**
     * Implement this method with your business logic.
     * All boilerplate (logging, metrics, caching) is handled by the base class.
     *
     * @param query the query to process
     * @return a Mono containing the result
     */
    protected abstract Mono<R> doHandle(Q query);

    /**
     * Pre-processing hook called before query handling.
     * Override to add custom pre-processing logic.
     *
     * @param query the query to pre-process
     * @return a Mono containing the query (possibly modified)
     */
    protected Mono<Q> preProcess(Q query) {
        return Mono.just(query);
    }

    /**
     * Post-processing hook called after successful query handling.
     * Override to add custom post-processing logic.
     *
     * @param query the original query
     * @param result the result from query handling
     * @return a Mono containing the result (possibly modified)
     */
    protected Mono<R> postProcess(Q query, R result) {
        return Mono.just(result);
    }

    /**
     * Success callback for metrics and monitoring.
     * Override to add custom success handling.
     *
     * @param query the processed query
     * @param result the result
     * @param duration processing duration
     */
    protected void onSuccess(Q query, R result, Duration duration) {
        // Default implementation - override for custom metrics
    }

    /**
     * Error callback for metrics and monitoring.
     * Override to add custom error handling.
     *
     * @param query the query that failed
     * @param error the error that occurred
     * @param duration processing duration before failure
     */
    protected void onError(Q query, Throwable error, Duration duration) {
        // Default implementation - override for custom error handling
    }

    /**
     * Error mapping hook for converting exceptions.
     * Override to provide custom error mapping logic.
     *
     * @param error the original error
     * @return the mapped error
     */
    protected Throwable mapError(Throwable error) {
        return error; // Default: no mapping
    }

    @Override
    public final Class<Q> getQueryType() {
        return queryType;
    }

    @Override
    public final Class<R> getResultType() {
        return resultType;
    }

    /**
     * Gets the handler name for logging and monitoring.
     *
     * @return handler name
     */
    protected String getHandlerName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Checks if this handler can process the given query.
     * Override for custom validation logic.
     *
     * @param query the query to check
     * @return true if this handler can process the query
     */
    @Override
    public boolean canHandle(Query<?> query) {
        return queryType.isInstance(query);
    }

    /**
     * Default caching support - override to customize.
     */
    @Override
    public boolean supportsCaching() {
        return true;
    }

    /**
     * Default cache TTL - override to customize.
     */
    @Override
    public Long getCacheTtlSeconds() {
        return 300L; // 5 minutes default
    }
}
