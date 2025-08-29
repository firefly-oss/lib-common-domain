package com.firefly.common.domain.actuator.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Application startup metrics collector for observability.
 * Tracks various phases of application startup including JVM start, 
 * Spring context initialization, and application readiness.
 */
@Component
public class ApplicationStartupMetrics {

    private final MeterRegistry meterRegistry;
    
    // Startup timing data
    private final long jvmStartTime;
    private final AtomicLong applicationStartingTime = new AtomicLong(0);
    private final AtomicLong contextRefreshedTime = new AtomicLong(0);
    private final AtomicLong applicationStartedTime = new AtomicLong(0);
    private final AtomicLong applicationReadyTime = new AtomicLong(0);
    
    // Calculated durations
    private final AtomicLong jvmToApplicationStart = new AtomicLong(0);
    private final AtomicLong applicationStartToContextRefresh = new AtomicLong(0);
    private final AtomicLong contextRefreshToStarted = new AtomicLong(0);
    private final AtomicLong startedToReady = new AtomicLong(0);
    private final AtomicLong totalStartupTime = new AtomicLong(0);

    public ApplicationStartupMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.jvmStartTime = System.currentTimeMillis();
    }

    @PostConstruct
    public void registerMetrics() {
        // Register gauges for startup timing metrics
        meterRegistry.gauge("application.startup.jvm_start_time", Tags.empty(), this,
                metrics -> metrics.jvmStartTime);
        
        meterRegistry.gauge("application.startup.duration.jvm_to_application_start", Tags.empty(), 
                jvmToApplicationStart, AtomicLong::get);
        
        meterRegistry.gauge("application.startup.duration.application_start_to_context_refresh", Tags.empty(), 
                applicationStartToContextRefresh, AtomicLong::get);
        
        meterRegistry.gauge("application.startup.duration.context_refresh_to_started", Tags.empty(), 
                contextRefreshToStarted, AtomicLong::get);
        
        meterRegistry.gauge("application.startup.duration.started_to_ready", Tags.empty(), 
                startedToReady, AtomicLong::get);
        
        meterRegistry.gauge("application.startup.duration.total", Tags.empty(), 
                totalStartupTime, AtomicLong::get);
        
        // Register timestamp gauges
        meterRegistry.gauge("application.startup.timestamp.application_starting", Tags.empty(), 
                applicationStartingTime, AtomicLong::get);
        
        meterRegistry.gauge("application.startup.timestamp.context_refreshed", Tags.empty(), 
                contextRefreshedTime, AtomicLong::get);
        
        meterRegistry.gauge("application.startup.timestamp.application_started", Tags.empty(), 
                applicationStartedTime, AtomicLong::get);
        
        meterRegistry.gauge("application.startup.timestamp.application_ready", Tags.empty(), 
                applicationReadyTime, AtomicLong::get);
    }

    @EventListener
    public void handleApplicationStartingEvent(ApplicationStartingEvent event) {
        long timestamp = Instant.now().toEpochMilli();
        applicationStartingTime.set(timestamp);
        
        // Calculate time from JVM start to application starting
        jvmToApplicationStart.set(timestamp - jvmStartTime);
        
        recordStartupPhase("application_starting", timestamp);
    }

    @EventListener
    public void handleContextRefreshedEvent(ContextRefreshedEvent event) {
        long timestamp = Instant.now().toEpochMilli();
        contextRefreshedTime.set(timestamp);
        
        // Calculate time from application starting to context refresh
        long appStartTime = applicationStartingTime.get();
        if (appStartTime > 0) {
            applicationStartToContextRefresh.set(timestamp - appStartTime);
        }
        
        recordStartupPhase("context_refreshed", timestamp);
    }

    @EventListener
    public void handleApplicationStartedEvent(ApplicationStartedEvent event) {
        long timestamp = Instant.now().toEpochMilli();
        applicationStartedTime.set(timestamp);
        
        // Calculate time from context refresh to started
        long contextRefreshTime = contextRefreshedTime.get();
        if (contextRefreshTime > 0) {
            contextRefreshToStarted.set(timestamp - contextRefreshTime);
        }
        
        recordStartupPhase("application_started", timestamp);
    }

    @EventListener
    public void handleApplicationReadyEvent(ApplicationReadyEvent event) {
        long timestamp = Instant.now().toEpochMilli();
        applicationReadyTime.set(timestamp);
        
        // Calculate time from started to ready
        long startedTime = applicationStartedTime.get();
        if (startedTime > 0) {
            startedToReady.set(timestamp - startedTime);
        }
        
        // Calculate total startup time from JVM start to ready
        totalStartupTime.set(timestamp - jvmStartTime);
        
        recordStartupPhase("application_ready", timestamp);
        
        // Log startup summary
        logStartupSummary();
    }

    private void recordStartupPhase(String phase, long timestamp) {
        meterRegistry.counter("application.startup.phases.total",
                "phase", phase)
                .increment();
    }

    private void logStartupSummary() {
        StringBuilder summary = new StringBuilder("\n=== Application Startup Summary ===\n");
        summary.append("JVM Start Time: ").append(jvmStartTime).append("ms\n");
        summary.append("JVM to Application Start: ").append(jvmToApplicationStart.get()).append("ms\n");
        summary.append("Application Start to Context Refresh: ").append(applicationStartToContextRefresh.get()).append("ms\n");
        summary.append("Context Refresh to Started: ").append(contextRefreshToStarted.get()).append("ms\n");
        summary.append("Started to Ready: ").append(startedToReady.get()).append("ms\n");
        summary.append("Total Startup Time: ").append(totalStartupTime.get()).append("ms\n");
        summary.append("=====================================");
        
        System.out.println(summary.toString());
    }

    /**
     * Records custom startup milestone.
     */
    public void recordStartupMilestone(String milestone, String description) {
        long timestamp = Instant.now().toEpochMilli();
        
        meterRegistry.counter("application.startup.milestones.total",
                "milestone", milestone)
                .increment();
        
        meterRegistry.gauge("application.startup.milestone." + milestone, Tags.empty(), 
                timestamp);
        
        System.out.println("Startup Milestone [" + milestone + "]: " + description + " at " + timestamp + "ms");
    }

    /**
     * Records startup error or issue.
     */
    public void recordStartupIssue(String component, String issue, String severity) {
        meterRegistry.counter("application.startup.issues.total",
                "component", component,
                "issue", issue,
                "severity", severity)
                .increment();
    }

    /**
     * Records resource initialization time.
     */
    public void recordResourceInitialization(String resourceType, String resourceName, long durationMs) {
        meterRegistry.timer("application.startup.resource_initialization.duration",
                "resource_type", resourceType,
                "resource_name", resourceName)
                .record(java.time.Duration.ofMillis(durationMs));
    }

    /**
     * Records the number of beans created during startup.
     */
    public void recordBeansCreated(int count) {
        meterRegistry.gauge("application.startup.beans_created", Tags.empty(), count);
    }

    /**
     * Records configuration properties loaded during startup.
     */
    public void recordConfigurationPropertiesLoaded(int count) {
        meterRegistry.gauge("application.startup.configuration_properties_loaded", Tags.empty(), count);
    }

    /**
     * Records auto-configuration classes processed.
     */
    public void recordAutoConfigurationClassesProcessed(int count) {
        meterRegistry.gauge("application.startup.auto_configuration_classes_processed", Tags.empty(), count);
    }

    // Getters for current values (useful for health checks or info endpoints)
    public long getJvmStartTime() { return jvmStartTime; }
    public long getApplicationStartingTime() { return applicationStartingTime.get(); }
    public long getContextRefreshedTime() { return contextRefreshedTime.get(); }
    public long getApplicationStartedTime() { return applicationStartedTime.get(); }
    public long getApplicationReadyTime() { return applicationReadyTime.get(); }
    public long getTotalStartupTime() { return totalStartupTime.get(); }
    public long getJvmToApplicationStartDuration() { return jvmToApplicationStart.get(); }
    public long getApplicationStartToContextRefreshDuration() { return applicationStartToContextRefresh.get(); }
    public long getContextRefreshToStartedDuration() { return contextRefreshToStarted.get(); }
    public long getStartedToReadyDuration() { return startedToReady.get(); }

    /**
     * Check if the application has completed startup.
     */
    public boolean isStartupComplete() {
        return applicationReadyTime.get() > 0;
    }
}