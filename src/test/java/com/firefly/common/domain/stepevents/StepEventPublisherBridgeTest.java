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

package com.firefly.common.domain.stepevents;

import com.firefly.common.domain.events.DomainEventEnvelope;
import com.firefly.common.domain.events.outbound.DomainEventPublisher;
import com.firefly.transactionalengine.events.StepEventEnvelope;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for StepEventPublisherBridge integration with lib-transactional-engine.
 * Tests cover step event publishing through domain events infrastructure, metadata handling,
 * and bridge pattern implementation for banking transaction workflows.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Banking Step Events Bridge - Transaction Workflow Step Publishing")
class StepEventPublisherBridgeTest {

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private StepEventPublisherBridge stepEventBridge;
    private final String defaultTopic = "banking-step-events";

    @BeforeEach
    void setUp() {
        when(domainEventPublisher.publish(any(DomainEventEnvelope.class)))
            .thenReturn(Mono.empty());
        
        stepEventBridge = new StepEventPublisherBridge(defaultTopic, domainEventPublisher);
    }

    @Test
    @DisplayName("Should publish money transfer step event with complete metadata")
    void shouldPublishMoneyTransferStepEvent() {
        // Given: A money transfer saga step has completed
        MoneyTransferStepPayload payload = new MoneyTransferStepPayload(
            "TXN-12345",
            "ACC-001",
            "ACC-002",
            new BigDecimal("1000.00"),
            "USD",
            "COMPLETED"
        );

        StepEventEnvelope stepEvent = new StepEventEnvelope();
        stepEvent.setSagaName("MoneyTransferSaga");
        stepEvent.setSagaId("SAGA-67890");
        stepEvent.setType("transfer.step.completed");
        stepEvent.setKey("TXN-12345");
        stepEvent.setPayload(payload);
        stepEvent.setTimestamp(Instant.now());
        stepEvent.setAttempts(1);
        stepEvent.setLatencyMs(250L);
        stepEvent.setStartedAt(Instant.now().minusMillis(250));
        stepEvent.setCompletedAt(Instant.now());
        stepEvent.setResultType("SUCCESS");
        stepEvent.setHeaders(Map.of(
            "source", "transfer-service",
            "version", "1.0"
        ));

        // When: The step event is published through the bridge
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete();

        // Then: Domain event publisher should be called with properly transformed event
        ArgumentCaptor<DomainEventEnvelope> eventCaptor = ArgumentCaptor.forClass(DomainEventEnvelope.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());

        DomainEventEnvelope domainEvent = eventCaptor.getValue();

        // Verify basic event properties
        assertThat(domainEvent.getTopic()).isEqualTo(defaultTopic);
        assertThat(domainEvent.getType()).isEqualTo("transfer.step.completed");
        assertThat(domainEvent.getKey()).isEqualTo("TXN-12345");
        assertThat(domainEvent.getPayload()).isEqualTo(stepEvent);
        assertThat(domainEvent.getTimestamp()).isEqualTo(stepEvent.getTimestamp());

        // Verify headers are preserved
        assertThat(domainEvent.getHeaders()).containsEntry("source", "transfer-service");
        assertThat(domainEvent.getHeaders()).containsEntry("version", "1.0");

        // Verify step-specific metadata is added
        assertThat(domainEvent.getMetadata()).containsEntry("step.attempts", 1);
        assertThat(domainEvent.getMetadata()).containsEntry("step.latency_ms", 250L);
        assertThat(domainEvent.getMetadata()).containsEntry("step.started_at", stepEvent.getStartedAt());
        assertThat(domainEvent.getMetadata()).containsEntry("step.completed_at", stepEvent.getCompletedAt());
        assertThat(domainEvent.getMetadata()).containsEntry("step.result_type", "SUCCESS");
    }

    @Test
    @DisplayName("Should auto-generate key from saga name and ID when key is missing")
    void shouldAutoGenerateKeyFromSagaNameAndId() {
        // Given: A step event without a key
        StepEventEnvelope stepEvent = new StepEventEnvelope();
        stepEvent.setSagaName("AccountOpeningSaga");
        stepEvent.setSagaId("SAGA-12345");
        stepEvent.setType("account.validation.completed");
        stepEvent.setKey(null); // No key provided
        stepEvent.setPayload("Account validation successful");
        stepEvent.setTimestamp(Instant.now());
        stepEvent.setAttempts(1);
        stepEvent.setLatencyMs(150L);
        stepEvent.setStartedAt(Instant.now().minusMillis(150));
        stepEvent.setCompletedAt(Instant.now());
        stepEvent.setResultType("SUCCESS");

        // When: The step event is published
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete();

        // Then: Key should be auto-generated from saga name and ID
        ArgumentCaptor<DomainEventEnvelope> eventCaptor = ArgumentCaptor.forClass(DomainEventEnvelope.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());

        DomainEventEnvelope domainEvent = eventCaptor.getValue();
        assertThat(domainEvent.getKey()).isEqualTo("AccountOpeningSaga:SAGA-12345");
    }

    @Test
    @DisplayName("Should use default topic when topic is missing")
    void shouldUseDefaultTopicWhenTopicMissing() {
        // Given: A step event without a topic
        StepEventEnvelope stepEvent = new StepEventEnvelope();
        stepEvent.setSagaName("LoanApprovalSaga");
        stepEvent.setSagaId("SAGA-54321");
        stepEvent.setType("loan.credit.check.completed");
        stepEvent.setKey("LOAN-98765");
        stepEvent.setTopic(null); // No topic provided
        stepEvent.setPayload("Credit check passed");
        stepEvent.setTimestamp(Instant.now());
        stepEvent.setAttempts(2);
        stepEvent.setLatencyMs(500L);
        stepEvent.setStartedAt(Instant.now().minusMillis(500));
        stepEvent.setCompletedAt(Instant.now());
        stepEvent.setResultType("SUCCESS");

        // When: The step event is published
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete();

        // Then: Default topic should be used
        ArgumentCaptor<DomainEventEnvelope> eventCaptor = ArgumentCaptor.forClass(DomainEventEnvelope.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());

        DomainEventEnvelope domainEvent = eventCaptor.getValue();
        assertThat(domainEvent.getTopic()).isEqualTo(defaultTopic);
    }

    @Test
    @DisplayName("Should handle step event with retry attempts and failure metadata")
    void shouldHandleStepEventWithRetryAttemptsAndFailureMetadata() {
        // Given: A step event that failed and was retried
        FraudCheckStepPayload payload = new FraudCheckStepPayload(
            "TXN-99999",
            "FRAUD_CHECK",
            "FAILED",
            "Suspicious transaction pattern detected",
            85.5
        );

        StepEventEnvelope stepEvent = new StepEventEnvelope();
        stepEvent.setSagaName("FraudDetectionSaga");
        stepEvent.setSagaId("SAGA-FRAUD-001");
        stepEvent.setType("fraud.check.failed");
        stepEvent.setKey("TXN-99999");
        stepEvent.setTopic("banking-fraud-events");
        stepEvent.setPayload(payload);
        stepEvent.setTimestamp(Instant.now());
        stepEvent.setAttempts(3); // Multiple attempts
        stepEvent.setLatencyMs(1200L); // Longer latency due to retries
        stepEvent.setStartedAt(Instant.now().minusMillis(1200));
        stepEvent.setCompletedAt(Instant.now());
        stepEvent.setResultType("FAILURE");
        stepEvent.setHeaders(Map.of(
            "source", "fraud-detection-service",
            "priority", "high",
            "alert-level", "critical"
        ));

        // When: The failed step event is published
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete();

        // Then: All failure and retry metadata should be preserved
        ArgumentCaptor<DomainEventEnvelope> eventCaptor = ArgumentCaptor.forClass(DomainEventEnvelope.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());

        DomainEventEnvelope domainEvent = eventCaptor.getValue();

        assertThat(domainEvent.getTopic()).isEqualTo("banking-fraud-events");
        assertThat(domainEvent.getType()).isEqualTo("fraud.check.failed");
        assertThat(domainEvent.getKey()).isEqualTo("TXN-99999");

        // Verify retry and failure metadata
        assertThat(domainEvent.getMetadata()).containsEntry("step.attempts", 3);
        assertThat(domainEvent.getMetadata()).containsEntry("step.latency_ms", 1200L);
        assertThat(domainEvent.getMetadata()).containsEntry("step.result_type", "FAILURE");

        // Verify headers are preserved
        assertThat(domainEvent.getHeaders()).containsEntry("priority", "high");
        assertThat(domainEvent.getHeaders()).containsEntry("alert-level", "critical");
    }

    @Test
    @DisplayName("Should handle step event with empty headers gracefully")
    void shouldHandleStepEventWithEmptyHeadersGracefully() {
        // Given: A step event with null headers
        StepEventEnvelope stepEvent = new StepEventEnvelope();
        stepEvent.setSagaName("SimpleTransferSaga");
        stepEvent.setSagaId("SAGA-SIMPLE-001");
        stepEvent.setType("transfer.initiated");
        stepEvent.setKey("TXN-SIMPLE-001");
        stepEvent.setPayload("Transfer initiated");
        stepEvent.setTimestamp(Instant.now());
        stepEvent.setAttempts(1);
        stepEvent.setLatencyMs(100L);
        stepEvent.setStartedAt(Instant.now().minusMillis(100));
        stepEvent.setCompletedAt(Instant.now());
        stepEvent.setResultType("SUCCESS");
        stepEvent.setHeaders(null); // Null headers

        // When: The step event is published
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete();

        // Then: Should handle null headers gracefully
        ArgumentCaptor<DomainEventEnvelope> eventCaptor = ArgumentCaptor.forClass(DomainEventEnvelope.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());

        DomainEventEnvelope domainEvent = eventCaptor.getValue();
        assertThat(domainEvent.getHeaders()).isNull();
        assertThat(domainEvent.getMetadata()).isNotNull();
        assertThat(domainEvent.getMetadata()).containsEntry("step.attempts", 1);
    }

    @Test
    @DisplayName("Should propagate domain event publisher errors")
    void shouldPropagateDomainEventPublisherErrors() {
        // Given: Domain event publisher that fails
        RuntimeException publisherError = new RuntimeException("Message broker unavailable");
        when(domainEventPublisher.publish(any(DomainEventEnvelope.class)))
            .thenReturn(Mono.error(publisherError));

        StepEventEnvelope stepEvent = new StepEventEnvelope();
        stepEvent.setSagaName("TestSaga");
        stepEvent.setSagaId("SAGA-ERROR-001");
        stepEvent.setType("test.step");
        stepEvent.setKey("TEST-001");
        stepEvent.setPayload("test payload");
        stepEvent.setTimestamp(Instant.now());
        stepEvent.setAttempts(1);
        stepEvent.setLatencyMs(50L);
        stepEvent.setStartedAt(Instant.now().minusMillis(50));
        stepEvent.setCompletedAt(Instant.now());
        stepEvent.setResultType("SUCCESS");

        // When: The step event is published and publisher fails
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .expectError(RuntimeException.class)
            .verify();

        // Then: Error should be propagated
        verify(domainEventPublisher).publish(any(DomainEventEnvelope.class));
    }

    // Test Payloads for Banking Domain Step Events
    @Data
    static class MoneyTransferStepPayload {
        private final String transactionId;
        private final String fromAccount;
        private final String toAccount;
        private final BigDecimal amount;
        private final String currency;
        private final String status;
    }

    @Data
    static class FraudCheckStepPayload {
        private final String transactionId;
        private final String checkType;
        private final String result;
        private final String reason;
        private final Double riskScore;
    }
}
