package com.catalis.common.domain.actuator.health;

import com.catalis.common.domain.events.properties.DomainEventsProperties;
import com.catalis.common.domain.stepevents.StepEventAdapterUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ApplicationContext;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class SqsDomainEventsHealthIndicatorTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private SqsAsyncClient sqsAsyncClient;

    @Mock
    private GetQueueAttributesResponse queueAttributesResponse;

    private DomainEventsProperties properties;
    private SqsDomainEventsHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        properties = new DomainEventsProperties();
        properties.setEnabled(true);
        properties.setAdapter(DomainEventsProperties.Adapter.SQS);
        properties.getSqs().setClientBeanName("sqsAsyncClient");
        healthIndicator = new SqsDomainEventsHealthIndicator(properties, applicationContext);
    }

    @Test
    void shouldReturnUpWhenDomainEventsDisabled() {
        // Given
        properties.setEnabled(false);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("status", "Domain events are disabled");
        assertThat(health.getDetails()).containsEntry("adapter", "SQS");
    }

    @Test
    void shouldReturnDownWhenSqsAsyncClientNotAvailable() {
        // Given
        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(null);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("status", "SqsAsyncClient not available");
            assertThat(health.getDetails()).containsEntry("adapter", "sqs");
            assertThat(health.getDetails()).containsEntry("clientBeanName", "sqsAsyncClient");
        }
    }

    @Test
    void shouldReturnUpWhenClientAvailableButNoQueueConfigured() {
        // Given
        properties.getSqs().setQueueUrl("");
        properties.getSqs().setQueueName("");

        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(sqsAsyncClient);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsEntry("status", "SQS client available but no queue configured");
            assertThat(health.getDetails()).containsEntry("adapter", "sqs");
            assertThat(health.getDetails()).containsEntry("clientBeanName", "sqsAsyncClient");
        }
    }

    @Test
    void shouldReturnUpWhenClientAvailableWithQueueNameOnly() {
        // Given
        properties.getSqs().setQueueUrl("");
        properties.getSqs().setQueueName("test-queue");

        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(sqsAsyncClient);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsEntry("status", "SQS client available, queue name configured");
            assertThat(health.getDetails()).containsEntry("adapter", "sqs");
            assertThat(health.getDetails()).containsEntry("queueName", "test-queue");
            assertThat(health.getDetails()).containsEntry("clientBeanName", "sqsAsyncClient");
        }
    }

    @Test
    void shouldReturnUpWhenQueueAccessible() {
        // Given
        properties.getSqs().setQueueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/test-queue");
        properties.getSqs().setQueueName("test-queue");

        CompletableFuture<GetQueueAttributesResponse> future = CompletableFuture.completedFuture(queueAttributesResponse);
        when(sqsAsyncClient.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenReturn(future);

        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(sqsAsyncClient);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsEntry("status", "SQS queue accessible");
            assertThat(health.getDetails()).containsEntry("adapter", "sqs");
            assertThat(health.getDetails()).containsEntry("queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789012/test-queue");
            assertThat(health.getDetails()).containsEntry("queueName", "test-queue");
            assertThat(health.getDetails()).containsEntry("clientBeanName", "sqsAsyncClient");
        }
    }

    @Test
    void shouldReturnDownWhenQueueAccessFails() {
        // Given
        properties.getSqs().setQueueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/test-queue");
        properties.getSqs().setQueueName("test-queue");

        CompletableFuture<GetQueueAttributesResponse> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Queue access failed"));
        when(sqsAsyncClient.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenReturn(future);

        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(sqsAsyncClient);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("status", "Failed to access SQS queue");
            assertThat(health.getDetails()).containsEntry("adapter", "sqs");
            assertThat(health.getDetails()).containsEntry("queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789012/test-queue");
            assertThat(health.getDetails()).containsEntry("queueName", "test-queue");
            assertThat(health.getDetails()).containsKey("error");
        }
    }

    @Test
    void shouldReturnDownWhenExceptionThrown() {
        // Given
        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Connection failed"));

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("status", "Error checking SQS health");
            assertThat(health.getDetails()).containsEntry("adapter", "sqs");
            assertThat(health.getDetails()).containsEntry("error", "Connection failed");
        }
    }

    @Test
    void shouldUseCustomClientBeanName() {
        // Given
        properties.getSqs().setClientBeanName("customSqsClient");

        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(null);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("clientBeanName", "customSqsClient");
        }
    }
}