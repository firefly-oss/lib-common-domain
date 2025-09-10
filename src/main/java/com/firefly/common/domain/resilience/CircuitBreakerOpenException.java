package com.firefly.common.domain.resilience;

/**
 * Exception thrown when a circuit breaker is in OPEN state and rejects calls.
 * 
 * <p>This exception indicates that the circuit breaker has detected too many
 * failures and is currently rejecting all calls to protect the downstream service
 * and prevent cascading failures.
 *
 * @author Firefly Software Solutions Inc
 * @since 2.0.0
 */
public class CircuitBreakerOpenException extends RuntimeException {
    
    /**
     * Constructs a new CircuitBreakerOpenException with the specified detail message.
     *
     * @param message the detail message
     */
    public CircuitBreakerOpenException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new CircuitBreakerOpenException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public CircuitBreakerOpenException(String message, Throwable cause) {
        super(message, cause);
    }
}
