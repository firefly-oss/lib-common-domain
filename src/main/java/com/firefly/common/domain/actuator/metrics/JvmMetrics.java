package com.firefly.common.domain.actuator.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
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
        // Heap memory metrics
        meterRegistry.gauge("jvm.memory.heap.used", Tags.empty(), memoryMXBean,
                bean -> bean.getHeapMemoryUsage().getUsed());
        
        meterRegistry.gauge("jvm.memory.heap.committed", Tags.empty(), memoryMXBean,
                bean -> bean.getHeapMemoryUsage().getCommitted());
        
        meterRegistry.gauge("jvm.memory.heap.max", Tags.empty(), memoryMXBean,
                bean -> bean.getHeapMemoryUsage().getMax());
        
        meterRegistry.gauge("jvm.memory.heap.init", Tags.empty(), memoryMXBean,
                bean -> bean.getHeapMemoryUsage().getInit());

        // Non-heap memory metrics
        meterRegistry.gauge("jvm.memory.non_heap.used", Tags.empty(), memoryMXBean,
                bean -> bean.getNonHeapMemoryUsage().getUsed());
        
        meterRegistry.gauge("jvm.memory.non_heap.committed", Tags.empty(), memoryMXBean,
                bean -> bean.getNonHeapMemoryUsage().getCommitted());
        
        meterRegistry.gauge("jvm.memory.non_heap.max", Tags.empty(), memoryMXBean,
                bean -> bean.getNonHeapMemoryUsage().getMax());
        
        meterRegistry.gauge("jvm.memory.non_heap.init", Tags.empty(), memoryMXBean,
                bean -> bean.getNonHeapMemoryUsage().getInit());
    }

    private void registerGarbageCollectionMetrics() {
        ManagementFactory.getGarbageCollectorMXBeans().forEach(gcBean -> {
            String gcName = gcBean.getName().toLowerCase().replace(" ", "_");
            
            meterRegistry.gauge("jvm.gc.collection.count", Tags.of("gc", gcName), gcBean,
                    GarbageCollectorMXBean::getCollectionCount);
            
            meterRegistry.gauge("jvm.gc.collection.time", Tags.of("gc", gcName), gcBean,
                    GarbageCollectorMXBean::getCollectionTime);
        });
    }

    private void registerThreadMetrics() {
        meterRegistry.gauge("jvm.threads.live", Tags.empty(), threadMXBean,
                ThreadMXBean::getThreadCount);
        
        meterRegistry.gauge("jvm.threads.daemon", Tags.empty(), threadMXBean,
                ThreadMXBean::getDaemonThreadCount);
        
        meterRegistry.gauge("jvm.threads.peak", Tags.empty(), threadMXBean,
                ThreadMXBean::getPeakThreadCount);
        
        meterRegistry.gauge("jvm.threads.started", Tags.empty(), threadMXBean,
                ThreadMXBean::getTotalStartedThreadCount);

        // Deadlocked threads
        meterRegistry.gauge("jvm.threads.deadlocked", Tags.empty(), threadMXBean, bean -> {
            long[] deadlockedThreads = bean.findDeadlockedThreads();
            return deadlockedThreads != null ? deadlockedThreads.length : 0;
        });
        
        meterRegistry.gauge("jvm.threads.deadlocked.monitor", Tags.empty(), threadMXBean, bean -> {
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