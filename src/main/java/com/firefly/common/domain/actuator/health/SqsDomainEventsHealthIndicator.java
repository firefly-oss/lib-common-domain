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

package com.firefly.common.domain.actuator.health;

import com.firefly.common.domain.events.properties.DomainEventsProperties;
import com.firefly.common.domain.util.DomainEventAdapterUtils;
import org.springframework.boot.actuate.health.Health;
import org.springframework.context.ApplicationContext;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Health indicator for SQS Domain Events adapter.
 * Checks the availability and health of the SqsAsyncClient.
 */
public class SqsDomainEventsHealthIndicator extends DomainEventsHealthIndicator {

    private final ApplicationContext applicationContext;

    public SqsDomainEventsHealthIndicator(DomainEventsProperties properties, ApplicationContext applicationContext) {
        super(properties);
        this.applicationContext = applicationContext;
    }

    @Override
    protected void performHealthCheck(Health.Builder builder) throws Exception {
        try {
            // Check if SqsAsyncClient is available
            Object client = DomainEventAdapterUtils.resolveBean(
                    applicationContext,
                    properties.getSqs().getClientBeanName(),
                    "software.amazon.awssdk.services.sqs.SqsAsyncClient"
            );

            if (client == null) {
                builder.down()
                        .withDetail("status", "SqsAsyncClient not available")
                        .withDetail("adapter", "sqs")
                        .withDetail("clientBeanName", properties.getSqs().getClientBeanName());
                return;
            }

            SqsAsyncClient sqsClient = (SqsAsyncClient) client;

            // Check if queue URL or name is configured
            String queueUrl = properties.getSqs().getQueueUrl();
            String queueName = properties.getSqs().getQueueName();

            if ((queueUrl == null || queueUrl.isEmpty()) && (queueName == null || queueName.isEmpty())) {
                builder.up()
                        .withDetail("status", "SQS client available but no queue configured")
                        .withDetail("adapter", "sqs")
                        .withDetail("clientBeanName", properties.getSqs().getClientBeanName());
                return;
            }

            // Try to test SQS connectivity by getting queue attributes
            try {
                String testUrl = queueUrl;
                if (testUrl == null || testUrl.isEmpty()) {
                    // If we only have queue name, we can't easily test without additional AWS calls
                    builder.up()
                            .withDetail("status", "SQS client available, queue name configured")
                            .withDetail("adapter", "sqs")
                            .withDetail("queueName", queueName)
                            .withDetail("clientBeanName", properties.getSqs().getClientBeanName());
                    return;
                }

                GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
                        .queueUrl(testUrl)
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build();

                CompletableFuture<Void> healthCheck = sqsClient.getQueueAttributes(request)
                        .thenApply(response -> {
                            // If we can get queue attributes, SQS is healthy
                            return null;
                        });

                // Wait for up to 5 seconds for the health check
                healthCheck.get(5, TimeUnit.SECONDS);

                builder.up()
                        .withDetail("status", "SQS queue accessible")
                        .withDetail("adapter", "sqs")
                        .withDetail("queueUrl", queueUrl)
                        .withDetail("queueName", queueName)
                        .withDetail("clientBeanName", properties.getSqs().getClientBeanName());

            } catch (Exception e) {
                builder.down()
                        .withDetail("status", "Failed to access SQS queue")
                        .withDetail("adapter", "sqs")
                        .withDetail("queueUrl", queueUrl)
                        .withDetail("queueName", queueName)
                        .withDetail("error", e.getMessage());
            }

        } catch (Exception e) {
            builder.down()
                    .withDetail("status", "Error checking SQS health")
                    .withDetail("adapter", "sqs")
                    .withDetail("error", e.getMessage());
        }
    }
}