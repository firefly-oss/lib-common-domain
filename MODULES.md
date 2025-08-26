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
- `@EventPublisher` and `@EventListener` annotations
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
- **Remote Sagas**: Step events published to Kafka, RabbitMQ, or SQS (based on configured adapter)

#### Simplified Step Events Configuration

Step Events **always** use Domain Events infrastructure - no separate configuration needed!

**Key Benefits:**
- **Zero Duplication**: No separate messaging configuration for Step Events
- **Automatic Inheritance**: Step Events use whatever adapter is configured for Domain Events
- **Single Source of Truth**: All messaging settings in `firefly.events.*` apply to both

**Simple Configuration:**
```yaml
firefly:
  stepevents:
    enabled: true  # Only setting needed - defaults to true
  
  events:
    adapter: kafka  # Step Events automatically use Kafka too
    kafka:
      bootstrap-servers: localhost:9092
      # All settings apply to both Domain Events AND Step Events
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
    adapter: auto  # Will auto-detect: Kafka -> RabbitMQ -> SQS -> ApplicationEvent
    kafka:
      bootstrap-servers: "localhost:9092"
    # OR
    rabbit:
      exchange: "events"
    # OR  
    sqs:
      queue-url: "https://sqs.us-east-1.amazonaws.com/123456789012/prod-events"
```

**Note**: The `auto` adapter selection chooses in priority order: Kafka (if KafkaTemplate available), RabbitMQ (if RabbitTemplate available), SQS (if SqsAsyncClient available), then Application Events as fallback.

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
| Kafka configured | Low-Medium | Kafka broker connections | Remote event streaming |
| RabbitMQ configured | Low-Medium | RabbitMQ connections | Remote reliable messaging |
| SQS configured | Low-Medium | AWS SQS connections | Remote cloud queuing |

## 🔍 Troubleshooting

### Common Issues

#### Messaging Adapter Not Working
```
ERROR: Kafka/RabbitMQ/SQS adapter selected but bean not found
```

**Kafka Issues:**
```yaml
# Ensure Spring Kafka is configured
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

**RabbitMQ Issues:**
```yaml
# Ensure Spring AMQP is configured
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

**SQS Issues:**
```xml
<!-- Ensure AWS SQS SDK is included -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sqs</artifactId>
    <version>2.25.32</version>
</dependency>
```

#### Auto-Detection Not Working as Expected
If `adapter: auto` doesn't select your preferred messaging system, check:
1. Required dependencies are on classpath
2. Required beans (KafkaTemplate, RabbitTemplate, SqsAsyncClient) are configured
3. Priority order: Kafka → RabbitMQ → SQS → Application Events

## 📈 Best Practices

### Production Recommendations

1. **Choose the Right Messaging System**: All adapters (Kafka, RabbitMQ, SQS) provide remote messaging
2. **Use Application Events for Local Communication**: They work reliably within a single JVM
3. **Configure Messaging Properly**: Ensure proper setup for your chosen adapter
4. **Monitor All Adapters**: Health checks available for all messaging systems

### Development Approach

1. **Start with Application Events**: They work immediately without infrastructure
2. **Test with Your Target Messaging System**: Verify behavior with your production adapter
3. **Use Auto-Detection Wisely**: Understand the priority order for adapter selection

## 🔗 Related Resources

- [Main README.md](README.md) - Complete library documentation
- [Configuration Reference](README.md#-configuration) - Detailed configuration options
- [Testing Guide](README.md#-testing) - Testing strategies and examples
- Spring Boot Documentation - Framework-specific integration guides