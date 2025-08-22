package com.catalis.common.domain.stepevents;

import com.catalis.common.domain.config.StepEventsProperties;
import com.catalis.transactionalengine.events.StepEventEnvelope;
import com.catalis.transactionalengine.events.StepEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.concurrent.CompletableFuture;

/**
 * AWS SQS adapter using AWS SDK v2 SqsAsyncClient.
 */
public class SqsAsyncClientStepEventPublisher implements StepEventPublisher {

    private final SqsAsyncClient sqsClient;
    private final ApplicationContext ctx;
    private final StepEventsProperties props;

    public SqsAsyncClientStepEventPublisher(ApplicationContext ctx, StepEventsProperties props) {
        this.ctx = ctx;
        this.props = props;
        Object client = StepEventAdapterUtils.resolveBean(
                ctx,
                props.getSqs().getClientBeanName(),
                "software.amazon.awssdk.services.sqs.SqsAsyncClient"
        );
        if (client == null) {
            throw new IllegalStateException("SQS adapter selected but SqsAsyncClient bean was not found. Add AWS SDK v2 SQS and define a SqsAsyncClient bean or set catalis.stepevents.sqs.clientBeanName.");
        }
        this.sqsClient = (SqsAsyncClient) client;
    }

    @Override
    public Mono<Void> publish(StepEventEnvelope e) {
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

    private String resolveQueueUrl(StepEventEnvelope e) throws Exception {
        String direct = props.getSqs().getQueueUrl();
        if (direct != null && !direct.isEmpty()) return direct;
        String queueName = props.getSqs().getQueueName();
        if (queueName == null || queueName.isEmpty()) queueName = e.topic;
        CompletableFuture<GetQueueUrlResponse> cf = sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build());
        return cf.get().queueUrl();
    }

    private String serializePayload(StepEventEnvelope e) {
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
