# 🏦 Firefly OpenCore Banking Platform
## Common Domain Library

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.org/)

**A comprehensive Spring Boot 3 library providing essential domain layer utilities and configurations for the Firefly OpenCore Banking Platform.**

This library serves as the foundational domain layer component, offering a complete set of utilities, annotations, and configurations that enable robust business logic implementation across all banking microservices in the platform.

---

## 🚀 Quick Start

Add the dependency to your Spring Boot 3 microservice:

```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**That's it!** Auto-configuration handles the rest.

---

## ✨ Core Features

| Feature | Description | Use Case |
|---------|-------------|----------|
| 🏷️ **Domain Annotations** | CQRS pattern markers | Distinguish commands from queries |
| ✅ **Business Validation** | Fluent rule validation | Input validation with error collection |
| 🔄 **Service Results** | Functional error handling | Graceful failure management |
| 📝 **Domain Events** | Immutable event records | Business event representation |
| 📊 **JSON Logging** | Structured logging | Production-ready observability |

---

## 🏷️ Domain Annotations

**Enforce CQRS patterns with clear semantic markers.**

### @CommandQuery - State-Changing Operations
```java
@CommandQuery("Creates a new customer account")
public class CreateCustomerCommand {
    public void execute(CreateCustomerRequest request) {
        // Business logic that modifies state
        // Must return void, throw exceptions on failure
    }
}
```

### @ViewQuery - Read-Only Operations
```java
@ViewQuery("Retrieves customer account details")
public class GetCustomerAccountQuery {
    public CustomerAccountDto execute(String customerId) {
        // Read-only logic, never modifies state
        return new CustomerAccountDto(...);
    }
}
```

## ✅ Business Validation

**Fluent, expressive validation with comprehensive error collection.**

```java
// Banking-specific validation example
new BusinessValidator()
    .notBlank(accountNumber, "Account number required")
    .matches(accountNumber, "^[0-9]{10,12}$", "Invalid account number format")
    .rule(amount.compareTo(BigDecimal.ZERO) > 0, "Amount must be positive")
    .rule(amount.compareTo(dailyLimit) <= 0, "Amount exceeds daily limit")
    .validate(); // Throws BusinessValidationException if any rule fails

// Built-in validation methods
new BusinessValidator()
    .notNull(customer, "Customer required")
    .notBlank(customer.getEmail(), "Email required")
    .range(customer.getAge(), 18, 120, "Age must be between 18 and 120")
    .minLength(password, 8, "Password too short")
    .validate();

// Conditional validation
new BusinessValidator()
    .when(customer.isVip(), () -> new BusinessValidator()
        .notNull(customer.getVipCode(), "VIP code required"))
    .validate();

// Check errors without throwing
BusinessValidator validator = new BusinessValidator()
    .rule(condition, "Error message");
    
if (validator.hasErrors()) {
    List<String> errors = validator.getErrors();
    // Handle validation errors gracefully
}
```

## 🔄 Service Results

**Functional error handling for operations that might fail gracefully.**

### When to Use ServiceResult

| ✅ **Use ServiceResult** | ❌ **Don't Use ServiceResult** |
|-------------------------|-------------------------------|
| External payment gateway calls | Input validation (use BusinessValidator) |
| Credit bureau API lookups | Programming errors (NPE, etc.) |
| Database operations that might not find data | Configuration errors |
| File operations that might fail | Operations that should always succeed |

### Basic Usage

```java
// Banking service example
public ServiceResult<Account> findAccount(String accountNumber) {
    return ServiceResult.of(() -> accountRepository.findByNumber(accountNumber)
        .orElseThrow(() -> new RuntimeException("Account not found")));
}

// Using ServiceResults with fallback
ServiceResult<Account> result = accountService.findAccount(accountNumber);
if (result.isSuccess()) {
    Account account = result.getData().orElseThrow();
    // Process account
} else {
    log.warn("Account lookup failed: {}", result.getError().orElse("Unknown"));
    // Handle gracefully
}

// Provide fallback values
Account account = accountService.findAccount(accountNumber)
    .orElse(Account.createTemporary());

// Transform successful results
ServiceResult<BigDecimal> balanceResult = accountService.findAccount(accountNumber)
    .map(Account::getBalance);
```

### 🔗 Saga Integration

For complex distributed banking workflows, ServiceResult integrates seamlessly with Saga orchestration. 

**📖 [Complete Integration Tutorial](./SAGA_INTEGRATION.md)**

Learn step-by-step how to combine ServiceResult with Saga patterns for:
- Payment processing with fallback strategies
- Account operations with graceful degradation  
- Robust compensation handling
- Error recovery patterns

## 📝 Domain Events

**Immutable event records for business logic and audit trails.**

```java
// Banking domain events
DomainEvent accountCreated = new DomainEvent("AccountCreated", 
    new AccountCreatedPayload(accountId, customerId, accountType));

DomainEvent transactionProcessed = new DomainEvent("TransactionProcessed", 
    Map.of("transactionId", txnId, "amount", amount, "accountId", accountId));

// Access event metadata
String eventId = accountCreated.eventId();        // Auto-generated UUID
String eventType = accountCreated.eventType();    // "AccountCreated"
Instant occurredAt = accountCreated.occurredAt(); // Auto-generated timestamp
Object payload = accountCreated.payload();        // Your event data
```

## 📊 JSON Logging

**Production-ready structured logging with zero configuration.**

```json
{
  "@timestamp": "2024-01-15T10:30:45.123+00:00",
  "level": "INFO",
  "logger": "com.firefly.banking.AccountService",
  "message": "Account created successfully",
  "thread": "http-nio-8080-exec-1",
  "service": "account-service"
}
```

**✨ Zero configuration required** - JSON logging is automatically enabled when the library is on the classpath.

---

## 🏗️ Architecture & Design

**This library is designed for the core domain layer of banking microservices.**

### ✅ What This Library Provides
- Business logic validation and rules
- Service result transformations and error handling
- Domain event representation
- CQRS pattern enforcement with annotations
- Structured logging configuration

### ❌ What This Library Does NOT Handle
- Database operations (use separate data/repository layers)
- HTTP clients or external service calls (use dedicated client libraries)
- Workflow orchestration (use Saga libraries at higher layers)
- Infrastructure concerns (use separate infrastructure modules)

---

## 📦 Package Structure

```
com.catalis.common.domain/
├── annotation/                    # 🏷️ CQRS domain annotations
│   ├── CommandQuery              # State-changing operations
│   └── ViewQuery                 # Read-only operations
├── util/                         # 🔧 Core domain utilities
│   ├── BusinessValidator         # Fluent validation
│   ├── BusinessValidationException # Validation failures
│   ├── ServiceResult            # Functional error handling
│   └── DomainEvent             # Immutable business events
└── config/                      # ⚙️ Auto-configuration
    └── JsonLoggingAutoConfiguration # Structured logging setup
```

---

## 📋 Best Practices

| Component | Use For | Example |
|-----------|---------|----------|
| `@CommandQuery` | State-changing operations | Account creation, transaction processing |
| `@ViewQuery` | Read-only operations | Account lookups, balance inquiries |
| `BusinessValidator` | Input validation | Account number format, amount limits |
| `ServiceResult` | Operations that might fail | External API calls, optional lookups |
| `DomainEvent` | Business occurrences | Account created, transaction completed |

---

## 📄 License

**Apache License 2.0**

Copyright 2024 Firefly OpenCore Banking Platform

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

---

## 🤝 Contributing

This library is part of the **Firefly OpenCore Banking Platform** - a comprehensive set of common configurations and utilities for the domain layer across all banking microservices.

For contribution guidelines, please refer to the main platform documentation.

---

**🏦 Firefly OpenCore Banking Platform** | **📚 [Documentation](./SAGA_INTEGRATION.md)** | **📄 [Apache 2.0 License](LICENSE)**

---

## 📌 Annotation Placement: Class vs Method

Both @CommandQuery and @ViewQuery can be applied at the class level or directly on the entrypoint method. Choose the style that best fits your code organization and scanning strategy.

- Class-level (recommended for handlers/services):
```java
@CommandQuery("Creates a new customer account")
public class CreateCustomerCommand {
    public void execute(CreateCustomerRequest request) { /* ... */ }
}
```

- Method-level (useful when a class contains multiple operations):
```java
public class AccountService {
    @CommandQuery("Creates a new customer account")
    public void createAccount(CreateCustomerRequest request) { /* ... */ }

    @ViewQuery("Retrieves account details")
    public CustomerAccountDto getAccount(String accountId) { /* ... */ }
}
```

Notes:
- Semantics are identical regardless of placement: commands are state-changing and should return void; queries are read-only and return DTOs/records.
- If you rely on component scanning that expects annotations on types, prefer class-level placement. If your tooling scans methods, method-level works equally well.
- Annotations are @Inherited for class hierarchies; method-level annotations are not inherited by overrides in Java.
