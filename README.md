# Firefly OpenCore Banking Platform - Common Domain Library
Last updated: 2025-08-22 17:06

This library provides common domain building blocks and auto-configurations shared across microservices.

## Installation

Artifact coordinates:

Maven (pom.xml):

```
<dependency>
  <groupId>com.catalis</groupId>
  <artifactId>lib-common-domain</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Gradle (Groovy DSL):

```
dependencies {
  implementation "com.catalis:lib-common-domain:1.0.0-SNAPSHOT"
}
```

Gradle (Kotlin DSL):

```
dependencies {
  implementation("com.catalis:lib-common-domain:1.0.0-SNAPSHOT")
}
```

Requirements:
- Spring Boot (auto-configured via spring.factories; works with the parent BOM used in this repository)
- Message client dependencies as needed by your chosen adapter (spring-kafka, spring-boot-starter-amqp, or AWS SDK v2 SQS)
- A JDK compatible with your Spring Boot version (Boot 3 typically requires Java 17+)

## Generic Domain Events (Publish & Read)

Use the built-in generic events API to publish and read events that are not part of Transactional Engine StepEvents.

- Port: `com.catalis.common.domain.events.outbound.DomainEventPublisher`
- Envelope: `com.catalis.common.domain.events.DomainEventEnvelope`
- Annotations:
  - `@EmitEvent` to publish after a method completes successfully (supports SpEL for topic/type/key/payload).
  - `@OnDomainEvent` to subscribe to in-process events for local handling (topic/type filters).
- Auto-configuration: `com.catalis.common.domain.config.DomainEventsAutoConfiguration`.

Quickstart publish:

```
@Service
class PaymentsService {
  // Emits event on success; payload defaults to method return (#result)
  @EmitEvent(topic = "payments.events", type = "CREATED", key = "#p0.id")
  public Mono<Payment> create(Payment p) {
    return repo.save(p);
  }
}
```

Quickstart read (in-process subscription):

```
@Component
class PaymentsProjection {
  @OnDomainEvent(topic = "payments.events", type = "CREATED")
  public void onCreated(Payment payload) {
    // update read model
  }
}
```

Adapter selection via application.yaml (prefix: catalis.events):

```
catalis:
  events:
    adapter: kafka   # auto | application-event | kafka | rabbit | sqs | noop (default: auto)
    kafka:
      templateBeanName: kafkaTemplate
      # If Spring Messaging is on the classpath, send as Message with headers (default: true)
      useMessagingIfAvailable: true
```

- AUTO detection order: Kafka -> Rabbit -> SQS -> ApplicationEvent.
- When using Rabbit:
```
catalis:
  events:
    adapter: rabbit
    rabbit:
      exchange: "${topic}"
      routingKey: "${type}"
```
- When using SQS:
```
catalis:
  events:
    adapter: sqs
    sqs:
      queueUrl: https://sqs.us-east-1.amazonaws.com/123456789012/my-queue
      # or
      queueName: my-queue
```

### Disable publishing (NOOP) for Domain Events

```
catalis:
  events:
    enabled: false
# or
# events:
#   adapter: noop
```

Note: This library emits in-process Spring events for local subscribers; for cross-service consumption, connect your consumer to the configured MQ (Kafka/Rabbit/SQS).

## Step Events (Hexagonal MQ Support)

The library integrates with `lib-transactional-engine` step events using a hexagonal architecture.

- Port: `com.catalis.transactionalengine.events.StepEventPublisher`
- Envelope: `com.catalis.transactionalengine.events.StepEventEnvelope`
- Default adapter: in-process via Spring `ApplicationEventPublisher`
- Override: provide your own `StepEventPublisher` bean (Kafka/Rabbit/SQS/etc.). If you do, the default adapter is not created.

Auto-configuration: `com.catalis.common.domain.config.StepEventsAutoConfiguration` (registered via `META-INF/spring.factories`). If a `DomainEventPublisher` bean is available, StepEvents automatically delegates to it via an internal bridge so both StepEvents and generic events share the same configured adapter.

### Quickstart: Publish Step Events from Sagas

Annotate your saga steps with `@StepEvent` (from transactional-engine). Events are batched and emitted only after the saga completes successfully.

Example:

```
@SagaStep(id = "reserve")
@StepEvent(topic = "inventory.events", type = "RESERVED", key = "#{in.orderId}")
public Mono<InventoryReservation> reserve(@Input Order in) { /* ... */ }
```

### Default In-Process Publisher

If you don't provide any `StepEventPublisher` bean, the library will create one that forwards events using Spring `ApplicationEventPublisher` (in-process).

### Custom MQ Publisher (Kafka example)

Create your own adapter bean implementing the port:

```
@Component
class KafkaStepEventPublisher implements StepEventPublisher {
  private final KafkaTemplate<String, Object> kafka;

  KafkaStepEventPublisher(KafkaTemplate<String, Object> kafka) { this.kafka = kafka; }

  @Override
  public Mono<Void> publish(StepEventEnvelope e) {
    // Map envelope fields to your Kafka record
    return Mono.create(sink ->
      kafka.send(e.topic, e.key, e.payload)
           .addCallback(r -> sink.success(), ex -> sink.error(ex))
    );
  }
}
```

With this bean present, the default in-process adapter is disabled automatically.

## Notes
- This library now bundles Kafka, RabbitMQ, and AWS SQS client dependencies so microservices don’t need to declare them. Selection is property-driven and remains non-intrusive.
- For a complete guide on sagas, see docs in lib-transactional-engine regarding `@Saga`, `@SagaStep`, and `@StepEvent`.

## Adapter selection via application.yaml

This library auto-wires the StepEventPublisher for you. Microservices just add the client dependency and set properties. Do NOT create your own StepEventPublisher bean unless you want to fully override.

- Property prefix: catalis.stepevents
- Enable/disable: catalis.stepevents.enabled (default: true)
- Adapter: catalis.stepevents.adapter: auto | application-event | kafka | rabbit | sqs | noop (default: auto)
- Auto detection order: Kafka -> Rabbit -> SQS -> ApplicationEvent

### Kafka (Spring for Apache Kafka)
Requirements: spring-kafka on classpath and a KafkaTemplate bean.

Example application.yaml:

catalis:
  stepevents:
    adapter: kafka
    kafka:
      # Optional if multiple templates registered
      templateBeanName: kafkaTemplate

Behavior:
- Sends using KafkaTemplate#send(topic, key, payload) when available.
- If Spring Messaging is on the classpath and enabled (default), it may send Message with headers copied from the envelope.

### RabbitMQ (Spring AMQP)
Requirements: spring-amqp/spring-rabbit on classpath and a RabbitTemplate bean.

Example application.yaml:

catalis:
  stepevents:
    adapter: rabbit
    rabbit:
      # Defaults shown; you can template with ${topic}, ${type}, ${key}
      exchange: "${topic}"
      routingKey: "${type}"
      # Optional if multiple templates
      templateBeanName: rabbitTemplate

Behavior:
- Publishes using RabbitTemplate#convertAndSend(exchange, routingKey, payload).
- Default mapping: exchange=envelope.topic, routingKey=envelope.type.

### AWS SQS (AWS SDK v2, async)
Requirements: software.amazon.awssdk:sqs on classpath and a SqsAsyncClient bean.

Example application.yaml:

catalis:
  stepevents:
    adapter: sqs
    sqs:
      # One of these
      queueUrl: https://sqs.us-east-1.amazonaws.com/123456789012/my-queue
      # or
      queueName: my-queue # defaults to envelope.topic if not set
      # Optional if multiple clients
      clientBeanName: sqsAsyncClient

Behavior:
- Sends using SqsAsyncClient#sendMessage. If queueUrl missing, resolves via getQueueUrl(queueName).
- Payload serialized to JSON via ObjectMapper if available; otherwise toString().

### Disable publishing (NOOP)

catalis:
  stepevents:
    enabled: false
# or
# stepevents:
#   adapter: noop

### Default in-process fallback (ApplicationEvent)
If no adapter is selected and AUTO cannot detect Kafka/Rabbit/SQS, events are published in-process via Spring ApplicationEventPublisher.

### Responsibilities
- Library: Provides StepEventPublisher and adapters; wires everything according to properties.
- Microservice: Only adds the chosen client dependency and configures application.yaml.


## Inbound subscription from MQs (Kafka/Rabbit/SQS)

You can subscribe to events from external message queues and dispatch them to local handlers annotated with `@OnDomainEvent`. The library provides inbound adapters and only starts them when explicitly enabled and when the chosen MQ is available.

- Enable with: `catalis.events.consumer.enabled: true`
- Adapter selection uses the same `catalis.events.adapter` as publishers: `auto | kafka | rabbit | sqs`
- Nothing is loaded unless enabled and the prerequisites (client on classpath and beans) are present.

Common options:
- `catalis.events.consumer.typeHeader`: header name for event type (default: `event_type`)
- `catalis.events.consumer.keyHeader`: header name for event key (default: `event_key`)

Kafka (requires spring-kafka and a ConsumerFactory bean):

catalis:
  events:
    adapter: kafka
    consumer:
      enabled: true
      kafka:
        topics: ["payments.events", "invoices.events"]
        # Optional overrides:
        # groupId: payments-read-model
        # consumerFactoryBeanName: kafkaConsumerFactory

RabbitMQ (requires spring-amqp and a ConnectionFactory bean):

catalis:
  events:
    adapter: rabbit
    consumer:
      enabled: true
      rabbit:
        queues: ["payments.events.queue"]

AWS SQS (requires AWS SDK v2 and a SqsAsyncClient bean):

catalis:
  events:
    adapter: sqs
    sqs:
      # Provide queueUrl here, or omit and set consumer.sqs.queueName below
      queueUrl: https://sqs.us-east-1.amazonaws.com/123456789012/my-queue
    consumer:
      enabled: true
      sqs:
        # If queueUrl above is not set, provide the queueName to resolve URL
        queueName: my-queue
        waitTimeSeconds: 10      # long polling
        maxMessages: 10
        pollDelayMillis: 1000

How it works:
- The inbound adapter reads messages and republishes a DomainSpringEvent that contains a DomainEventEnvelope.
- Your `@OnDomainEvent` methods will receive these events as if they were published in-process, and you can filter with `topic` and `type`.
- If no topics/queues are configured, the subscriber is not started.

Non-intrusive/optional loading:
- Beans are created only when `catalis.events.consumer.enabled=true` and the adapter is selected.
- Additionally, the adapter requires its client bean (`ConsumerFactory`, `ConnectionFactory`, or `SqsAsyncClient`) to be present; otherwise, nothing is created.

## Package layout (ports and adapters)

- Ports (outbound):
  - Domain events: `com.catalis.common.domain.events.outbound.DomainEventPublisher`
  - Step events (from transactional engine): `com.catalis.transactionalengine.events.StepEventPublisher`
- Adapters (outbound publishers):
  - Kafka/Rabbit/SQS/ApplicationEvent under `com.catalis.common.domain.events` and `com.catalis.common.domain.stepevents`
- Adapters (inbound subscribers):
  - Kafka/Rabbit/SQS under `com.catalis.common.domain.events.inbound`
- Auto-configurations:
  - `com.catalis.common.domain.config.DomainEventsAutoConfiguration`
  - `com.catalis.common.domain.config.StepEventsAutoConfiguration`



## JSON logging

JSON logging is enabled by default when using this library because a logback-spring.xml is bundled on the classpath. It uses net.logstash.logback encoder to emit structured JSON logs.

Notes:
- Auto-configuration class: com.catalis.common.domain.config.JsonLoggingAutoConfiguration (no additional beans; config is in logback-spring.xml).
- To customize or disable, add your own logback-spring.xml in your application resources; Spring Boot will pick up the nearest one on the classpath and override the default.

## Development

- Build: mvn clean verify
- Run tests: mvn test
- Java version: align with your Spring Boot version (typically Java 17+)

## Changelog

- Unreleased — 2025-08-22
  - README refreshed: Installation instructions, accurate property names for catalis.events and catalis.stepevents, inbound subscriber configuration, Kafka useMessagingIfAvailable, and JSON logging notes.
  - Clarified adapter auto-detection order and in-process fallbacks.

## License

Licensed under the Apache License, Version 2.0. See the LICENSE file for details.
