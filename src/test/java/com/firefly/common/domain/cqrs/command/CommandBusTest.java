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

package com.firefly.common.domain.cqrs.command;

import com.firefly.common.domain.cqrs.command.DefaultCommandBus.CommandHandlerNotFoundException;
import com.firefly.common.domain.cqrs.command.DefaultCommandBus.CommandProcessingException;
import com.firefly.common.domain.tracing.CorrelationContext;
import com.firefly.common.domain.validation.ValidationResult;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for CommandBus functionality using banking domain examples.
 * Tests cover command processing, handler registration, correlation context, and error scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Banking Command Bus - Processing Customer and Account Operations")
class CommandBusTest {

    @Mock
    private ApplicationContext applicationContext;
    
    @Mock
    private CorrelationContext correlationContext;

    private DefaultCommandBus commandBus;

    @BeforeEach
    void setUp() {
        when(applicationContext.getBeansOfType(CommandHandler.class))
            .thenReturn(Map.of());
        
        commandBus = new DefaultCommandBus(applicationContext, correlationContext);
    }

    @Test
    @DisplayName("Should successfully process customer account opening command")
    void shouldProcessCustomerAccountOpeningCommand() {
        // Given: A customer wants to open a new savings account
        OpenSavingsAccountCommand command = new OpenSavingsAccountCommand(
            "CUST-12345", 
            "John Doe", 
            new BigDecimal("1000.00"),
            "CORR-789"
        );
        
        OpenSavingsAccountHandler handler = new OpenSavingsAccountHandler();
        commandBus.registerHandler(handler);

        // When: The command is processed through the command bus
        Mono<AccountOpenedResult> result = commandBus.send(command);

        // Then: The account should be successfully opened
        StepVerifier.create(result)
            .assertNext(accountResult -> {
                assertThat(accountResult.getAccountNumber()).isNotNull();
                assertThat(accountResult.getCustomerId()).isEqualTo("CUST-12345");
                assertThat(accountResult.getInitialBalance()).isEqualTo(new BigDecimal("1000.00"));
                assertThat(accountResult.getAccountType()).isEqualTo("SAVINGS");
                assertThat(accountResult.getStatus()).isEqualTo("ACTIVE");
            })
            .verifyComplete();

        // And: Correlation context should be properly managed
        verify(correlationContext).setCorrelationId("CORR-789");
        verify(correlationContext).clear();
    }

    @Test
    @DisplayName("Should handle money transfer between customer accounts")
    void shouldHandleMoneyTransferBetweenAccounts() {
        // Given: A customer wants to transfer money between accounts
        TransferMoneyCommand command = new TransferMoneyCommand(
            "ACC-001", 
            "ACC-002", 
            new BigDecimal("500.00"),
            "Monthly savings transfer",
            "CORR-456"
        );
        
        TransferMoneyHandler handler = new TransferMoneyHandler();
        commandBus.registerHandler(handler);

        // When: The transfer command is processed
        Mono<TransferResult> result = commandBus.send(command);

        // Then: The transfer should be completed successfully
        StepVerifier.create(result)
            .assertNext(transferResult -> {
                assertThat(transferResult.getTransactionId()).isNotNull();
                assertThat(transferResult.getFromAccount()).isEqualTo("ACC-001");
                assertThat(transferResult.getToAccount()).isEqualTo("ACC-002");
                assertThat(transferResult.getAmount()).isEqualTo(new BigDecimal("500.00"));
                assertThat(transferResult.getStatus()).isEqualTo("COMPLETED");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should reject command when no handler is registered")
    void shouldRejectCommandWhenNoHandlerRegistered() {
        // Given: A command with no registered handler
        OpenSavingsAccountCommand command = new OpenSavingsAccountCommand(
            "CUST-99999", 
            "Jane Smith", 
            new BigDecimal("500.00"),
            null
        );

        // When: The command is sent to the bus
        Mono<AccountOpenedResult> result = commandBus.send(command);

        // Then: Should throw CommandHandlerNotFoundException
        StepVerifier.create(result)
            .expectError(CommandHandlerNotFoundException.class)
            .verify();
    }

    @Test
    @DisplayName("Should handle command validation failures gracefully")
    void shouldHandleCommandValidationFailures() {
        // Given: A command that will fail validation (negative amount)
        OpenSavingsAccountCommand invalidCommand = new OpenSavingsAccountCommand(
            "CUST-12345", 
            "John Doe", 
            new BigDecimal("-100.00"), // Invalid negative amount
            "CORR-123"
        );
        
        OpenSavingsAccountHandler handler = new OpenSavingsAccountHandler();
        commandBus.registerHandler(handler);

        // When: The invalid command is processed
        Mono<AccountOpenedResult> result = commandBus.send(invalidCommand);

        // Then: Should handle validation error appropriately
        StepVerifier.create(result)
            .expectError(CommandProcessingException.class)
            .verify();
    }

    @Test
    @DisplayName("Should support handler registration and unregistration")
    void shouldSupportHandlerRegistrationAndUnregistration() {
        // Given: A command handler
        OpenSavingsAccountHandler handler = new OpenSavingsAccountHandler();

        // When: Handler is registered
        commandBus.registerHandler(handler);

        // Then: Handler should be available
        assertThat(commandBus.hasHandler(OpenSavingsAccountCommand.class)).isTrue();

        // When: Handler is unregistered
        commandBus.unregisterHandler(OpenSavingsAccountCommand.class);

        // Then: Handler should no longer be available
        assertThat(commandBus.hasHandler(OpenSavingsAccountCommand.class)).isFalse();
    }

    // Test Command: Open Savings Account
    @Data
    static class OpenSavingsAccountCommand implements Command<AccountOpenedResult> {
        private final String customerId;
        private final String customerName;
        private final BigDecimal initialDeposit;
        private final String correlationId;

        @Override
        public Mono<ValidationResult> validate() {
            ValidationResult.Builder builder = ValidationResult.builder();

            if (customerId == null || customerId.trim().isEmpty()) {
                builder.addError("customerId", "Customer ID is required for account opening");
            }

            if (customerName == null || customerName.trim().isEmpty()) {
                builder.addError("customerName", "Customer name is required");
            }

            if (initialDeposit == null || initialDeposit.compareTo(BigDecimal.ZERO) <= 0) {
                builder.addError("initialDeposit", "Initial deposit must be greater than zero");
            }

            if (initialDeposit != null && initialDeposit.compareTo(new BigDecimal("10000000")) > 0) {
                builder.addError("initialDeposit", "Initial deposit cannot exceed $10,000,000");
            }

            return Mono.just(builder.build());
        }
    }

    // Test Command: Transfer Money
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

            if (fromAccount == null || fromAccount.trim().isEmpty()) {
                builder.addError("fromAccount", "Source account is required");
            }

            if (toAccount == null || toAccount.trim().isEmpty()) {
                builder.addError("toAccount", "Destination account is required");
            }

            if (fromAccount != null && fromAccount.equals(toAccount)) {
                builder.addError("accounts", "Cannot transfer to the same account");
            }

            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                builder.addError("amount", "Transfer amount must be greater than zero");
            }

            return Mono.just(builder.build());
        }
    }

    // Test Result: Account Opened
    @Data
    static class AccountOpenedResult {
        private final String accountNumber;
        private final String customerId;
        private final BigDecimal initialBalance;
        private final String accountType;
        private final String status;
    }

    // Test Result: Transfer
    @Data
    static class TransferResult {
        private final String transactionId;
        private final String fromAccount;
        private final String toAccount;
        private final BigDecimal amount;
        private final String status;
    }

    // Test Handler: Open Savings Account
    static class OpenSavingsAccountHandler implements CommandHandler<OpenSavingsAccountCommand, AccountOpenedResult> {
        
        @Override
        public Mono<AccountOpenedResult> handle(OpenSavingsAccountCommand command) {
            return command.validate()
                .flatMap(validation -> {
                    if (!validation.isValid()) {
                        return Mono.error(new CommandProcessingException("Validation failed: " + validation.getErrors(), new RuntimeException("Validation error")));
                    }
                    
                    // Simulate account creation
                    String accountNumber = "SAV-" + System.currentTimeMillis();
                    AccountOpenedResult result = new AccountOpenedResult(
                        accountNumber,
                        command.getCustomerId(),
                        command.getInitialDeposit(),
                        "SAVINGS",
                        "ACTIVE"
                    );
                    
                    return Mono.just(result);
                });
        }

        @Override
        public Class<OpenSavingsAccountCommand> getCommandType() {
            return OpenSavingsAccountCommand.class;
        }
    }

    // Test Handler: Transfer Money
    static class TransferMoneyHandler implements CommandHandler<TransferMoneyCommand, TransferResult> {
        
        @Override
        public Mono<TransferResult> handle(TransferMoneyCommand command) {
            return command.validate()
                .flatMap(validation -> {
                    if (!validation.isValid()) {
                        return Mono.error(new CommandProcessingException("Transfer validation failed: " + validation.getErrors(), new RuntimeException("Transfer validation error")));
                    }
                    
                    // Simulate money transfer
                    String transactionId = "TXN-" + System.currentTimeMillis();
                    TransferResult result = new TransferResult(
                        transactionId,
                        command.getFromAccount(),
                        command.getToAccount(),
                        command.getAmount(),
                        "COMPLETED"
                    );
                    
                    return Mono.just(result);
                });
        }

        @Override
        public Class<TransferMoneyCommand> getCommandType() {
            return TransferMoneyCommand.class;
        }
    }
}
