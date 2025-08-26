package com.catalis.common.domain.config;

import com.catalis.common.domain.events.inbound.EventListenerDispatcher;
import com.catalis.common.domain.events.inbound.SqsDomainEventsSubscriber;
import com.catalis.common.domain.events.outbound.*;
import com.catalis.common.domain.events.properties.DomainEventsProperties;
import com.catalis.common.domain.util.DomainEventAdapterUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(DomainEventsProperties.class)
@ConditionalOnClass({ApplicationEventPublisher.class})
@ConditionalOnProperty(prefix = "firefly.events", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DomainEventsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    public DomainEventPublisher domainEventPublisher(ApplicationContext ctx,
                                                     ApplicationEventPublisher applicationEventPublisher,
                                                     DomainEventsProperties props) {
        DomainEventsProperties.Adapter adapter = props.getAdapter();
        
        // Handle explicit adapter selection
        switch (adapter) {
            case NOOP:
                return new NoopDomainEventPublisher();
            case APPLICATION_EVENT:
                return new ApplicationEventDomainEventPublisher(applicationEventPublisher);
            case SQS:
                return new SqsAsyncClientDomainEventPublisher(ctx, props.getSqs());
            case KAFKA:
                return new KafkaDomainEventPublisher(ctx, props.getKafka());
            case RABBIT:
                return new RabbitMqDomainEventPublisher(ctx, props.getRabbit());
            case KINESIS:
                return new KinesisDomainEventPublisher(ctx, props.getKinesis());
            case AUTO:
            default:
                // Auto-detection order: Kafka -> RabbitMQ -> Kinesis -> SQS -> ApplicationEvent
                if (isKafkaAvailable(ctx)) {
                    return new KafkaDomainEventPublisher(ctx, props.getKafka());
                }
                if (isRabbitMqAvailable(ctx)) {
                    return new RabbitMqDomainEventPublisher(ctx, props.getRabbit());
                }
                if (isKinesisAvailable(ctx)) {
                    return new KinesisDomainEventPublisher(ctx, props.getKinesis());
                }
                if (isSqsAvailable(ctx)) {
                    return new SqsAsyncClientDomainEventPublisher(ctx, props.getSqs());
                }
                return new ApplicationEventDomainEventPublisher(applicationEventPublisher);
        }
    }

    @Bean
    public EventPublisherAspect eventPublisherAspect(DomainEventPublisher publisher) {
        return new EventPublisherAspect(publisher);
    }

    @Bean
    public EventListenerDispatcher eventListenerDispatcher() {
        return new EventListenerDispatcher();
    }

    // Inbound subscribers (conditional, disabled by default)

    @Bean
    @ConditionalOnProperty(prefix = "firefly.events.consumer", name = "enabled", havingValue = "true")
    @ConditionalOnExpression("'${firefly.events.adapter:auto}' == 'kafka' or '${firefly.events.adapter:auto}' == 'auto'")
    @ConditionalOnClass(name = {
            "org.apache.kafka.clients.consumer.KafkaConsumer",
            "org.apache.kafka.clients.consumer.ConsumerRecord"
    })
    public SmartLifecycle domainEventsKafkaSubscriber(ApplicationContext ctx,
                                                      DomainEventsProperties props,
                                                      ApplicationEventPublisher events) {
        // Only create if Kafka is explicitly configured or auto-detected as the active adapter
        DomainEventsProperties.Adapter adapter = props.getAdapter();
        if (adapter == DomainEventsProperties.Adapter.KAFKA || 
            (adapter == DomainEventsProperties.Adapter.AUTO && isKafkaAvailable(ctx))) {
            return new com.catalis.common.domain.events.inbound.KafkaDomainEventsSubscriber(ctx, props, events);
        }
        return noopLifecycle();
    }

    @Bean
    @ConditionalOnProperty(prefix = "firefly.events.consumer", name = "enabled", havingValue = "true")
    @ConditionalOnExpression("'${firefly.events.adapter:auto}' == 'rabbit' or '${firefly.events.adapter:auto}' == 'auto'")
    @ConditionalOnClass(name = {
            "org.springframework.amqp.rabbit.connection.ConnectionFactory",
            "org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer"
    })
    @ConditionalOnBean(type = "org.springframework.amqp.rabbit.connection.ConnectionFactory")
    public SmartLifecycle domainEventsRabbitSubscriber(ApplicationContext ctx,
                                                       DomainEventsProperties props,
                                                       ApplicationEventPublisher events) {
        // Only create if RabbitMQ is explicitly configured or auto-detected as the active adapter
        DomainEventsProperties.Adapter adapter = props.getAdapter();
        if (adapter == DomainEventsProperties.Adapter.RABBIT || 
            (adapter == DomainEventsProperties.Adapter.AUTO && isRabbitMqAvailable(ctx) && !isKafkaAvailable(ctx))) {
            return new com.catalis.common.domain.events.inbound.RabbitMqDomainEventsSubscriber(ctx, props, events);
        }
        return noopLifecycle();
    }

    @Bean
    @ConditionalOnProperty(prefix = "firefly.events.consumer", name = "enabled", havingValue = "true")
    @ConditionalOnExpression("'${firefly.events.adapter:auto}' == 'sqs' or '${firefly.events.adapter:auto}' == 'auto'")
    @ConditionalOnClass(name = "software.amazon.awssdk.services.sqs.SqsAsyncClient")
    @ConditionalOnBean(type = "software.amazon.awssdk.services.sqs.SqsAsyncClient")
    public SmartLifecycle domainEventsSqsSubscriber(ApplicationContext ctx,
                                                    DomainEventsProperties props,
                                                    ApplicationEventPublisher events) {
        // Only create if SQS is explicitly configured or auto-detected as the active adapter
        DomainEventsProperties.Adapter adapter = props.getAdapter();
        if (adapter == DomainEventsProperties.Adapter.SQS || 
            (adapter == DomainEventsProperties.Adapter.AUTO && isSqsAvailable(ctx) && 
             !isKafkaAvailable(ctx) && !isRabbitMqAvailable(ctx))) {
            
            String url = props.getSqs().getQueueUrl();
            String name = props.getConsumer().getSqs().getQueueName();
            if ((url == null || url.isEmpty()) && (name == null || name.isEmpty())) {
                return noopLifecycle();
            }
            return new SqsDomainEventsSubscriber(ctx, props, events);
        }
        return noopLifecycle();
    }

    @Bean
    @ConditionalOnProperty(prefix = "firefly.events.consumer", name = "enabled", havingValue = "true")
    @ConditionalOnExpression("'${firefly.events.adapter:auto}' == 'kinesis' or '${firefly.events.adapter:auto}' == 'auto'")
    @ConditionalOnClass(name = "software.amazon.awssdk.services.kinesis.KinesisAsyncClient")
    @ConditionalOnBean(type = "software.amazon.awssdk.services.kinesis.KinesisAsyncClient")
    public SmartLifecycle domainEventsKinesisSubscriber(ApplicationContext ctx,
                                                        DomainEventsProperties props,
                                                        ApplicationEventPublisher events) {
        // Only create if Kinesis is explicitly configured or auto-detected as the active adapter
        DomainEventsProperties.Adapter adapter = props.getAdapter();
        if (adapter == DomainEventsProperties.Adapter.KINESIS || 
            (adapter == DomainEventsProperties.Adapter.AUTO && isKinesisAvailable(ctx) && 
             !isKafkaAvailable(ctx) && !isRabbitMqAvailable(ctx))) {
            
            String streamName = props.getConsumer().getKinesis().getStreamName();
            if (streamName == null || streamName.isEmpty()) {
                streamName = props.getKinesis().getStreamName();
            }
            if (streamName == null || streamName.isEmpty()) {
                return noopLifecycle();
            }
            return new com.catalis.common.domain.events.inbound.KinesisDomainEventsSubscriber(ctx, props, events);
        }
        return noopLifecycle();
    }

    private SmartLifecycle noopLifecycle() {
        return new SmartLifecycle() {
            private volatile boolean running;
            @Override public void start() { running = true; }
            @Override public void stop() { running = false; }
            @Override public boolean isRunning() { return running; }
        };
    }

    private boolean isKafkaAvailable(ApplicationContext ctx) {
        return DomainEventAdapterUtils.isClassPresent("org.springframework.kafka.core.KafkaTemplate") &&
                DomainEventAdapterUtils.resolveBean(ctx, null, "org.springframework.kafka.core.KafkaTemplate") != null;
    }
    
    private boolean isRabbitMqAvailable(ApplicationContext ctx) {
        return DomainEventAdapterUtils.isClassPresent("org.springframework.amqp.rabbit.core.RabbitTemplate") &&
                DomainEventAdapterUtils.resolveBean(ctx, null, "org.springframework.amqp.rabbit.core.RabbitTemplate") != null;
    }

    private boolean isSqsAvailable(ApplicationContext ctx) {
        return DomainEventAdapterUtils.isClassPresent("software.amazon.awssdk.services.sqs.SqsAsyncClient") &&
                DomainEventAdapterUtils.resolveBean(ctx, null, "software.amazon.awssdk.services.sqs.SqsAsyncClient") != null;
    }

    private boolean isKinesisAvailable(ApplicationContext ctx) {
        return DomainEventAdapterUtils.isClassPresent("software.amazon.awssdk.services.kinesis.KinesisAsyncClient") &&
                DomainEventAdapterUtils.resolveBean(ctx, null, "software.amazon.awssdk.services.kinesis.KinesisAsyncClient") != null;
    }
}
