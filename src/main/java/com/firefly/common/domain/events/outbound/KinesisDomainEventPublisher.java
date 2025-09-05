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

import com.firefly.common.domain.events.DomainEventEnvelope;
import com.firefly.common.domain.events.properties.DomainEventsProperties;
import com.firefly.common.domain.util.DomainEventAdapterUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.retry.support.RetryTemplate;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.core.SdkBytes;

/**
 * AWS Kinesis adapter for DomainEventPublisher using AWS SDK v2 KinesisAsyncClient.
 */
public class KinesisDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KinesisDomainEventPublisher.class);
    private final KinesisAsyncClient kinesisClient;
    private final ApplicationContext ctx;
    private final DomainEventsProperties.Kinesis props;
    private final RetryTemplate retryTemplate;

    public KinesisDomainEventPublisher(ApplicationContext ctx, DomainEventsProperties.Kinesis props) {
        this.ctx = ctx;
        this.props = props;
        Object client = DomainEventAdapterUtils.resolveBean(
                ctx,
                props.getClientBeanName(),
                "software.amazon.awssdk.services.kinesis.KinesisAsyncClient"
        );
        if (client == null) {
            throw new IllegalStateException("Kinesis adapter selected but KinesisAsyncClient bean was not found.");
        }
        this.kinesisClient = (KinesisAsyncClient) client;
        
        // Get retry template if available, otherwise use null (no retry)
        RetryTemplate retryTemplateBean = null;
        try {
            retryTemplateBean = ctx.getBean("domainEventsRetryTemplate", RetryTemplate.class);
            log.debug("Kinesis publisher configured with retry template");
        } catch (Exception e) {
            log.debug("Kinesis publisher configured without retry template");
        }
        this.retryTemplate = retryTemplateBean;
    }

    @Override
    public Mono<Void> publish(DomainEventEnvelope e) {
        if (retryTemplate != null) {
            // Use retry template for resilient publishing
            return Mono.fromCallable(() -> retryTemplate.execute(retryContext -> {
                retryContext.setAttribute("operationName", "kinesis-event-publish:" + e.getTopic() + ":" + e.getType());
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
                        log.error("Failed to publish event to Kinesis: topic={}, type={}, key={}: {}", 
                                e.getTopic(), e.getType(), e.getKey(), ex.getMessage());
                        return ex;
                    });
        }
    }

    private Void trySendBlocking(DomainEventEnvelope e) {
        try {
            String streamName = resolveStreamName(e);
            String partitionKey = resolvePartitionKey(e);
            String messageData = serializePayload(e);
            
            log.debug("Attempting to send event to Kinesis: topic={}, type={}, key={}, streamName={}, partitionKey={}", 
                     e.getTopic(), e.getType(), e.getKey(), streamName, partitionKey);
            
            PutRecordRequest request = PutRecordRequest.builder()
                    .streamName(streamName)
                    .partitionKey(partitionKey)
                    .data(SdkBytes.fromUtf8String(messageData))
                    .build();
            
            kinesisClient.putRecord(request).get(); // Blocking call for retry template
            
            log.debug("Successfully sent event to Kinesis: topic={}, type={}, streamName={}, partitionKey={}", 
                     e.getTopic(), e.getType(), streamName, partitionKey);
            
            return null;
        } catch (Exception ex) {
            log.error("Failed to send event to Kinesis: topic={}, type={}, key={}: {}", 
                     e.getTopic(), e.getType(), e.getKey(), ex.getMessage());
            throw new RuntimeException("Kinesis send failed", ex);
        }
    }

    private Mono<Void> trySendReactive(DomainEventEnvelope e) {
        String streamName = resolveStreamName(e);
        String partitionKey = resolvePartitionKey(e);
        String messageData = serializePayload(e);
        
        log.debug("Attempting to send event to Kinesis: topic={}, type={}, key={}, streamName={}, partitionKey={}", 
                 e.getTopic(), e.getType(), e.getKey(), streamName, partitionKey);
        
        PutRecordRequest request = PutRecordRequest.builder()
                .streamName(streamName)
                .partitionKey(partitionKey)
                .data(SdkBytes.fromUtf8String(messageData))
                .build();
        
        return Mono.fromFuture(kinesisClient.putRecord(request))
                .doOnSuccess(response -> log.debug("Successfully sent event to Kinesis: topic={}, type={}, streamName={}, partitionKey={}, sequenceNumber={}", 
                                                  e.getTopic(), e.getType(), streamName, partitionKey, response.sequenceNumber()))
                .then();
    }

    private String resolveStreamName(DomainEventEnvelope e) {
        String streamName = props.getStreamName();
        if (streamName != null && !streamName.isEmpty()) {
            return streamName;
        }
        // Use topic as stream name if no specific stream configured
        return e.getTopic();
    }

    private String resolvePartitionKey(DomainEventEnvelope e) {
        String configuredPartitionKey = props.getPartitionKey();
        if (configuredPartitionKey != null && !configuredPartitionKey.isEmpty()) {
            // Support placeholder replacement
            return configuredPartitionKey
                    .replace("${topic}", e.getTopic() != null ? e.getTopic() : "")
                    .replace("${type}", e.getType() != null ? e.getType() : "")
                    .replace("${key}", e.getKey() != null ? e.getKey() : "");
        }
        // Use event key as partition key if available, otherwise use topic
        return e.getKey() != null ? e.getKey() : e.getTopic();
    }

    private String serializePayload(DomainEventEnvelope e) {
        try {
            Object mapperObj = DomainEventAdapterUtils.resolveBean(ctx, null,
                    "com.fasterxml.jackson.databind.ObjectMapper");
            if (mapperObj instanceof ObjectMapper mapper) {
                // Create a wrapper object that includes event metadata
                var eventData = new java.util.HashMap<String, Object>();
                eventData.put("topic", e.getTopic());
                eventData.put("type", e.getType());
                eventData.put("key", e.getKey());
                eventData.put("payload", e.getPayload());
                eventData.put("headers", e.getHeaders());
                return mapper.writeValueAsString(eventData);
            }
        } catch (Exception ex) {
            log.warn("Failed to serialize payload using ObjectMapper, falling back to String.valueOf: {}", ex.getMessage());
        }
        return String.valueOf(e.getPayload());
    }
}