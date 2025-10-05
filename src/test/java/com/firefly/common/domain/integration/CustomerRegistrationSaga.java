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

import com.firefly.common.cqrs.command.CommandBus;
import com.firefly.common.cqrs.query.QueryBus;
import com.firefly.transactional.annotations.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.firefly.common.domain.integration.CustomerRegistrationTestData.*;

/**
 * Customer Registration Saga demonstrating CQRS + lib-transactional-engine integration.
 * This saga orchestrates a complex customer onboarding process using CQRS commands and queries
 * within saga steps, with automatic compensation on failures.
 */
@Component
@Saga(name = "customer-registration")
@EnableTransactionalEngine
public class CustomerRegistrationSaga {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    public CustomerRegistrationSaga(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    /**
     * Step 1: Validate customer data using CQRS Query
     * This step validates email uniqueness and phone number format
     */
    @SagaStep(id = "validate-customer", retry = 3, backoffMs = 1000)
    public Mono<CustomerValidationResult> validateCustomer(@Input CustomerRegistrationRequest request) {
        ValidateCustomerQuery query = ValidateCustomerQuery.builder()
            .email(request.getEmail())
            .phoneNumber(request.getPhoneNumber())
            .correlationId(request.getCorrelationId())
            .build();
            
        return queryBus.query(query)
            .map(validation -> CustomerValidationResult.builder()
                .customerId(request.getCustomerId())
                .isValid(validation.isValid())
                .validationErrors(validation.getValidationErrors())
                .build());
    }

    /**
     * Step 2: Create customer profile using CQRS Command
     * This step creates the customer profile in the system
     * Compensation: deleteProfile
     */
    @SagaStep(id = "create-profile",
              dependsOn = "validate-customer",
              compensate = "deleteProfile",
              timeoutMs = 30000)
    public Mono<CustomerProfileResult> createProfile(
            @FromStep("validate-customer") CustomerValidationResult validation) {

        if (!validation.isValid()) {
            return Mono.error(new CustomerValidationException(validation.getValidationErrors()));
        }

        // Get the original request from the saga context
        // For now, we'll create a mock profile since we don't have access to the original request
        CreateCustomerProfileCommand command = CreateCustomerProfileCommand.builder()
            .customerId(validation.getCustomerId())
            .firstName("John")  // Mock data
            .lastName("Doe")    // Mock data
            .email("john.doe@example.com")  // Mock data
            .phoneNumber("+1-555-123-4567") // Mock data
            .correlationId("CORR-" + java.util.UUID.randomUUID().toString())
            .build();

        return commandBus.send(command)
            .map(result -> CustomerProfileResult.builder()
                .customerId(result.getCustomerId())
                .profileId(result.getProfileId())
                .email(result.getEmail())
                .status("CREATED")
                .build());
    }

    /**
     * Step 3: Perform KYC verification using external service
     * This step initiates KYC verification process
     * Compensation: cancelKyc
     */
    @SagaStep(id = "kyc-verification",
              dependsOn = "create-profile",
              compensate = "cancelKyc",
              retry = 2,
              timeoutMs = 60000)
    public Mono<KycResult> performKycVerification(
            @FromStep("create-profile") CustomerProfileResult profile) {

        // Determine document type based on customer ID pattern for testing
        String documentType = profile.getCustomerId().contains("FAIL") ? "INVALID_DOCUMENT" : "PASSPORT";
        String documentNumber = profile.getCustomerId().contains("FAIL") ? "INVALID123" : "P123456789";

        StartKycVerificationCommand command = StartKycVerificationCommand.builder()
            .customerId(profile.getCustomerId())
            .profileId(profile.getProfileId())
            .documentType(documentType)
            .documentNumber(documentNumber)
            .correlationId("CORR-" + java.util.UUID.randomUUID().toString())
            .build();

        return commandBus.send(command);
    }

    /**
     * Step 4: Create initial account using CQRS Command
     * This step creates the customer's initial bank account
     * Compensation: closeAccount
     */
    @SagaStep(id = "create-account",
              dependsOn = "kyc-verification",
              compensate = "closeAccount")
    public Mono<AccountCreationResult> createInitialAccount(
            @FromStep("create-profile") CustomerProfileResult profile,
            @FromStep("kyc-verification") KycResult kyc) {

        if (!kyc.isApproved()) {
            return Mono.error(new KycRejectionException("KYC verification failed"));
        }

        CreateAccountCommand command = CreateAccountCommand.builder()
            .customerId(profile.getCustomerId())
            .accountType("CHECKING")
            .initialDeposit(new java.math.BigDecimal("1000.00"))  // Mock data
            .currency("USD")
            .correlationId("CORR-" + java.util.UUID.randomUUID().toString())
            .build();

        return commandBus.send(command);
    }

    /**
     * Step 5: Send welcome notification
     * This step sends a welcome email to the customer
     * No compensation needed as this is a notification
     */
    @SagaStep(id = "send-welcome", dependsOn = "create-account")
    public Mono<NotificationResult> sendWelcomeNotification(
            @FromStep("create-profile") CustomerProfileResult profile,
            @FromStep("create-account") AccountCreationResult account) {
        
        // For testing purposes, we'll simulate sending a notification
        return Mono.just(NotificationResult.builder()
            .notificationId("NOTIF-" + java.util.UUID.randomUUID().toString())
            .customerId(profile.getCustomerId())
            .type("WELCOME_EMAIL")
            .status("SENT")
            .sentAt(java.time.Instant.now())
            .build());
    }

    // Compensation Methods

    /**
     * Compensation for create-profile step
     * Deletes the customer profile if subsequent steps fail
     */
    public Mono<Void> deleteProfile(@FromStep("create-profile") CustomerProfileResult profile) {
        // In a real implementation, this would call a DeleteCustomerProfileCommand
        // For testing, we'll just log and return success
        System.out.println("Compensating: Deleting customer profile " + profile.getProfileId());
        return Mono.empty();
    }

    /**
     * Compensation for kyc-verification step
     * Cancels the KYC verification process
     */
    public Mono<Void> cancelKyc(@FromStep("kyc-verification") KycResult kyc) {
        // In a real implementation, this would call a CancelKycVerificationCommand
        // For testing, we'll just log and return success
        System.out.println("Compensating: Canceling KYC verification " + kyc.getKycId());
        return Mono.empty();
    }

    /**
     * Compensation for create-account step
     * Closes the created account
     */
    public Mono<Void> closeAccount(@FromStep("create-account") AccountCreationResult account) {
        // In a real implementation, this would call a CloseAccountCommand
        // For testing, we'll just log and return success
        System.out.println("Compensating: Closing account " + account.getAccountNumber());
        return Mono.empty();
    }

    // Exception classes for saga flow control
    public static class CustomerValidationException extends RuntimeException {
        public CustomerValidationException(java.util.List<String> errors) {
            super("Customer validation failed: " + String.join(", ", errors));
        }
    }

    public static class KycRejectionException extends RuntimeException {
        public KycRejectionException(String message) {
            super(message);
        }
    }
}
