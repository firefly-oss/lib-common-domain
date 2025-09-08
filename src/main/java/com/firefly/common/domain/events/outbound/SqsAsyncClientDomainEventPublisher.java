/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.common.domain.events.outbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firefly.common.domain.events.DomainEventEnvelope;
import com.firefly.common.domain.events.properties.DomainEventsProperties;
import com.firefly.common.domain.util.DomainEventAdapterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.retry.support.RetryTemplate;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * AWS SQS adapter for DomainEventPublisher using AWS SDK v2 SqsAsyncClient.
 */
public class SqsAsyncClientDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SqsAsyncClientDomainEventPublisher.class);
    private final SqsAsyncClient sqsClient;
    private final ApplicationContext ctx;
    private final DomainEventsProperties.Sqs props;
    private final RetryTemplate retryTemplate;

    public SqsAsyncClientDomainEventPublisher(ApplicationContext ctx, DomainEventsProperties.Sqs props) {
        this.ctx = ctx;
        this.props = props;
        Object client = DomainEventAdapterUtils.resolveBean(
                ctx,
                props.getClientBeanName(),
                "software.amazon.awssdk.services.sqs.SqsAsyncClient"
        );
        if (client == null) {
            throw new IllegalStateException("SQS adapter selected but SqsAsyncClient bean was not found.");
        }
        this.sqsClient = (SqsAsyncClient) client;
        
        // Get retry template if available, otherwise use null (no retry)
        RetryTemplate retryTemplateBean = null;
        try {
            retryTemplateBean = ctx.getBean("domainEventsRetryTemplate", RetryTemplate.class);
            log.debug("SQS publisher configured with retry template");
        } catch (Exception e) {
            log.debug("SQS publisher configured without retry template");
        }
        this.retryTemplate = retryTemplateBean;
    }

    @Override
    public Mono<Void> publish(DomainEventEnvelope e) {
        if (retryTemplate != null) {
            // Use retry template for resilient publishing
            return Mono.fromCallable(() -> retryTemplate.execute(retryContext -> {
                retryContext.setAttribute("operationName", "sqs-event-publish:" + e.getTopic() + ":" + e.getType());
                return trySendBlocking(e);
            }))
            .then()
            .onErrorMap(throwable -> {
                log.error("Failed to publish event after retries: topic={}, type={}, key={}", 
                         e.getTopic(), e.getType(), e.getKey(), throwable);
                return throwable;
            });
        } else {
            // Fallback to reactive logic without retry
            return trySendReactive(e)
                    .onErrorMap(ex -> {
                        log.error("Failed to publish event to SQS: topic={}, type={}, key={}: {}", 
                                e.getTopic(), e.getType(), e.getKey(), ex.getMessage());
                        return ex;
                    });
        }
    }

    private Void trySendBlocking(DomainEventEnvelope e) {
        try {
            String queueUrl = resolveQueueUrlBlocking(e);
            String messageBody = serializePayload(e);
            
            log.debug("Attempting to send event to SQS: topic={}, type={}, key={}, queueUrl={}", 
                     e.getTopic(), e.getType(), e.getKey(), queueUrl);
            
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();
            
            sqsClient.sendMessage(request).get(); // Blocking call for retry template
            
            log.debug("Successfully sent event to SQS: topic={}, type={}, queueUrl={}", 
                     e.getTopic(), e.getType(), queueUrl);
            
            return null;
        } catch (Exception ex) {
            log.error("Failed to send event to SQS: topic={}, type={}, key={}: {}", 
                     e.getTopic(), e.getType(), e.getKey(), ex.getMessage());
            throw new RuntimeException("SQS send failed", ex);
        }
    }

    private Mono<Void> trySendReactive(DomainEventEnvelope e) {
        return resolveQueueUrl(e)
                .flatMap(queueUrl -> {
                    String messageBody = serializePayload(e);
                    
                    log.debug("Attempting to send event to SQS: topic={}, type={}, key={}, queueUrl={}", 
                             e.getTopic(), e.getType(), e.getKey(), queueUrl);
                    
                    SendMessageRequest request = SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageBody(messageBody)
                            .build();
                    
                    return Mono.fromFuture(sqsClient.sendMessage(request))
                            .doOnSuccess(response -> log.debug("Successfully sent event to SQS: topic={}, type={}, queueUrl={}", 
                                                              e.getTopic(), e.getType(), queueUrl))
                            .then();
                });
    }

    private String resolveQueueUrlBlocking(DomainEventEnvelope e) throws Exception {
        String direct = props.getQueueUrl();
        if (direct != null && !direct.isEmpty()) {
            return direct;
        }
        
        String queueName = props.getQueueName() != null && !props.getQueueName().isEmpty() 
                ? props.getQueueName() : e.getTopic();
        
        GetQueueUrlRequest request = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();
        
        return sqsClient.getQueueUrl(request).get().queueUrl();
    }

    private Mono<String> resolveQueueUrl(DomainEventEnvelope e) {
        String direct = props.getQueueUrl();
        if (direct != null && !direct.isEmpty()) {
            return Mono.just(direct);
        }
        
        final String queueName = props.getQueueName() != null && !props.getQueueName().isEmpty() 
                ? props.getQueueName() : e.getTopic();
        
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

    private String serializePayload(DomainEventEnvelope e) {
        try {
            Object mapperObj = DomainEventAdapterUtils.resolveBean(ctx, null,
                    "com.fasterxml.jackson.databind.ObjectMapper");
            if (mapperObj instanceof ObjectMapper mapper) {
                return mapper.writeValueAsString(e.getPayload());
            }
        } catch (Exception ex) {
            log.warn("Failed to serialize payload using ObjectMapper, falling back to String.valueOf: {}", ex.getMessage());
        }
        return String.valueOf(e.getPayload());
    }
}
