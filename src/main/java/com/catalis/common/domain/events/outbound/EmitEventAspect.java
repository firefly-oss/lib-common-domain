package com.catalis.common.domain.events.outbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Aspect
public class EmitEventAspect {

    private final DomainEventPublisher publisher;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    public EmitEventAspect(DomainEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Around("@annotation(com.catalis.common.domain.events.outbound.EmitEvent)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        EmitEvent ann = method.getAnnotation(EmitEvent.class);
        Object result = pjp.proceed();

        if (result instanceof Mono<?> mono) {
            return mono.flatMap(res -> publishFrom(method, pjp.getArgs(), ann, res).thenReturn(res));
        } else {
            publishFrom(method, pjp.getArgs(), ann, result).subscribe();
            return result;
        }
    }

    private Mono<Void> publishFrom(Method method, Object[] args, EmitEvent ann, Object result) {
        MethodBasedEvaluationContext ctx = new MethodBasedEvaluationContext(null, method, args, nameDiscoverer);
        ctx.setVariable("result", result);

        String topic = evalString(ctx, ann.topic());
        String type = safeEvalString(ctx, ann.type());
        String key = safeEvalString(ctx, ann.key());
        Object payload = evalPayload(ctx, ann.payload(), result);
        Map<String, Object> headers = new HashMap<>();

        DomainEventEnvelope env = DomainEventEnvelope.builder()
                .topic(topic).type(type).key(key).payload(payload).headers(headers)
                .build();
        return publisher.publish(env);
    }

    private String evalString(MethodBasedEvaluationContext ctx, String expr) {
        Expression e = parser.parseExpression(expr);
        Object v = e.getValue(ctx);
        return v == null ? null : String.valueOf(v);
    }

    private String safeEvalString(MethodBasedEvaluationContext ctx, String expr) {
        if (expr == null || expr.isEmpty()) return null;
        return evalString(ctx, expr);
    }

    private Object evalPayload(MethodBasedEvaluationContext ctx, String expr, Object defaultVal) {
        if (expr == null || expr.isEmpty() || "#result".equals(expr)) return defaultVal;
        Expression e = parser.parseExpression(expr);
        return e.getValue(ctx);
    }
}
