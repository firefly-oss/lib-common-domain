package com.catalis.common.domain.config;

import com.catalis.common.domain.events.inbound.EventListenerDispatcher;
import com.catalis.common.domain.events.inbound.SqsDomainEventsSubscriber;
import com.catalis.common.domain.events.outbound.*;
import com.catalis.common.domain.events.properties.DomainEventsProperties;
import com.catalis.common.domain.util.DomainEventAdapterUtils;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@AutoConfiguration
@EnableConfigurationProperties(DomainEventsProperties.class)
@ConditionalOnClass({ApplicationEventPublisher.class})
@ConditionalOnProperty(prefix = "firefly.events", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DomainEventsAutoConfiguration {

    // Infrastructure Bean Creation - These must come before the publisher beans
    
    /**
     * Creates a Kafka ProducerFactory from Firefly properties when:
     * - Kafka classes are available on classpath
     * - No existing ProducerFactory bean exists
     * - Bootstrap servers are configured in Firefly properties
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    @ConditionalOnMissingBean(name = "kafkaProducerFactory")
    @ConditionalOnProperty(prefix = "firefly.events.kafka", name = "bootstrap-servers")
    public ProducerFactory<String, String> kafkaProducerFactory(DomainEventsProperties props) {
        DomainEventsProperties.Kafka kafkaProps = props.getKafka();
        
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProps.getBootstrapServers());
        
        // Set serializers
        String keySerializer = kafkaProps.getKeySerializer() != null ? 
            kafkaProps.getKeySerializer() : StringSerializer.class.getName();
        String valueSerializer = kafkaProps.getValueSerializer() != null ? 
            kafkaProps.getValueSerializer() : StringSerializer.class.getName();
            
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        
        // Optional Kafka producer configurations
        if (kafkaProps.getRetries() != null) {
            configProps.put(ProducerConfig.RETRIES_CONFIG, kafkaProps.getRetries());
        }
        if (kafkaProps.getBatchSize() != null) {
            configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, kafkaProps.getBatchSize());
        }
        if (kafkaProps.getLingerMs() != null) {
            configProps.put(ProducerConfig.LINGER_MS_CONFIG, kafkaProps.getLingerMs());
        }
        if (kafkaProps.getBufferMemory() != null) {
            configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, kafkaProps.getBufferMemory());
        }
        if (kafkaProps.getAcks() != null) {
            configProps.put(ProducerConfig.ACKS_CONFIG, kafkaProps.getAcks());
        }
        
        // Add any additional properties
        if (kafkaProps.getProperties() != null && !kafkaProps.getProperties().isEmpty()) {
            configProps.putAll(kafkaProps.getProperties());
        }
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    /**
     * Creates a KafkaTemplate from Firefly-created ProducerFactory when:
     * - Kafka classes are available on classpath
     * - No existing KafkaTemplate bean exists
     * - ProducerFactory is available (either user-provided or Firefly-created)
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    @ConditionalOnMissingBean(name = "kafkaTemplate")
    @ConditionalOnBean(ProducerFactory.class)
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
    
    /**
     * Creates a RabbitMQ ConnectionFactory from Firefly properties when:
     * - RabbitMQ classes are available on classpath
     * - No existing ConnectionFactory bean exists
     * - Host is configured in Firefly properties
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
    @ConditionalOnMissingBean(ConnectionFactory.class)
    public ConnectionFactory rabbitConnectionFactory(DomainEventsProperties props) {
        DomainEventsProperties.Rabbit rabbitProps = props.getRabbit();
        
        CachingConnectionFactory factory = new CachingConnectionFactory();
        
        // Configure connection properties from Firefly configuration
        factory.setHost(rabbitProps.getHost());
        factory.setPort(rabbitProps.getPort());
        factory.setUsername(rabbitProps.getUsername());
        factory.setPassword(rabbitProps.getPassword());
        factory.setVirtualHost(rabbitProps.getVirtualHost());
        
        // Optional connection settings
        if (rabbitProps.getConnectionTimeout() != null) {
            factory.setConnectionTimeout(rabbitProps.getConnectionTimeout());
        }
        if (rabbitProps.getRequestedHeartbeat() != null) {
            factory.getRabbitConnectionFactory().setRequestedHeartbeat(rabbitProps.getRequestedHeartbeat());
        }
        
        factory.setPublisherConfirmType(rabbitProps.isPublisherConfirms() ? 
            CachingConnectionFactory.ConfirmType.CORRELATED : CachingConnectionFactory.ConfirmType.NONE);
        factory.setPublisherReturns(rabbitProps.isPublisherReturns());
        
        return factory;
    }
    
    /**
     * Creates a RabbitTemplate from Firefly-created ConnectionFactory when:
     * - RabbitMQ classes are available on classpath
     * - No existing RabbitTemplate bean exists
     * - ConnectionFactory is available (either user-provided or Firefly-created)
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
    @ConditionalOnMissingBean(name = "rabbitTemplate")
    @ConditionalOnBean(ConnectionFactory.class)
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }
    
    /**
     * Creates an SQS AsyncClient from Firefly properties when:
     * - SQS classes are available on classpath
     * - No existing SqsAsyncClient bean exists
     * - Region is configured in Firefly properties
     */
    @Bean
    @ConditionalOnClass(name = "software.amazon.awssdk.services.sqs.SqsAsyncClient")
    @ConditionalOnMissingBean(SqsAsyncClient.class)
    @ConditionalOnProperty(prefix = "firefly.events.sqs", name = "region")
    public SqsAsyncClient sqsAsyncClient(DomainEventsProperties props) {
        DomainEventsProperties.Sqs sqsProps = props.getSqs();
        
        var builder = SqsAsyncClient.builder();
        
        // Configure AWS region
        builder.region(software.amazon.awssdk.regions.Region.of(sqsProps.getRegion()));
        
        // Configure credentials if provided
        if (sqsProps.getAccessKeyId() != null && sqsProps.getSecretAccessKey() != null) {
            var credentialsBuilder = software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                    sqsProps.getAccessKeyId(), 
                    sqsProps.getSecretAccessKey()
                )
            );
            if (sqsProps.getSessionToken() != null) {
                credentialsBuilder = software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                    software.amazon.awssdk.auth.credentials.AwsSessionCredentials.create(
                        sqsProps.getAccessKeyId(), 
                        sqsProps.getSecretAccessKey(),
                        sqsProps.getSessionToken()
                    )
                );
            }
            builder.credentialsProvider(credentialsBuilder);
        }
        
        // Optional endpoint override (for local testing, etc.)
        if (sqsProps.getEndpointOverride() != null) {
            builder.endpointOverride(URI.create(sqsProps.getEndpointOverride()));
        }
        
        return builder.build();
    }
    
    /**
     * Creates a Kinesis AsyncClient from Firefly properties when:
     * - Kinesis classes are available on classpath
     * - No existing KinesisAsyncClient bean exists
     * - Region is configured in Firefly properties
     */
    @Bean
    @ConditionalOnClass(name = "software.amazon.awssdk.services.kinesis.KinesisAsyncClient")
    @ConditionalOnMissingBean(KinesisAsyncClient.class)
    @ConditionalOnProperty(prefix = "firefly.events.kinesis", name = "region")
    public KinesisAsyncClient kinesisAsyncClient(DomainEventsProperties props) {
        DomainEventsProperties.Kinesis kinesisProps = props.getKinesis();
        
        var builder = KinesisAsyncClient.builder();
        
        // Configure AWS region
        builder.region(software.amazon.awssdk.regions.Region.of(kinesisProps.getRegion()));
        
        // Configure credentials if provided
        if (kinesisProps.getAccessKeyId() != null && kinesisProps.getSecretAccessKey() != null) {
            var credentialsProvider = software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                    kinesisProps.getAccessKeyId(), 
                    kinesisProps.getSecretAccessKey()
                )
            );
            if (kinesisProps.getSessionToken() != null) {
                credentialsProvider = software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                    software.amazon.awssdk.auth.credentials.AwsSessionCredentials.create(
                        kinesisProps.getAccessKeyId(), 
                        kinesisProps.getSecretAccessKey(),
                        kinesisProps.getSessionToken()
                    )
                );
            }
            builder.credentialsProvider(credentialsProvider);
        }
        
        // Optional endpoint override (for local testing, etc.)
        if (kinesisProps.getEndpointOverride() != null) {
            builder.endpointOverride(URI.create(kinesisProps.getEndpointOverride()));
        }
        
        return builder.build();
    }

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
        // Check if Kafka classes are present and either:
        // 1. A KafkaTemplate bean already exists (user-provided), OR
        // 2. Bootstrap servers are configured (will create our own infrastructure)
        boolean classPresent = DomainEventAdapterUtils.isClassPresent("org.springframework.kafka.core.KafkaTemplate");
        if (!classPresent) {
            return false;
        }
        
        // Check for existing bean first
        boolean beanExists = DomainEventAdapterUtils.resolveBean(ctx, null, "org.springframework.kafka.core.KafkaTemplate") != null;
        if (beanExists) {
            return true;
        }
        
        // Check if we can create our own infrastructure
        try {
            DomainEventsProperties props = ctx.getBean(DomainEventsProperties.class);
            return props.getKafka().getBootstrapServers() != null && !props.getKafka().getBootstrapServers().trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isRabbitMqAvailable(ApplicationContext ctx) {
        // Check if RabbitMQ classes are present and either:
        // 1. A RabbitTemplate bean already exists (user-provided), OR
        // 2. Host is configured (will create our own infrastructure)
        boolean classPresent = DomainEventAdapterUtils.isClassPresent("org.springframework.amqp.rabbit.core.RabbitTemplate");
        if (!classPresent) {
            return false;
        }
        
        // Check for existing bean first
        boolean beanExists = DomainEventAdapterUtils.resolveBean(ctx, null, "org.springframework.amqp.rabbit.core.RabbitTemplate") != null;
        if (beanExists) {
            return true;
        }
        
        // Check if we can create our own infrastructure
        try {
            DomainEventsProperties props = ctx.getBean(DomainEventsProperties.class);
            return props.getRabbit().getHost() != null && !props.getRabbit().getHost().trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSqsAvailable(ApplicationContext ctx) {
        // Check if SQS classes are present and either:
        // 1. An SqsAsyncClient bean already exists (user-provided), OR
        // 2. Region is configured (will create our own infrastructure)
        boolean classPresent = DomainEventAdapterUtils.isClassPresent("software.amazon.awssdk.services.sqs.SqsAsyncClient");
        if (!classPresent) {
            return false;
        }
        
        // Check for existing bean first
        boolean beanExists = DomainEventAdapterUtils.resolveBean(ctx, null, "software.amazon.awssdk.services.sqs.SqsAsyncClient") != null;
        if (beanExists) {
            return true;
        }
        
        // Check if we can create our own infrastructure
        try {
            DomainEventsProperties props = ctx.getBean(DomainEventsProperties.class);
            return props.getSqs().getRegion() != null && !props.getSqs().getRegion().trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isKinesisAvailable(ApplicationContext ctx) {
        // Check if Kinesis classes are present and either:
        // 1. A KinesisAsyncClient bean already exists (user-provided), OR
        // 2. Region is configured (will create our own infrastructure)
        boolean classPresent = DomainEventAdapterUtils.isClassPresent("software.amazon.awssdk.services.kinesis.KinesisAsyncClient");
        if (!classPresent) {
            return false;
        }
        
        // Check for existing bean first
        boolean beanExists = DomainEventAdapterUtils.resolveBean(ctx, null, "software.amazon.awssdk.services.kinesis.KinesisAsyncClient") != null;
        if (beanExists) {
            return true;
        }
        
        // Check if we can create our own infrastructure
        try {
            DomainEventsProperties props = ctx.getBean(DomainEventsProperties.class);
            return props.getKinesis().getRegion() != null && !props.getKinesis().getRegion().trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
