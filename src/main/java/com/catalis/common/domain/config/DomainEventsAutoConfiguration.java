package com.catalis.common.domain.config;

import com.catalis.common.domain.events.inbound.OnDomainEventDispatcher;
import com.catalis.common.domain.events.inbound.RabbitDomainEventsSubscriber;
import com.catalis.common.domain.events.inbound.SqsDomainEventsSubscriber;
import com.catalis.common.domain.events.outbound.*;
import com.catalis.common.domain.events.inbound.KafkaDomainEventsSubscriber;
import com.catalis.common.domain.events.properties.DomainEventsProperties;
import com.catalis.common.domain.stepevents.StepEventAdapterUtils;
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
        if (adapter == DomainEventsProperties.Adapter.NOOP) {
            return new NoopDomainEventPublisher();
        }
        if (adapter == DomainEventsProperties.Adapter.APPLICATION_EVENT) {
            return new ApplicationEventDomainEventPublisher(applicationEventPublisher);
        }
        if (adapter == DomainEventsProperties.Adapter.KAFKA) {
            return new KafkaTemplateDomainEventPublisher(ctx, props.getKafka().isUseMessagingIfAvailable(), props.getKafka().getTemplateBeanName());
        }
        if (adapter == DomainEventsProperties.Adapter.RABBIT) {
            return new RabbitTemplateDomainEventPublisher(ctx, props.getRabbit());
        }
        if (adapter == DomainEventsProperties.Adapter.SQS) {
            return new SqsAsyncClientDomainEventPublisher(ctx, props.getSqs());
        }
        // AUTO detection order: Kafka -> Rabbit -> SQS -> ApplicationEvent
        if (isKafkaProducerAvailable(ctx)) {
            return new KafkaTemplateDomainEventPublisher(ctx, props.getKafka().isUseMessagingIfAvailable(), props.getKafka().getTemplateBeanName());
        }
        if (isRabbitProducerAvailable(ctx)) {
            return new RabbitTemplateDomainEventPublisher(ctx, props.getRabbit());
        }
        if (isSqsAvailable(ctx)) {
            return new SqsAsyncClientDomainEventPublisher(ctx, props.getSqs());
        }
        return new ApplicationEventDomainEventPublisher(applicationEventPublisher);
    }

    @Bean
    public EmitEventAspect emitEventAspect(DomainEventPublisher publisher) {
        return new EmitEventAspect(publisher);
    }

    @Bean
    public OnDomainEventDispatcher onDomainEventDispatcher() {
        return new OnDomainEventDispatcher();
    }

    // Inbound subscribers (conditional, disabled by default)

    @Bean
    @ConditionalOnExpression("'${firefly.events.consumer.enabled:false}'=='true' and '${firefly.events.adapter:auto}'=='kafka'")
    @ConditionalOnClass(name = {
            "org.springframework.kafka.core.ConsumerFactory",
            "org.springframework.kafka.listener.KafkaMessageListenerContainer"
    })
    @ConditionalOnBean(type = "org.springframework.kafka.core.ConsumerFactory")
    public SmartLifecycle domainEventsKafkaSubscriber(org.springframework.kafka.core.ConsumerFactory<Object, Object> consumerFactory,
                                                      DomainEventsProperties props,
                                                      ApplicationEventPublisher events) {
        if (props.getConsumer().getKafka().getTopics() == null || props.getConsumer().getKafka().getTopics().isEmpty()) {
            return noopLifecycle();
        }
        return new KafkaDomainEventsSubscriber(consumerFactory, props, events);
    }

    @Bean
    @ConditionalOnExpression("'${firefly.events.consumer.enabled:false}'=='true' and '${firefly.events.adapter:auto}'=='rabbit'")
    @ConditionalOnClass(name = {
            "org.springframework.amqp.rabbit.connection.ConnectionFactory",
            "org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer"
    })
    @ConditionalOnBean(type = "org.springframework.amqp.rabbit.connection.ConnectionFactory")
    public SmartLifecycle domainEventsRabbitSubscriber(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory,
                                                       DomainEventsProperties props,
                                                       ApplicationEventPublisher events) {
        if (props.getConsumer().getRabbit().getQueues() == null || props.getConsumer().getRabbit().getQueues().isEmpty()) {
            return noopLifecycle();
        }
        return new RabbitDomainEventsSubscriber(connectionFactory, props, events);
    }

    @Bean
    @ConditionalOnExpression("'${firefly.events.consumer.enabled:false}'=='true' and '${firefly.events.adapter:auto}'=='sqs'")
    @ConditionalOnClass(name = "software.amazon.awssdk.services.sqs.SqsAsyncClient")
    @ConditionalOnBean(type = "software.amazon.awssdk.services.sqs.SqsAsyncClient")
    public SmartLifecycle domainEventsSqsSubscriber(ApplicationContext ctx,
                                                    DomainEventsProperties props,
                                                    ApplicationEventPublisher events) {
        String url = props.getSqs().getQueueUrl();
        String name = props.getConsumer().getSqs().getQueueName();
        if ((url == null || url.isEmpty()) && (name == null || name.isEmpty())) {
            return noopLifecycle();
        }
        return new SqsDomainEventsSubscriber(ctx, props, events);
    }

    @Bean
    @ConditionalOnExpression("'${firefly.events.consumer.enabled:false}'=='true' and '${firefly.events.adapter:auto}'=='auto'")
    public SmartLifecycle domainEventsAutoSubscriber(ApplicationContext ctx,
                                                     DomainEventsProperties props,
                                                     ApplicationEventPublisher events) {
        // AUTO detection order for inbound: Kafka -> Rabbit -> SQS
        if (isKafkaConsumerAvailable(ctx)) {
            if (props.getConsumer().getKafka().getTopics() == null || props.getConsumer().getKafka().getTopics().isEmpty()) {
                return noopLifecycle();
            }
            org.springframework.kafka.core.ConsumerFactory<Object, Object> cf = (org.springframework.kafka.core.ConsumerFactory<Object, Object>)
                    StepEventAdapterUtils.resolveBean(ctx, null, "org.springframework.kafka.core.ConsumerFactory");
            return new KafkaDomainEventsSubscriber(cf, props, events);
        }
        if (isRabbitConsumerAvailable(ctx)) {
            if (props.getConsumer().getRabbit().getQueues() == null || props.getConsumer().getRabbit().getQueues().isEmpty()) {
                return noopLifecycle();
            }
            org.springframework.amqp.rabbit.connection.ConnectionFactory cf = (org.springframework.amqp.rabbit.connection.ConnectionFactory)
                    StepEventAdapterUtils.resolveBean(ctx, null, "org.springframework.amqp.rabbit.connection.ConnectionFactory");
            return new RabbitDomainEventsSubscriber(cf, props, events);
        }
        if (isSqsAvailable(ctx)) {
            String url = props.getSqs().getQueueUrl();
            String name = props.getConsumer().getSqs().getQueueName();
            if ((url == null || url.isEmpty()) && (name == null || name.isEmpty())) {
                return noopLifecycle();
            }
            return new SqsDomainEventsSubscriber(ctx, props, events);
        }
        // No inbound transport available; provide a no-op lifecycle
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

    private boolean isKafkaProducerAvailable(ApplicationContext ctx) {
        return StepEventAdapterUtils.isClassPresent("org.springframework.kafka.core.KafkaTemplate") &&
                StepEventAdapterUtils.resolveBean(ctx, null, "org.springframework.kafka.core.KafkaTemplate") != null;
    }

    private boolean isKafkaConsumerAvailable(ApplicationContext ctx) {
        return StepEventAdapterUtils.isClassPresent("org.springframework.kafka.core.ConsumerFactory") &&
                StepEventAdapterUtils.resolveBean(ctx, null, "org.springframework.kafka.core.ConsumerFactory") != null;
    }

    private boolean isRabbitProducerAvailable(ApplicationContext ctx) {
        return StepEventAdapterUtils.isClassPresent("org.springframework.amqp.rabbit.core.RabbitTemplate") &&
                StepEventAdapterUtils.resolveBean(ctx, null, "org.springframework.amqp.rabbit.core.RabbitTemplate") != null;
    }

    private boolean isRabbitConsumerAvailable(ApplicationContext ctx) {
        return StepEventAdapterUtils.isClassPresent("org.springframework.amqp.rabbit.connection.ConnectionFactory") &&
                StepEventAdapterUtils.resolveBean(ctx, null, "org.springframework.amqp.rabbit.connection.ConnectionFactory") != null;
    }

    private boolean isSqsAvailable(ApplicationContext ctx) {
        return StepEventAdapterUtils.isClassPresent("software.amazon.awssdk.services.sqs.SqsAsyncClient") &&
                StepEventAdapterUtils.resolveBean(ctx, null, "software.amazon.awssdk.services.sqs.SqsAsyncClient") != null;
    }
}
