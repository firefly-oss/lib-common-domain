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

package com.firefly.common.domain.events.inbound;

import com.firefly.common.domain.events.DomainEventEnvelope;
import com.firefly.common.domain.events.DomainSpringEvent;
import com.firefly.common.domain.events.properties.DomainEventsProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Inbound SQS subscriber that polls and republishes as DomainSpringEvent using AWS SDK v2 SqsAsyncClient.
 */
public class SqsDomainEventsSubscriber implements SmartLifecycle {

    private final ApplicationContext ctx;
    private final DomainEventsProperties props;
    private final ApplicationEventPublisher events;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sqs-events-subscriber");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean running = false;

    public SqsDomainEventsSubscriber(ApplicationContext ctx,
                                     DomainEventsProperties props,
                                     ApplicationEventPublisher events) {
        this.ctx = ctx;
        this.props = props;
        this.events = events;
    }

    @Override
    public void start() {
        if (running) return;
        long delay = props.getConsumer().getSqs().getPollDelayMillis();
        executor.scheduleWithFixedDelay(this::pollOnceSafe, 0L, delay, TimeUnit.MILLISECONDS);
        running = true;
    }

    private void pollOnceSafe() {
        try { pollOnce(); } catch (Throwable ignored) { }
    }

    private void pollOnce() throws Exception {
        SqsAsyncClient client = ctx.getBean(SqsAsyncClient.class);
        String queueUrl = props.getSqs().getQueueUrl();
        if (queueUrl == null || queueUrl.isEmpty()) {
            String queueName = props.getConsumer().getSqs().getQueueName();
            if (queueName == null || queueName.isEmpty()) queueName = props.getSqs().getQueueName();
            if (queueName == null || queueName.isEmpty()) return;
            queueUrl = client.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).get().queueUrl();
        }
        if (queueUrl == null || queueUrl.isEmpty()) return;
        Integer max = props.getConsumer().getSqs().getMaxMessages();
        Integer wait = props.getConsumer().getSqs().getWaitTimeSeconds();
        ReceiveMessageRequest.Builder br = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl);
        if (max != null) br = br.maxNumberOfMessages(max);
        if (wait != null) br = br.waitTimeSeconds(wait);
        br = br.messageAttributeNames("All");
        ReceiveMessageResponse resp = client.receiveMessage(br.build()).get();
        List<Message> messages = resp.messages();
        if (messages == null || messages.isEmpty()) return;
        for (Message msg : messages) {
            String body = msg.body();
            Map<String, Object> headers = new HashMap<>();
            Map<String, MessageAttributeValue> attrs = msg.messageAttributes();
            if (attrs != null) {
                for (Map.Entry<String, MessageAttributeValue> e : attrs.entrySet()) {
                    headers.put(e.getKey(), e.getValue().stringValue());
                }
            }
            String type = toStringOrNull(headers.get(props.getConsumer().getTypeHeader()));
            String key = toStringOrNull(headers.get(props.getConsumer().getKeyHeader()));
            DomainEventEnvelope env = DomainEventEnvelope.builder()
                    .topic(props.getSqs().getQueueName())
                    .type(type)
                    .key(key)
                    .payload(body)
                    .headers(headers)
                    .build();
            events.publishEvent(new DomainSpringEvent(env));
            // delete
            client.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(msg.receiptHandle()).build());
        }
    }

    private String toStringOrNull(Object o) { return o == null ? null : String.valueOf(o); }

    @Override
    public void stop() {
        executor.shutdownNow();
        running = false;
    }

    @Override
    public boolean isRunning() { return running; }
}
