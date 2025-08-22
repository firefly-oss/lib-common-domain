# Module Selection Guide

This guide helps you choose the right combination of Firefly Common Domain modules for your specific use case.

## 🎯 Quick Decision Matrix

| Use Case | Recommended Modules | Rationale |
|----------|-------------------|-----------|
| **Getting Started** | `lib-common-domain-all` | All features included, easy setup |
| **Production Microservice** | `core` + specific adapters | Minimal dependencies, faster builds |
| **Multi-Cloud Deployment** | `core` + `kafka` + `sqs` | Flexibility across environments |
| **Enterprise Integration** | `core` + `rabbit` | Complex routing, reliable messaging |
| **Testing/Development** | `core` only | Application events for local testing |
| **Serverless/Lambda** | `core` + `sqs` | Minimal footprint, cloud-native |
| **Saga/Transactional Workflows** | `core` + transactional-engine + adapters | Step events for distributed transactions |

## 📦 Module Combinations

### Minimal Setup (Application Events Only)
```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Use When:**
- Local development and testing
- Monolithic applications
- Simple event handling within single JVM

**Features:**
- Spring Application Events
- `@EmitEvent` and `@OnDomainEvent` annotations
- Health checks and metrics
- No external messaging dependencies

### Single Messaging System

#### Kafka-Only Setup
```xml
<!-- Core module -->
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Kafka adapter -->
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-kafka</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Spring Kafka dependency -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

**Use When:**
- High-throughput event streaming
- Distributed microservices architecture
- Event sourcing patterns
- Real-time analytics

#### RabbitMQ-Only Setup
```xml
<!-- Core module -->
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- RabbitMQ adapter -->
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-rabbit</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Spring AMQP dependency -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

**Use When:**
- Complex routing requirements
- Enterprise integration patterns
- Reliable message delivery guarantees
- Legacy system integration

#### AWS SQS-Only Setup
```xml
<!-- Core module (includes SQS functionality) -->
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- AWS SDK dependency -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sqs</artifactId>
    <version>2.25.32</version>
</dependency>
```

**Use When:**
- AWS-native applications
- Serverless architectures
- Managed queue service preference
- Auto-scaling requirements

### Multi-Messaging Environments

#### Hybrid Cloud Setup
```xml
<!-- Core module -->
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Kafka for high-throughput events -->
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-kafka</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- SQS for AWS integration (included in core module) -->

<!-- Required dependencies -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sqs</artifactId>
    <version>2.25.32</version>
</dependency>
```

**Configuration:**
```yaml
firefly:
  events:
    adapter: auto  # Will prioritize Kafka if available
    # Can also route different events to different adapters programmatically
```

#### Enterprise Integration Setup
```xml
<!-- All adapters for maximum flexibility -->
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-kafka</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-rabbit</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<!-- SQS functionality is included in core module - no separate dependency needed -->
```

### Transactional Engine Integration

For saga patterns and distributed transaction workflows using lib-transactional-engine-core.

#### Basic Transactional Engine Setup
```xml
<!-- Core module -->
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Transactional Engine -->
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-transactional-engine-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Use When:**
- Implementing saga patterns
- Distributed transaction workflows
- Step-by-step process orchestration
- Complex business process management

**Features:**
- `StepEventPublisher` integration via bridge pattern
- Automatic reuse of domain event messaging adapters
- Same configuration and monitoring as domain events
- Consistent error handling and retry mechanisms

#### With Kafka (Recommended for High-Throughput Sagas)
```xml
<!-- Add Kafka adapter -->
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-domain-kafka</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

#### With SQS (Recommended for Cloud-Native Sagas)
```xml
<!-- SQS functionality is included in core module -->
<!-- Only AWS SDK dependency is needed -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sqs</artifactId>
    <version>2.25.32</version>
</dependency>
```

## 🔧 Configuration Strategies

### Environment-Based Selection

#### Development Environment
```yaml
# application-dev.yml
firefly:
  events:
    adapter: application_event  # Fast, no infrastructure needed
```

#### Staging Environment
```yaml
# application-staging.yml
firefly:
  events:
    adapter: kafka  # Production-like setup
    kafka:
      template-bean-name: stagingKafkaTemplate
```

#### Production Environment
```yaml
# application-prod.yml
firefly:
  events:
    adapter: auto  # Let the library choose based on available infrastructure
    kafka:
      use-messaging-if-available: true
    rabbit:
      exchange: "prod.events.${topic}"
      routing-key: "${type}"
```

### Profile-Based Module Activation

```xml
<!-- Conditional dependencies using Maven profiles -->
<profiles>
    <profile>
        <id>kafka</id>
        <dependencies>
            <dependency>
                <groupId>com.catalis</groupId>
                <artifactId>lib-common-domain-kafka</artifactId>
                <version>1.0.0-SNAPSHOT</version>
            </dependency>
        </dependencies>
    </profile>
    
    <profile>
        <id>rabbit</id>
        <dependencies>
            <dependency>
                <groupId>com.catalis</groupId>
                <artifactId>lib-common-domain-rabbit</artifactId>
                <version>1.0.0-SNAPSHOT</version>
            </dependency>
        </dependencies>
    </profile>
</profiles>
```

Build with specific profile:
```bash
mvn clean package -Pkafka
```

## 📊 Dependency Impact Analysis

### Size Comparison (Approximate)

| Module Combination | JAR Size | Transitive Dependencies | Build Time Impact |
|-------------------|----------|------------------------|-------------------|
| Core only | ~500KB | Minimal | Fastest |
| Core + Kafka | ~2MB | Spring Kafka, Kafka clients | Medium |
| Core + RabbitMQ | ~1.5MB | Spring AMQP, RabbitMQ client | Medium |
| Core + SQS | ~1MB | AWS SDK SQS | Medium |
| All modules | ~4MB | All messaging libraries | Slowest |

### Memory Footprint

| Configuration | Heap Usage | Connection Pools | Thread Pools |
|--------------|------------|------------------|-------------|
| Application Events | Minimal | None | Spring default |
| Single Adapter | Low | 1 connection pool | 1 thread pool |
| Multiple Adapters | Medium | Multiple pools | Multiple pools |

## 🚀 Migration Strategies

### From Monolithic to Modular

1. **Assessment Phase**
   ```bash
   # Analyze current dependencies
   mvn dependency:tree | grep -E "(kafka|rabbit|sqs|amqp)"
   ```

2. **Gradual Migration**
   ```xml
   <!-- Step 1: Start with all-in-one -->
   <dependency>
       <groupId>com.catalis</groupId>
       <artifactId>lib-common-domain-all</artifactId>
       <version>1.0.0-SNAPSHOT</version>
   </dependency>
   
   <!-- Step 2: Switch to modular approach -->
   <!-- Replace with core + specific adapters -->
   ```

3. **Testing Strategy**
   ```yaml
   # Use application events for testing regardless of production setup
   firefly:
     events:
       adapter: application_event
   ```

### Adding New Messaging Systems

1. **Add Module Dependency**
2. **Configure Adapter Settings**
3. **Test Adapter Priority**
4. **Update Health Checks**

## 🔍 Troubleshooting

### Common Issues

#### Multiple Adapters Detected
```
WARN: Multiple messaging adapters detected. Using priority order: kafka > rabbit > sqs > application_event
```

**Solution**: Explicitly specify adapter in configuration:
```yaml
firefly:
  events:
    adapter: kafka  # Force specific adapter
```

#### Missing Dependencies
```
ERROR: KafkaTemplate bean not found but kafka adapter is configured
```

**Solution**: Add required Spring dependencies:
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

#### ClassPath Conflicts
```
ERROR: Multiple versions of messaging libraries detected
```

**Solution**: Use dependency management:
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
            <version>${spring-kafka.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## 📈 Best Practices

### Production Recommendations

1. **Use Modular Approach**: Include only needed adapters
2. **Explicit Configuration**: Don't rely on auto-detection in production
3. **Health Check Monitoring**: Monitor all configured adapters
4. **Graceful Degradation**: Configure fallback adapters

### Development Recommendations

1. **Start Simple**: Use `application_event` for initial development
2. **Infrastructure Parity**: Match production messaging systems in staging
3. **Testing Isolation**: Use separate modules for integration tests

### Performance Optimization

1. **Connection Pooling**: Configure appropriate pool sizes
2. **Batch Processing**: Use adapter-specific batching features  
3. **Async Processing**: Leverage reactive patterns consistently

## 🔗 Related Resources

- [Main README.md](README.md) - Complete library documentation
- [Configuration Reference](README.md#-configuration) - Detailed configuration options
- [Testing Guide](README.md#-testing) - Testing strategies and examples
- Spring Boot Documentation - Framework-specific integration guides