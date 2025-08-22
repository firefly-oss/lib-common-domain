package com.catalis.common.domain.events.outbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import com.catalis.common.domain.events.properties.DomainEventsProperties;
import com.catalis.common.domain.stepevents.StepEventAdapterUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.concurrent.CompletableFuture;

/**
 * AWS SQS adapter for DomainEventPublisher using AWS SDK v2 SqsAsyncClient.
 */
public class SqsAsyncClientDomainEventPublisher implements DomainEventPublisher {

    private final SqsAsyncClient sqsClient;
    private final ApplicationContext ctx;
    private final DomainEventsProperties.Sqs props;

    public SqsAsyncClientDomainEventPublisher(ApplicationContext ctx, DomainEventsProperties.Sqs props) {
        this.ctx = ctx;
        this.props = props;
        Object client = StepEventAdapterUtils.resolveBean(
                ctx,
                props.getClientBeanName(),
                "software.amazon.awssdk.services.sqs.SqsAsyncClient"
        );
        if (client == null) {
            throw new IllegalStateException("SQS adapter selected but SqsAsyncClient bean was not found.");
        }
        this.sqsClient = (SqsAsyncClient) client;
    }

    @Override
    public Mono<Void> publish(DomainEventEnvelope e) {
        try {
            String queueUrl = resolveQueueUrl(e);
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(serializePayload(e))
                    .build();
            CompletableFuture<?> future = sqsClient.sendMessage(request);
            return Mono.fromFuture(future).then();
        } catch (Exception ex) {
            return Mono.error(ex);
        }
    }

    private String resolveQueueUrl(DomainEventEnvelope e) throws Exception {
        String direct = props.getQueueUrl();
        if (direct != null && !direct.isEmpty()) return direct;
        String queueName = props.getQueueName();
        if (queueName == null || queueName.isEmpty()) queueName = e.topic;
        CompletableFuture<GetQueueUrlResponse> cf = sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build());
        return cf.get().queueUrl();
    }

    private String serializePayload(DomainEventEnvelope e) {
        try {
            Object mapperObj = StepEventAdapterUtils.resolveBean(ctx, null,
                    "com.fasterxml.jackson.databind.ObjectMapper");
            if (mapperObj instanceof ObjectMapper mapper) {
                return mapper.writeValueAsString(e.payload);
            }
        } catch (Exception ignored) {}
        return String.valueOf(e.payload);
    }
}
