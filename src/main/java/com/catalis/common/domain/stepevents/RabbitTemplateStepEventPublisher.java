package com.catalis.common.domain.stepevents;

import com.catalis.common.domain.config.StepEventsProperties;
import com.catalis.transactionalengine.events.StepEventEnvelope;
import com.catalis.transactionalengine.events.StepEventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;

/**
 * RabbitMQ adapter using Spring AMQP RabbitTemplate.
 */
public class RabbitTemplateStepEventPublisher implements StepEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeTpl;
    private final String routingKeyTpl;

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
    }

    @Override
    public Mono<Void> publish(StepEventEnvelope e) {
        try {
            String exchange = StepEventAdapterUtils.template(exchangeTpl, e.topic, e.type, e.key);
            String routingKey = StepEventAdapterUtils.template(routingKeyTpl, e.topic, e.type, e.key);
            rabbitTemplate.convertAndSend(exchange, routingKey, e.payload);
            return Mono.empty();
        } catch (Exception ex) {
            return Mono.error(ex);
        }
    }
}
