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

package com.firefly.common.domain.integration;

import com.firefly.common.domain.cqrs.command.Command;
import com.firefly.common.domain.cqrs.command.CommandBus;
import com.firefly.common.domain.cqrs.command.CommandHandler;
import com.firefly.common.domain.cqrs.query.Query;
import com.firefly.common.domain.cqrs.query.QueryBus;
import com.firefly.common.domain.cqrs.query.QueryHandler;
import com.firefly.common.domain.events.DomainEventEnvelope;
import com.firefly.common.domain.events.DomainSpringEvent;
import com.firefly.common.domain.events.outbound.DomainEventPublisher;
import com.firefly.common.domain.stepevents.StepEventPublisherBridge;
import com.firefly.common.domain.validation.ValidationResult;
import com.firefly.transactionalengine.events.StepEventEnvelope;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test for complete banking workflows using all components
 * of the Firefly Common Domain library. Tests cross-component interactions,
 * event-driven workflows, and complete customer journey scenarios.
 */
@SpringBootTest(classes = BankingWorkflowIntegrationTest.TestConfiguration.class)
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("Banking Workflow Integration - Complete Customer Journey")
class BankingWorkflowIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private QueryBus queryBus;

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private StepEventPublisherBridge stepEventBridge;

    @Autowired
    private TestEventListener eventListener;

    @Test
    @DisplayName("Should complete end-to-end customer account opening workflow")
    void shouldCompleteEndToEndCustomerAccountOpeningWorkflow() throws InterruptedException {
        // Given: A new customer wants to open a savings account
        OpenAccountCommand command = new OpenAccountCommand(
            "CUST-12345",
            "John Doe",
            "john.doe@example.com",
            new BigDecimal("5000.00"),
            "SAVINGS",
            "CORR-INTEGRATION-001"
        );

        // When: The account opening command is processed
        StepVerifier.create(commandBus.send(command))
            .assertNext(result -> {
                assertThat(result.getAccountNumber()).isNotNull();
                assertThat(result.getCustomerId()).isEqualTo("CUST-12345");
                assertThat(result.getAccountType()).isEqualTo("SAVINGS");
                assertThat(result.getInitialBalance()).isEqualTo(new BigDecimal("5000.00"));
                assertThat(result.getStatus()).isEqualTo("ACTIVE");
            })
            .verifyComplete();

        // Then: Domain event should be published and processed
        boolean eventReceived = eventListener.waitForEvent("account.opened", 5, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();

        DomainEventEnvelope receivedEvent = eventListener.getLastEvent();
        assertThat(receivedEvent.getType()).isEqualTo("account.opened");
        assertThat(receivedEvent.getTopic()).isEqualTo("banking.accounts");

        AccountOpenedEvent eventPayload = (AccountOpenedEvent) receivedEvent.getPayload();
        assertThat(eventPayload.getCustomerId()).isEqualTo("CUST-12345");
        assertThat(eventPayload.getAccountType()).isEqualTo("SAVINGS");
    }

    @Test
    @DisplayName("Should process money transfer with step events and domain events")
    void shouldProcessMoneyTransferWithStepEventsAndDomainEvents() throws InterruptedException {
        // Given: A money transfer between accounts
        TransferMoneyCommand command = new TransferMoneyCommand(
            "ACC-001",
            "ACC-002",
            new BigDecimal("1000.00"),
            "Monthly savings transfer",
            "CORR-TRANSFER-001"
        );

        // When: The transfer command is processed
        StepVerifier.create(commandBus.send(command))
            .assertNext(result -> {
                assertThat(result.getTransactionId()).isNotNull();
                assertThat(result.getFromAccount()).isEqualTo("ACC-001");
                assertThat(result.getToAccount()).isEqualTo("ACC-002");
                assertThat(result.getAmount()).isEqualTo(new BigDecimal("1000.00"));
                assertThat(result.getStatus()).isEqualTo("COMPLETED");
            })
            .verifyComplete();

        // Then: Both step events and domain events should be published
        boolean stepEventReceived = eventListener.waitForStepEvent("transfer.step.completed", 5, TimeUnit.SECONDS);
        assertThat(stepEventReceived).isTrue();

        boolean domainEventReceived = eventListener.waitForEvent("transfer.completed", 5, TimeUnit.SECONDS);
        assertThat(domainEventReceived).isTrue();
    }

    @Test
    @DisplayName("Should query account balance with caching")
    void shouldQueryAccountBalanceWithCaching() {
        // Given: An account balance query
        GetAccountBalanceQuery query = new GetAccountBalanceQuery("ACC-12345", "CORR-QUERY-001");

        // When: The query is executed twice
        Mono<AccountBalance> firstResult = queryBus.query(query);
        Mono<AccountBalance> secondResult = queryBus.query(query);

        // Then: Both queries should return the same cached result
        StepVerifier.create(firstResult)
            .assertNext(balance -> {
                assertThat(balance.getAccountNumber()).isEqualTo("ACC-12345");
                assertThat(balance.getAvailableBalance()).isEqualTo(new BigDecimal("2500.00"));
                assertThat(balance.getCurrency()).isEqualTo("USD");
            })
            .verifyComplete();

        StepVerifier.create(secondResult)
            .assertNext(balance -> {
                assertThat(balance.getAccountNumber()).isEqualTo("ACC-12345");
                assertThat(balance.getAvailableBalance()).isEqualTo(new BigDecimal("2500.00"));
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should publish and consume step events through bridge")
    void shouldPublishAndConsumeStepEventsThroughBridge() throws InterruptedException {
        // Given: A step event from a transaction saga
        StepEventEnvelope stepEvent = new StepEventEnvelope();
        stepEvent.setSagaName("PaymentProcessingSaga");
        stepEvent.setSagaId("SAGA-PAY-001");
        stepEvent.setType("payment.validation.completed");
        stepEvent.setKey("TXN-98765");
        stepEvent.setPayload("Payment validation successful");
        stepEvent.setTimestamp(Instant.now());
        stepEvent.setAttempts(1);
        stepEvent.setLatencyMs(150L);
        stepEvent.setStartedAt(Instant.now().minusMillis(150));
        stepEvent.setCompletedAt(Instant.now());
        stepEvent.setResultType("SUCCESS");

        // When: The step event is published through the bridge
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete();

        // Then: Step event should be received as domain event
        boolean eventReceived = eventListener.waitForStepEvent("payment.validation.completed", 5, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();

        DomainEventEnvelope receivedEvent = eventListener.getLastStepEvent();
        assertThat(receivedEvent.getType()).isEqualTo("payment.validation.completed");
        assertThat(receivedEvent.getKey()).isEqualTo("TXN-98765");
        assertThat(receivedEvent.getMetadata()).containsEntry("step.attempts", 1);
        assertThat(receivedEvent.getMetadata()).containsEntry("step.result_type", "SUCCESS");
    }

    // Test Commands, Queries, and Results
    @Data
    static class OpenAccountCommand implements Command<AccountOpenedResult> {
        private final String customerId;
        private final String customerName;
        private final String email;
        private final BigDecimal initialDeposit;
        private final String accountType;
        private final String correlationId;

        @Override
        public Mono<ValidationResult> validate() {
            ValidationResult.Builder builder = ValidationResult.builder();
            if (customerId == null || customerId.trim().isEmpty()) {
                builder.addError("customerId", "Customer ID is required");
            }
            if (initialDeposit == null || initialDeposit.compareTo(BigDecimal.ZERO) <= 0) {
                builder.addError("initialDeposit", "Initial deposit must be greater than zero");
            }
            return Mono.just(builder.build());
        }
    }

    @Data
    static class TransferMoneyCommand implements Command<TransferResult> {
        private final String fromAccount;
        private final String toAccount;
        private final BigDecimal amount;
        private final String description;
        private final String correlationId;

        @Override
        public Mono<ValidationResult> validate() {
            ValidationResult.Builder builder = ValidationResult.builder();
            if (fromAccount == null || fromAccount.equals(toAccount)) {
                builder.addError("accounts", "Invalid account configuration");
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                builder.addError("amount", "Amount must be greater than zero");
            }
            return Mono.just(builder.build());
        }
    }

    @Data
    static class GetAccountBalanceQuery implements Query<AccountBalance> {
        private final String accountNumber;
        private final String correlationId;

        @Override
        public String getCacheKey() {
            return "account-balance:" + accountNumber;
        }
    }

    @Data
    static class AccountOpenedResult {
        private final String accountNumber;
        private final String customerId;
        private final String accountType;
        private final BigDecimal initialBalance;
        private final String status;
    }

    @Data
    static class TransferResult {
        private final String transactionId;
        private final String fromAccount;
        private final String toAccount;
        private final BigDecimal amount;
        private final String status;
    }

    @Data
    static class AccountBalance {
        private final String accountNumber;
        private final BigDecimal availableBalance;
        private final String currency;
    }

    @Data
    static class AccountOpenedEvent {
        private final String customerId;
        private final String accountNumber;
        private final String accountType;
        private final BigDecimal initialBalance;
        private final Instant openedAt;
    }

    // Test Handlers
    @Component
    static class OpenAccountHandler implements CommandHandler<OpenAccountCommand, AccountOpenedResult> {
        
        @Autowired
        private DomainEventPublisher eventPublisher;

        @Override
        public Mono<AccountOpenedResult> handle(OpenAccountCommand command) {
            return command.validate()
                .flatMap(validation -> {
                    if (!validation.isValid()) {
                        return Mono.error(new RuntimeException("Validation failed"));
                    }
                    
                    String accountNumber = "ACC-" + System.currentTimeMillis();
                    AccountOpenedResult result = new AccountOpenedResult(
                        accountNumber,
                        command.getCustomerId(),
                        command.getAccountType(),
                        command.getInitialDeposit(),
                        "ACTIVE"
                    );
                    
                    // Publish domain event
                    AccountOpenedEvent eventPayload = new AccountOpenedEvent(
                        command.getCustomerId(),
                        accountNumber,
                        command.getAccountType(),
                        command.getInitialDeposit(),
                        Instant.now()
                    );
                    
                    DomainEventEnvelope event = DomainEventEnvelope.builder()
                        .topic("banking.accounts")
                        .type("account.opened")
                        .key(accountNumber)
                        .payload(eventPayload)
                        .timestamp(Instant.now())
                        .build();
                    
                    return eventPublisher.publish(event).thenReturn(result);
                });
        }

        @Override
        public Class<OpenAccountCommand> getCommandType() {
            return OpenAccountCommand.class;
        }
    }

    @Component
    static class TransferMoneyHandler implements CommandHandler<TransferMoneyCommand, TransferResult> {
        
        @Autowired
        private DomainEventPublisher eventPublisher;
        
        @Autowired
        private StepEventPublisherBridge stepEventBridge;

        @Override
        public Mono<TransferResult> handle(TransferMoneyCommand command) {
            return command.validate()
                .flatMap(validation -> {
                    if (!validation.isValid()) {
                        return Mono.error(new RuntimeException("Validation failed"));
                    }
                    
                    String transactionId = "TXN-" + System.currentTimeMillis();
                    TransferResult result = new TransferResult(
                        transactionId,
                        command.getFromAccount(),
                        command.getToAccount(),
                        command.getAmount(),
                        "COMPLETED"
                    );
                    
                    // Publish step event
                    StepEventEnvelope stepEvent = new StepEventEnvelope();
                    stepEvent.setSagaName("MoneyTransferSaga");
                    stepEvent.setSagaId("SAGA-" + transactionId);
                    stepEvent.setType("transfer.step.completed");
                    stepEvent.setKey(transactionId);
                    stepEvent.setPayload("Transfer step completed");
                    stepEvent.setTimestamp(Instant.now());
                    stepEvent.setAttempts(1);
                    stepEvent.setLatencyMs(100L);
                    stepEvent.setStartedAt(Instant.now().minusMillis(100));
                    stepEvent.setCompletedAt(Instant.now());
                    stepEvent.setResultType("SUCCESS");
                    
                    // Publish domain event
                    DomainEventEnvelope domainEvent = DomainEventEnvelope.builder()
                        .topic("banking.transactions")
                        .type("transfer.completed")
                        .key(transactionId)
                        .payload(result)
                        .timestamp(Instant.now())
                        .build();
                    
                    return stepEventBridge.publish(stepEvent)
                        .then(eventPublisher.publish(domainEvent))
                        .thenReturn(result);
                });
        }

        @Override
        public Class<TransferMoneyCommand> getCommandType() {
            return TransferMoneyCommand.class;
        }
    }

    @Component
    static class GetAccountBalanceHandler implements QueryHandler<GetAccountBalanceQuery, AccountBalance> {
        
        @Override
        public Mono<AccountBalance> handle(GetAccountBalanceQuery query) {
            AccountBalance balance = new AccountBalance(
                query.getAccountNumber(),
                new BigDecimal("2500.00"),
                "USD"
            );
            return Mono.just(balance);
        }

        @Override
        public Class<GetAccountBalanceQuery> getQueryType() {
            return GetAccountBalanceQuery.class;
        }

        @Override
        public boolean supportsCaching() {
            return true;
        }

        @Override
        public Long getCacheTtlSeconds() {
            return 300L;
        }
    }

    @Component
    static class TestEventListener {
        
        private final AtomicReference<DomainEventEnvelope> lastEvent = new AtomicReference<>();
        private final AtomicReference<DomainEventEnvelope> lastStepEvent = new AtomicReference<>();
        private final CountDownLatch eventLatch = new CountDownLatch(1);
        private final CountDownLatch stepEventLatch = new CountDownLatch(1);

        @EventListener
        public void handleDomainEvent(DomainSpringEvent event) {
            DomainEventEnvelope envelope = event.getEnvelope();
            
            if (envelope.getType().contains("step")) {
                lastStepEvent.set(envelope);
                stepEventLatch.countDown();
            } else {
                lastEvent.set(envelope);
                eventLatch.countDown();
            }
        }

        public boolean waitForEvent(String eventType, long timeout, TimeUnit unit) throws InterruptedException {
            boolean received = eventLatch.await(timeout, unit);
            return received && lastEvent.get() != null && lastEvent.get().getType().equals(eventType);
        }

        public boolean waitForStepEvent(String eventType, long timeout, TimeUnit unit) throws InterruptedException {
            boolean received = stepEventLatch.await(timeout, unit);
            return received && lastStepEvent.get() != null && lastStepEvent.get().getType().equals(eventType);
        }

        public DomainEventEnvelope getLastEvent() {
            return lastEvent.get();
        }

        public DomainEventEnvelope getLastStepEvent() {
            return lastStepEvent.get();
        }
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfiguration {
        // Test configuration beans if needed
    }
}
