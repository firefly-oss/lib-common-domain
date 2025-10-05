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

import com.firefly.common.cqrs.command.Command;
import com.firefly.common.cqrs.query.Query;
import com.firefly.common.cqrs.validation.ValidationResult;
import lombok.Builder;
import lombok.Data;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Test data classes for CQRS + Saga integration testing
 */
public class CustomerRegistrationTestData {

    // Input Request
    @Data
    @Builder
    public static class CustomerRegistrationRequest {
        private final String customerId;
        private final String firstName;
        private final String lastName;
        private final String email;
        private final String phoneNumber;
        private final String documentType;
        private final String documentNumber;
        private final BigDecimal initialDeposit;
        private final String correlationId;
    }

    // Commands
    @Data
    @Builder
    public static class CreateCustomerProfileCommand implements Command<CustomerProfileResult> {
        private final String customerId;
        private final String firstName;
        private final String lastName;
        private final String email;
        private final String phoneNumber;
        private final String correlationId;

        @Override
        public Mono<ValidationResult> validate() {
            return Mono.just(ValidationResult.success());
        }

        @Override
        public String getCorrelationId() {
            return correlationId;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return Map.of(
                "customerId", customerId,
                "email", email
            );
        }
    }

    @Data
    @Builder
    public static class StartKycVerificationCommand implements Command<KycResult> {
        private final String customerId;
        private final String profileId;
        private final String documentType;
        private final String documentNumber;
        private final String correlationId;

        @Override
        public Mono<ValidationResult> validate() {
            return Mono.just(ValidationResult.success());
        }

        @Override
        public String getCorrelationId() {
            return correlationId;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return Map.of(
                "customerId", customerId,
                "documentType", documentType
            );
        }
    }

    @Data
    @Builder
    public static class CreateAccountCommand implements Command<AccountCreationResult> {
        private final String customerId;
        private final String accountType;
        private final BigDecimal initialDeposit;
        private final String currency;
        private final String correlationId;

        @Override
        public Mono<ValidationResult> validate() {
            return Mono.just(ValidationResult.success());
        }

        @Override
        public String getCorrelationId() {
            return correlationId;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return Map.of(
                "customerId", customerId,
                "accountType", accountType,
                "initialDeposit", initialDeposit
            );
        }
    }

    // Queries
    @Data
    @Builder
    public static class ValidateCustomerQuery implements Query<CustomerValidationResult> {
        private final String email;
        private final String phoneNumber;
        private final String correlationId;

        @Override
        public boolean isCacheable() {
            return true;
        }

        @Override
        public String getCacheKey() {
            return String.format("customer_validation_%s_%s", email, phoneNumber);
        }

        @Override
        public String getCorrelationId() {
            return correlationId;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return Map.of(
                "email", email,
                "phoneNumber", phoneNumber
            );
        }
    }

    @Data
    @Builder
    public static class GetCustomerProfileQuery implements Query<CustomerProfile> {
        private final String customerId;

        @Override
        public boolean isCacheable() {
            return true;
        }

        @Override
        public String getCacheKey() {
            return String.format("customer_profile_%s", customerId);
        }

        @Override
        public String getCorrelationId() {
            return null;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return Map.of("customerId", customerId);
        }
    }

    // Results
    @Data
    @Builder
    public static class CustomerValidationResult {
        private final String customerId;
        private final boolean isValid;
        private final List<String> validationErrors;
    }

    @Data
    @Builder
    public static class CustomerProfileResult {
        private final String customerId;
        private final String profileId;
        private final String email;
        private final String status;
    }

    @Data
    @Builder
    public static class KycResult {
        private final String kycId;
        private final String customerId;
        private final boolean isApproved;
        private final String verificationStatus;
    }

    @Data
    @Builder
    public static class AccountCreationResult {
        private final String accountId;
        private final String accountNumber;
        private final String customerId;
        private final String accountType;
        private final BigDecimal initialBalance;
        private final String currency;
        private final String status;
    }

    @Data
    @Builder
    public static class CustomerProfile {
        private final String customerId;
        private final String firstName;
        private final String lastName;
        private final String email;
        private final String phoneNumber;
        private final String status;
    }

    @Data
    @Builder
    public static class NotificationResult {
        private final String notificationId;
        private final String customerId;
        private final String type;
        private final String status;
        private final Instant sentAt;
    }

    @Data
    @Builder
    public static class CustomerRegistrationResult {
        private final String customerId;
        private final String profileId;
        private final String accountId;
        private final String accountNumber;
        private final String status;
        private final List<String> failedSteps;
        private final List<String> compensatedSteps;
        private final String errorMessage;
        private final Instant registrationDate;
    }
}
