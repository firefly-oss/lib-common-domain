# Migration Guide: lib-common-domain v2.0

## Overview

Version 2.0 introduces significant architectural changes to improve modularity and separation of concerns:

1. **CQRS Extraction**: CQRS functionality moved to `lib-common-cqrs`
2. **Event Infrastructure Migration**: Event publishing/consumption moved to `lib-common-eda`
3. **SAGA Integration**: Simplified to focus on bridging step events to EDA infrastructure

This separation provides:
- **Better modularity**: Use CQRS, events, and SAGA independently
- **Clearer responsibilities**: Each library has a focused scope
- **Improved maintainability**: Easier to maintain and extend
- **Easier testing**: Test each concern separately

## What Changed

### 1. CQRS Migration (to lib-common-cqrs)

CQRS classes have moved from `com.firefly.common.domain.*` to `com.firefly.common.cqrs.*`:

| Old Package | New Package |
|------------|-------------|
| `com.firefly.common.domain.command.*` | `com.firefly.common.cqrs.command.*` |
| `com.firefly.common.domain.query.*` | `com.firefly.common.cqrs.query.*` |
| `com.firefly.common.domain.authorization.*` | `com.firefly.common.cqrs.authorization.*` |
| `com.firefly.common.domain.validation.*` | `com.firefly.common.cqrs.validation.*` |
| `com.firefly.common.domain.annotations.*` | `com.firefly.common.cqrs.annotations.*` |
| `com.firefly.common.domain.config.Cqrs*` | `com.firefly.common.cqrs.config.*` |

### 2. Event Infrastructure Migration (to lib-common-eda)

All event publishing and consumption functionality has been removed from `lib-common-domain` and should now use `lib-common-eda`:

| Removed from lib-common-domain | Use Instead (lib-common-eda) |
|-------------------------------|------------------------------|
| `DomainEventPublisher` | `EventPublisher` |
| `DomainEvent` interface | Plain POJOs with `Event<T>` wrapper |
| `@EventHandler` annotation | `@EventListener` with `Event<T>` parameter |
| Event filtering infrastructure | EDA filtering capabilities |
| Messaging platform adapters | EDA platform adapters |
| Event health indicators | EDA health indicators |

### 3. SAGA Integration Changes

The SAGA integration has been simplified:
- **Removed**: Direct SAGA orchestration (use `lib-transactional-engine`)
- **Kept**: `StepEventPublisherBridge` - bridges step events to EDA infrastructure
- **Changed**: Now uses `lib-common-eda` for event publishing instead of internal infrastructure

## Maven Dependencies

**Before (v1.x):**
```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>1.x.x</version>
</dependency>
```

**After (v2.0):**
```xml
<!-- Core domain library with CQRS and SAGA integration -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>2.0.0-SNAPSHOT</version>
    <!-- lib-common-cqrs is included transitively -->
</dependency>

<!-- Event-driven architecture (required for event publishing) -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-eda</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- SAGA orchestration (optional, only if using SAGAs) -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-transactional-engine</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Migration Steps

### Step 1: Update Maven Dependencies

Add the new dependencies to your `pom.xml`:

```xml
<!-- Add lib-common-eda for event publishing -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-eda</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Add lib-transactional-engine if using SAGAs -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-transactional-engine</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Step 2: Update CQRS Imports

Update all CQRS-related imports in your codebase:

```java
// OLD imports
import com.firefly.common.domain.command.*;
import com.firefly.common.domain.query.*;
import com.firefly.common.domain.authorization.*;
import com.firefly.common.domain.annotations.*;

// NEW imports
import com.firefly.common.cqrs.command.*;
import com.firefly.common.cqrs.query.*;
import com.firefly.common.cqrs.authorization.*;
import com.firefly.common.cqrs.annotations.*;
```

### Step 3: Migrate Event Publishing Code

Replace `DomainEventPublisher` with `EventPublisher` from lib-common-eda:

**Before:**
```java
@Service
public class AccountService {
    private final DomainEventPublisher eventPublisher;

    private Mono<Account> publishEvent(Account account) {
        AccountCreatedEvent event = new AccountCreatedEvent(account.getId());
        return eventPublisher.publish(event).thenReturn(account);
    }
}
```

**After:**
```java
@Service
public class AccountService {
    private final EventPublisher eventPublisher;

    private Mono<Account> publishEvent(Account account) {
        AccountCreatedEvent event = new AccountCreatedEvent(account.getId());

        Map<String, Object> headers = Map.of(
            "event_type", "account.created",
            "aggregate_id", account.getId()
        );

        return eventPublisher.publish(event, "account-events", headers)
            .thenReturn(account);
    }
}
```

### Step 4: Migrate Event Listeners

Replace `@EventHandler` with `@EventListener` and use `Event<T>` wrapper:

**Before:**
```java
@Component
public class AccountEventHandler {
    @EventHandler(eventType = "account.created")
    public Mono<Void> handleAccountCreated(AccountCreatedEvent event) {
        // Handle event
        return Mono.empty();
    }
}
```

**After:**
```java
@Component
public class AccountEventHandler {
    @EventListener
    public Mono<Void> handleAccountCreated(Event<AccountCreatedEvent> event) {
        AccountCreatedEvent payload = event.getPayload();
        // Handle event
        return Mono.empty();
    }
}
```

### Step 5: Update Configuration Properties

**Before:**
```yaml
firefly:
  events:
    enabled: true
    adapter: KAFKA
    default-topic: domain.events
    kafka:
      bootstrap-servers: localhost:9092

  step-events:
    enabled: true
    publisher-type: KAFKA
    topic-name: saga-steps
```

**After:**
```yaml
firefly:
  # Event publishing configuration (lib-common-eda)
  eda:
    enabled: true
    default-publisher-type: KAFKA
    kafka:
      bootstrap-servers: localhost:9092

  # SAGA step events configuration
  stepevents:
    enabled: true
    topic: saga-step-events
```

### Step 6: Verify Tests

Run your tests to ensure everything still works:

```bash
mvn clean test
```

### Step 7: Update Documentation References

Update any internal documentation or comments that reference:
- Domain events in lib-common-domain → point to lib-common-eda
- CQRS in lib-common-domain → point to lib-common-cqrs
- SAGA orchestration → point to lib-transactional-engine

## Compatibility

### Backward Compatibility

- **CQRS Configuration**: All CQRS configuration properties remain unchanged
- **CQRS Functionality**: All CQRS features work exactly as before
- **Integration**: lib-common-domain still depends on lib-common-cqrs transitively

### Breaking Changes

#### 1. Package Names
- **CQRS imports**: Must update from `com.firefly.common.domain.*` to `com.firefly.common.cqrs.*`

#### 2. Event Publishing API
- **DomainEventPublisher removed**: Use `EventPublisher` from lib-common-eda
- **DomainEvent interface removed**: Use plain POJOs
- **@EventHandler removed**: Use `@EventListener` with `Event<T>` parameter
- **Event filtering**: Use lib-common-eda filtering capabilities

#### 3. Configuration Properties
- **firefly.events.*** removed**: Use `firefly.eda.*` from lib-common-eda
- **firefly.step-events.*** renamed**: Use `firefly.stepevents.*`

#### 4. Dependencies
- **lib-common-eda required**: Must add explicitly for event publishing
- **lib-transactional-engine required**: Must add explicitly for SAGA orchestration

#### 5. Removed Components
- All domain event infrastructure (publishers, listeners, filters, adapters)
- Event health indicators (moved to lib-common-eda)
- Event metrics (moved to lib-common-eda)
- Messaging platform adapters (moved to lib-common-eda)

## Complete Migration Examples

### Example 1: CQRS Handler Migration

**Before:**
```java
package com.example.banking.commands;

import com.firefly.common.domain.command.Command;
import com.firefly.common.domain.command.CommandHandler;
import com.firefly.common.domain.annotations.CommandHandlerComponent;

@CommandHandlerComponent
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountResult> {
    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command) {
        return processCommand(command);
    }
}
```

**After:**
```java
package com.example.banking.commands;

import com.firefly.common.cqrs.command.Command;           // Changed
import com.firefly.common.cqrs.command.CommandHandler;    // Changed
import com.firefly.common.cqrs.annotations.CommandHandlerComponent; // Changed

@CommandHandlerComponent
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountResult> {
    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command) {
        return processCommand(command);  // Business logic unchanged
    }
}
```

### Example 2: Event Publishing Migration

**Before:**
```java
@Service
public class OrderService {
    private final DomainEventPublisher eventPublisher;

    public Mono<Order> createOrder(CreateOrderCommand command) {
        return processOrder(command)
            .flatMap(this::publishOrderCreatedEvent);
    }

    private Mono<Order> publishOrderCreatedEvent(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getCustomerId(),
            order.getTotal()
        );

        return eventPublisher.publish(event)
            .thenReturn(order);
    }
}
```

**After:**
```java
@Service
public class OrderService {
    private final EventPublisher eventPublisher;  // Changed to lib-common-eda

    public Mono<Order> createOrder(CreateOrderCommand command) {
        return processOrder(command)
            .flatMap(this::publishOrderCreatedEvent);
    }

    private Mono<Order> publishOrderCreatedEvent(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getCustomerId(),
            order.getTotal()
        );

        // Now requires topic and headers
        Map<String, Object> headers = Map.of(
            "event_type", "order.created",
            "aggregate_id", order.getId(),
            "source", "order-service"
        );

        return eventPublisher.publish(event, "order-events", headers)
            .thenReturn(order);
    }
}
```

### Example 3: Event Listener Migration

**Before:**
```java
@Component
public class OrderEventHandler {

    @EventHandler(eventType = "order.created")
    public Mono<Void> handleOrderCreated(OrderCreatedEvent event) {
        return notificationService.sendConfirmation(
            event.getCustomerId(),
            event.getOrderId()
        );
    }
}
```

**After:**
```java
@Component
public class OrderEventHandler {

    @EventListener  // Changed to Spring's @EventListener
    public Mono<Void> handleOrderCreated(Event<OrderCreatedEvent> event) {
        OrderCreatedEvent payload = event.getPayload();  // Extract payload

        return notificationService.sendConfirmation(
            payload.getCustomerId(),
            payload.getOrderId()
        );
    }
}
```

### Example 4: SAGA Step Event Configuration

**Before:**
```yaml
firefly:
  step-events:
    enabled: true
    publisher-type: KAFKA
    topic-name: saga-steps
    include-metadata: true

  events:
    adapter: KAFKA
    kafka:
      bootstrap-servers: localhost:9092
```

**After:**
```yaml
firefly:
  stepevents:
    enabled: true
    topic: saga-step-events

  eda:
    enabled: true
    default-publisher-type: KAFKA
    kafka:
      bootstrap-servers: localhost:9092
```

## Automated Migration Scripts

### Script 1: Update CQRS Imports

```bash
#!/bin/bash
# update-cqrs-imports.sh

find . -type f -name "*.java" -exec sed -i '' \
  -e 's/import com\.firefly\.common\.domain\.command\./import com.firefly.common.cqrs.command./g' \
  -e 's/import com\.firefly\.common\.domain\.query\./import com.firefly.common.cqrs.query./g' \
  -e 's/import com\.firefly\.common\.domain\.authorization\./import com.firefly.common.cqrs.authorization./g' \
  -e 's/import com\.firefly\.common\.domain\.validation\./import com.firefly.common.cqrs.validation./g' \
  -e 's/import com\.firefly\.common\.domain\.annotations\./import com.firefly.common.cqrs.annotations./g' \
  -e 's/import com\.firefly\.common\.domain\.tracing\./import com.firefly.common.cqrs.tracing./g' \
  -e 's/import com\.firefly\.common\.domain\.context\./import com.firefly.common.cqrs.context./g' \
  {} \;

echo "CQRS import statements updated!"
```

### Script 2: Update Event Publishing Imports

```bash
#!/bin/bash
# update-event-imports.sh

find . -type f -name "*.java" -exec sed -i '' \
  -e 's/import com\.firefly\.common\.domain\.events\.DomainEventPublisher/import com.firefly.common.eda.publisher.EventPublisher/g' \
  -e 's/import com\.firefly\.common\.domain\.events\.DomainEvent/\/\/ MANUAL MIGRATION NEEDED: Remove DomainEvent interface/g' \
  -e 's/@EventHandler/@EventListener \/\/ MANUAL MIGRATION NEEDED: Update method signature to use Event<T>/g' \
  {} \;

echo "Event import statements updated!"
echo "WARNING: Manual migration needed for event handlers and DomainEvent implementations"
```

### Script 3: Update Configuration Files

```bash
#!/bin/bash
# update-config.sh

find . -type f -name "application*.yml" -exec sed -i '' \
  -e 's/firefly\.events\./firefly.eda./g' \
  -e 's/firefly\.step-events\./firefly.stepevents./g' \
  -e 's/publisher-type:/default-publisher-type:/g' \
  -e 's/topic-name:/topic:/g' \
  {} \;

echo "Configuration files updated!"
```

### Run All Scripts

```bash
chmod +x update-cqrs-imports.sh update-event-imports.sh update-config.sh
./update-cqrs-imports.sh
./update-event-imports.sh
./update-config.sh

echo "Automated migration complete!"
echo "Please review changes and complete manual migration steps"
echo "Run tests: mvn clean test"
```

## Manual Migration Checklist

After running automated scripts, complete these manual steps:

- [ ] Update `pom.xml` to add `lib-common-eda` dependency
- [ ] Update `pom.xml` to add `lib-transactional-engine` dependency (if using SAGAs)
- [ ] Review all event publishing code and add topic + headers parameters
- [ ] Update event listener methods to use `Event<T>` wrapper
- [ ] Remove `DomainEvent` interface implementations from event classes
- [ ] Update configuration files (`application.yml`, `application-*.yml`)
- [ ] Update test configuration files
- [ ] Review and update integration tests
- [ ] Run `mvn clean test` to verify all tests pass
- [ ] Update internal documentation and comments
- [ ] Review health check endpoints and metrics

## Support

If you encounter any issues during migration:

1. Check the [lib-common-domain README](README.md)
2. Check the [lib-common-cqrs README](../lib-common-cqrs/README.md)
3. Check the [lib-common-eda README](../lib-common-eda/README.md)
4. Check the [lib-transactional-engine README](../lib-transactional-engine/README.md)
5. Contact the Firefly Platform Team

## What's in lib-common-domain v2.0

The library now focuses on:

- **CQRS Framework** (via lib-common-cqrs dependency):
  - CommandBus and QueryBus
  - Handler auto-discovery
  - Validation and authorization
  - ExecutionContext and multi-tenancy

- **SAGA Integration**:
  - StepEventPublisherBridge (bridges to lib-common-eda)
  - Integration with lib-transactional-engine

- **Observability**:
  - JSON logging configuration
  - Metrics collection
  - Health indicators
  - Distributed tracing

## What Moved to Other Libraries

- **Event Publishing** → `lib-common-eda`
  - EventPublisher interface
  - Multi-platform support (Kafka, RabbitMQ, SQS, Kinesis)
  - Event listeners and handlers
  - Resilience patterns for events
  - Event health indicators and metrics

- **Service Communication** → `lib-common-client`
  - ServiceClient framework
  - REST and gRPC support
  - Circuit breakers and retries
  - Health checks

- **SAGA Orchestration** → `lib-transactional-engine`
  - Saga orchestration engine
  - Step definitions
  - Compensation logic
  - Transaction management
