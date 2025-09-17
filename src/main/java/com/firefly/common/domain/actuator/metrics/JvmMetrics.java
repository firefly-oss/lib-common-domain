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

package com.firefly.common.domain.actuator.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;

/**
 * Custom JVM metrics collector for enhanced observability.
 * Provides detailed JVM memory, garbage collection, and thread metrics.
 */
@Component
public class JvmMetrics {

    private final MeterRegistry meterRegistry;
    private final MemoryMXBean memoryMXBean;
    private final ThreadMXBean threadMXBean;

    public JvmMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
    }

    @PostConstruct
    public void registerMetrics() {
        registerMemoryMetrics();
        registerGarbageCollectionMetrics();
        registerThreadMetrics();
    }

    private void registerMemoryMetrics() {
        // Heap memory metrics - using firefly prefix to avoid conflicts with built-in metrics
        meterRegistry.gauge("firefly.jvm.memory.heap.used", Tags.empty(), memoryMXBean,
                bean -> bean.getHeapMemoryUsage().getUsed());

        meterRegistry.gauge("firefly.jvm.memory.heap.committed", Tags.empty(), memoryMXBean,
                bean -> bean.getHeapMemoryUsage().getCommitted());

        meterRegistry.gauge("firefly.jvm.memory.heap.max", Tags.empty(), memoryMXBean,
                bean -> bean.getHeapMemoryUsage().getMax());

        meterRegistry.gauge("firefly.jvm.memory.heap.init", Tags.empty(), memoryMXBean,
                bean -> bean.getHeapMemoryUsage().getInit());

        // Non-heap memory metrics - using firefly prefix to avoid conflicts with built-in metrics
        meterRegistry.gauge("firefly.jvm.memory.non_heap.used", Tags.empty(), memoryMXBean,
                bean -> bean.getNonHeapMemoryUsage().getUsed());

        meterRegistry.gauge("firefly.jvm.memory.non_heap.committed", Tags.empty(), memoryMXBean,
                bean -> bean.getNonHeapMemoryUsage().getCommitted());

        meterRegistry.gauge("firefly.jvm.memory.non_heap.max", Tags.empty(), memoryMXBean,
                bean -> bean.getNonHeapMemoryUsage().getMax());

        meterRegistry.gauge("firefly.jvm.memory.non_heap.init", Tags.empty(), memoryMXBean,
                bean -> bean.getNonHeapMemoryUsage().getInit());
    }

    private void registerGarbageCollectionMetrics() {
        ManagementFactory.getGarbageCollectorMXBeans().forEach(gcBean -> {
            String gcName = gcBean.getName().toLowerCase().replace(" ", "_");

            // Using firefly prefix to avoid conflicts with built-in GC metrics
            meterRegistry.gauge("firefly.jvm.gc.collection.count", Tags.of("gc", gcName), gcBean,
                    GarbageCollectorMXBean::getCollectionCount);

            meterRegistry.gauge("firefly.jvm.gc.collection.time", Tags.of("gc", gcName), gcBean,
                    GarbageCollectorMXBean::getCollectionTime);
        });
    }

    private void registerThreadMetrics() {
        // Using firefly prefix to avoid conflicts with built-in thread metrics
        meterRegistry.gauge("firefly.jvm.threads.live", Tags.empty(), threadMXBean,
                ThreadMXBean::getThreadCount);

        meterRegistry.gauge("firefly.jvm.threads.daemon", Tags.empty(), threadMXBean,
                ThreadMXBean::getDaemonThreadCount);

        meterRegistry.gauge("firefly.jvm.threads.peak", Tags.empty(), threadMXBean,
                ThreadMXBean::getPeakThreadCount);

        meterRegistry.gauge("firefly.jvm.threads.started", Tags.empty(), threadMXBean,
                ThreadMXBean::getTotalStartedThreadCount);

        // Enhanced deadlock detection metrics (not available in built-in metrics)
        meterRegistry.gauge("firefly.jvm.threads.deadlocked", Tags.empty(), threadMXBean, bean -> {
            long[] deadlockedThreads = bean.findDeadlockedThreads();
            return deadlockedThreads != null ? deadlockedThreads.length : 0;
        });

        meterRegistry.gauge("firefly.jvm.threads.deadlocked.monitor", Tags.empty(), threadMXBean, bean -> {
            long[] deadlockedThreads = bean.findMonitorDeadlockedThreads();
            return deadlockedThreads != null ? deadlockedThreads.length : 0;
        });
    }

    /**
     * Records JVM memory usage.
     */
    public void recordMemoryUsage() {
        // This method can be called periodically to ensure metrics are updated
        // The actual metrics are automatically updated by Micrometer
    }

    /**
     * Records GC activity.
     */
    public void recordGcActivity() {
        // This method can be called after GC events
        // The actual metrics are automatically updated by Micrometer
    }
}