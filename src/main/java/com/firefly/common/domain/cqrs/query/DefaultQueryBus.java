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

import com.firefly.common.domain.tracing.CorrelationContext;
import com.firefly.common.domain.validation.AutoValidationProcessor;
import com.firefly.common.domain.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of QueryBus with automatic handler discovery,
 * caching support, tracing, and error handling.
 */
@Slf4j
@Component
public class DefaultQueryBus implements QueryBus {

    private static final String DEFAULT_CACHE_NAME = "query-cache";
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(15);

    private final Map<Class<? extends Query<?>>, QueryHandler<?, ?>> handlers = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;
    private final CorrelationContext correlationContext;
    private final AutoValidationProcessor autoValidationProcessor;
    private final CacheManager cacheManager;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    // Metrics
    private io.micrometer.core.instrument.Counter processedCounter;
    private io.micrometer.core.instrument.Timer processingTimer;

    @Autowired
    public DefaultQueryBus(ApplicationContext applicationContext,
                          CorrelationContext correlationContext,
                          AutoValidationProcessor autoValidationProcessor,
                          @Autowired(required = false) CacheManager cacheManager,
                          @Autowired(required = false) io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        this.applicationContext = applicationContext;
        this.correlationContext = correlationContext;
        this.autoValidationProcessor = autoValidationProcessor;
        this.cacheManager = cacheManager;
        this.meterRegistry = meterRegistry;

        if (meterRegistry != null) {
            initializeMetrics();
        }

        discoverHandlers();
    }

    /**
     * Initialize metrics for query processing.
     */
    private void initializeMetrics() {
        this.processedCounter = io.micrometer.core.instrument.Counter.builder("firefly.cqrs.query.processed")
            .description("Total number of queries processed")
            .register(meterRegistry);

        this.processingTimer = io.micrometer.core.instrument.Timer.builder("firefly.cqrs.query.processing.time")
            .description("Query processing time")
            .register(meterRegistry);

        log.debug("Initialized CQRS query metrics");
    }

    /**
     * Execute query handler with metrics collection.
     */
    private <R> Mono<R> executeWithMetrics(QueryHandler<Query<R>, R> handler, Query<R> query) {
        if (meterRegistry != null && processingTimer != null) {
            io.micrometer.core.instrument.Timer.Sample sample = io.micrometer.core.instrument.Timer.start(meterRegistry);
            return handler.handle(query)
                    .doOnSuccess(result -> {
                        sample.stop(processingTimer);
                        if (processedCounter != null) {
                            processedCounter.increment();
                        }
                    })
                    .doOnError(error -> sample.stop(processingTimer));
        } else {
            return handler.handle(query);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Mono<R> query(Query<R> query) {
        return Mono.fromCallable(() -> {
                    log.debug("Processing query: {} with ID: {}", query.getClass().getSimpleName(), query.getQueryId());
                    
                    QueryHandler<Query<R>, R> handler = (QueryHandler<Query<R>, R>) handlers.get(query.getClass());
                    if (handler == null) {
                        throw new QueryHandlerNotFoundException("No handler found for query: " + query.getClass().getName());
                    }
                    
                    return handler;
                })
                .flatMap(handler -> {
                    // Set correlation context if available
                    if (query.getCorrelationId() != null) {
                        correlationContext.setCorrelationId(query.getCorrelationId());
                    }

                    // Perform automatic Jakarta validation first
                    return autoValidationProcessor.validate(query)
                            .flatMap(validationResult -> {
                                if (!validationResult.isValid()) {
                                    String errorMessage = String.format("Query validation failed for %s: %s",
                                            query.getClass().getSimpleName(), validationResult.getSummary());
                                    log.warn(errorMessage);
                                    return Mono.error(new ValidationException(validationResult));
                                }

                                // Check cache if enabled
                                if (query.isCacheable() && handler.supportsCaching()) {
                                    String cacheKey = query.getCacheKey();
                                    if (cacheKey != null) {
                                        return getCachedResult(cacheKey, query.getResultType())
                                                .switchIfEmpty(executeAndCache(handler, query, cacheKey));
                                    }
                                }

                                // Execute without caching
                                return executeWithMetrics(handler, query)
                                        .doOnSuccess(result -> log.debug("Query {} processed successfully", query.getQueryId()))
                                        .doOnError(error -> log.error("Query {} processing failed: {}", query.getQueryId(), error.getMessage()))
                                        .doFinally(signalType -> correlationContext.clear());
                            });
                })
                .onErrorMap(throwable -> {
                    if (throwable instanceof QueryHandlerNotFoundException) {
                        return throwable;
                    }
                    if (throwable instanceof ValidationException) {
                        return throwable;
                    }
                    return new QueryProcessingException("Failed to process query: " + query.getQueryId(), throwable);
                });
    }

    @Override
    public <Q extends Query<R>, R> void registerHandler(QueryHandler<Q, R> handler) {
        Class<Q> queryType = handler.getQueryType();
        
        if (handlers.containsKey(queryType)) {
            log.warn("Replacing existing handler for query: {}", queryType.getName());
        }
        
        handlers.put(queryType, handler);
        log.info("Registered query handler for: {}", queryType.getName());
    }

    @Override
    public <Q extends Query<?>> void unregisterHandler(Class<Q> queryType) {
        QueryHandler<?, ?> removed = handlers.remove(queryType);
        if (removed != null) {
            log.info("Unregistered query handler for: {}", queryType.getName());
        } else {
            log.warn("No handler found to unregister for query: {}", queryType.getName());
        }
    }

    @Override
    public boolean hasHandler(Class<? extends Query<?>> queryType) {
        return handlers.containsKey(queryType);
    }

    @Override
    public Mono<Void> clearCache(String cacheKey) {
        return Mono.fromRunnable(() -> {
            Cache cache = cacheManager.getCache(DEFAULT_CACHE_NAME);
            if (cache != null) {
                cache.evict(cacheKey);
                log.debug("Cleared cache for key: {}", cacheKey);
            }
        });
    }

    @Override
    public Mono<Void> clearAllCache() {
        return Mono.fromRunnable(() -> {
            Cache cache = cacheManager.getCache(DEFAULT_CACHE_NAME);
            if (cache != null) {
                cache.clear();
                log.debug("Cleared all query cache");
            }
        });
    }

    /**
     * Discovers all QueryHandler beans in the ApplicationContext and registers them.
     */
    @SuppressWarnings("unchecked")
    private void discoverHandlers() {
        Map<String, QueryHandler> handlerBeans = applicationContext.getBeansOfType(QueryHandler.class);
        
        for (QueryHandler<?, ?> handler : handlerBeans.values()) {
            try {
                registerHandler(handler);
            } catch (Exception e) {
                log.error("Failed to register query handler: {}", handler.getClass().getName(), e);
            }
        }
        
        log.info("Discovered and registered {} query handlers", handlers.size());
    }

    @SuppressWarnings("unchecked")
    private <R> Mono<R> getCachedResult(String cacheKey, Class<R> resultType) {
        return Mono.fromCallable(() -> {
            Cache cache = cacheManager.getCache(DEFAULT_CACHE_NAME);
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(cacheKey);
                if (wrapper != null && wrapper.get() != null) {
                    log.debug("Cache hit for key: {}", cacheKey);
                    return (R) wrapper.get();
                }
            }
            return null;
        });
    }

    private <R> Mono<R> executeAndCache(QueryHandler<Query<R>, R> handler, Query<R> query, String cacheKey) {
        return executeWithMetrics(handler, query)
                .doOnSuccess(result -> {
                    if (result != null) {
                        Cache cache = cacheManager.getCache(DEFAULT_CACHE_NAME);
                        if (cache != null) {
                            cache.put(cacheKey, result);
                            log.debug("Cached result for key: {}", cacheKey);
                        }
                    }
                    log.debug("Query {} processed successfully", query.getQueryId());
                })
                .doOnError(error -> log.error("Query {} processing failed: {}", query.getQueryId(), error.getMessage()))
                .doFinally(signalType -> correlationContext.clear());
    }

    /**
     * Exception thrown when no handler is found for a query.
     */
    public static class QueryHandlerNotFoundException extends RuntimeException {
        public QueryHandlerNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when query processing fails.
     */
    public static class QueryProcessingException extends RuntimeException {
        public QueryProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}