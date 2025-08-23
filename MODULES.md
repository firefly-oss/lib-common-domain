# Module Guide

This guide explains the current single-module architecture of the Firefly Common Domain library.

## ✅ Current Architecture: Single Module

The library has been refactored from a confusing multi-module structure to an honest, single-module design:

- **Single Module**: `lib-common-domain` contains ALL functionality
- **All Dependencies Included**: Kafka, RabbitMQ, and SQS dependencies are included
- **Functional Adapters**: Application Events (default), Kafka, RabbitMQ, and SQS

## 🎯 Usage Decision Matrix

| Use Case | Dependency | Additional Setup Required |
|----------|------------|--------------------------|
| **Getting Started** | `lib-common-domain` | None - Application Events work by default |
| **Local Development** | `lib-common-domain` | None - Application Events sufficient |
| **Production with Kafka** | `lib-common-domain` | Configure Kafka bootstrap servers |
| **Production with RabbitMQ** | `lib-common-domain` | Configure RabbitMQ connection |
| **Cloud-Native/AWS** | `lib-common-domain` | Configure AWS SQS settings |
| **Saga/Transactional Workflows** | `lib-common-domain` + `lib-transactional-engine-core` | Configure desired messaging adapter |

## 📦 Installation

### Single Module Installation
```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**What You Get:**
- All messaging adapters (Application Events, Kafka, RabbitMQ, SQS)
- `@EmitEvent` and `@OnDomainEvent` annotations
- Health checks and metrics
- Transactional engine integration
- Complete testing support for all messaging systems

**What Works Out of the Box:**
- Spring Application Events (local JVM events)
- All annotations and event handling infrastructure
- Health checks and monitoring endpoints

**What Requires Additional Configuration:**
- **Kafka**: Configure `firefly.events.adapter=kafka` and bootstrap servers
- **RabbitMQ**: Configure `firefly.events.adapter=rabbit` and connection settings
- **AWS SQS**: Configure `firefly.events.adapter=sqs` and queue settings

### Transactional Engine Integration

For saga patterns and distributed transaction workflows using lib-transactional-engine-core.

#### Transactional Engine Setup
```xml
<!-- Core module (transactional engine already included) -->
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Transactional Engine (if not using core module) -->
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-transactional-engine-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**What Actually Works:**
- `StepEventPublisher` integration via bridge pattern
- Step events reuse the same adapters as domain events
- **Local Sagas**: Step events published as Application Events (default)
- **Remote Sagas**: Step events published to SQS (if SQS is configured)

**Limitations:**
- Step events are limited by the same adapter restrictions as domain events
- Kafka and RabbitMQ configurations for step events fall back to Application Events
- No dedicated messaging infrastructure for step events

#### For Remote Step Events (SQS Only)
```xml
<!-- Add AWS SQS SDK for remote step event publishing -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sqs</artifactId>
    <version>2.25.32</version>
</dependency>
```

## 🔧 Configuration Strategies

### Environment-Based Configuration

#### Development Environment
```yaml
# application-dev.yml
firefly:
  events:
    enabled: true
    adapter: application_event  # Local events only, fast and simple
```

#### Staging Environment
```yaml
# application-staging.yml
firefly:
  events:
    enabled: true
    adapter: sqs  # Test with remote events
    sqs:
      queue-url: "https://sqs.us-east-1.amazonaws.com/123456789012/staging-events"
```

#### Production Environment
```yaml
# application-prod.yml
firefly:
  events:
    enabled: true
    adapter: auto  # Will select sqs if available, otherwise application_event
    sqs:
      queue-url: "https://sqs.us-east-1.amazonaws.com/123456789012/prod-events"
```

**Note**: The `auto` adapter selection only chooses between `application_event` (default) and `sqs` (if configured). Kafka and RabbitMQ configurations are ignored and fall back to `application_event`.

### SQS Configuration Example

For remote event publishing via AWS SQS:

```yaml
firefly:
  events:
    enabled: true
    adapter: sqs
    sqs:
      queue-url: "https://sqs.region.amazonaws.com/account-id/queue-name"
      # OR use queue name (requires additional AWS configuration)
      queue-name: "my-events-queue"
      client-bean-name: "sqsAsyncClient"  # Optional, defaults to auto-detection
```

**AWS Configuration:**
```yaml
# AWS credentials and region
aws:
  region: us-east-1
  credentials:
    access-key: ${AWS_ACCESS_KEY_ID}
    secret-key: ${AWS_SECRET_ACCESS_KEY}
```

## 📊 Actual Dependency Impact

### Reality Check

**Important**: Due to the core module including all messaging dependencies, the modular approach provides no size benefits.

| Setup | JAR Size | Transitive Dependencies | Functional Adapters |
|-------|----------|------------------------|-------------------|
| Core only | ~4MB | ALL messaging libraries | ApplicationEvent only |
| Core + SQS SDK | ~5MB | Core + AWS SQS | ApplicationEvent + SQS |
| Any adapter module | ~4MB+ | Same as core | No additional functionality |

### Memory Footprint Reality

| Actual Configuration | Heap Usage | Active Connections | Notes |
|---------------------|------------|-------------------|-------|
| ApplicationEvent (default) | Minimal | None | Local JVM events only |
| SQS configured | Low-Medium | AWS SQS connections | Remote event publishing |
| Kafka/Rabbit configured | Minimal | None (fallback to ApplicationEvent) | Non-functional |

## 🔍 Troubleshooting

### Common Issues

#### Expecting Kafka/RabbitMQ but Getting Local Events
```yaml
# This configuration doesn't work as expected
firefly:
  events:
    adapter: kafka
```

**Reality**: Events will be published as Application Events (local JVM), not to Kafka.

**Solution**: Use SQS for remote events or accept Application Events for local events:
```yaml
# For remote events (only option that works)
firefly:
  events:
    adapter: sqs
    sqs:
      queue-url: "your-sqs-queue-url"
```

```yaml  
# For local events (honest configuration)
firefly:
  events:
    adapter: application_event
```

#### SQS Configuration Issues
```
ERROR: SqsAsyncClient bean not found but sqs adapter is configured
```

**Solution**: Ensure AWS SQS SDK is included and properly configured:
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sqs</artifactId>
    <version>2.25.32</version>
</dependency>
```

## 📈 Realistic Best Practices

### Production Recommendations

1. **Be Honest About Messaging**: Only SQS provides actual remote messaging
2. **Use Application Events for Local Communication**: They work reliably within a single JVM
3. **Configure SQS Properly**: For distributed event communication
4. **Monitor What Actually Works**: Focus on Application Event and SQS health checks

### Development Approach

1. **Start with Application Events**: They work immediately without infrastructure
2. **Test with SQS in Staging**: To verify remote event behavior
3. **Don't Rely on Non-Functional Adapters**: Kafka and RabbitMQ configurations are misleading

## 🔗 Related Resources

- [Main README.md](README.md) - Complete library documentation
- [Configuration Reference](README.md#-configuration) - Detailed configuration options
- [Testing Guide](README.md#-testing) - Testing strategies and examples
- Spring Boot Documentation - Framework-specific integration guides