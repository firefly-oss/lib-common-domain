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

package com.firefly.common.domain.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for retry mechanisms in domain events publishing.
 * Provides RetryTemplate with exponential backoff for resilient event publishing.
 */
@Configuration
@EnableRetry
@ConditionalOnClass(RetryTemplate.class)
public class DomainEventsRetryConfig {

    private static final Logger log = LoggerFactory.getLogger(DomainEventsRetryConfig.class);

    /**
     * RetryTemplate for event publishing operations with exponential backoff.
     */
    @Bean("domainEventsRetryTemplate")
    public RetryTemplate domainEventsRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Configure retry policy
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        
        // Retry on common transient exceptions
        retryableExceptions.put(org.springframework.dao.TransientDataAccessException.class, true);
        retryableExceptions.put(org.springframework.web.client.ResourceAccessException.class, true);
        retryableExceptions.put(java.net.ConnectException.class, true);
        retryableExceptions.put(java.net.SocketTimeoutException.class, true);
        retryableExceptions.put(java.util.concurrent.TimeoutException.class, true);
        
        // Add Kafka specific exceptions
        try {
            Class<?> kafkaException = Class.forName("org.apache.kafka.common.errors.RetriableException");
            retryableExceptions.put((Class<? extends Throwable>) kafkaException, true);
        } catch (ClassNotFoundException e) {
            log.debug("Kafka exceptions not available on classpath");
        }
        
        // Add RabbitMQ specific exceptions
        try {
            Class<?> amqpException = Class.forName("org.springframework.amqp.AmqpException");
            retryableExceptions.put((Class<? extends Throwable>) amqpException, true);
        } catch (ClassNotFoundException e) {
            log.debug("RabbitMQ exceptions not available on classpath");
        }
        
        // Add AWS SQS specific exceptions
        try {
            Class<?> sqsException = Class.forName("software.amazon.awssdk.core.exception.SdkException");
            retryableExceptions.put((Class<? extends Throwable>) sqsException, true);
        } catch (ClassNotFoundException e) {
            log.debug("AWS SDK exceptions not available on classpath");
        }

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);

        // Configure exponential backoff
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L); // Start with 1 second
        backOffPolicy.setMultiplier(2.0); // Double the interval each retry
        backOffPolicy.setMaxInterval(30000L); // Max 30 seconds between retries
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Add retry listener for logging
        retryTemplate.setListeners(new RetryListener[] { new DomainEventsRetryListener() });

        log.info("Configured domain events retry template with exponential backoff: maxAttempts=3, initialInterval=1s, multiplier=2.0, maxInterval=30s");
        
        return retryTemplate;
    }

    /**
     * Retry listener for logging retry attempts and failures.
     */
    public static class DomainEventsRetryListener implements RetryListener {
        
        private static final Logger log = LoggerFactory.getLogger(DomainEventsRetryListener.class);

        @Override
        public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
            String operationName = getOperationName(context);
            log.debug("Starting retry operation: {}", operationName);
            return true;
        }

        @Override
        public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            String operationName = getOperationName(context);
            int retryCount = context.getRetryCount();
            
            log.warn("Retry attempt {} failed for operation {}: {} - {}", 
                    retryCount, operationName, throwable.getClass().getSimpleName(), throwable.getMessage());
            
            // Log full stack trace on final failure
            if (retryCount >= 3) {
                log.error("Final retry attempt failed for operation: " + operationName, throwable);
            }
        }

        @Override
        public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            String operationName = getOperationName(context);
            int retryCount = context.getRetryCount();
            
            if (throwable == null) {
                if (retryCount > 0) {
                    log.info("Retry operation succeeded after {} attempts: {}", retryCount, operationName);
                } else {
                    log.debug("Operation succeeded on first attempt: {}", operationName);
                }
            } else {
                log.error("Retry operation failed permanently after {} attempts: {}", retryCount, operationName);
            }
        }

        private String getOperationName(RetryContext context) {
            Object operationName = context.getAttribute("operationName");
            return operationName != null ? operationName.toString() : "unknown";
        }
    }
}