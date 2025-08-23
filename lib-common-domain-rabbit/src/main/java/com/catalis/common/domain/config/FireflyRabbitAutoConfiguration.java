package com.catalis.common.domain.config;

import com.catalis.common.domain.events.outbound.DomainEventPublisher;
import com.catalis.common.domain.events.outbound.RabbitTemplateDomainEventPublisher;
import com.catalis.common.domain.events.properties.DomainEventsProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for RabbitMQ domain events using firefly properties.
 * This configuration creates RabbitTemplate beans and DomainEventPublisher
 * when RabbitMQ connection factory is available and rabbit properties are configured.
 */
@AutoConfiguration(before = RabbitAutoConfiguration.class)
@ConditionalOnClass({RabbitTemplate.class, ConnectionFactory.class})
@EnableConfigurationProperties(DomainEventsProperties.class)
@ConditionalOnProperty(prefix = "firefly.events.rabbit", name = "exchange")
public class FireflyRabbitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "fireflyRabbitTemplate")
    public RabbitTemplate fireflyRabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

    @Bean(name = "rabbitTemplate")
    @ConditionalOnMissingBean(name = "rabbitTemplate")
    public RabbitTemplate rabbitTemplate(RabbitTemplate fireflyRabbitTemplate) {
        // Provide the default rabbitTemplate bean if none exists
        return fireflyRabbitTemplate;
    }

    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    public DomainEventPublisher domainEventPublisher(ApplicationContext ctx, DomainEventsProperties properties) {
        return new RabbitTemplateDomainEventPublisher(ctx, properties.getRabbit());
    }
}