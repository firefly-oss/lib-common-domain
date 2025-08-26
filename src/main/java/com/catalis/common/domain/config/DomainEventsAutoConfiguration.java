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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(DomainEventsAutoConfiguration.class);
    
    public DomainEventsAutoConfiguration() {
        log.debug("Firefly Domain Events Auto-Configuration - Starting initialization");
    }

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
        log.debug("Creating Kafka ProducerFactory from Firefly properties");
        DomainEventsProperties.Kafka kafkaProps = props.getKafka();
        
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProps.getBootstrapServers());
        log.info("   • Bootstrap servers: {}", kafkaProps.getBootstrapServers());
        
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
        log.debug("Creating KafkaTemplate from ProducerFactory");
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
        log.debug("Creating RabbitMQ ConnectionFactory from Firefly properties");
        DomainEventsProperties.Rabbit rabbitProps = props.getRabbit();
        
        CachingConnectionFactory factory = new CachingConnectionFactory();
        
        // Configure connection properties from Firefly configuration
        factory.setHost(rabbitProps.getHost());
        factory.setPort(rabbitProps.getPort());
        log.info("   • Host: {}:{}", rabbitProps.getHost(), rabbitProps.getPort());
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
        log.debug("Creating RabbitTemplate from ConnectionFactory");
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
        log.debug("Creating AWS SQS AsyncClient from Firefly properties");
        DomainEventsProperties.Sqs sqsProps = props.getSqs();
        
        var builder = SqsAsyncClient.builder();
        
        // Configure AWS region
        builder.region(software.amazon.awssdk.regions.Region.of(sqsProps.getRegion()));
        log.info("   • Region: {}", sqsProps.getRegion());
        
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
        log.debug("Creating AWS Kinesis AsyncClient from Firefly properties");
        DomainEventsProperties.Kinesis kinesisProps = props.getKinesis();
        
        var builder = KinesisAsyncClient.builder();
        
        // Configure AWS region
        builder.region(software.amazon.awssdk.regions.Region.of(kinesisProps.getRegion()));
        log.info("   • Region: {}", kinesisProps.getRegion());
        
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
        
        log.info("Firefly Domain Events - Configuring event publisher adapter");
        log.debug("Configuration: adapter={}, enabled={}", adapter, props.isEnabled());
        
        // Handle explicit adapter selection
        switch (adapter) {
            case NOOP:
                log.info("Using NOOP adapter - events will be discarded");
                return new NoopDomainEventPublisher();
            case APPLICATION_EVENT:
                log.info("Using APPLICATION_EVENT adapter - events published via Spring ApplicationEventPublisher");
                log.debug("Bean status: ApplicationEventPublisher={}", 
                        applicationEventPublisher != null ? "available" : "not available");
                return new ApplicationEventDomainEventPublisher(applicationEventPublisher);
            case SQS:
                logSqsConfiguration(ctx, props.getSqs(), true);
                return new SqsAsyncClientDomainEventPublisher(ctx, props.getSqs());
            case KAFKA:
                logKafkaConfiguration(ctx, props.getKafka(), true);
                return new KafkaDomainEventPublisher(ctx, props.getKafka());
            case RABBIT:
                logRabbitConfiguration(ctx, props.getRabbit(), true);
                return new RabbitMqDomainEventPublisher(ctx, props.getRabbit());
            case KINESIS:
                logKinesisConfiguration(ctx, props.getKinesis(), true);
                return new KinesisDomainEventPublisher(ctx, props.getKinesis());
            case AUTO:
            default:
                log.info("Auto-detecting available messaging adapter (priority: Kafka → RabbitMQ → Kinesis → SQS → ApplicationEvent)");
                
                // Auto-detection order: Kafka -> RabbitMQ -> Kinesis -> SQS -> ApplicationEvent
                if (isKafkaAvailable(ctx)) {
                    logKafkaConfiguration(ctx, props.getKafka(), false);
                    return new KafkaDomainEventPublisher(ctx, props.getKafka());
                }
                if (isRabbitMqAvailable(ctx)) {
                    logRabbitConfiguration(ctx, props.getRabbit(), false);
                    return new RabbitMqDomainEventPublisher(ctx, props.getRabbit());
                }
                if (isKinesisAvailable(ctx)) {
                    logKinesisConfiguration(ctx, props.getKinesis(), false);
                    return new KinesisDomainEventPublisher(ctx, props.getKinesis());
                }
                if (isSqsAvailable(ctx)) {
                    logSqsConfiguration(ctx, props.getSqs(), false);
                    return new SqsAsyncClientDomainEventPublisher(ctx, props.getSqs());
                }
                
                log.info("Using APPLICATION_EVENT adapter (fallback) - events published via Spring ApplicationEventPublisher");
                log.debug("Bean status: ApplicationEventPublisher={}", 
                        applicationEventPublisher != null ? "available" : "not available");
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

    private void logKafkaConfiguration(ApplicationContext ctx, DomainEventsProperties.Kafka kafkaProps, boolean explicit) {
        String mode = explicit ? "explicitly configured" : "auto-detected";
        log.info("Using KAFKA adapter ({}) - events published to Apache Kafka", mode);
        
        // Check bean status
        Object kafkaTemplate = DomainEventAdapterUtils.resolveBean(ctx, kafkaProps.getTemplateBeanName(), "org.springframework.kafka.core.KafkaTemplate");
        String kafkaTemplateBeanName = kafkaProps.getTemplateBeanName() != null ? kafkaProps.getTemplateBeanName() : "kafkaTemplate";
        
        if (kafkaTemplate != null) {
            log.debug("Bean status: KafkaTemplate bean '{}' found (user-provided)", kafkaTemplateBeanName);
        } else {
            log.debug("Bean status: KafkaTemplate bean '{}' not found, using Firefly-created infrastructure", kafkaTemplateBeanName);
        }
        
        // Configuration details
        log.debug("Kafka configuration:");
        log.debug("   • Bootstrap servers: {}", kafkaProps.getBootstrapServers() != null ? kafkaProps.getBootstrapServers() : "not configured");
        log.debug("   • Key serializer: {}", kafkaProps.getKeySerializer() != null ? kafkaProps.getKeySerializer() : "default (StringSerializer)");
        log.debug("   • Value serializer: {}", kafkaProps.getValueSerializer() != null ? kafkaProps.getValueSerializer() : "default (StringSerializer)");
        if (kafkaProps.getRetries() != null) {
            log.debug("   • Retries: {}", kafkaProps.getRetries());
        }
        if (kafkaProps.getBatchSize() != null) {
            log.debug("   • Batch size: {}", kafkaProps.getBatchSize());
        }
        if (kafkaProps.getAcks() != null) {
            log.debug("   • Acks: {}", kafkaProps.getAcks());
        }
    }

    private void logRabbitConfiguration(ApplicationContext ctx, DomainEventsProperties.Rabbit rabbitProps, boolean explicit) {
        String mode = explicit ? "explicitly configured" : "auto-detected";
        log.info("Using RABBIT adapter ({}) - events published to RabbitMQ", mode);
        
        // Check bean status
        Object rabbitTemplate = DomainEventAdapterUtils.resolveBean(ctx, rabbitProps.getTemplateBeanName(), "org.springframework.amqp.rabbit.core.RabbitTemplate");
        String rabbitTemplateBeanName = rabbitProps.getTemplateBeanName() != null ? rabbitProps.getTemplateBeanName() : "rabbitTemplate";
        
        if (rabbitTemplate != null) {
            log.debug("Bean status: RabbitTemplate bean '{}' found (user-provided)", rabbitTemplateBeanName);
        } else {
            log.debug("Bean status: RabbitTemplate bean '{}' not found, using Firefly-created infrastructure", rabbitTemplateBeanName);
        }
        
        // Configuration details
        log.debug("RabbitMQ configuration:");
        log.debug("   • Host: {}", rabbitProps.getHost() != null ? rabbitProps.getHost() : "not configured");
        log.debug("   • Port: {}", rabbitProps.getPort() != null ? rabbitProps.getPort() : "not configured");
        log.debug("   • Username: {}", rabbitProps.getUsername() != null ? rabbitProps.getUsername() : "not configured");
        log.debug("   • Virtual host: {}", rabbitProps.getVirtualHost() != null ? rabbitProps.getVirtualHost() : "not configured");
        log.debug("   • Exchange pattern: {}", rabbitProps.getExchange() != null ? rabbitProps.getExchange() : "not configured");
        log.debug("   • Routing key pattern: {}", rabbitProps.getRoutingKey() != null ? rabbitProps.getRoutingKey() : "not configured");
        log.debug("   • Publisher confirms: {}", rabbitProps.isPublisherConfirms());
        log.debug("   • Publisher returns: {}", rabbitProps.isPublisherReturns());
    }

    private void logSqsConfiguration(ApplicationContext ctx, DomainEventsProperties.Sqs sqsProps, boolean explicit) {
        String mode = explicit ? "explicitly configured" : "auto-detected";
        log.info("Using SQS adapter ({}) - events published to AWS SQS", mode);
        
        // Check bean status
        Object sqsClient = DomainEventAdapterUtils.resolveBean(ctx, sqsProps.getClientBeanName(), "software.amazon.awssdk.services.sqs.SqsAsyncClient");
        String sqsClientBeanName = sqsProps.getClientBeanName() != null ? sqsProps.getClientBeanName() : "sqsAsyncClient";
        
        if (sqsClient != null) {
            log.debug("Bean status: SqsAsyncClient bean '{}' found (user-provided)", sqsClientBeanName);
        } else {
            log.debug("Bean status: SqsAsyncClient bean '{}' not found, using Firefly-created infrastructure", sqsClientBeanName);
        }
        
        // Configuration details
        log.debug("AWS SQS configuration:");
        log.debug("   • Region: {}", sqsProps.getRegion() != null ? sqsProps.getRegion() : "not configured");
        log.debug("   • Queue URL: {}", sqsProps.getQueueUrl() != null ? sqsProps.getQueueUrl() : "not configured");
        log.debug("   • Queue name: {}", sqsProps.getQueueName() != null ? sqsProps.getQueueName() : "not configured");
        if (sqsProps.getEndpointOverride() != null) {
            log.debug("   • Endpoint override: {}", sqsProps.getEndpointOverride());
        }
        if (sqsProps.getAccessKeyId() != null) {
            log.debug("   • Credentials: configured (access key provided)");
        } else {
            log.debug("   • Credentials: using default AWS credential chain");
        }
    }

    private void logKinesisConfiguration(ApplicationContext ctx, DomainEventsProperties.Kinesis kinesisProps, boolean explicit) {
        String mode = explicit ? "explicitly configured" : "auto-detected";
        log.info("Using KINESIS adapter ({}) - events published to AWS Kinesis", mode);
        
        // Check bean status
        Object kinesisClient = DomainEventAdapterUtils.resolveBean(ctx, kinesisProps.getClientBeanName(), "software.amazon.awssdk.services.kinesis.KinesisAsyncClient");
        String kinesisClientBeanName = kinesisProps.getClientBeanName() != null ? kinesisProps.getClientBeanName() : "kinesisAsyncClient";
        
        if (kinesisClient != null) {
            log.debug("Bean status: KinesisAsyncClient bean '{}' found (user-provided)", kinesisClientBeanName);
        } else {
            log.debug("Bean status: KinesisAsyncClient bean '{}' not found, using Firefly-created infrastructure", kinesisClientBeanName);
        }
        
        // Configuration details
        log.debug("AWS Kinesis configuration:");
        log.debug("   • Region: {}", kinesisProps.getRegion() != null ? kinesisProps.getRegion() : "not configured");
        log.debug("   • Stream name: {}", kinesisProps.getStreamName() != null ? kinesisProps.getStreamName() : "not configured");
        log.debug("   • Partition key pattern: {}", kinesisProps.getPartitionKey() != null ? kinesisProps.getPartitionKey() : "not configured");
        if (kinesisProps.getEndpointOverride() != null) {
            log.debug("   • Endpoint override: {}", kinesisProps.getEndpointOverride());
        }
        if (kinesisProps.getAccessKeyId() != null) {
            log.debug("   • Credentials: configured (access key provided)");
        } else {
            log.debug("   • Credentials: using default AWS credential chain");
        }
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
