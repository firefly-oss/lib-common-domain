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

/**
 * Exception thrown when a call through a circuit breaker times out.
 * 
 * <p>This exception indicates that a call took longer than the configured
 * timeout duration and was cancelled by the circuit breaker.
 *
 * @author Firefly Software Solutions Inc
 * @since 2.0.0
 */
public class CircuitBreakerTimeoutException extends RuntimeException {
    
    /**
     * Constructs a new CircuitBreakerTimeoutException with the specified detail message.
     *
     * @param message the detail message
     */
    public CircuitBreakerTimeoutException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new CircuitBreakerTimeoutException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public CircuitBreakerTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
