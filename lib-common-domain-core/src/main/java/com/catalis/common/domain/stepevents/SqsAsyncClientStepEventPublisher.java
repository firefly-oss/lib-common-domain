package com.catalis.common.domain.stepevents;

import com.catalis.common.domain.config.StepEventsProperties;
import com.catalis.transactionalengine.events.StepEventEnvelope;
import com.catalis.transactionalengine.events.StepEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.retry.support.RetryTemplate;
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

    private static final Logger log = LoggerFactory.getLogger(SqsAsyncClientStepEventPublisher.class);
    private final SqsAsyncClient sqsClient;
    private final ApplicationContext ctx;
    private final StepEventsProperties props;
    private final RetryTemplate retryTemplate;

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
        
        // Get retry template if available, otherwise use null (no retry)
        RetryTemplate retryTemplateBean = null;
        try {
            retryTemplateBean = ctx.getBean("domainEventsRetryTemplate", RetryTemplate.class);
            log.debug("SQS step event publisher configured with retry template");
        } catch (Exception e) {
            log.debug("SQS step event publisher configured without retry template");
        }
        this.retryTemplate = retryTemplateBean;
    }

    @Override
    public Mono<Void> publish(StepEventEnvelope e) {
        if (retryTemplate != null) {
            // Use retry template for resilient publishing
            return Mono.fromCallable(() -> retryTemplate.execute(retryContext -> {
                retryContext.setAttribute("operationName", "sqs-step-event-publish:" + e.topic + ":" + e.type);
                return trySendBlocking(e);
            }))
            .then()
            .onErrorMap(throwable -> {
                log.error("Failed to publish step event after retries: topic={}, type={}, key={}", 
                         e.topic, e.type, e.key, throwable);
                return throwable;
            });
        } else {
            // Fallback to reactive logic without retry
            return trySendReactive(e)
                    .onErrorMap(ex -> {
                        log.error("Failed to publish step event to SQS: topic={}, type={}, key={}: {}", 
                                e.topic, e.type, e.key, ex.getMessage());
                        return ex;
                    });
        }
    }

    private Void trySendBlocking(StepEventEnvelope e) {
        try {
            String queueUrl = resolveQueueUrlBlocking(e);
            String messageBody = serializePayload(e);
            
            log.debug("Attempting to send step event to SQS: topic={}, type={}, key={}, queueUrl={}", 
                     e.topic, e.type, e.key, queueUrl);
            
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();
            
            sqsClient.sendMessage(request).get(); // Blocking call for retry template
            
            log.debug("Successfully sent step event to SQS: topic={}, type={}, queueUrl={}", 
                     e.topic, e.type, queueUrl);
            
            return null;
        } catch (Exception ex) {
            log.error("Failed to send step event to SQS: topic={}, type={}, key={}: {}", 
                     e.topic, e.type, e.key, ex.getMessage());
            throw new RuntimeException("SQS step event send failed", ex);
        }
    }

    private Mono<Void> trySendReactive(StepEventEnvelope e) {
        return resolveQueueUrl(e)
                .flatMap(queueUrl -> {
                    String messageBody = serializePayload(e);
                    
                    log.debug("Attempting to send step event to SQS: topic={}, type={}, key={}, queueUrl={}", 
                             e.topic, e.type, e.key, queueUrl);
                    
                    SendMessageRequest request = SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageBody(messageBody)
                            .build();
                    
                    return Mono.fromFuture(sqsClient.sendMessage(request))
                            .doOnSuccess(response -> log.debug("Successfully sent step event to SQS: topic={}, type={}, queueUrl={}", 
                                                              e.topic, e.type, queueUrl))
                            .then();
                });
    }

    private String resolveQueueUrlBlocking(StepEventEnvelope e) throws Exception {
        String direct = props.getSqs().getQueueUrl();
        if (direct != null && !direct.isEmpty()) {
            return direct;
        }
        
        String queueName = props.getSqs().getQueueName() != null && !props.getSqs().getQueueName().isEmpty() 
                ? props.getSqs().getQueueName() : e.topic;
        
        GetQueueUrlRequest request = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();
        
        return sqsClient.getQueueUrl(request).get().queueUrl();
    }

    private Mono<String> resolveQueueUrl(StepEventEnvelope e) {
        String direct = props.getSqs().getQueueUrl();
        if (direct != null && !direct.isEmpty()) {
            return Mono.just(direct);
        }
        
        final String queueName = props.getSqs().getQueueName() != null && !props.getSqs().getQueueName().isEmpty() 
                ? props.getSqs().getQueueName() : e.topic;
        
        GetQueueUrlRequest request = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();
        
        return Mono.fromFuture(sqsClient.getQueueUrl(request))
                .map(GetQueueUrlResponse::queueUrl)
                .onErrorMap(ex -> {
                    log.error("Failed to resolve SQS queue URL for queue '{}': {}", queueName, ex.getMessage());
                    return new IllegalStateException("Could not resolve SQS queue URL for queue: " + queueName, ex);
                });
    }

    private String serializePayload(StepEventEnvelope e) {
        try {
            Object mapperObj = StepEventAdapterUtils.resolveBean(ctx, null,
                    "com.fasterxml.jackson.databind.ObjectMapper");
            if (mapperObj instanceof ObjectMapper mapper) {
                return mapper.writeValueAsString(e.payload);
            }
        } catch (Exception ex) {
            log.warn("Failed to serialize step event payload using ObjectMapper, falling back to String.valueOf: {}", ex.getMessage());
        }
        return String.valueOf(e.payload);
    }
}
