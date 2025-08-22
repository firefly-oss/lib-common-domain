package com.catalis.common.domain.stepevents;

import com.catalis.transactionalengine.events.StepEventEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class NoopStepEventPublisherTest {

    @Mock
    private StepEventEnvelope envelope;

    private NoopStepEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new NoopStepEventPublisher();
    }

    @Test
    void shouldReturnEmptyMonoWhenPublishing() {
        // When
        Mono<Void> result = publisher.publish(envelope);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyMonoWithNullEnvelope() {
        // When
        Mono<Void> result = publisher.publish(null);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyMonoMultipleTimes() {
        // When & Then - calling multiple times should always return empty
        StepVerifier.create(publisher.publish(envelope))
                .verifyComplete();
                
        StepVerifier.create(publisher.publish(envelope))
                .verifyComplete();
                
        StepVerifier.create(publisher.publish(envelope))
                .verifyComplete();
    }
}