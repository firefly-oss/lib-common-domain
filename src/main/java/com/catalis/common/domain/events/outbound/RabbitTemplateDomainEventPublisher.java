package com.catalis.common.domain.events.outbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import com.catalis.common.domain.events.properties.DomainEventsProperties.Rabbit;
import com.catalis.common.domain.stepevents.StepEventAdapterUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;

/**
 * RabbitMQ adapter for DomainEventPublisher using Spring AMQP RabbitTemplate.
 */
public class RabbitTemplateDomainEventPublisher implements DomainEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeTpl;
    private final String routingKeyTpl;

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
    }

    @Override
    public Mono<Void> publish(DomainEventEnvelope e) {
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
