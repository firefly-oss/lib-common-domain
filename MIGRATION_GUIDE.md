# Migration Guide: lib-common-domain v2.0

## Overview

In version 2.0, CQRS functionality has been extracted from `lib-common-domain` into a separate library: `lib-common-cqrs`.

This separation provides:
- **Better modularity**: Use CQRS independently of domain events
- **Clearer responsibilities**: Domain library focuses on events, service communication, and resilience
- **Improved maintainability**: Each library has a focused scope
- **Easier testing**: Test CQRS logic separately from domain logic

## What Changed

### Package Structure

CQRS classes have moved from `com.firefly.common.domain.*` to `com.firefly.common.cqrs.*`:

| Old Package | New Package |
|------------|-------------|
| `com.firefly.common.domain.command.*` | `com.firefly.common.cqrs.command.*` |
| `com.firefly.common.domain.query.*` | `com.firefly.common.cqrs.query.*` |
| `com.firefly.common.domain.authorization.*` | `com.firefly.common.cqrs.authorization.*` |
| `com.firefly.common.domain.validation.*` | `com.firefly.common.cqrs.validation.*` |
| `com.firefly.common.domain.annotations.*` | `com.firefly.common.cqrs.annotations.*` |
| `com.firefly.common.domain.config.Cqrs*` | `com.firefly.common.cqrs.config.*` |

### Maven Dependency

**Before:**
```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>1.x.x</version>
</dependency>
```

**After:**
```xml
<!-- For domain events, service clients, saga integration -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>

<!-- lib-common-cqrs is included transitively -->
<!-- Or add explicitly if you only need CQRS: -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-cqrs</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Migration Steps

### Step 1: Update Imports

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

### Step 2: Update Configuration Properties

Configuration properties remain the same:

```yaml
firefly:
  cqrs:
    enabled: true
    command:
      validation:
        enabled: true
    query:
      cache:
        enabled: true
        type: LOCAL
```

No changes needed in your `application.yml` files!

### Step 3: Verify Tests

Run your tests to ensure everything still works:

```bash
mvn clean test
```

### Step 4: Update Documentation References

Update any internal documentation or comments that reference CQRS in lib-common-domain to point to lib-common-cqrs.

## Compatibility

### Backward Compatibility

- **Configuration**: All CQRS configuration properties remain unchanged
- **Functionality**: All CQRS features work exactly as before
- **Integration**: lib-common-domain still depends on lib-common-cqrs, so CQRS + Domain features work together

### Breaking Changes

- **Package names**: You must update import statements
- **Maven artifactId**: If you were explicitly depending on CQRS features, you may want to add lib-common-cqrs dependency

## Examples

### Before Migration

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

### After Migration

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

## Automated Migration Script

You can use this bash script to automatically update imports:

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

echo "Import statements updated!"
echo "Please review changes and run tests: mvn clean test"
```

## Support

If you encounter any issues during migration:

1. Check the [lib-common-cqrs README](../lib-common-cqrs/README.md)
2. Review the [lib-common-domain README](README.md)
3. Contact the Firefly Platform Team

## What Stays in lib-common-domain

The following features remain in lib-common-domain:

- **Domain Events**: Multi-messaging event publishing (Kafka, RabbitMQ, SQS, Kinesis)
- **ServiceClient Framework**: REST and gRPC service communication
- **Resilience Patterns**: Circuit breakers, retries, bulkhead isolation
- **Saga Integration**: Integration with lib-transactional-engine
- **Observability**: Metrics, health checks, distributed tracing

These features continue to work with CQRS through the lib-common-cqrs dependency.
