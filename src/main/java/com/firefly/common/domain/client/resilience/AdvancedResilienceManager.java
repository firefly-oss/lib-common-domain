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

package com.firefly.common.domain.client.resilience;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced resilience manager providing bulkhead isolation, rate limiting, and adaptive patterns.
 * 
 * <p>This manager implements advanced resilience patterns beyond basic circuit breakers and retries:
 * <ul>
 *   <li>Bulkhead isolation to prevent resource exhaustion</li>
 *   <li>Rate limiting to control request throughput</li>
 *   <li>Adaptive timeout based on historical performance</li>
 *   <li>Load shedding under high load conditions</li>
 *   <li>Graceful degradation strategies</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc
 * @since 2.0.0
 */
@Slf4j
public class AdvancedResilienceManager {

    private final Map<String, BulkheadIsolation> bulkheads = new ConcurrentHashMap<>();
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    private final Map<String, AdaptiveTimeout> adaptiveTimeouts = new ConcurrentHashMap<>();
    private final LoadSheddingStrategy loadSheddingStrategy;

    public AdvancedResilienceManager(LoadSheddingStrategy loadSheddingStrategy) {
        this.loadSheddingStrategy = loadSheddingStrategy;
    }

    /**
     * Applies resilience patterns to a service operation.
     */
    public <T> Mono<T> applyResilience(String serviceName, Mono<T> operation, ResilienceConfig config) {
        return Mono.defer(() -> {
            // Check load shedding first
            if (loadSheddingStrategy.shouldShedLoad(serviceName)) {
                return Mono.error(new LoadSheddingException("Request shed due to high load for service: " + serviceName));
            }

            // Apply rate limiting
            RateLimiter rateLimiter = getRateLimiter(serviceName, config);
            if (!rateLimiter.tryAcquire()) {
                return Mono.error(new RateLimitExceededException("Rate limit exceeded for service: " + serviceName));
            }

            // Apply bulkhead isolation
            BulkheadIsolation bulkhead = getBulkhead(serviceName, config);
            
            return bulkhead.execute(() -> {
                // Apply adaptive timeout
                AdaptiveTimeout adaptiveTimeout = getAdaptiveTimeout(serviceName, config);
                Duration timeout = adaptiveTimeout.calculateTimeout();
                
                long startTime = System.nanoTime();
                
                return operation
                    .timeout(timeout)
                    .doOnSuccess(result -> {
                        long duration = System.nanoTime() - startTime;
                        adaptiveTimeout.recordSuccess(Duration.ofNanos(duration));
                        rateLimiter.onSuccess();
                    })
                    .doOnError(error -> {
                        long duration = System.nanoTime() - startTime;
                        adaptiveTimeout.recordFailure(Duration.ofNanos(duration), error);
                        rateLimiter.onError();
                    });
            });
        });
    }

    private BulkheadIsolation getBulkhead(String serviceName, ResilienceConfig config) {
        return bulkheads.computeIfAbsent(serviceName, 
            key -> new BulkheadIsolation(config.getMaxConcurrentCalls(), config.getMaxWaitTime()));
    }

    private RateLimiter getRateLimiter(String serviceName, ResilienceConfig config) {
        return rateLimiters.computeIfAbsent(serviceName, 
            key -> new RateLimiter(config.getRequestsPerSecond(), config.getBurstCapacity()));
    }

    private AdaptiveTimeout getAdaptiveTimeout(String serviceName, ResilienceConfig config) {
        return adaptiveTimeouts.computeIfAbsent(serviceName, 
            key -> new AdaptiveTimeout(config.getBaseTimeout(), config.getMaxTimeout()));
    }

    /**
     * Bulkhead isolation implementation.
     */
    public static class BulkheadIsolation {
        private final Semaphore semaphore;
        private final Duration maxWaitTime;
        private final Scheduler scheduler;

        public BulkheadIsolation(int maxConcurrentCalls, Duration maxWaitTime) {
            this.semaphore = new Semaphore(maxConcurrentCalls);
            this.maxWaitTime = maxWaitTime;
            this.scheduler = Schedulers.newBoundedElastic(maxConcurrentCalls, Integer.MAX_VALUE, "bulkhead");
        }

        public <T> Mono<T> execute(java.util.function.Supplier<Mono<T>> operation) {
            return Mono.fromCallable(() -> {
                if (!semaphore.tryAcquire(maxWaitTime.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    throw new BulkheadFullException("Bulkhead is full, cannot acquire permit within " + maxWaitTime);
                }
                return true;
            })
            .subscribeOn(scheduler)
            .flatMap(acquired -> operation.get())
            .doFinally(signal -> semaphore.release());
        }

        public int getAvailablePermits() {
            return semaphore.availablePermits();
        }
    }

    /**
     * Rate limiter implementation using token bucket algorithm.
     */
    public static class RateLimiter {
        private final double requestsPerSecond;
        private final int burstCapacity;
        private final AtomicLong lastRefillTime = new AtomicLong(System.nanoTime());
        private volatile double availableTokens;

        public RateLimiter(double requestsPerSecond, int burstCapacity) {
            this.requestsPerSecond = requestsPerSecond;
            this.burstCapacity = burstCapacity;
            this.availableTokens = burstCapacity;
        }

        public boolean tryAcquire() {
            refillTokens();
            
            synchronized (this) {
                if (availableTokens >= 1.0) {
                    availableTokens -= 1.0;
                    return true;
                }
                return false;
            }
        }

        private void refillTokens() {
            long now = System.nanoTime();
            long lastRefill = lastRefillTime.get();
            
            if (now > lastRefill) {
                double timeDelta = (now - lastRefill) / 1_000_000_000.0; // Convert to seconds
                double tokensToAdd = timeDelta * requestsPerSecond;
                
                synchronized (this) {
                    availableTokens = Math.min(burstCapacity, availableTokens + tokensToAdd);
                }
                
                lastRefillTime.set(now);
            }
        }

        public void onSuccess() {
            // Could implement adaptive rate limiting based on success rate
        }

        public void onError() {
            // Could implement adaptive rate limiting based on error rate
        }

        public double getAvailableTokens() {
            refillTokens();
            return availableTokens;
        }
    }

    /**
     * Adaptive timeout implementation that adjusts based on historical performance.
     */
    public static class AdaptiveTimeout {
        private final Duration baseTimeout;
        private final Duration maxTimeout;
        private final java.util.concurrent.atomic.DoubleAdder totalResponseTime = new java.util.concurrent.atomic.DoubleAdder();
        private final AtomicLong successCount = new AtomicLong();
        private final AtomicLong failureCount = new AtomicLong();
        private volatile Duration currentTimeout;

        public AdaptiveTimeout(Duration baseTimeout, Duration maxTimeout) {
            this.baseTimeout = baseTimeout;
            this.maxTimeout = maxTimeout;
            this.currentTimeout = baseTimeout;
        }

        public Duration calculateTimeout() {
            long totalCalls = successCount.get() + failureCount.get();
            
            if (totalCalls < 10) {
                return baseTimeout; // Not enough data, use base timeout
            }

            double avgResponseTime = totalResponseTime.sum() / successCount.get();
            double failureRate = (double) failureCount.get() / totalCalls;

            // Adjust timeout based on average response time and failure rate
            double multiplier = 1.0 + (failureRate * 2.0); // Increase timeout if high failure rate
            Duration adaptedTimeout = Duration.ofMillis((long) (avgResponseTime * multiplier * 2.0)); // 2x avg response time

            // Ensure timeout is within bounds
            if (adaptedTimeout.compareTo(baseTimeout) < 0) {
                adaptedTimeout = baseTimeout;
            } else if (adaptedTimeout.compareTo(maxTimeout) > 0) {
                adaptedTimeout = maxTimeout;
            }

            currentTimeout = adaptedTimeout;
            return adaptedTimeout;
        }

        public void recordSuccess(Duration responseTime) {
            totalResponseTime.add(responseTime.toMillis());
            successCount.incrementAndGet();
        }

        public void recordFailure(Duration responseTime, Throwable error) {
            failureCount.incrementAndGet();
            
            // Don't include timeout failures in average response time calculation
            if (!(error instanceof java.util.concurrent.TimeoutException)) {
                totalResponseTime.add(responseTime.toMillis());
            }
        }

        public Duration getCurrentTimeout() {
            return currentTimeout;
        }
    }

    /**
     * Load shedding strategy interface.
     */
    public interface LoadSheddingStrategy {
        boolean shouldShedLoad(String serviceName);
    }

    /**
     * Simple load shedding strategy based on system load.
     */
    public static class SystemLoadSheddingStrategy implements LoadSheddingStrategy {
        private final double maxCpuUsage;
        private final double maxMemoryUsage;

        public SystemLoadSheddingStrategy(double maxCpuUsage, double maxMemoryUsage) {
            this.maxCpuUsage = maxCpuUsage;
            this.maxMemoryUsage = maxMemoryUsage;
        }

        @Override
        public boolean shouldShedLoad(String serviceName) {
            // Simple implementation - in practice, you'd use proper system monitoring
            Runtime runtime = Runtime.getRuntime();
            double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
            
            return memoryUsage > maxMemoryUsage;
        }
    }

    /**
     * Configuration for resilience patterns.
     */
    public static class ResilienceConfig {
        private final int maxConcurrentCalls;
        private final Duration maxWaitTime;
        private final double requestsPerSecond;
        private final int burstCapacity;
        private final Duration baseTimeout;
        private final Duration maxTimeout;

        public ResilienceConfig(int maxConcurrentCalls, Duration maxWaitTime, double requestsPerSecond, 
                              int burstCapacity, Duration baseTimeout, Duration maxTimeout) {
            this.maxConcurrentCalls = maxConcurrentCalls;
            this.maxWaitTime = maxWaitTime;
            this.requestsPerSecond = requestsPerSecond;
            this.burstCapacity = burstCapacity;
            this.baseTimeout = baseTimeout;
            this.maxTimeout = maxTimeout;
        }

        // Getters
        public int getMaxConcurrentCalls() { return maxConcurrentCalls; }
        public Duration getMaxWaitTime() { return maxWaitTime; }
        public double getRequestsPerSecond() { return requestsPerSecond; }
        public int getBurstCapacity() { return burstCapacity; }
        public Duration getBaseTimeout() { return baseTimeout; }
        public Duration getMaxTimeout() { return maxTimeout; }
    }

    // Exception classes
    public static class LoadSheddingException extends RuntimeException {
        public LoadSheddingException(String message) { super(message); }
    }

    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) { super(message); }
    }

    public static class BulkheadFullException extends RuntimeException {
        public BulkheadFullException(String message) { super(message); }
    }
}
