package com.catalis.common.domain.stepevents;

import com.catalis.common.domain.config.StepEventsProperties;
import com.catalis.transactionalengine.events.StepEventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SqsAsyncClientStepEventPublisherTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private SqsAsyncClient sqsAsyncClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SendMessageResponse sendMessageResponse;

    @Mock
    private GetQueueUrlResponse getQueueUrlResponse;

    @Mock
    private StepEventEnvelope envelope;

    private StepEventsProperties properties;

    @BeforeEach
    void setUp() {
        properties = new StepEventsProperties();
        properties.getSqs().setClientBeanName("sqsAsyncClient");
        
        // Setup mock envelope fields - since they're accessed directly as fields,
        // we need to set them via reflection in a simpler way
        setEnvelopeFields(envelope, "test-topic", "test-payload");
    }

    private void setEnvelopeFields(StepEventEnvelope envelope, String topic, Object payload) {
        try {
            Field topicField = StepEventEnvelope.class.getDeclaredField("topic");
            topicField.setAccessible(true);
            topicField.set(envelope, topic);
            
            Field payloadField = StepEventEnvelope.class.getDeclaredField("payload");
            payloadField.setAccessible(true);
            payloadField.set(envelope, payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set envelope fields", e);
        }
    }

    @Test
    void shouldThrowExceptionWhenSqsClientNotFound() {
        // Given
        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> new SqsAsyncClientStepEventPublisher(applicationContext, properties))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SQS adapter selected but SqsAsyncClient bean was not found");
        }
    }

    @Test
    void shouldPublishMessageWithDirectQueueUrl() {
        // Given
        properties.getSqs().setQueueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/test-queue");
        
        CompletableFuture<SendMessageResponse> future = CompletableFuture.completedFuture(sendMessageResponse);
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class))).thenReturn(future);

        SqsAsyncClientStepEventPublisher publisher = createPublisher();

        // When & Then
        StepVerifier.create(publisher.publish(envelope))
                .verifyComplete();

        verify(sqsAsyncClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldPublishMessageWithQueueNameResolution() {
        // Given
        properties.getSqs().setQueueName("test-queue");
        
        when(getQueueUrlResponse.queueUrl()).thenReturn("https://sqs.us-east-1.amazonaws.com/123456789012/test-queue");
        CompletableFuture<GetQueueUrlResponse> queueUrlFuture = CompletableFuture.completedFuture(getQueueUrlResponse);
        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(queueUrlFuture);
        
        CompletableFuture<SendMessageResponse> sendFuture = CompletableFuture.completedFuture(sendMessageResponse);
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class))).thenReturn(sendFuture);

        SqsAsyncClientStepEventPublisher publisher = createPublisher();

        // When & Then
        StepVerifier.create(publisher.publish(envelope))
                .verifyComplete();

        verify(sqsAsyncClient).getQueueUrl(any(GetQueueUrlRequest.class));
        verify(sqsAsyncClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldUseTopicAsQueueNameWhenNotConfigured() {
        // Given
        when(getQueueUrlResponse.queueUrl()).thenReturn("https://sqs.us-east-1.amazonaws.com/123456789012/test-topic");
        CompletableFuture<GetQueueUrlResponse> queueUrlFuture = CompletableFuture.completedFuture(getQueueUrlResponse);
        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(queueUrlFuture);
        
        CompletableFuture<SendMessageResponse> sendFuture = CompletableFuture.completedFuture(sendMessageResponse);
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class))).thenReturn(sendFuture);

        SqsAsyncClientStepEventPublisher publisher = createPublisher();

        // When & Then
        StepVerifier.create(publisher.publish(envelope))
                .verifyComplete();
    }

    @Test
    void shouldSerializePayloadWithObjectMapper() throws Exception {
        // Given
        properties.getSqs().setQueueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/test-queue");
        
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"data\":\"test-payload\"}");
        CompletableFuture<SendMessageResponse> future = CompletableFuture.completedFuture(sendMessageResponse);
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class))).thenReturn(future);

        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(sqsAsyncClient);
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), isNull(), anyString()))
                    .thenReturn(objectMapper);

            SqsAsyncClientStepEventPublisher publisher = new SqsAsyncClientStepEventPublisher(applicationContext, properties);

            // When & Then
            StepVerifier.create(publisher.publish(envelope))
                    .verifyComplete();

            verify(objectMapper).writeValueAsString(envelope.payload);
        }
    }

    @Test
    void shouldFallbackToStringWhenObjectMapperNotAvailable() {
        // Given
        properties.getSqs().setQueueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/test-queue");
        
        CompletableFuture<SendMessageResponse> future = CompletableFuture.completedFuture(sendMessageResponse);
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class))).thenReturn(future);

        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(sqsAsyncClient);
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), isNull(), anyString()))
                    .thenReturn(null);

            SqsAsyncClientStepEventPublisher publisher = new SqsAsyncClientStepEventPublisher(applicationContext, properties);

            // When & Then
            StepVerifier.create(publisher.publish(envelope))
                    .verifyComplete();
        }
    }

    @Test
    void shouldHandlePublishException() {
        // Given
        properties.getSqs().setQueueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/test-queue");
        
        CompletableFuture<SendMessageResponse> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Send failed"));
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class))).thenReturn(future);

        SqsAsyncClientStepEventPublisher publisher = createPublisher();

        // When & Then
        StepVerifier.create(publisher.publish(envelope))
                .expectErrorMessage("Send failed")
                .verify();
    }

    @Test
    void shouldHandleQueueUrlResolutionException() {
        // Given
        properties.getSqs().setQueueName("test-queue");
        
        CompletableFuture<GetQueueUrlResponse> queueUrlFuture = new CompletableFuture<>();
        queueUrlFuture.completeExceptionally(new RuntimeException("Queue not found"));
        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(queueUrlFuture);

        SqsAsyncClientStepEventPublisher publisher = createPublisher();

        // When & Then
        StepVerifier.create(publisher.publish(envelope))
                .expectErrorMessage("java.lang.RuntimeException: Queue not found")
                .verify();
    }

    @Test
    void shouldHandleSerializationException() throws Exception {
        // Given
        properties.getSqs().setQueueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/test-queue");
        
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization failed"));

        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(sqsAsyncClient);
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), isNull(), anyString()))
                    .thenReturn(objectMapper);

            SqsAsyncClientStepEventPublisher publisher = new SqsAsyncClientStepEventPublisher(applicationContext, properties);

            // When & Then - Should fallback to String.valueOf() instead of failing
            CompletableFuture<SendMessageResponse> future = CompletableFuture.completedFuture(sendMessageResponse);
            when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class))).thenReturn(future);
            
            StepVerifier.create(publisher.publish(envelope))
                    .verifyComplete();
        }
    }

    private SqsAsyncClientStepEventPublisher createPublisher() {
        try (MockedStatic<StepEventAdapterUtils> mockedStatic = mockStatic(StepEventAdapterUtils.class)) {
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), anyString(), anyString()))
                    .thenReturn(sqsAsyncClient);
            mockedStatic.when(() -> StepEventAdapterUtils.resolveBean(any(), isNull(), anyString()))
                    .thenReturn(null);
            return new SqsAsyncClientStepEventPublisher(applicationContext, properties);
        }
    }
}