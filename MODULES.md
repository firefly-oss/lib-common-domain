# Module Selection Guide

This guide helps you understand the actual module structure and functionality of the Firefly Common Domain library.

## ⚠️ Important: Current Module Reality

**The modular architecture described in previous versions of this documentation is not accurate.** Here's the actual situation:

- **Core Module**: Contains ALL messaging dependencies and functionality
- **Adapter Modules**: Provide minimal or no additional functionality beyond the core
- **Functional Adapters**: Only Application Events and AWS SQS actually work

## 🎯 Actual Decision Matrix

| Use Case | Recommended Module | Rationale |
|----------|-------------------|-----------|
| **Getting Started** | `lib-common-domain-core` | Contains all functionality, simplest setup |
| **Production Applications** | `lib-common-domain-core` + AWS SQS SDK | For remote event publishing |
| **Testing/Development** | `lib-common-domain-core` | Application events work out of the box |
| **Cloud-Native/Serverless** | `lib-common-domain-core` + AWS SQS SDK | SQS is the only working remote adapter |
| **Monolithic Applications** | `lib-common-domain-core` | Application events sufficient for single JVM |
| **Saga/Transactional Workflows** | `lib-common-domain-core` + `lib-transactional-engine-core` | Step events reuse domain event adapters |

## 📦 Available Setups

### Setup 1: Local Events Only (Default)
```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**What You Get:**
- Spring Application Events (local JVM only)
- `@EmitEvent` and `@OnDomainEvent` annotations
- Health checks and metrics
- All messaging dependencies included (but only ApplicationEvent adapter works)

**Use When:**
- Local development and testing
- Monolithic applications
- Simple event handling within single JVM
- Getting started with the library

### Setup 2: Remote Events via AWS SQS
```xml
<!-- Core module (contains all functionality) -->
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- AWS SDK for SQS functionality -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sqs</artifactId>
    <version>2.25.32</version>
</dependency>
```

**What You Get:**
- All features from Setup 1
- AWS SQS event publishing and consumption
- Remote event distribution across services
- Cloud-native messaging capabilities

**Use When:**
- AWS-native applications
- Distributed microservices architecture
- Serverless architectures
- Production applications requiring remote events

### ❌ What Doesn't Work

**Important**: Despite the existence of separate modules, the following setups DO NOT provide the advertised functionality:

#### Non-Functional: Kafka Setup
```xml
<!-- This setup does NOT work for domain events -->
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-kafka</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
- The Kafka module only creates KafkaTemplate infrastructure beans
- Domain events configured for Kafka fall back to Application Events
- No actual domain event publishing to Kafka occurs

#### Non-Functional: RabbitMQ Setup
```xml
<!-- This setup does NOT work for domain events -->
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-rabbit</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
- The RabbitMQ module provides no functionality whatsoever
- Domain events configured for RabbitMQ fall back to Application Events
- No actual domain event publishing to RabbitMQ occurs

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