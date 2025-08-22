package com.catalis.common.domain.actuator.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventsMetricsTest {

    private MeterRegistry meterRegistry;
    private DomainEventsMetrics metrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metrics = new DomainEventsMetrics(meterRegistry);
    }

    @Test
    void shouldRecordEventPublished() {
        // When
        metrics.recordEventPublished("kafka", "orders", "order.created");

        // Then
        Counter counter = meterRegistry.find("domain_events_published_total")
                .tag("adapter", "kafka")
                .tag("topic", "orders")
                .tag("type", "order.created")
                .tag("result", "success")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void shouldRecordEventPublishFailed() {
        // When
        metrics.recordEventPublishFailed("rabbit", "orders", "order.created", "ConnectionException");

        // Then
        Counter counter = meterRegistry.find("domain_events_published_total")
                .tag("adapter", "rabbit")
                .tag("topic", "orders")
                .tag("type", "order.created")
                .tag("result", "error")
                .tag("error_type", "ConnectionException")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void shouldRecordEventPublishDuration() {
        // When
        metrics.recordEventPublishDuration("sqs", "orders", "order.created", Duration.ofMillis(100));

        // Then
        Timer timer = meterRegistry.find("domain_events_publish_duration")
                .tag("adapter", "sqs")
                .tag("topic", "orders")
                .tag("type", "order.created")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }

    @Test
    void shouldRecordEventConsumed() {
        // When
        metrics.recordEventConsumed("kafka", "orders", "order.created");

        // Then
        Counter counter = meterRegistry.find("domain_events_consumed_total")
                .tag("adapter", "kafka")
                .tag("topic", "orders")
                .tag("type", "order.created")
                .tag("result", "success")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void shouldRecordEventConsumeFailed() {
        // When
        metrics.recordEventConsumeFailed("rabbit", "orders", "order.created", "DeserializationException");

        // Then
        Counter counter = meterRegistry.find("domain_events_consumed_total")
                .tag("adapter", "rabbit")
                .tag("topic", "orders")
                .tag("type", "order.created")
                .tag("result", "error")
                .tag("error_type", "DeserializationException")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void shouldRecordEventConsumeDuration() {
        // When
        metrics.recordEventConsumeDuration("sqs", "orders", "order.created", Duration.ofMillis(50));

        // Then
        Timer timer = meterRegistry.find("domain_events_consume_duration")
                .tag("adapter", "sqs")
                .tag("topic", "orders")
                .tag("type", "order.created")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }

    @Test
    void shouldRecordAdapterHealthCheck() {
        // When
        metrics.recordAdapterHealthCheck("kafka", true);
        metrics.recordAdapterHealthCheck("rabbit", false);

        // Then
        Counter healthyCounter = meterRegistry.find("domain_events_health_checks_total")
                .tag("adapter", "kafka")
                .tag("result", "healthy")
                .counter();

        Counter unhealthyCounter = meterRegistry.find("domain_events_health_checks_total")
                .tag("adapter", "rabbit")
                .tag("result", "unhealthy")
                .counter();

        assertThat(healthyCounter).isNotNull();
        assertThat(healthyCounter.count()).isEqualTo(1.0);
        assertThat(unhealthyCounter).isNotNull();
        assertThat(unhealthyCounter.count()).isEqualTo(1.0);
    }

    @Test
    void shouldIncrementExistingCounters() {
        // When
        metrics.recordEventPublished("kafka", "orders", "order.created");
        metrics.recordEventPublished("kafka", "orders", "order.created");
        metrics.recordEventPublished("kafka", "orders", "order.created");

        // Then
        Counter counter = meterRegistry.find("domain_events_published_total")
                .tag("adapter", "kafka")
                .tag("topic", "orders")
                .tag("type", "order.created")
                .tag("result", "success")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(3.0);
    }

    @Test
    void shouldCreateSeparateMetricsForDifferentTags() {
        // When
        metrics.recordEventPublished("kafka", "orders", "order.created");
        metrics.recordEventPublished("rabbit", "orders", "order.created");
        metrics.recordEventPublished("kafka", "payments", "payment.processed");

        // Then
        assertThat(meterRegistry.find("domain_events_published_total").counters()).hasSize(3);
        
        Counter kafkaOrdersCounter = meterRegistry.find("domain_events_published_total")
                .tag("adapter", "kafka")
                .tag("topic", "orders")
                .counter();
        
        Counter rabbitOrdersCounter = meterRegistry.find("domain_events_published_total")
                .tag("adapter", "rabbit")
                .tag("topic", "orders")
                .counter();
        
        Counter kafkaPaymentsCounter = meterRegistry.find("domain_events_published_total")
                .tag("adapter", "kafka")
                .tag("topic", "payments")
                .counter();

        assertThat(kafkaOrdersCounter.count()).isEqualTo(1.0);
        assertThat(rabbitOrdersCounter.count()).isEqualTo(1.0);
        assertThat(kafkaPaymentsCounter.count()).isEqualTo(1.0);
    }
}