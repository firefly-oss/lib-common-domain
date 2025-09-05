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

package com.firefly.common.domain.events.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "firefly.events")
@Validated
@Data
public class DomainEventsProperties {

    public enum Adapter { AUTO, APPLICATION_EVENT, KAFKA, RABBIT, SQS, KINESIS, NOOP }

    private boolean enabled = true;
    
    @NotNull(message = "Adapter cannot be null")
    private Adapter adapter = Adapter.AUTO;

    @Valid
    private final Kafka kafka = new Kafka();
    
    @Valid
    private final Rabbit rabbit = new Rabbit();
    
    @Valid
    private final Sqs sqs = new Sqs();
    
    @Valid
    private final Kinesis kinesis = new Kinesis();
    
    @Valid
    private final Consumer consumer = new Consumer();

    @Data
    public static class Kafka {
        private String templateBeanName;
        private boolean useMessagingIfAvailable = true;
        
        // Kafka-specific properties
        @Size(min = 1, message = "Bootstrap servers cannot be empty")
        private String bootstrapServers;
        
        private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";
        private String valueSerializer = "org.apache.kafka.common.serialization.StringSerializer";
        
        @Min(value = 0, message = "Retries must be 0 or greater")
        private Integer retries;
        
        @Min(value = 1, message = "Batch size must be 1 or greater")
        private Integer batchSize;
        
        @Min(value = 0, message = "Linger ms must be 0 or greater")
        private Integer lingerMs;
        
        @Min(value = 1, message = "Buffer memory must be 1 or greater")
        private Long bufferMemory;
        
        @Pattern(regexp = "^(none|all|1|-1)$", message = "Acks must be 'none', 'all', '1', or '-1'")
        private String acks;
        
        // Additional properties for advanced configuration
        private Map<String, Object> properties = new HashMap<>();
    }

    @Data
    public static class Rabbit {
        private String templateBeanName;
        private String exchange = "${topic}";
        private String routingKey = "${type}";
        
        // RabbitMQ connection properties for infrastructure creation
        private String host = "localhost";
        
        @Min(value = 1, message = "Port must be greater than 0")
        @Max(value = 65535, message = "Port must be less than 65536")
        private Integer port = 5672;
        
        private String username = "guest";
        private String password = "guest";
        private String virtualHost = "/";
        
        // Connection factory settings
        private Integer connectionTimeout;
        private Integer requestedHeartbeat;
        private boolean publisherConfirms = false;
        private boolean publisherReturns = false;
    }

    @Data
    public static class Sqs {
        private String clientBeanName;
        
        @Pattern(regexp = "^https://sqs\\.[a-z0-9-]+\\.amazonaws\\.com/\\d+/.+$", 
                message = "Queue URL must be a valid SQS URL format")
        private String queueUrl;
        
        @Size(min = 1, max = 80, message = "Queue name must be between 1 and 80 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+(\\.fifo)?$", 
                message = "Queue name can only contain alphanumeric characters, hyphens, underscores, and optional .fifo suffix")
        private String queueName;
        
        // AWS configuration properties for infrastructure creation
        @Pattern(regexp = "^[a-z0-9-]+$", message = "AWS region must be in valid format (e.g., us-east-1)")
        private String region;
        
        // AWS credentials (optional - will use default credential chain if not provided)
        private String accessKeyId;
        private String secretAccessKey;
        private String sessionToken;
        
        // SQS-specific client configuration
        private String endpointOverride;
        private Integer maxConcurrency;
        private Integer connectionTimeoutMillis;
        private Integer socketTimeoutMillis;
    }

    @Data
    public static class Kinesis {
        private String clientBeanName;
        
        @Size(min = 1, max = 128, message = "Stream name must be between 1 and 128 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", 
                message = "Stream name can only contain alphanumeric characters, hyphens, underscores, and periods")
        private String streamName;
        
        @Size(min = 1, max = 256, message = "Partition key must be between 1 and 256 characters")
        private String partitionKey = "${key}";
        
        // AWS configuration properties for infrastructure creation
        @Pattern(regexp = "^[a-z0-9-]+$", message = "AWS region must be in valid format (e.g., us-east-1)")
        private String region;
        
        // AWS credentials (optional - will use default credential chain if not provided)
        private String accessKeyId;
        private String secretAccessKey;
        private String sessionToken;
        
        // Kinesis-specific client configuration
        private String endpointOverride;
        private Integer maxConcurrency;
        private Integer connectionTimeoutMillis;
        private Integer socketTimeoutMillis;
    }

    @Data
    public static class Consumer {
        private boolean enabled = false;
        
        @NotBlank(message = "Type header cannot be blank")
        @Size(max = 255, message = "Type header must not exceed 255 characters")
        private String typeHeader = "event_type";
        
        @NotBlank(message = "Key header cannot be blank")
        @Size(max = 255, message = "Key header must not exceed 255 characters")
        private String keyHeader = "event_key";
        
        @Valid
        private final KafkaConsumer kafka = new KafkaConsumer();
        
        @Valid
        private final RabbitConsumer rabbit = new RabbitConsumer();
        
        @Valid
        private final SqsConsumer sqs = new SqsConsumer();
        
        @Valid
        private final KinesisConsumer kinesis = new KinesisConsumer();

        @Data
        public static class KafkaConsumer {
            private List<String> topics = new ArrayList<>();
            private String consumerFactoryBeanName; // optional override
            private String groupId; // optional
        }

        @Data
        public static class RabbitConsumer {
            private List<String> queues = new ArrayList<>();
        }

        @Data
        public static class SqsConsumer {
            @Pattern(regexp = "^https://sqs\\.[a-z0-9-]+\\.amazonaws\\.com/\\d+/.+$", 
                    message = "Queue URL must be a valid SQS URL format")
            private String queueUrl;
            
            @Size(min = 1, max = 80, message = "Queue name must be between 1 and 80 characters")
            @Pattern(regexp = "^[a-zA-Z0-9_-]+(\\.fifo)?$", 
                    message = "Queue name can only contain alphanumeric characters, hyphens, underscores, and optional .fifo suffix")
            private String queueName;
            
            @Min(value = 0, message = "Wait time seconds must be between 0 and 20")
            @Max(value = 20, message = "Wait time seconds must be between 0 and 20")
            private Integer waitTimeSeconds = 10;
            
            @Min(value = 1, message = "Max messages must be between 1 and 10")
            @Max(value = 10, message = "Max messages must be between 1 and 10")
            private Integer maxMessages = 10;
            
            @Min(value = 100, message = "Poll delay must be at least 100 milliseconds")
            @Max(value = 300000, message = "Poll delay must not exceed 300000 milliseconds (5 minutes)")
            private Long pollDelayMillis = 1000L;
        }

        @Data
        public static class KinesisConsumer {
            @Size(min = 1, max = 128, message = "Stream name must be between 1 and 128 characters")
            @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", 
                    message = "Stream name can only contain alphanumeric characters, hyphens, underscores, and periods")
            private String streamName;
            
            @Size(min = 1, max = 256, message = "Application name must be between 1 and 256 characters")
            private String applicationName = "domain-events-consumer";
            
            @Min(value = 1000, message = "Poll delay must be at least 1000 milliseconds")
            @Max(value = 300000, message = "Poll delay must not exceed 300000 milliseconds (5 minutes)")
            private Long pollDelayMillis = 5000L;
        }
    }
}
