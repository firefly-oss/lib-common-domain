package com.catalis.common.domain.stepevents;

import com.catalis.common.domain.config.StepEventsProperties;
import com.catalis.transactionalengine.events.StepEventEnvelope;
import com.catalis.transactionalengine.events.StepEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.retry.support.RetryTemplate;
import reactor.core.publisher.Mono;

/**
 * RabbitMQ adapter using Spring AMQP RabbitTemplate.
 */
public class RabbitTemplateStepEventPublisher implements StepEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitTemplateStepEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final String exchangeTpl;
    private final String routingKeyTpl;
    private final RetryTemplate retryTemplate;

    public RabbitTemplateStepEventPublisher(ApplicationContext ctx, StepEventsProperties props) {
        Object template = StepEventAdapterUtils.resolveBean(
                ctx,
                props.getRabbit().getTemplateBeanName(),
                "org.springframework.amqp.rabbit.core.RabbitTemplate"
        );
        if (template == null) {
            throw new IllegalStateException("Rabbit adapter selected but RabbitTemplate bean was not found. Add spring-amqp/spring-rabbit dependency and define a RabbitTemplate bean or set catalis.stepevents.rabbit.templateBeanName.");
        }
        this.rabbitTemplate = (RabbitTemplate) template;
        this.exchangeTpl = props.getRabbit().getExchange();
        this.routingKeyTpl = props.getRabbit().getRoutingKey();
        
        // Get retry template if available, otherwise use null (no retry)
        RetryTemplate retryTemplateBean = null;
        try {
            retryTemplateBean = ctx.getBean("domainEventsRetryTemplate", RetryTemplate.class);
            log.debug("RabbitMQ step event publisher configured with retry template");
        } catch (Exception e) {
            log.debug("RabbitMQ step event publisher configured without retry template");
        }
        this.retryTemplate = retryTemplateBean;
    }

    @Override
    public Mono<Void> publish(StepEventEnvelope e) {
        if (retryTemplate != null) {
            // Use retry template for resilient publishing
            return Mono.fromCallable(() -> retryTemplate.execute(retryContext -> {
                retryContext.setAttribute("operationName", "rabbit-step-event-publish:" + e.topic + ":" + e.type);
                return trySend(e);
            }))
            .then()
            .onErrorMap(throwable -> {
                log.error("Failed to publish step event after retries: topic={}, type={}, key={}", 
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

    private Void trySend(StepEventEnvelope e) {
        try {
            String exchange = StepEventAdapterUtils.template(exchangeTpl, e.topic, e.type, e.key);
            String routingKey = StepEventAdapterUtils.template(routingKeyTpl, e.topic, e.type, e.key);
            
            log.debug("Attempting to send step event to RabbitMQ: topic={}, type={}, key={}, exchange={}, routingKey={}", 
                     e.topic, e.type, e.key, exchange, routingKey);
            
            rabbitTemplate.convertAndSend(exchange, routingKey, e.payload);
            
            log.debug("Successfully sent step event to RabbitMQ: topic={}, type={}, exchange={}, routingKey={}", 
                     e.topic, e.type, exchange, routingKey);
            
            return null;
        } catch (Exception ex) {
            log.error("Failed to send step event to RabbitMQ: topic={}, type={}, key={}: {}", 
                     e.topic, e.type, e.key, ex.getMessage());
            throw ex;
        }
    }
}