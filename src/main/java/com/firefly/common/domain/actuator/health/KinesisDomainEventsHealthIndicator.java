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
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;

import java.util.concurrent.TimeUnit;

/**
 * Health indicator for Kinesis Domain Events adapter.
 * Checks the availability and health of the KinesisAsyncClient.
 */
public class KinesisDomainEventsHealthIndicator extends DomainEventsHealthIndicator {

    private final ApplicationContext applicationContext;

    public KinesisDomainEventsHealthIndicator(DomainEventsProperties properties, ApplicationContext applicationContext) {
        super(properties);
        this.applicationContext = applicationContext;
    }

    @Override
    protected void performHealthCheck(Health.Builder builder) throws Exception {
        try {
            // Check if KinesisAsyncClient is available
            Object client = DomainEventAdapterUtils.resolveBean(
                    applicationContext,
                    properties.getKinesis().getClientBeanName(),
                    "software.amazon.awssdk.services.kinesis.KinesisAsyncClient"
            );

            if (client == null) {
                builder.down()
                        .withDetail("status", "KinesisAsyncClient not available")
                        .withDetail("adapter", "kinesis")
                        .withDetail("clientBeanName", properties.getKinesis().getClientBeanName());
                return;
            }

            KinesisAsyncClient kinesisClient = (KinesisAsyncClient) client;

            // Check if stream name is configured
            String streamName = properties.getKinesis().getStreamName();
            String partitionKey = properties.getKinesis().getPartitionKey();

            if (streamName == null || streamName.isEmpty()) {
                builder.up()
                        .withDetail("status", "Kinesis client available but no stream name configured")
                        .withDetail("adapter", "kinesis")
                        .withDetail("clientBeanName", properties.getKinesis().getClientBeanName());
                return;
            }

            // Try to test Kinesis connectivity by describing the stream
            try {
                DescribeStreamRequest request = DescribeStreamRequest.builder()
                        .streamName(streamName)
                        .build();

                var streamDescription = kinesisClient.describeStream(request)
                        .get(5, TimeUnit.SECONDS); // Wait for up to 5 seconds

                builder.up()
                        .withDetail("status", "Kinesis stream accessible")
                        .withDetail("adapter", "kinesis")
                        .withDetail("streamName", streamName)
                        .withDetail("partitionKey", partitionKey)
                        .withDetail("clientBeanName", properties.getKinesis().getClientBeanName())
                        .withDetail("streamStatus", streamDescription.streamDescription().streamStatus().toString());

            } catch (Exception e) {
                builder.down()
                        .withDetail("status", "Failed to access Kinesis stream")
                        .withDetail("adapter", "kinesis")
                        .withDetail("streamName", streamName)
                        .withDetail("partitionKey", partitionKey)
                        .withDetail("error", e.getMessage());
            }

        } catch (Exception e) {
            builder.down()
                    .withDetail("status", "Error checking Kinesis health")
                    .withDetail("adapter", "kinesis")
                    .withDetail("error", e.getMessage());
        }
    }
}