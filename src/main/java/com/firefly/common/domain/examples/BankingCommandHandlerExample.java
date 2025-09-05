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

package com.firefly.common.domain.examples;

import com.firefly.common.domain.client.rest.RestServiceClient;
import com.firefly.common.domain.client.sdk.SdkServiceClient;
import com.firefly.common.domain.cqrs.command.Command;
import com.firefly.common.domain.cqrs.command.CommandHandler;
import com.firefly.common.domain.validation.ValidationResult;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Example demonstrating the CORRECT CQRS architecture where Command Handlers
 * use ServiceClient as a dependency to interact with external services.
 * 
 * <p>This example shows:
 * <ul>
 *   <li>ServiceClient dependency injection into Command Handlers</li>
 *   <li>SDK-based ServiceClient usage for banking operations</li>
 *   <li>REST-based ServiceClient usage for supporting services</li>
 *   <li>Proper separation of concerns between CQRS and external communication</li>
 * </ul>
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public class BankingCommandHandlerExample {

    /**
     * Example command for processing a money transfer.
     */
    @Data
    @Builder
    public static class ProcessMoneyTransferCommand implements Command<TransferResult> {
        private String commandId;
        private String fromAccountNumber;
        private String toAccountNumber;
        private BigDecimal amount;
        private String currency;
        private String purpose;
        private Instant timestamp;
        private String correlationId;
        private String initiatedBy;
        private Map<String, Object> metadata;

        @Override
        public String getCommandId() {
            return commandId;
        }

        @Override
        public Instant getTimestamp() {
            return timestamp != null ? timestamp : Instant.now();
        }

        @Override
        public String getCorrelationId() {
            return correlationId;
        }

        @Override
        public String getInitiatedBy() {
            return initiatedBy;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return metadata;
        }

        @Override
        public Mono<ValidationResult> validate() {
            if (fromAccountNumber == null || fromAccountNumber.trim().isEmpty()) {
                return Mono.just(ValidationResult.failure("fromAccountNumber", "Source account is required"));
            }
            if (toAccountNumber == null || toAccountNumber.trim().isEmpty()) {
                return Mono.just(ValidationResult.failure("toAccountNumber", "Destination account is required"));
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return Mono.just(ValidationResult.failure("amount", "Amount must be positive"));
            }
            return Mono.just(ValidationResult.success());
        }
    }

    /**
     * Transfer result returned by the command handler.
     */
    @Data
    @Builder
    public static class TransferResult {
        private String transferId;
        private String status;
        private BigDecimal amount;
        private String currency;
        private Instant processedAt;
    }

    /**
     * Example Command Handler demonstrating CORRECT architecture:
     * ServiceClients are DEPENDENCIES injected into the handler.
     */
    @Component
    public static class ProcessMoneyTransferCommandHandler 
            implements CommandHandler<ProcessMoneyTransferCommand, TransferResult> {

        // ServiceClients are DEPENDENCIES injected into CQRS handlers
        private final SdkServiceClient<CoreBankingSDK> coreBankingClient;
        private final RestServiceClient fraudDetectionClient;
        private final RestServiceClient notificationClient;

        public ProcessMoneyTransferCommandHandler(
                SdkServiceClient<CoreBankingSDK> coreBankingClient,
                RestServiceClient fraudDetectionClient,
                RestServiceClient notificationClient) {
            this.coreBankingClient = coreBankingClient;
            this.fraudDetectionClient = fraudDetectionClient;
            this.notificationClient = notificationClient;
        }

        @Override
        public Mono<TransferResult> handle(ProcessMoneyTransferCommand command) {
            log.info("Processing money transfer from {} to {} for amount {}", 
                    command.getFromAccountNumber(), command.getToAccountNumber(), command.getAmount());

            return command.validate()
                    .flatMap(validation -> {
                        if (!validation.isValid()) {
                            return Mono.error(new IllegalArgumentException(validation.getSummary()));
                        }
                        return processTransfer(command);
                    });
        }

        private Mono<TransferResult> processTransfer(ProcessMoneyTransferCommand command) {
            return performFraudCheck(command)
                    .flatMap(fraudResult -> {
                        if (!fraudResult.isApproved()) {
                            return Mono.error(new SecurityException("Transfer blocked by fraud detection"));
                        }
                        return executeTransferViaSdk(command);
                    })
                    .flatMap(result -> sendNotification(command, result).thenReturn(result));
        }

        private Mono<FraudCheckResult> performFraudCheck(ProcessMoneyTransferCommand command) {
            // Command handler USES REST ServiceClient for fraud detection
            FraudCheckRequest request = FraudCheckRequest.builder()
                    .fromAccount(command.getFromAccountNumber())
                    .toAccount(command.getToAccountNumber())
                    .amount(command.getAmount())
                    .currency(command.getCurrency())
                    .build();

            return fraudDetectionClient.post("/fraud/check", request, FraudCheckResult.class);
        }

        private Mono<TransferResult> executeTransferViaSdk(ProcessMoneyTransferCommand command) {
            // Command handler USES SDK ServiceClient for core banking operations
            return coreBankingClient.execute(sdk -> 
                sdk.transfers().execute(TransferRequest.builder()
                    .fromAccount(command.getFromAccountNumber())
                    .toAccount(command.getToAccountNumber())
                    .amount(command.getAmount())
                    .currency(command.getCurrency())
                    .purpose(command.getPurpose())
                    .build())
            ).map(sdkResult -> TransferResult.builder()
                    .transferId(sdkResult.getTransferId())
                    .status(sdkResult.getStatus())
                    .amount(command.getAmount())
                    .currency(command.getCurrency())
                    .processedAt(Instant.now())
                    .build());
        }

        private Mono<Void> sendNotification(ProcessMoneyTransferCommand command, TransferResult result) {
            // Command handler USES REST ServiceClient for notifications
            NotificationRequest notification = NotificationRequest.builder()
                    .transferId(result.getTransferId())
                    .fromAccount(command.getFromAccountNumber())
                    .toAccount(command.getToAccountNumber())
                    .amount(command.getAmount())
                    .status(result.getStatus())
                    .build();

            return notificationClient.post("/notifications/transfer", notification, Void.class);
        }

        @Override
        public Class<ProcessMoneyTransferCommand> getCommandType() {
            return ProcessMoneyTransferCommand.class;
        }
    }

    // Supporting classes for the example (would normally be in separate files)

    public interface CoreBankingSDK {
        TransferOperations transfers();
    }

    public interface TransferOperations {
        SdkTransferResult execute(TransferRequest request);
    }

    @Data
    @Builder
    public static class TransferRequest {
        private String fromAccount;
        private String toAccount;
        private BigDecimal amount;
        private String currency;
        private String purpose;
    }

    @Data
    @Builder
    public static class SdkTransferResult {
        private String transferId;
        private String status;
    }

    @Data
    @Builder
    public static class FraudCheckRequest {
        private String fromAccount;
        private String toAccount;
        private BigDecimal amount;
        private String currency;
    }

    @Data
    @Builder
    public static class FraudCheckResult {
        private boolean approved;
        private String reason;
        private String riskScore;
    }

    @Data
    @Builder
    public static class NotificationRequest {
        private String transferId;
        private String fromAccount;
        private String toAccount;
        private BigDecimal amount;
        private String status;
    }
}
