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

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Health indicator for thread pool monitoring.
 * Monitors various executor services and thread pools for health and capacity issues.
 */
@Component
public class ThreadPoolHealthIndicator implements HealthIndicator {

    private final ApplicationContext applicationContext;
    
    // Thresholds for health checks
    private static final double HIGH_USAGE_THRESHOLD = 0.8; // 80%
    private static final double CRITICAL_USAGE_THRESHOLD = 0.95; // 95%

    public ThreadPoolHealthIndicator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            Health.Builder healthBuilder = Health.up();
            
            // Check common fork join pool
            checkCommonForkJoinPool(details, healthBuilder);
            
            // Check application context executor services
            checkApplicationExecutors(details, healthBuilder);
            
            return healthBuilder.withDetails(details).build();
            
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", "Failed to check thread pool health: " + e.getMessage())
                    .build();
        }
    }

    private void checkCommonForkJoinPool(Map<String, Object> details, Health.Builder healthBuilder) {
        ForkJoinPool commonPool = ForkJoinPool.commonPool();
        
        Map<String, Object> poolDetails = new HashMap<>();
        poolDetails.put("parallelism", commonPool.getParallelism());
        poolDetails.put("activeThreadCount", commonPool.getActiveThreadCount());
        poolDetails.put("runningThreadCount", commonPool.getRunningThreadCount());
        poolDetails.put("queuedSubmissionCount", commonPool.getQueuedSubmissionCount());
        poolDetails.put("queuedTaskCount", commonPool.getQueuedTaskCount());
        poolDetails.put("stealCount", commonPool.getStealCount());
        poolDetails.put("isShutdown", commonPool.isShutdown());
        poolDetails.put("isTerminated", commonPool.isTerminated());
        
        // Calculate usage percentage
        double usage = (double) commonPool.getActiveThreadCount() / commonPool.getParallelism();
        poolDetails.put("usage", String.format("%.2f%%", usage * 100));
        
        // Determine health status
        if (usage >= CRITICAL_USAGE_THRESHOLD) {
            healthBuilder.down();
            poolDetails.put("status", "CRITICAL - Very high thread usage");
        } else if (usage >= HIGH_USAGE_THRESHOLD) {
            healthBuilder.status("DEGRADED");
            poolDetails.put("status", "DEGRADED - High thread usage");
        } else {
            poolDetails.put("status", "HEALTHY");
        }
        
        details.put("commonForkJoinPool", poolDetails);
    }

    private void checkApplicationExecutors(Map<String, Object> details, Health.Builder healthBuilder) {
        Map<String, ExecutorService> executors = applicationContext.getBeansOfType(ExecutorService.class);
        
        for (Map.Entry<String, ExecutorService> entry : executors.entrySet()) {
            String beanName = entry.getKey();
            ExecutorService executor = entry.getValue();
            
            Map<String, Object> executorDetails = getExecutorDetails(executor);
            details.put("executor_" + beanName, executorDetails);
            
            // Check if executor is in critical state
            Object status = executorDetails.get("status");
            if ("CRITICAL".equals(status)) {
                healthBuilder.down();
            } else if ("DEGRADED".equals(status)) {
                healthBuilder.status("DEGRADED");
            }
        }
    }

    private Map<String, Object> getExecutorDetails(ExecutorService executor) {
        Map<String, Object> details = new HashMap<>();
        
        if (executor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
            
            details.put("type", "ThreadPoolExecutor");
            details.put("corePoolSize", tpe.getCorePoolSize());
            details.put("maximumPoolSize", tpe.getMaximumPoolSize());
            details.put("activeCount", tpe.getActiveCount());
            details.put("poolSize", tpe.getPoolSize());
            details.put("largestPoolSize", tpe.getLargestPoolSize());
            details.put("taskCount", tpe.getTaskCount());
            details.put("completedTaskCount", tpe.getCompletedTaskCount());
            details.put("queueSize", tpe.getQueue().size());
            details.put("queueRemainingCapacity", tpe.getQueue().remainingCapacity());
            details.put("isShutdown", tpe.isShutdown());
            details.put("isTerminated", tpe.isTerminated());
            details.put("isTerminating", tpe.isTerminating());
            
            // Calculate usage metrics
            double poolUsage = (double) tpe.getActiveCount() / tpe.getMaximumPoolSize();
            details.put("poolUsage", String.format("%.2f%%", poolUsage * 100));
            
            // Check queue usage if bounded
            if (tpe.getQueue().remainingCapacity() != Integer.MAX_VALUE) {
                int totalCapacity = tpe.getQueue().size() + tpe.getQueue().remainingCapacity();
                double queueUsage = (double) tpe.getQueue().size() / totalCapacity;
                details.put("queueUsage", String.format("%.2f%%", queueUsage * 100));
                
                // Determine health status based on pool and queue usage
                if (poolUsage >= CRITICAL_USAGE_THRESHOLD || queueUsage >= CRITICAL_USAGE_THRESHOLD) {
                    details.put("status", "CRITICAL");
                } else if (poolUsage >= HIGH_USAGE_THRESHOLD || queueUsage >= HIGH_USAGE_THRESHOLD) {
                    details.put("status", "DEGRADED");
                } else {
                    details.put("status", "HEALTHY");
                }
            } else {
                // Unbounded queue - only check pool usage
                if (poolUsage >= CRITICAL_USAGE_THRESHOLD) {
                    details.put("status", "CRITICAL");
                } else if (poolUsage >= HIGH_USAGE_THRESHOLD) {
                    details.put("status", "DEGRADED");
                } else {
                    details.put("status", "HEALTHY");
                }
            }
            
        } else if (executor instanceof ForkJoinPool) {
            ForkJoinPool fjp = (ForkJoinPool) executor;
            
            details.put("type", "ForkJoinPool");
            details.put("parallelism", fjp.getParallelism());
            details.put("activeThreadCount", fjp.getActiveThreadCount());
            details.put("runningThreadCount", fjp.getRunningThreadCount());
            details.put("queuedSubmissionCount", fjp.getQueuedSubmissionCount());
            details.put("queuedTaskCount", fjp.getQueuedTaskCount());
            details.put("stealCount", fjp.getStealCount());
            details.put("isShutdown", fjp.isShutdown());
            details.put("isTerminated", fjp.isTerminated());
            
            double usage = (double) fjp.getActiveThreadCount() / fjp.getParallelism();
            details.put("usage", String.format("%.2f%%", usage * 100));
            
            if (usage >= CRITICAL_USAGE_THRESHOLD) {
                details.put("status", "CRITICAL");
            } else if (usage >= HIGH_USAGE_THRESHOLD) {
                details.put("status", "DEGRADED");
            } else {
                details.put("status", "HEALTHY");
            }
            
        } else {
            details.put("type", executor.getClass().getSimpleName());
            details.put("isShutdown", executor.isShutdown());
            details.put("isTerminated", executor.isTerminated());
            details.put("status", "UNKNOWN");
        }
        
        return details;
    }
}