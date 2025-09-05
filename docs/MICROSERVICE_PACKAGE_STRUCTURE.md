# Microservice Package Structure for CQRS and Saga Implementation

This document provides a comprehensive guide for structuring a microservice that implements the Firefly Common Domain Library with CQRS patterns and Saga orchestration.

## Table of Contents

1. [Overview](#overview)
2. [Multimodule Maven Structure](#multimodule-maven-structure)
3. [Module-Specific Package Organization](#module-specific-package-organization)
4. [Dependency Management](#dependency-management)
5. [Configuration Structure](#configuration-structure)
6. [Testing Structure](#testing-structure)
7. [Implementation Examples](#implementation-examples)
8. [Best Practices](#best-practices)

## Overview

The proposed structure follows a clean architecture approach with clear separation of concerns across multiple Maven modules. This structure leverages the Firefly Common Domain Library's CQRS and Saga capabilities while maintaining modularity and testability.

### Architecture Principles

- **Separation of Concerns**: Each module has a single responsibility
- **Dependency Direction**: Dependencies flow inward (web → core → interfaces)
- **CQRS Implementation**: Commands and queries are properly separated
- **Saga Orchestration**: Business workflows are managed through saga patterns
- **Domain-Driven Design**: Business logic is centralized in the core module

## Multimodule Maven Structure

```
banking-service/
├── pom.xml                                   # Parent POM
├── banking-interfaces/                       # DTOs, Enums, Contracts
├── banking-core/                             # Business Logic, CQRS, Sagas
├── banking-sdk/                              # OpenAPI Generated SDK
├── banking-web/                              # Controllers, REST APIs
└── banking-integration-tests/                # End-to-End Tests
```

### Parent POM Structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.company.banking</groupId>
    <artifactId>banking-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>banking-interfaces</module>
        <module>banking-core</module>
        <module>banking-sdk</module>
        <module>banking-web</module>
        <module>banking-integration-tests</module>
    </modules>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <spring.boot.version>3.2.0</spring.boot.version>
        <firefly.version>1.0.0-SNAPSHOT</firefly.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring.boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <!-- Firefly Common Domain Library -->
            <dependency>
                <groupId>com.firefly</groupId>
                <artifactId>lib-common-domain</artifactId>
                <version>${firefly.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

## Module-Specific Package Organization

### 1. banking-interfaces Module

**Purpose**: Contains DTOs, enums, and contracts shared across modules.

```
banking-interfaces/
├── pom.xml
└── src/main/java/com/company/banking/interfaces/
    ├── dto/                                  # Data Transfer Objects
    │   ├── request/                         # Request DTOs
    │   │   ├── CreateAccountRequest.java
    │   │   ├── CustomerRegistrationRequest.java
    │   │   └── TransferMoneyRequest.java
    │   ├── response/                        # Response DTOs
    │   │   ├── AccountResponse.java
    │   │   ├── CustomerResponse.java
    │   │   └── TransactionResponse.java
    │   └── internal/                        # Internal DTOs (between modules)
    │       ├── CustomerValidationResult.java
    │       ├── AccountCreationResult.java
    │       └── KycVerificationResult.java
    ├── enums/                               # Enumeration types
    │   ├── AccountType.java
    │   ├── TransactionType.java
    │   ├── CustomerStatus.java
    │   └── KycStatus.java
    ├── contracts/                           # API contracts
    │   ├── CustomerApi.java
    │   ├── AccountApi.java
    │   └── TransactionApi.java
    └── validation/                          # Validation annotations
        ├── ValidAccountType.java
        ├── ValidCustomerId.java
        └── ValidAmount.java
```

**POM Dependencies**: Minimal - only validation and serialization libraries.

```xml
<dependencies>
    <dependency>
        <groupId>jakarta.validation</groupId>
        <artifactId>jakarta.validation-api</artifactId>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-annotations</artifactId>
    </dependency>
</dependencies>
```

### 2. banking-core Module

**Purpose**: Contains all business logic, CQRS components, and Saga orchestrators.

```
banking-core/
├── pom.xml
└── src/main/java/com/company/banking/core/
    ├── domain/                              # Domain Models
    │   ├── model/                          # Domain Entities
    │   │   ├── Customer.java
    │   │   ├── Account.java
    │   │   └── Transaction.java
    │   ├── repository/                     # Repository Interfaces
    │   │   ├── CustomerRepository.java
    │   │   ├── AccountRepository.java
    │   │   └── TransactionRepository.java
    │   └── service/                        # Domain Services
    │       ├── CustomerDomainService.java
    │       ├── AccountDomainService.java
    │       └── TransactionDomainService.java
    ├── cqrs/                               # CQRS Implementation
    │   ├── command/                        # Commands
    │   │   ├── customer/
    │   │   │   ├── CreateCustomerCommand.java
    │   │   │   ├── UpdateCustomerCommand.java
    │   │   │   └── ActivateCustomerCommand.java
    │   │   ├── account/
    │   │   │   ├── CreateAccountCommand.java
    │   │   │   ├── DebitAccountCommand.java
    │   │   │   └── CreditAccountCommand.java
    │   │   └── transaction/
    │   │       ├── ProcessTransactionCommand.java
    │   │       └── ReverseTransactionCommand.java
    │   ├── query/                          # Queries
    │   │   ├── customer/
    │   │   │   ├── GetCustomerQuery.java
    │   │   │   ├── FindCustomersByStatusQuery.java
    │   │   │   └── ValidateCustomerQuery.java
    │   │   ├── account/
    │   │   │   ├── GetAccountQuery.java
    │   │   │   ├── GetAccountBalanceQuery.java
    │   │   │   └── GetAccountsByCustomerQuery.java
    │   │   └── transaction/
    │   │       ├── GetTransactionQuery.java
    │   │       └── GetTransactionHistoryQuery.java
    │   └── handler/                        # CQRS Handlers
    │       ├── command/                    # Command Handlers
    │       │   ├── customer/
    │       │   │   ├── CreateCustomerHandler.java
    │       │   │   ├── UpdateCustomerHandler.java
    │       │   │   └── ActivateCustomerHandler.java
    │       │   ├── account/
    │       │   │   ├── CreateAccountHandler.java
    │       │   │   ├── DebitAccountHandler.java
    │       │   │   └── CreditAccountHandler.java
    │       │   └── transaction/
    │       │       ├── ProcessTransactionHandler.java
    │       │       └── ReverseTransactionHandler.java
    │       └── query/                      # Query Handlers
    │           ├── customer/
    │           │   ├── GetCustomerHandler.java
    │           │   ├── FindCustomersByStatusHandler.java
    │           │   └── ValidateCustomerHandler.java
    │           ├── account/
    │           │   ├── GetAccountHandler.java
    │           │   ├── GetAccountBalanceHandler.java
    │           │   └── GetAccountsByCustomerHandler.java
    │           └── transaction/
    │               ├── GetTransactionHandler.java
    │               └── GetTransactionHistoryHandler.java
    ├── saga/                               # Saga Orchestrators
    │   ├── CustomerOnboardingSaga.java
    │   ├── MoneyTransferSaga.java
    │   ├── AccountClosureSaga.java
    │   └── compensation/                   # Compensation Logic
    │       ├── CustomerCompensationHandler.java
    │       ├── AccountCompensationHandler.java
    │       └── TransactionCompensationHandler.java
    ├── service/                            # Application Services
    │   ├── CustomerService.java
    │   ├── AccountService.java
    │   ├── TransactionService.java
    │   └── NotificationService.java
    ├── mapper/                             # Object Mappers
    │   ├── CustomerMapper.java
    │   ├── AccountMapper.java
    │   └── TransactionMapper.java
    ├── client/                             # External Service Clients
    │   ├── KycServiceClient.java
    │   ├── NotificationServiceClient.java
    │   └── CoreBankingServiceClient.java
    ├── event/                              # Domain Event Handlers
    │   ├── listener/
    │   │   ├── CustomerEventListener.java
    │   │   ├── AccountEventListener.java
    │   │   └── TransactionEventListener.java
    │   └── publisher/
    │       ├── CustomerEventPublisher.java
    │       ├── AccountEventPublisher.java
    │       └── TransactionEventPublisher.java
    ├── config/                             # Configuration Classes
    │   ├── BankingCoreConfiguration.java
    │   ├── CqrsConfiguration.java
    │   ├── SagaConfiguration.java
    │   └── ClientConfiguration.java
    └── exception/                          # Custom Exceptions
        ├── CustomerNotFoundException.java
        ├── InsufficientFundsException.java
        ├── AccountNotFoundException.java
        └── ValidationException.java
```

**POM Dependencies**: Core business dependencies and Firefly libraries.

```xml
<dependencies>
    <!-- Internal Dependencies -->
    <dependency>
        <groupId>com.company.banking</groupId>
        <artifactId>banking-interfaces</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- Firefly Dependencies -->
    <dependency>
        <groupId>com.firefly</groupId>
        <artifactId>lib-common-domain</artifactId>
    </dependency>
    <dependency>
        <groupId>com.firefly</groupId>
        <artifactId>lib-transactional-engine</artifactId>
    </dependency>
    
    <!-- Spring Dependencies -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    
    <!-- Mappers -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
    </dependency>
</dependencies>
```

### 3. banking-sdk Module

**Purpose**: Contains OpenAPI generated SDK for external consumers.

```
banking-sdk/
├── pom.xml
├── src/
│   └── main/
│       ├── resources/
│       │   └── openapi/
│       │       └── banking-api.yaml        # OpenAPI Specification
│       └── java/com/company/banking/sdk/   # Generated Code (by plugin)
│           ├── api/
│           │   ├── CustomerApi.java
│           │   ├── AccountApi.java
│           │   └── TransactionApi.java
│           ├── model/
│           │   ├── CreateAccountRequest.java
│           │   ├── CustomerResponse.java
│           │   └── TransactionResponse.java
│           └── client/
│               └── BankingApiClient.java
└── target/generated-sources/              # Generated by OpenAPI plugin
```

**POM with OpenAPI Generation**:

```xml
<dependencies>
    <dependency>
        <groupId>com.company.banking</groupId>
        <artifactId>banking-interfaces</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.openapitools</groupId>
            <artifactId>openapi-generator-maven-plugin</artifactId>
            <version>7.0.1</version>
            <executions>
                <execution>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                    <configuration>
                        <inputSpec>${project.basedir}/src/main/resources/openapi/banking-api.yaml</inputSpec>
                        <generatorName>spring</generatorName>
                        <configOptions>
                            <sourceFolder>src/gen/java/main</sourceFolder>
                            <basePackage>com.company.banking.sdk</basePackage>
                            <configPackage>com.company.banking.sdk.config</configPackage>
                            <apiPackage>com.company.banking.sdk.api</apiPackage>
                            <modelPackage>com.company.banking.sdk.model</modelPackage>
                            <reactive>true</reactive>
                            <interfaceOnly>true</interfaceOnly>
                        </configOptions>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 4. banking-web Module

**Purpose**: Contains lightweight controllers that delegate to core services.

```
banking-web/
├── pom.xml
└── src/main/java/com/company/banking/web/
    ├── controller/                         # REST Controllers
    │   ├── CustomerController.java
    │   ├── AccountController.java
    │   └── TransactionController.java
    ├── filter/                            # Web Filters
    │   ├── CorrelationIdFilter.java
    │   └── RequestLoggingFilter.java
    ├── exception/                         # Exception Handlers
    │   ├── GlobalExceptionHandler.java
    │   └── ValidationExceptionHandler.java
    ├── config/                            # Web Configuration
    │   ├── WebConfiguration.java
    │   ├── SecurityConfiguration.java
    │   └── OpenApiConfiguration.java
    └── BankingServiceApplication.java     # Main Application Class
```

**Controller Example**:

```java
@RestController
@RequestMapping("/api/v1/customers")
@Validated
@Slf4j
public class CustomerController {
    
    private final CustomerService customerService;
    private final CustomerMapper customerMapper;
    
    public CustomerController(CustomerService customerService, CustomerMapper customerMapper) {
        this.customerService = customerService;
        this.customerMapper = customerMapper;
    }
    
    @PostMapping
    public Mono<ResponseEntity<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        log.info("Creating customer with correlation ID: {}", correlationId);
        
        return customerService.createCustomer(request, correlationId)
            .map(customerMapper::toResponse)
            .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
            .doOnSuccess(response -> log.info("Customer created successfully"))
            .doOnError(error -> log.error("Failed to create customer", error));
    }
}
```

**POM Dependencies**:

```xml
<dependencies>
    <!-- Internal Dependencies -->
    <dependency>
        <groupId>com.company.banking</groupId>
        <artifactId>banking-interfaces</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>com.company.banking</groupId>
        <artifactId>banking-core</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- Spring Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- Documentation -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
    </dependency>
</dependencies>
```

### 5. banking-integration-tests Module

**Purpose**: Contains end-to-end integration tests.

```
banking-integration-tests/
├── pom.xml
└── src/test/java/com/company/banking/integration/
    ├── saga/                              # Saga Integration Tests
    │   ├── CustomerOnboardingSagaTest.java
    │   ├── MoneyTransferSagaTest.java
    │   └── AccountClosureSagaTest.java
    ├── api/                               # API Integration Tests
    │   ├── CustomerApiTest.java
    │   ├── AccountApiTest.java
    │   └── TransactionApiTest.java
    ├── cqrs/                              # CQRS Integration Tests
    │   ├── CustomerCqrsTest.java
    │   ├── AccountCqrsTest.java
    │   └── TransactionCqrsTest.java
    ├── event/                             # Event Integration Tests
    │   ├── DomainEventTest.java
    │   └── EventPublishingTest.java
    └── config/                            # Test Configuration
        ├── TestConfiguration.java
        └── TestContainersConfiguration.java
```

## Dependency Management

### Module Dependency Flow

```
banking-web
    ↓ (depends on)
banking-core
    ↓ (depends on)
banking-interfaces

banking-sdk
    ↓ (depends on)
banking-interfaces

banking-integration-tests
    ↓ (depends on)
banking-web + banking-core + banking-interfaces
```

### Key Dependency Rules

1. **interfaces** module has minimal external dependencies
2. **core** module contains all business logic dependencies
3. **web** module only depends on Spring Web and core
4. **sdk** module is independently deployable
5. **integration-tests** can depend on all modules for testing

## Configuration Structure

### Application Configuration Organization

```yaml
# banking-web/src/main/resources/application.yml
spring:
  application:
    name: banking-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

# Firefly CQRS Configuration
firefly:
  cqrs:
    enabled: true
    command:
      timeout: 30s
      metrics-enabled: true
    query:
      timeout: 15s
      caching-enabled: true
      cache-ttl: 15m
  
  # Domain Events Configuration
  events:
    enabled: true
    adapter: ${EVENTS_ADAPTER:kafka}
    kafka:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
  
  # Saga Configuration
  stepevents:
    enabled: true

# Custom Banking Configuration
banking:
  customer:
    kyc:
      timeout: 60s
      retry-attempts: 3
  account:
    initial-balance-limit: 50000
  transaction:
    daily-limit: 100000
    
# External Services
external:
  services:
    kyc-service:
      base-url: ${KYC_SERVICE_URL:http://kyc-service:8080}
      timeout: 30s
    core-banking:
      base-url: ${CORE_BANKING_URL:http://core-banking:8080}
      timeout: 45s
```

### Environment-Specific Configuration

Create profile-specific configurations:

- `application-local.yml` - Local development
- `application-dev.yml` - Development environment  
- `application-staging.yml` - Staging environment
- `application-prod.yml` - Production environment

## Testing Structure

### Unit Test Organization

```
Module/src/test/java/com/company/banking/{module}/
├── cqrs/
│   ├── command/
│   │   └── handler/                       # Command Handler Tests
│   └── query/
│       └── handler/                       # Query Handler Tests
├── saga/                                  # Saga Unit Tests
├── service/                               # Service Unit Tests
├── mapper/                                # Mapper Tests
└── util/                                  # Test Utilities
```

### Test Configuration Examples

```java
// CustomerService Test
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    
    @Mock
    private CommandBus commandBus;
    
    @Mock
    private QueryBus queryBus;
    
    @InjectMocks
    private CustomerService customerService;
    
    @Test
    void shouldCreateCustomerSuccessfully() {
        // Given
        CreateCustomerRequest request = createValidRequest();
        CustomerResult expectedResult = createExpectedResult();
        
        when(commandBus.send(any(CreateCustomerCommand.class)))
            .thenReturn(Mono.just(expectedResult));
        
        // When
        Mono<CustomerResult> result = customerService.createCustomer(request, "correlation-123");
        
        // Then
        StepVerifier.create(result)
            .expectNext(expectedResult)
            .verifyComplete();
    }
}
```

## Implementation Examples

### CQRS Command Example

```java
// Customer Creation Command
@Data
@Builder
public class CreateCustomerCommand implements Command<CustomerResult> {
    private final String customerId;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phoneNumber;
    private final String correlationId;
    
    @Override
    public Mono<ValidationResult> validate() {
        ValidationResult.Builder builder = ValidationResult.builder();
        
        if (customerId == null || customerId.trim().isEmpty()) {
            builder.addError("customerId", "Customer ID is required");
        }
        
        if (email == null || !isValidEmail(email)) {
            builder.addError("email", "Valid email is required");
        }
        
        return Mono.just(builder.build());
    }
    
    @Override
    public String getCorrelationId() {
        return correlationId;
    }
    
    @Override
    public Class<CustomerResult> getResultType() {
        return CustomerResult.class;
    }
    
    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }
}
```

### Saga Implementation Example

```java
// Customer Onboarding Saga
@Component
@Saga(name = "customer-onboarding")
@Slf4j
public class CustomerOnboardingSaga {
    
    private final CommandBus commandBus;
    private final QueryBus queryBus;
    
    @SagaStep(id = "validate-customer", retry = 3)
    public Mono<ValidationResult> validateCustomer(@Input CustomerOnboardingRequest request) {
        ValidateCustomerQuery query = ValidateCustomerQuery.builder()
            .email(request.getEmail())
            .phoneNumber(request.getPhoneNumber())
            .correlationId(request.getCorrelationId())
            .build();
            
        return queryBus.query(query);
    }
    
    @SagaStep(id = "create-customer", 
              dependsOn = "validate-customer",
              compensate = "deleteCustomer",
              timeoutMs = 30000)
    public Mono<CustomerResult> createCustomer(
            @FromStep("validate-customer") ValidationResult validation,
            @Input CustomerOnboardingRequest request) {
        
        if (!validation.isValid()) {
            return Mono.error(new ValidationException(validation.getErrors()));
        }
        
        CreateCustomerCommand command = CreateCustomerCommand.builder()
            .customerId(request.getCustomerId())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .phoneNumber(request.getPhoneNumber())
            .correlationId(request.getCorrelationId())
            .build();
            
        return commandBus.send(command);
    }
    
    // Compensation method
    public Mono<Void> deleteCustomer(@FromStep("create-customer") CustomerResult customer) {
        log.warn("COMPENSATING: Deleting customer {}", customer.getCustomerId());
        
        DeleteCustomerCommand command = DeleteCustomerCommand.builder()
            .customerId(customer.getCustomerId())
            .reason("Saga compensation")
            .build();
            
        return commandBus.send(command).then();
    }
}
```

### Service Implementation Example

```java
// Customer Service
@Service
@Slf4j
public class CustomerService {
    
    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final SagaEngine sagaEngine;
    private final CustomerMapper mapper;
    
    public Mono<CustomerResponse> createCustomer(CreateCustomerRequest request, String correlationId) {
        log.info("Creating customer with correlation ID: {}", correlationId);
        
        // For simple operations, use CQRS directly
        CreateCustomerCommand command = CreateCustomerCommand.builder()
            .customerId(UUID.randomUUID().toString())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .phoneNumber(request.getPhoneNumber())
            .correlationId(correlationId)
            .build();
            
        return commandBus.send(command)
            .map(mapper::toResponse);
    }
    
    public Mono<CustomerOnboardingResponse> startCustomerOnboarding(
            CustomerOnboardingRequest request, String correlationId) {
        log.info("Starting customer onboarding saga with correlation ID: {}", correlationId);
        
        // For complex workflows, use Saga
        StepInputs inputs = StepInputs.of("validate-customer", request);
        
        return sagaEngine.execute("customer-onboarding", inputs)
            .map(sagaResult -> CustomerOnboardingResponse.builder()
                .onboardingId(sagaResult.getSagaId())
                .status(sagaResult.isSuccess() ? "COMPLETED" : "FAILED")
                .completedSteps(sagaResult.steps().keySet())
                .build());
    }
}
```

## Best Practices

### 1. Package Organization Guidelines

**CQRS Structure**:
- Organize commands and queries by business domain (customer, account, transaction)
- Keep handlers close to their respective commands/queries
- Use consistent naming conventions (CreateCustomerCommand → CreateCustomerHandler)

**Saga Organization**:
- One saga per complex business workflow
- Group related compensation methods with their saga
- Use clear step naming that reflects business operations

### 2. Dependency Management

**Module Dependencies**:
- Keep the interfaces module lightweight with minimal dependencies
- Centralize business logic in the core module
- Make controllers in web module thin - delegate to services

**External Dependencies**:
- Use dependency management in parent POM for version consistency
- Avoid transitive dependency conflicts between modules

### 3. Configuration Management

**Environment Configuration**:
- Use Spring profiles for environment-specific configuration
- Externalize environment-specific values using environment variables
- Keep default values suitable for local development

**Service Configuration**:
- Group related configuration properties in custom configuration classes
- Use validation annotations on configuration properties
- Document configuration properties and their purpose

### 4. Testing Strategy

**Unit Testing**:
- Test command/query handlers independently using mocks
- Test saga steps individually with proper input/output verification
- Use StepVerifier for reactive code testing

**Integration Testing**:
- Test complete saga flows with real CQRS bus implementations
- Test API endpoints end-to-end including validation
- Use TestContainers for external service dependencies

**Performance Testing**:
- Load test saga execution under concurrent scenarios
- Monitor command/query processing times
- Test timeout and retry behaviors

### 5. Error Handling and Monitoring

**Exception Management**:
- Use domain-specific exceptions for business rule violations
- Implement global exception handlers in the web layer
- Log errors with proper correlation IDs for distributed tracing

**Observability**:
- Include correlation IDs in all log messages
- Monitor saga execution metrics (success rate, execution time)
- Set up alerts for compensation executions and failures

### 6. Security Considerations

**API Security**:
- Implement proper authentication and authorization
- Validate all input at the controller level
- Use HTTPS for all external communications

**Data Protection**:
- Encrypt sensitive data in commands and events
- Implement proper data masking in logs
- Follow data retention policies for saga state

---

This package structure provides a solid foundation for implementing microservices using the Firefly Common Domain Library's CQRS and Saga patterns while maintaining clean architecture principles and separation of concerns.