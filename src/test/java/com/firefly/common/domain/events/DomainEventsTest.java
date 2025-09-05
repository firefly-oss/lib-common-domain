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

package com.firefly.common.domain.events;

import com.firefly.common.domain.events.outbound.ApplicationEventDomainEventPublisher;
import com.firefly.common.domain.events.outbound.DomainEventPublisher;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for Domain Events functionality using banking domain examples.
 * Tests cover event publishing, envelope structure, and Spring ApplicationEvent integration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Banking Domain Events - Customer and Transaction Event Publishing")
class DomainEventsTest {

    @Mock
    private ApplicationEventPublisher springEventPublisher;

    private DomainEventPublisher domainEventPublisher;

    @BeforeEach
    void setUp() {
        domainEventPublisher = new ApplicationEventDomainEventPublisher(springEventPublisher);
    }

    @Test
    @DisplayName("Should publish customer account opened event successfully")
    void shouldPublishCustomerAccountOpenedEvent() {
        // Given: A customer has successfully opened a new account
        CustomerAccountOpenedEvent payload = new CustomerAccountOpenedEvent(
            "CUST-12345",
            "ACC-67890",
            "SAVINGS",
            new BigDecimal("1500.00"),
            "USD",
            Instant.now()
        );

        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("banking.accounts")
            .type("account.opened")
            .key("ACC-67890")
            .payload(payload)
            .timestamp(Instant.now())
            .headers(Map.of(
                "source", "account-service",
                "version", "1.0",
                "region", "us-east-1"
            ))
            .metadata(Map.of(
                "correlationId", "CORR-123",
                "userId", "user-456",
                "branchCode", "BR-001"
            ))
            .build();

        // When: The event is published
        StepVerifier.create(domainEventPublisher.publish(event))
            .verifyComplete();

        // Then: Spring ApplicationEventPublisher should be called with DomainSpringEvent
        ArgumentCaptor<DomainSpringEvent> eventCaptor = ArgumentCaptor.forClass(DomainSpringEvent.class);
        verify(springEventPublisher).publishEvent(eventCaptor.capture());

        DomainSpringEvent publishedEvent = eventCaptor.getValue();
        DomainEventEnvelope envelope = publishedEvent.getEnvelope();

        assertThat(envelope.getTopic()).isEqualTo("banking.accounts");
        assertThat(envelope.getType()).isEqualTo("account.opened");
        assertThat(envelope.getKey()).isEqualTo("ACC-67890");
        assertThat(envelope.getPayload()).isInstanceOf(CustomerAccountOpenedEvent.class);
        
        CustomerAccountOpenedEvent eventPayload = (CustomerAccountOpenedEvent) envelope.getPayload();
        assertThat(eventPayload.getCustomerId()).isEqualTo("CUST-12345");
        assertThat(eventPayload.getAccountNumber()).isEqualTo("ACC-67890");
        assertThat(eventPayload.getAccountType()).isEqualTo("SAVINGS");
        assertThat(eventPayload.getInitialBalance()).isEqualTo(new BigDecimal("1500.00"));

        // Verify headers and metadata
        assertThat(envelope.getHeaders()).containsEntry("source", "account-service");
        assertThat(envelope.getHeaders()).containsEntry("version", "1.0");
        assertThat(envelope.getMetadata()).containsEntry("correlationId", "CORR-123");
        assertThat(envelope.getMetadata()).containsEntry("branchCode", "BR-001");
    }

    @Test
    @DisplayName("Should publish money transfer completed event with transaction details")
    void shouldPublishMoneyTransferCompletedEvent() {
        // Given: A money transfer has been completed between accounts
        MoneyTransferCompletedEvent payload = new MoneyTransferCompletedEvent(
            "TXN-98765",
            "ACC-001",
            "ACC-002",
            new BigDecimal("750.00"),
            "USD",
            "Monthly savings transfer",
            Instant.now()
        );

        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("banking.transactions")
            .type("transfer.completed")
            .key("TXN-98765")
            .payload(payload)
            .timestamp(Instant.now())
            .headers(Map.of(
                "source", "transaction-service",
                "priority", "high",
                "channel", "mobile-app"
            ))
            .metadata(Map.of(
                "correlationId", "CORR-789",
                "sessionId", "SESSION-456",
                "deviceId", "DEVICE-123"
            ))
            .build();

        // When: The transfer event is published
        StepVerifier.create(domainEventPublisher.publish(event))
            .verifyComplete();

        // Then: Event should be properly published with all details
        ArgumentCaptor<DomainSpringEvent> eventCaptor = ArgumentCaptor.forClass(DomainSpringEvent.class);
        verify(springEventPublisher).publishEvent(eventCaptor.capture());

        DomainEventEnvelope envelope = eventCaptor.getValue().getEnvelope();
        MoneyTransferCompletedEvent eventPayload = (MoneyTransferCompletedEvent) envelope.getPayload();

        assertThat(envelope.getTopic()).isEqualTo("banking.transactions");
        assertThat(envelope.getType()).isEqualTo("transfer.completed");
        assertThat(eventPayload.getTransactionId()).isEqualTo("TXN-98765");
        assertThat(eventPayload.getFromAccount()).isEqualTo("ACC-001");
        assertThat(eventPayload.getToAccount()).isEqualTo("ACC-002");
        assertThat(eventPayload.getAmount()).isEqualTo(new BigDecimal("750.00"));
        assertThat(eventPayload.getDescription()).isEqualTo("Monthly savings transfer");
    }

    @Test
    @DisplayName("Should publish customer KYC verification event with compliance data")
    void shouldPublishCustomerKycVerificationEvent() {
        // Given: A customer's KYC verification has been completed
        CustomerKycVerifiedEvent payload = new CustomerKycVerifiedEvent(
            "CUST-54321",
            "KYC-11111",
            "APPROVED",
            "TIER_2",
            Map.of(
                "documentType", "PASSPORT",
                "documentNumber", "P123456789",
                "verificationScore", "95"
            ),
            Instant.now()
        );

        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("banking.compliance")
            .type("kyc.verified")
            .key("CUST-54321")
            .payload(payload)
            .timestamp(Instant.now())
            .headers(Map.of(
                "source", "kyc-service",
                "compliance-level", "tier-2",
                "jurisdiction", "US"
            ))
            .metadata(Map.of(
                "correlationId", "CORR-KYC-001",
                "auditTrail", "AUDIT-789",
                "regulatoryReporting", "true"
            ))
            .build();

        // When: The KYC event is published
        StepVerifier.create(domainEventPublisher.publish(event))
            .verifyComplete();

        // Then: Event should contain all compliance-related information
        ArgumentCaptor<DomainSpringEvent> eventCaptor = ArgumentCaptor.forClass(DomainSpringEvent.class);
        verify(springEventPublisher).publishEvent(eventCaptor.capture());

        DomainEventEnvelope envelope = eventCaptor.getValue().getEnvelope();
        CustomerKycVerifiedEvent eventPayload = (CustomerKycVerifiedEvent) envelope.getPayload();

        assertThat(envelope.getTopic()).isEqualTo("banking.compliance");
        assertThat(envelope.getType()).isEqualTo("kyc.verified");
        assertThat(eventPayload.getCustomerId()).isEqualTo("CUST-54321");
        assertThat(eventPayload.getKycId()).isEqualTo("KYC-11111");
        assertThat(eventPayload.getStatus()).isEqualTo("APPROVED");
        assertThat(eventPayload.getTier()).isEqualTo("TIER_2");
        assertThat(eventPayload.getVerificationData()).containsEntry("documentType", "PASSPORT");
        assertThat(eventPayload.getVerificationData()).containsEntry("verificationScore", "95");
    }

    @Test
    @DisplayName("Should handle event envelope with minimal required fields")
    void shouldHandleEventEnvelopeWithMinimalFields() {
        // Given: An event with only required fields
        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("banking.notifications")
            .type("notification.sent")
            .key("NOTIF-001")
            .payload("SMS notification sent to customer")
            .timestamp(Instant.now())
            .build();

        // When: The minimal event is published
        StepVerifier.create(domainEventPublisher.publish(event))
            .verifyComplete();

        // Then: Event should be published successfully
        verify(springEventPublisher).publishEvent(any(DomainSpringEvent.class));
    }

    @Test
    @DisplayName("Should preserve event envelope immutability")
    void shouldPreserveEventEnvelopeImmutability() {
        // Given: An event envelope with headers and metadata
        Map<String, Object> originalHeaders = Map.of("source", "test-service");
        Map<String, Object> originalMetadata = Map.of("correlationId", "CORR-TEST");

        DomainEventEnvelope event = DomainEventEnvelope.builder()
            .topic("test.topic")
            .type("test.event")
            .key("TEST-001")
            .payload("test payload")
            .timestamp(Instant.now())
            .headers(originalHeaders)
            .metadata(originalMetadata)
            .build();

        // When: Attempting to access headers and metadata
        Map<String, Object> retrievedHeaders = event.getHeaders();
        Map<String, Object> retrievedMetadata = event.getMetadata();

        // Then: Collections should be immutable (unmodifiable)
        assertThat(retrievedHeaders).isEqualTo(originalHeaders);
        assertThat(retrievedMetadata).isEqualTo(originalMetadata);
        
        // Verify immutability by attempting modification (should not affect original)
        assertThat(retrievedHeaders).isUnmodifiable();
        assertThat(retrievedMetadata).isUnmodifiable();
    }

    // Test Event Payloads
    @Data
    static class CustomerAccountOpenedEvent {
        private final String customerId;
        private final String accountNumber;
        private final String accountType;
        private final BigDecimal initialBalance;
        private final String currency;
        private final Instant openedAt;
    }

    @Data
    static class MoneyTransferCompletedEvent {
        private final String transactionId;
        private final String fromAccount;
        private final String toAccount;
        private final BigDecimal amount;
        private final String currency;
        private final String description;
        private final Instant completedAt;
    }

    @Data
    static class CustomerKycVerifiedEvent {
        private final String customerId;
        private final String kycId;
        private final String status;
        private final String tier;
        private final Map<String, Object> verificationData;
        private final Instant verifiedAt;
    }
}
