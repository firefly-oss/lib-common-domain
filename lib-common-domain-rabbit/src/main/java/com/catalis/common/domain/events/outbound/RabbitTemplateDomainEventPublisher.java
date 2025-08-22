package com.catalis.common.domain.events.outbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import com.catalis.common.domain.events.properties.DomainEventsProperties.Rabbit;
import com.catalis.common.domain.stepevents.StepEventAdapterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.retry.support.RetryTemplate;
import reactor.core.publisher.Mono;

/**
 * RabbitMQ adapter for DomainEventPublisher using Spring AMQP RabbitTemplate.
 */
public class RabbitTemplateDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitTemplateDomainEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final String exchangeTpl;
    private final String routingKeyTpl;
    private final RetryTemplate retryTemplate;

    public RabbitTemplateDomainEventPublisher(ApplicationContext ctx, Rabbit props) {
        Object template = StepEventAdapterUtils.resolveBean(
                ctx,
                props.getTemplateBeanName(),
                "org.springframework.amqp.rabbit.core.RabbitTemplate"
        );
        if (template == null) {
            throw new IllegalStateException("Rabbit adapter selected but RabbitTemplate bean was not found.");
        }
        this.rabbitTemplate = (RabbitTemplate) template;
        this.exchangeTpl = props.getExchange();
        this.routingKeyTpl = props.getRoutingKey();
        
        // Get retry template if available, otherwise use null (no retry)
        RetryTemplate retryTemplateBean = null;
        try {
            retryTemplateBean = ctx.getBean("domainEventsRetryTemplate", RetryTemplate.class);
            log.debug("RabbitMQ publisher configured with retry template");
        } catch (Exception e) {
            log.debug("RabbitMQ publisher configured without retry template");
        }
        this.retryTemplate = retryTemplateBean;
    }

    @Override
    public Mono<Void> publish(DomainEventEnvelope e) {
        if (retryTemplate != null) {
            // Use retry template for resilient publishing
            return Mono.fromCallable(() -> retryTemplate.execute(retryContext -> {
                retryContext.setAttribute("operationName", "rabbit-event-publish:" + e.topic + ":" + e.type);
                return trySend(e);
            }))
            .then()
            .onErrorMap(throwable -> {
                log.error("Failed to publish event after retries: topic={}, type={}, key={}", 
                         e.topic, e.type, e.key, throwable);
                return throwable;
            });
        } else {
            // Fallback to original logic without retry
            try {
                trySend(e);
                return Mono.empty();
            } catch (Exception ex) {
                return Mono.error(ex);
            }
        }
    }

    private Void trySend(DomainEventEnvelope e) {
        try {
            String exchange = StepEventAdapterUtils.template(exchangeTpl, e.topic, e.type, e.key);
            String routingKey = StepEventAdapterUtils.template(routingKeyTpl, e.topic, e.type, e.key);
            
            log.debug("Attempting to send event to RabbitMQ: topic={}, type={}, key={}, exchange={}, routingKey={}", 
                     e.topic, e.type, e.key, exchange, routingKey);
            
            rabbitTemplate.convertAndSend(exchange, routingKey, e.payload);
            
            log.debug("Successfully sent event to RabbitMQ: topic={}, type={}, exchange={}, routingKey={}", 
                     e.topic, e.type, exchange, routingKey);
            
            return null;
        } catch (Exception ex) {
            log.error("Failed to send event to RabbitMQ: topic={}, type={}, key={}: {}", 
                     e.topic, e.type, e.key, ex.getMessage());
            throw ex;
        }
    }
}