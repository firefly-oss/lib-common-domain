# Package Structure Guide

Best-in-class standardized package structure for **Core-Domain Layer microservices** using the **Firefly Consolidated CQRS Framework**.

## 🏗️ Microservice Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     MICROSERVICE LAYERS                     │
├─────────────────────────────────────────────────────────────┤
│  xxx-web        │ Controllers, REST endpoints               │
│  xxx-interfaces │ DTOs, API contracts, external interfaces  │
│  xxx-core       │ Business logic, CQRS handlers, domain     │
│  xxx-infra      │ External integrations, configurations     │
└─────────────────────────────────────────────────────────────┘
```

## 📁 Core-Domain Layer Package Structure

### xxx-core Module (Business Logic & Consolidated CQRS)

```
com.company.banking.core/
├── domain/
│   ├── customer/
│   │   ├── Customer.java                # Domain model (internal)
│   │   ├── CustomerId.java              # Value object
│   │   └── CustomerService.java         # Domain service (business logic)
│   ├── account/
│   │   ├── Account.java                 # Domain model (internal)
│   │   ├── AccountNumber.java           # Value object
│   │   └── AccountService.java          # Domain service (business logic)
│   ├── transaction/
│   │   ├── Transaction.java             # Domain model (internal)
│   │   ├── TransactionId.java           # Value object
│   │   └── TransactionService.java      # Domain service (business logic)
│   └── shared/
│       ├── Money.java                   # Shared value object
│       ├── Currency.java                # Shared value object
│       └── AuditInfo.java               # Shared value object
├── application/
│   ├── customer/
│   │   ├── command/
│   │   │   ├── CreateCustomerCommand.java       # Command (simple POJO)
│   │   │   ├── CreateCustomerResult.java        # Result (simple POJO)
│   │   │   ├── CreateCustomerHandler.java       # THE ONLY WAY - extends CommandHandler
│   │   │   ├── UpdateCustomerCommand.java
│   │   │   ├── UpdateCustomerResult.java
│   │   │   └── UpdateCustomerHandler.java
│   │   ├── query/
│   │   │   ├── GetCustomerQuery.java            # Query (simple POJO)
│   │   │   ├── CustomerDetails.java             # Result (simple POJO)
│   │   │   ├── GetCustomerHandler.java          # THE ONLY WAY - extends QueryHandler
│   │   │   ├── SearchCustomersQuery.java
│   │   │   ├── CustomerSearchResult.java
│   │   │   └── SearchCustomersHandler.java
│   │   └── handler/
│   │       ├── CreateCustomerHandler.java       # @CommandHandlerComponent
│   │       ├── UpdateCustomerHandler.java       # @CommandHandlerComponent
│   │       ├── GetCustomerHandler.java          # @QueryHandlerComponent
│   │       └── SearchCustomersHandler.java      # @QueryHandlerComponent
│   ├── account/
│   │   ├── command/
│   │   │   ├── OpenAccountCommand.java
│   │   │   ├── CloseAccountCommand.java
│   │   │   └── TransferMoneyCommand.java
│   │   ├── query/
│   │   │   ├── GetAccountBalanceQuery.java
│   │   │   ├── GetAccountHistoryQuery.java
│   │   │   └── GetAccountSummaryQuery.java
│   │   └── handler/
│   │       ├── OpenAccountHandler.java          # @CommandHandlerComponent
│   │       ├── TransferMoneyHandler.java        # @CommandHandlerComponent
│   │       ├── GetAccountBalanceHandler.java    # @QueryHandlerComponent
│   │       └── GetAccountHistoryHandler.java    # @QueryHandlerComponent
└── config/
    ├── CqrsConfiguration.java                   # CQRS framework configuration
    └── DomainConfiguration.java                 # Domain services configuration
```

### xxx-interfaces Module (DTOs & API Contracts)

```
com.company.banking.interfaces/
├── dto/
│   ├── request/
│   │   ├── CreateCustomerRequest.java           # API request DTOs
│   │   ├── UpdateCustomerRequest.java
│   │   ├── OpenAccountRequest.java
│   │   └── TransferMoneyRequest.java
│   ├── response/
│   │   ├── CustomerResponse.java                # API response DTOs
│   │   ├── AccountResponse.java
│   │   ├── TransferResponse.java
│   │   └── AccountBalanceResponse.java
│   └── shared/
│       ├── MoneyDto.java                        # Shared DTOs
│       ├── AddressDto.java
│       └── ContactInfoDto.java
├── contract/
│   ├── CustomerServiceContract.java             # Service interface contracts
│   ├── AccountServiceContract.java
│   └── TransactionServiceContract.java
└── mapper/
    ├── CustomerMapper.java                      # DTO-Domain mapping
    ├── AccountMapper.java
    └── TransactionMapper.java
```

### xxx-web Module (Controllers & REST API)

```
com.company.banking.web/
├── controller/
│   ├── CustomerController.java                  # REST API controllers
│   ├── AccountController.java
│   └── TransactionController.java
├── config/
│   ├── WebConfiguration.java                    # Web layer configuration
│   ├── SecurityConfiguration.java               # Security configuration
│   └── SwaggerConfiguration.java                # API documentation
├── exception/
│   ├── GlobalExceptionHandler.java              # Global error handling
│   └── ApiErrorResponse.java                    # Error response format
└── filter/
    ├── CorrelationIdFilter.java                 # Request correlation
    └── LoggingFilter.java                       # Request/response logging
```

### xxx-infra Module (External Integrations)

```
com.company.banking.infra/
├── client/
│   ├── CustomerDataServiceClient.java           # Calls data layer services
│   ├── AccountDataServiceClient.java            # Calls data layer services
│   ├── PaymentServiceClient.java                # External service integration
│   ├── NotificationServiceClient.java           # External service integration
│   └── KycServiceClient.java                    # External service integration
├── events/
│   ├── CustomerEventHandler.java                # Inbound domain event handlers
│   ├── AccountEventHandler.java                 # Inbound domain event handlers
│   └── TransactionEventHandler.java             # Inbound domain event handlers
└── config/
    ├── ServiceClientConfiguration.java          # Service client configuration
    ├── EventConfiguration.java                  # Event handling configuration
    └── InfrastructureConfiguration.java         # Infrastructure setup
## 🔄 Data Flow Between Layers
```
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   xxx-web       │───▶│ xxx-interfaces  │───▶│   xxx-core      │───▶│   xxx-infra     │
│                 │    │                 │    │                 │    │                 │
│ • Controllers   │    │ • DTOs          │    │ • Commands      │    │ • Service       │
│ • REST API      │    │ • Requests      │    │ • Queries       │    │   Clients       │
│ • Error         │    │ • Responses     │    │ • Handlers      │    │ • Event         │
│   Handling      │    │ • Mappers       │    │ • Domain Logic  │    │   Handlers      │
│                 │    │ • Contracts     │    │ • Business      │    │ • External      │
│                 │    │                 │    │   Services      │    │   Integration   │
└─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🎯 Key Design Principles for Core-Domain Layer

### **Separation of Concerns**
```
┌─────────────────────────────────────────────────────────────────┐
│                        RESPONSIBILITY MATRIX                    │
├─────────────────┬───────────────────────────────────────────────┤
│ xxx-web         │ HTTP handling, routing, authentication        │
│ xxx-interfaces  │ Data contracts, API definitions, mapping      │
│ xxx-core        │ Business logic, CQRS, domain services         │
│ xxx-infra       │ External integrations, service clients        │
└─────────────────┴───────────────────────────────────────────────┘
```

### **Core-Domain Layer Principles**
- ✅ Focus on business logic and orchestration, not data persistence
- ✅ Domain models represent business concepts for internal processing
- ✅ Domain services contain complex business logic and workflow orchestration
- ✅ No direct database access - communicate with data layer via service clients
- ✅ Use CQRS pattern with @CommandHandlerComponent and @QueryHandlerComponent annotations
- ✅ Leverage Jakarta validation for automatic input validation

## 📋 Best Practices & Examples

### **Command Handler Example (xxx-core)**
```java
// xxx-core/application/account/handler/OpenAccountHandler.java
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class OpenAccountHandler extends CommandHandler<OpenAccountCommand, AccountResult> {

    @Autowired
    private CustomerDataServiceClient customerClient;

    @Autowired
    private KycServiceClient kycClient;

    @Override
    protected Mono<AccountResult> doHandle(OpenAccountCommand command) {
        // Only business logic - everything else automatic!
        return validateCustomer(command.getCustomerId())
            .flatMap(customer -> performKycCheck(customer))
            .flatMap(kycResult -> openAccount(command))
            .flatMap(this::publishAccountOpenedEvent);
    }
}
```

### **DTO Mapping Example (xxx-interfaces)**
```java
// xxx-interfaces/dto/request/OpenAccountRequest.java
public class OpenAccountRequest {
    @NotBlank private String customerId;
    @NotBlank private String accountType;
    @NotNull @Min(0) private BigDecimal initialDeposit;
}

// xxx-interfaces/mapper/AccountMapper.java
@Mapper
public interface AccountMapper {
    OpenAccountCommand toCommand(OpenAccountRequest request);
    AccountResponse toResponse(AccountResult result);
}
```

### **Controller Example (xxx-web)**
```java
// xxx-web/controller/AccountController.java
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final CommandBus commandBus;
    private final AccountMapper mapper;

    @PostMapping
    public Mono<AccountResponse> openAccount(@Valid @RequestBody OpenAccountRequest request) {
        OpenAccountCommand command = mapper.toCommand(request);
        return commandBus.send(command)
            .map(mapper::toResponse);
    }
}
```

### **Service Client Example (xxx-infra)**
```java
// xxx-infra/client/CustomerDataServiceClient.java
@Component
public class CustomerDataServiceClient {

    private final ServiceClient serviceClient;

    public Mono<Customer> getCustomer(String customerId) {
        return serviceClient.get("/customers/{id}", Customer.class)
            .withPathParam("id", customerId)
            .execute();
    }
}
```

## ⚙️ Configuration Examples

### **Maven Dependencies (pom.xml)**
```xml
<dependencies>
    <!-- Firefly Common Domain Library -->
    <dependency>
        <groupId>com.firefly</groupId>
        <artifactId>lib-common-domain</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- Jakarta Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Firefly Commons Validators -->
    <dependency>
        <groupId>com.firefly</groupId>
        <artifactId>lib-commons-validators</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### **Application Configuration (application.yml)**
```yaml
# Note: MeterRegistry is auto-configured by default (SimpleMeterRegistry if none exists)
# No manual metrics configuration required!
firefly:
  cqrs:
    enabled: true
    command:
      timeout: 30s
      metrics-enabled: true  # Optional - metrics work by default
      tracing-enabled: true
      auto-validation: true
    query:
      timeout: 15s
      caching-enabled: true
      cache-ttl: 5m
      metrics-enabled: true  # Optional - metrics work by default
  service-clients:
    customer-data-service:
      type: REST
      base-url: http://customer-data-service:8080
    kyc-service:
      type: REST
      base-url: http://kyc-service:8080
  events:
    enabled: true
    publisher: kafka
```

## 🧪 Testing Strategy

### **Unit Testing Structure**
```
src/test/java/com/company/banking/core/
├── domain/
│   ├── CustomerTest.java                     # Domain model tests
│   ├── AccountTest.java
│   └── TransactionTest.java
├── application/
│   ├── CreateCustomerHandlerTest.java        # Handler unit tests
│   ├── OpenAccountHandlerTest.java
│   └── GetAccountBalanceHandlerTest.java
└── infrastructure/
    ├── CustomerDataServiceClientTest.java    # Service client tests
    └── KycServiceClientTest.java
```

### **Integration Testing**
```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
class AccountIntegrationTest {

    @Autowired private CommandBus commandBus;
    @Autowired private QueryBus queryBus;

    @Test
    void shouldOpenAccountSuccessfully() {
        // Test complete flow from command to result
        OpenAccountCommand command = OpenAccountCommand.builder()
            .customerId("CUST-123")
            .accountType("SAVINGS")
            .initialDeposit(new BigDecimal("1000.00"))
            .build();

        StepVerifier.create(commandBus.send(command))
            .assertNext(result -> {
                assertThat(result.getAccountId()).isNotNull();
                assertThat(result.getStatus()).isEqualTo("ACTIVE");
            })
            .verifyComplete();
    }
}
```

## 📚 Summary

This package structure provides:

- ✅ **Clear separation** between web, interfaces, core, and infrastructure
- ✅ **CQRS best practices** with zero-boilerplate handlers
- ✅ **Jakarta validation** integration for automatic input validation
- ✅ **Service client** patterns for external communication
- ✅ **Domain-driven design** principles for business logic organization
- ✅ **Testable architecture** with clear boundaries and dependencies

> **Remember**: The core-domain layer focuses on business logic and orchestration, not data persistence. Use service clients to communicate with data layer services.


