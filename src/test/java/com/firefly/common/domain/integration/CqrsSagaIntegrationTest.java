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

package com.firefly.commondomain.integration;

import com.firefly.commondomain.cqrs.CommandBus;
import com.firefly.commondomain.cqrs.CommandHandler;
import com.firefly.commondomain.cqrs.QueryBus;
import com.firefly.commondomain.cqrs.QueryHandler;
import com.firefly.transactionalengine.engine.SagaEngine;
import com.firefly.transactionalengine.engine.StepInputs;
import com.firefly.transactionalengine.engine.SagaResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.firefly.commondomain.integration.CustomerRegistrationTestData.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test demonstrating how to combine CQRS framework with lib-transactional-engine
 * for complex saga orchestration. This test shows a complete customer registration saga
 * that uses CQRS commands and queries within saga steps.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "firefly.cqrs.enabled=true",
    "firefly.events.enabled=true",
    "firefly.stepevents.enabled=true",
    "logging.level.com.firefly=DEBUG"
})
class CqrsSagaIntegrationTest {

    @Autowired
    private SagaEngine sagaEngine;

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private QueryBus queryBus;

    @Test
    void shouldExecuteCustomerRegistrationSagaSuccessfully() {
        // Given: A customer registration request
        CustomerRegistrationRequest request = CustomerRegistrationRequest.builder()
            .customerId("CUST-" + UUID.randomUUID().toString())
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .phoneNumber("+1-555-123-4567")
            .documentType("PASSPORT")
            .documentNumber("P123456789")
            .initialDeposit(new BigDecimal("1000.00"))
            .correlationId("CORR-" + UUID.randomUUID().toString())
            .build();

        // When: Execute the customer registration saga
        StepInputs inputs = StepInputs.of("validate-customer", request);
        
        Mono<SagaResult> sagaExecution = sagaEngine.execute("customer-registration", inputs);

        // Then: Saga should complete successfully
        StepVerifier.create(sagaExecution)
            .assertNext(result -> {
                assertThat(result.isSuccess()).isTrue();
                assertThat(result.completedSteps()).containsExactly(
                    "validate-customer",
                    "create-profile", 
                    "kyc-verification",
                    "create-account",
                    "send-welcome"
                );
                
                // Verify step results
                CustomerValidationResult validation = result.resultOf("validate-customer", CustomerValidationResult.class)
                    .orElseThrow(() -> new AssertionError("Validation step result not found"));
                assertThat(validation.isValid()).isTrue();
                
                CustomerProfileResult profile = result.resultOf("create-profile", CustomerProfileResult.class)
                    .orElseThrow(() -> new AssertionError("Profile creation step result not found"));
                assertThat(profile.getCustomerId()).isEqualTo(request.getCustomerId());
                assertThat(profile.getStatus()).isEqualTo("CREATED");
                
                KycResult kyc = result.resultOf("kyc-verification", KycResult.class)
                    .orElseThrow(() -> new AssertionError("KYC step result not found"));
                assertThat(kyc.isApproved()).isTrue();
                
                AccountCreationResult account = result.resultOf("create-account", AccountCreationResult.class)
                    .orElseThrow(() -> new AssertionError("Account creation step result not found"));
                assertThat(account.getAccountNumber()).isNotNull();
                assertThat(account.getInitialBalance()).isEqualTo(request.getInitialDeposit());
            })
            .expectComplete()
            .verify(Duration.ofSeconds(30));
    }

    @Test
    void shouldCompensateWhenSagaStepFails() {
        // Given: A customer registration request that will fail at KYC step
        CustomerRegistrationRequest request = CustomerRegistrationRequest.builder()
            .customerId("CUST-FAIL-" + UUID.randomUUID().toString())
            .firstName("Jane")
            .lastName("Doe")
            .email("jane.doe.fail@example.com")
            .phoneNumber("+1-555-987-6543")
            .documentType("INVALID_DOCUMENT")  // This will cause KYC to fail
            .documentNumber("INVALID123")
            .initialDeposit(new BigDecimal("500.00"))
            .correlationId("CORR-FAIL-" + UUID.randomUUID().toString())
            .build();

        // When: Execute the customer registration saga
        StepInputs inputs = StepInputs.of("validate-customer", request);
        
        Mono<SagaResult> sagaExecution = sagaEngine.execute("customer-registration", inputs);

        // Then: Saga should fail and compensate completed steps
        StepVerifier.create(sagaExecution)
            .assertNext(result -> {
                assertThat(result.isSuccess()).isFalse();
                assertThat(result.failedSteps()).contains("kyc-verification");
                assertThat(result.compensatedSteps()).contains("create-profile");
                
                // Verify that profile was created but then compensated
                assertThat(result.completedSteps()).contains("validate-customer", "create-profile");
                assertThat(result.compensatedSteps()).contains("create-profile");
            })
            .expectComplete()
            .verify(Duration.ofSeconds(30));
    }

    @Test
    void shouldExecuteIndividualCommandsAndQueries() {
        // Given: Individual CQRS commands and queries
        String customerId = "CUST-INDIVIDUAL-" + UUID.randomUUID().toString();
        
        CreateCustomerProfileCommand createCommand = CreateCustomerProfileCommand.builder()
            .customerId(customerId)
            .firstName("Alice")
            .lastName("Smith")
            .email("alice.smith@example.com")
            .phoneNumber("+1-555-111-2222")
            .correlationId("CORR-INDIVIDUAL-" + UUID.randomUUID().toString())
            .build();

        // When: Execute command through CommandBus
        Mono<CustomerProfileResult> commandResult = commandBus.send(createCommand);

        // Then: Command should execute successfully
        StepVerifier.create(commandResult)
            .assertNext(result -> {
                assertThat(result.getCustomerId()).isEqualTo(customerId);
                assertThat(result.getProfileId()).isNotNull();
                assertThat(result.getStatus()).isEqualTo("CREATED");
            })
            .expectComplete()
            .verify(Duration.ofSeconds(10));

        // Given: Query to retrieve customer profile
        GetCustomerProfileQuery query = GetCustomerProfileQuery.builder()
            .customerId(customerId)
            .build();

        // When: Execute query through QueryBus
        Mono<CustomerProfile> queryResult = queryBus.query(query);

        // Then: Query should return the created profile
        StepVerifier.create(queryResult)
            .assertNext(profile -> {
                assertThat(profile.getCustomerId()).isEqualTo(customerId);
                assertThat(profile.getFirstName()).isEqualTo("Alice");
                assertThat(profile.getLastName()).isEqualTo("Smith");
                assertThat(profile.getEmail()).isEqualTo("alice.smith@example.com");
            })
            .expectComplete()
            .verify(Duration.ofSeconds(5));
    }

    @Test
    void shouldExecuteSagaWithTypeSafetyUsingClassReference() {
        // Given: A customer registration request
        CustomerRegistrationRequest request = CustomerRegistrationRequest.builder()
            .customerId("CUST-TYPESAFE-" + UUID.randomUUID().toString())
            .firstName("Bob")
            .lastName("Johnson")
            .email("bob.johnson@example.com")
            .phoneNumber("+1-555-333-4444")
            .documentType("DRIVERS_LICENSE")
            .documentNumber("DL987654321")
            .initialDeposit(new BigDecimal("2000.00"))
            .correlationId("CORR-TYPESAFE-" + UUID.randomUUID().toString())
            .build();

        // When: Execute saga using class reference for type safety
        StepInputs inputs = StepInputs.of("validate-customer", request);
        
        Mono<SagaResult> sagaExecution = sagaEngine.execute(CustomerRegistrationSaga.class, inputs);

        // Then: Saga should complete successfully with type-safe execution
        StepVerifier.create(sagaExecution)
            .assertNext(result -> {
                assertThat(result.isSuccess()).isTrue();
                assertThat(result.completedSteps()).hasSize(5);
                
                // Type-safe result extraction
                CustomerProfileResult profile = result.resultOf("create-profile", CustomerProfileResult.class)
                    .orElseThrow(() -> new AssertionError("Profile creation step result not found"));
                assertThat(profile.getCustomerId()).isEqualTo(request.getCustomerId());
            })
            .expectComplete()
            .verify(Duration.ofSeconds(30));
    }

    @Test
    void shouldHandleQueryCachingWithinSagaSteps() {
        // Given: A validation query that should be cached
        String email = "cached.user@example.com";
        
        ValidateCustomerQuery query = ValidateCustomerQuery.builder()
            .email(email)
            .phoneNumber("+1-555-777-8888")
            .correlationId("CORR-CACHE-" + UUID.randomUUID().toString())
            .build();

        // When: Execute query multiple times
        Mono<CustomerValidationResult> firstQuery = queryBus.query(query);
        Mono<CustomerValidationResult> secondQuery = queryBus.query(query);

        // Then: Both queries should return the same result (second from cache)
        StepVerifier.create(Mono.zip(firstQuery, secondQuery))
            .assertNext(tuple -> {
                CustomerValidationResult first = tuple.getT1();
                CustomerValidationResult second = tuple.getT2();
                
                assertThat(first.isValid()).isEqualTo(second.isValid());
                assertThat(first.getCustomerId()).isEqualTo(second.getCustomerId());
                // Second query should be faster due to caching
            })
            .expectComplete()
            .verify(Duration.ofSeconds(10));
    }

    // Test Data Classes and Mock Implementations

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public CustomerRegistrationSaga customerRegistrationSaga(CommandBus commandBus, QueryBus queryBus) {
            return new CustomerRegistrationSaga(commandBus, queryBus);
        }

        @Bean
        @Primary
        public ValidateCustomerHandler validateCustomerHandler() {
            return new MockValidateCustomerHandler();
        }

        @Bean
        @Primary
        public CreateCustomerProfileHandler createCustomerProfileHandler() {
            return new MockCreateCustomerProfileHandler();
        }

        @Bean
        @Primary
        public StartKycVerificationHandler startKycVerificationHandler() {
            return new MockStartKycVerificationHandler();
        }

        @Bean
        @Primary
        public CreateAccountHandler createAccountHandler() {
            return new MockCreateAccountHandler();
        }

        @Bean
        @Primary
        public GetCustomerProfileHandler getCustomerProfileHandler() {
            return new MockGetCustomerProfileHandler();
        }
    }

    // Mock Command Handlers
    static class MockValidateCustomerHandler implements QueryHandler<ValidateCustomerQuery, CustomerValidationResult> {
        @Override
        public Mono<CustomerValidationResult> handle(ValidateCustomerQuery query) {
            return Mono.just(CustomerValidationResult.builder()
                .customerId("VALIDATED-" + UUID.randomUUID().toString())
                .isValid(true)
                .validationErrors(List.of())
                .build())
                .delayElement(Duration.ofMillis(100)); // Simulate processing time
        }

        @Override
        public Class<ValidateCustomerQuery> getQueryType() {
            return ValidateCustomerQuery.class;
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

    static class MockCreateCustomerProfileHandler implements CommandHandler<CreateCustomerProfileCommand, CustomerProfileResult> {
        @Override
        public Mono<CustomerProfileResult> handle(CreateCustomerProfileCommand command) {
            return Mono.just(CustomerProfileResult.builder()
                .customerId(command.getCustomerId())
                .profileId("PROFILE-" + UUID.randomUUID().toString())
                .email(command.getEmail())
                .status("CREATED")
                .build())
                .delayElement(Duration.ofMillis(200));
        }

        @Override
        public Class<CreateCustomerProfileCommand> getCommandType() {
            return CreateCustomerProfileCommand.class;
        }
    }

    static class MockStartKycVerificationHandler implements CommandHandler<StartKycVerificationCommand, KycResult> {
        @Override
        public Mono<KycResult> handle(StartKycVerificationCommand command) {
            // Simulate KYC failure for invalid documents
            boolean approved = !command.getDocumentType().equals("INVALID_DOCUMENT");

            return Mono.just(KycResult.builder()
                .kycId("KYC-" + UUID.randomUUID().toString())
                .customerId(command.getCustomerId())
                .isApproved(approved)
                .verificationStatus(approved ? "APPROVED" : "REJECTED")
                .build())
                .delayElement(Duration.ofMillis(500));
        }

        @Override
        public Class<StartKycVerificationCommand> getCommandType() {
            return StartKycVerificationCommand.class;
        }
    }

    static class MockCreateAccountHandler implements CommandHandler<CreateAccountCommand, AccountCreationResult> {
        @Override
        public Mono<AccountCreationResult> handle(CreateAccountCommand command) {
            return Mono.just(AccountCreationResult.builder()
                .accountId("ACC-" + UUID.randomUUID().toString())
                .accountNumber("1234567890")
                .customerId(command.getCustomerId())
                .accountType(command.getAccountType())
                .initialBalance(command.getInitialDeposit())
                .currency(command.getCurrency())
                .status("ACTIVE")
                .build())
                .delayElement(Duration.ofMillis(300));
        }

        @Override
        public Class<CreateAccountCommand> getCommandType() {
            return CreateAccountCommand.class;
        }
    }

    static class MockGetCustomerProfileHandler implements QueryHandler<GetCustomerProfileQuery, CustomerProfile> {
        @Override
        public Mono<CustomerProfile> handle(GetCustomerProfileQuery query) {
            return Mono.just(CustomerProfile.builder()
                .customerId(query.getCustomerId())
                .firstName("Alice")
                .lastName("Smith")
                .email("alice.smith@example.com")
                .phoneNumber("+1-555-111-2222")
                .status("ACTIVE")
                .build())
                .delayElement(Duration.ofMillis(50));
        }

        @Override
        public Class<GetCustomerProfileQuery> getQueryType() {
            return GetCustomerProfileQuery.class;
        }
    }
}
