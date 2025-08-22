package com.catalis.common.domain.events.outbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmitEventAspectTest {

    static class Sample {
        @EmitEvent(topic = "'orders'", type = "'created'", key = "'k'", payload = "#result")
        public String doWork(String input) {
            return input.toUpperCase();
        }
        @EmitEvent(topic = "'orders'", type = "'async'", key = "'k'")
        public reactor.core.publisher.Mono<String> doMono(String input) {
            return reactor.core.publisher.Mono.just(input + "!");
        }
    }

    @Test
    void publishesAfterNonMonoMethod() throws Throwable {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        when(publisher.publish(any())).thenReturn(reactor.core.publisher.Mono.empty());
        EmitEventAspect aspect = new EmitEventAspect(publisher);

        Sample target = new Sample();
        Method m = Sample.class.getMethod("doWork", String.class);

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature sig = mock(MethodSignature.class);
        when(sig.getMethod()).thenReturn(m);
        when(pjp.getSignature()).thenReturn(sig);
        when(pjp.getArgs()).thenReturn(new Object[]{"abc"});
        when(publisher.publish(any())).thenReturn(reactor.core.publisher.Mono.empty());
        when(pjp.proceed()).then(inv -> m.invoke(target, "abc"));

        Object out = aspect.around(pjp);
        assertEquals("ABC", out);

        ArgumentCaptor<DomainEventEnvelope> captor = ArgumentCaptor.forClass(DomainEventEnvelope.class);
        verify(publisher, times(1)).publish(captor.capture());
        DomainEventEnvelope env = captor.getValue();
        assertEquals("orders", env.topic);
        assertEquals("created", env.type);
        assertEquals("k", env.key);
        assertEquals("ABC", env.payload);
    }

    @Test
    void publishesAfterMonoEmits() throws Throwable {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        when(publisher.publish(any())).thenReturn(reactor.core.publisher.Mono.empty());
        EmitEventAspect aspect = new EmitEventAspect(publisher);

        Sample target = new Sample();
        Method m = Sample.class.getMethod("doMono", String.class);

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature sig = mock(MethodSignature.class);
        when(sig.getMethod()).thenReturn(m);
        when(pjp.getSignature()).thenReturn(sig);
        when(pjp.getArgs()).thenReturn(new Object[]{"x"});
        when(pjp.proceed()).then(inv -> m.invoke(target, "x"));

        Object out = aspect.around(pjp);
        assertTrue(out instanceof reactor.core.publisher.Mono);
        String v = ((reactor.core.publisher.Mono<String>) out).block();
        assertEquals("x!", v);

        ArgumentCaptor<DomainEventEnvelope> captor = ArgumentCaptor.forClass(DomainEventEnvelope.class);
        verify(publisher, times(1)).publish(captor.capture());
        DomainEventEnvelope env = captor.getValue();
        assertEquals("orders", env.topic);
        assertEquals("async", env.type);
        assertEquals("k", env.key);
        assertEquals("x!", env.payload);
    }
}
