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

package com.firefly.common.domain.events.outbound;

import com.firefly.common.domain.events.DomainEventEnvelope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Aspect
public class EventPublisherAspect {

    private static final Logger log = LoggerFactory.getLogger(EventPublisherAspect.class);
    private final DomainEventPublisher publisher;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();
    
    // Expression cache for performance optimization
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();
    
    // Security patterns to prevent dangerous SpEL expressions
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
        "(?i).*(Runtime|Process|Class|Thread|System|exec|getClass|getDeclaredMethod|getDeclaredField|newInstance|forName).*"
    );
    
    // Maximum expression length to prevent DoS
    private static final int MAX_EXPRESSION_LENGTH = 500;

    public EventPublisherAspect(DomainEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Around("@annotation(com.firefly.common.domain.events.outbound.EventPublisher)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        EventPublisher ann = method.getAnnotation(EventPublisher.class);
        Object result = pjp.proceed();

        if (result instanceof Mono<?> mono) {
            return mono.flatMap(res -> publishFrom(method, pjp.getArgs(), ann, res).thenReturn(res));
        } else {
            publishFrom(method, pjp.getArgs(), ann, result).subscribe();
            return result;
        }
    }

    private Mono<Void> publishFrom(Method method, Object[] args, EventPublisher ann, Object result) {
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

    /**
     * Validates that the SpEL expression is safe to execute
     */
    private void validateExpression(String expr) {
        if (expr == null || expr.isEmpty()) {
            return;
        }
        
        if (expr.length() > MAX_EXPRESSION_LENGTH) {
            throw new IllegalArgumentException("SpEL expression too long (max " + MAX_EXPRESSION_LENGTH + " characters): " + expr.substring(0, 50) + "...");
        }
        
        if (DANGEROUS_PATTERN.matcher(expr).matches()) {
            throw new SecurityException("Potentially dangerous SpEL expression detected: " + expr);
        }
    }
    
    /**
     * Gets or creates a cached expression with security validation
     */
    private Expression getExpression(String expr) {
        if (expr == null || expr.isEmpty()) {
            return null;
        }
        
        return expressionCache.computeIfAbsent(expr, key -> {
            validateExpression(key);
            try {
                Expression expression = parser.parseExpression(key);
                log.debug("Parsed and cached SpEL expression: {}", key);
                return expression;
            } catch (ParseException ex) {
                log.error("Failed to parse SpEL expression '{}': {}", key, ex.getMessage());
                throw new IllegalArgumentException("Invalid SpEL expression: " + key, ex);
            }
        });
    }

    private String evalString(MethodBasedEvaluationContext ctx, String expr) {
        Expression e = getExpression(expr);
        if (e == null) return null;
        
        try {
            Object v = e.getValue(ctx);
            return v == null ? null : String.valueOf(v);
        } catch (Exception ex) {
            log.warn("Failed to evaluate SpEL expression '{}': {}", expr, ex.getMessage());
            throw new RuntimeException("SpEL expression evaluation failed: " + expr, ex);
        }
    }

    private String safeEvalString(MethodBasedEvaluationContext ctx, String expr) {
        if (expr == null || expr.isEmpty()) return null;
        return evalString(ctx, expr);
    }

    private Object evalPayload(MethodBasedEvaluationContext ctx, String expr, Object defaultVal) {
        if (expr == null || expr.isEmpty() || "#result".equals(expr)) return defaultVal;
        
        Expression e = getExpression(expr);
        if (e == null) return defaultVal;
        
        try {
            return e.getValue(ctx);
        } catch (Exception ex) {
            log.warn("Failed to evaluate payload SpEL expression '{}': {}", expr, ex.getMessage());
            throw new RuntimeException("SpEL expression evaluation failed for payload: " + expr, ex);
        }
    }
}