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

import com.firefly.common.eda.publisher.EventPublisher;
import com.firefly.transactional.events.StepEventEnvelope;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprehensive test suite for StepEventPublisherBridge integration with lib-transactional-engine.
 * Tests cover step event publishing through EDA infrastructure, metadata handling,
 * and bridge pattern implementation for banking transaction workflows.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Banking Step Events Bridge - Transaction Workflow Step Publishing")
class StepEventPublisherBridgeTest {

    @Mock
    private EventPublisher edaPublisher;

    private StepEventPublisherBridge stepEventBridge;
    private final String defaultTopic = "domain-layer";

    @BeforeEach
    void setUp() {
        stepEventBridge = new StepEventPublisherBridge(defaultTopic, edaPublisher);
    }

    private StepEventEnvelope createStepEvent(String sagaName, String sagaId, String type, String key, Object payload) {
        return new StepEventEnvelope(
            sagaName,
            sagaId,
            "step-1", // stepId
            null, // topic
            type,
            key,
            payload,
            Map.of(), // headers
            1, // attempts
            250L, // latencyMs
            Instant.now().minusMillis(250), // startedAt
            Instant.now(), // completedAt
            "SUCCESS" // resultType
        );
    }

    @Test
    @DisplayName("Should publish money transfer step event with complete metadata")
    void shouldPublishMoneyTransferStepEvent() {
        when(edaPublisher.publish(any(), anyString(), any()))
            .thenReturn(Mono.empty());

        // Given: A money transfer saga step has completed
        MoneyTransferStepPayload payload = new MoneyTransferStepPayload(
            "TXN-12345",
            "ACC-001",
            "ACC-002",
            new BigDecimal("1000.00"),
            "USD",
            "COMPLETED"
        );

        StepEventEnvelope stepEvent = createStepEvent(
            "MoneyTransferSaga",
            "SAGA-67890",
            "transfer.step.completed",
            "TXN-12345",
            payload
        );

        // When: The step event is published through the bridge
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete();

        // Then: EDA publisher should be called with properly transformed event
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> headersCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        verify(edaPublisher).publish(eventCaptor.capture(), destinationCaptor.capture(), headersCaptor.capture());

        // Verify destination
        assertThat(destinationCaptor.getValue()).isEqualTo(defaultTopic);

        // Verify event payload
        assertThat(eventCaptor.getValue()).isEqualTo(stepEvent);

        // Verify headers contain step metadata
        Map<String, Object> headers = headersCaptor.getValue();
        assertThat(headers).containsEntry("step.saga_name", "MoneyTransferSaga");
        assertThat(headers).containsEntry("step.saga_id", "SAGA-67890");
        assertThat(headers).containsEntry("step.type", "transfer.step.completed");
        assertThat(headers).containsEntry("step.attempts", 1);
        assertThat(headers).containsEntry("step.latency_ms", 250L);
        assertThat(headers).containsEntry("step.result_type", "SUCCESS");
        assertThat(headers).containsEntry("routing_key", "TXN-12345");
        assertThat(headers).containsEntry("event_type", "transfer.step.completed");
    }

    @Test
    @DisplayName("Should auto-generate key from saga name and ID when key is missing")
    void shouldAutoGenerateKeyFromSagaNameAndId() {
        when(edaPublisher.publish(any(), anyString(), any()))
            .thenReturn(Mono.empty());

        // Given: A step event without a key
        StepEventEnvelope stepEvent = createStepEvent(
            "AccountOpeningSaga",
            "SAGA-12345",
            "account.validation.completed",
            null, // No key provided
            "Account validation successful"
        );

        // When: The step event is published
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete();

        // Then: Key should be auto-generated from saga name and ID
        ArgumentCaptor<Map> headersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(edaPublisher).publish(any(), anyString(), headersCaptor.capture());

        Map<String, Object> headers = headersCaptor.getValue();
        assertThat(headers).containsEntry("routing_key", "AccountOpeningSaga:SAGA-12345");
    }

    @Test
    @DisplayName("Should use default topic when topic is missing")
    void shouldUseDefaultTopicWhenTopicMissing() {
        when(edaPublisher.publish(any(), anyString(), any()))
            .thenReturn(Mono.empty());

        // Given: A step event without a topic
        StepEventEnvelope stepEvent = createStepEvent(
            "LoanApprovalSaga",
            "SAGA-54321",
            "loan.credit.check.completed",
            "LOAN-98765",
            "Credit check passed"
        );

        // When: The step event is published
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete();

        // Then: Default topic should be used
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        verify(edaPublisher).publish(any(), destinationCaptor.capture(), any());

        assertThat(destinationCaptor.getValue()).isEqualTo(defaultTopic);
    }

    @Test
    @DisplayName("Should handle step event with retry attempts and failure metadata")
    void shouldHandleStepEventWithRetryAttemptsAndFailureMetadata() {
        when(edaPublisher.publish(any(), anyString(), any()))
            .thenReturn(Mono.empty());

        // Given: A step event that failed and was retried
        FraudCheckStepPayload payload = new FraudCheckStepPayload(
            "TXN-99999",
            "FRAUD_CHECK",
            "FAILED",
            "Suspicious transaction pattern detected",
            85.5
        );

        StepEventEnvelope stepEvent = new StepEventEnvelope(
            "FraudDetectionSaga",
            "SAGA-FRAUD-001",
            "step-fraud-check", // stepId
            "banking-fraud-events", // topic
            "fraud.check.failed", // type
            "TXN-99999", // key
            payload,
            Map.of(
                "source", "fraud-detection-service",
                "priority", "high",
                "alert-level", "critical"
            ),
            3, // Multiple attempts
            1200L, // Longer latency due to retries
            Instant.now().minusMillis(1200),
            Instant.now(),
            "FAILURE"
        );

        // When: The failed step event is published
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete();

        // Then: All failure and retry metadata should be preserved
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> headersCaptor = ArgumentCaptor.forClass(Map.class);

        verify(edaPublisher).publish(any(), destinationCaptor.capture(), headersCaptor.capture());

        assertThat(destinationCaptor.getValue()).isEqualTo("banking-fraud-events");

        Map<String, Object> headers = headersCaptor.getValue();
        assertThat(headers).containsEntry("step.type", "fraud.check.failed");
        assertThat(headers).containsEntry("routing_key", "TXN-99999");

        // Verify retry and failure metadata
        assertThat(headers).containsEntry("step.attempts", 3);
        assertThat(headers).containsEntry("step.latency_ms", 1200L);
        assertThat(headers).containsEntry("step.result_type", "FAILURE");

        // Verify original headers are preserved
        assertThat(headers).containsEntry("priority", "high");
        assertThat(headers).containsEntry("alert-level", "critical");
    }

    @Test
    @DisplayName("Should handle step event with empty headers gracefully")
    void shouldHandleStepEventWithEmptyHeadersGracefully() {
        when(edaPublisher.publish(any(), anyString(), any()))
            .thenReturn(Mono.empty());

        // Given: A step event with null headers
        StepEventEnvelope stepEvent = new StepEventEnvelope(
            "SimpleTransferSaga",
            "SAGA-SIMPLE-001",
            "step-transfer", // stepId
            null, // topic
            "transfer.initiated", // type
            "TXN-SIMPLE-001", // key
            "Transfer initiated",
            null, // Null headers
            1,
            100L,
            Instant.now().minusMillis(100),
            Instant.now(),
            "SUCCESS"
        );

        // When: The step event is published
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete();

        // Then: Should handle null headers gracefully
        ArgumentCaptor<Map> headersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(edaPublisher).publish(any(), anyString(), headersCaptor.capture());

        Map<String, Object> headers = headersCaptor.getValue();
        assertThat(headers).isNotNull();
        assertThat(headers).containsEntry("step.attempts", 1);
        assertThat(headers).containsEntry("step.saga_name", "SimpleTransferSaga");
    }

    @Test
    @DisplayName("Should propagate EDA publisher errors")
    void shouldPropagateEdaPublisherErrors() {
        // Given: EDA publisher that fails
        RuntimeException publisherError = new RuntimeException("Message broker unavailable");
        when(edaPublisher.publish(any(), anyString(), any()))
            .thenReturn(Mono.error(publisherError));

        StepEventEnvelope stepEvent = createStepEvent(
            "TestSaga",
            "SAGA-ERROR-001",
            "test.step",
            "TEST-001",
            "test payload"
        );

        // When: The step event is published and publisher fails
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .expectError(RuntimeException.class)
            .verify();

        // Then: Error should be propagated
        verify(edaPublisher).publish(any(), anyString(), any());
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
