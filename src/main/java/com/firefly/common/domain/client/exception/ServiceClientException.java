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
package com.firefly.common.domain.client.exception;

/**
 * Base exception for all ServiceClient-related errors.
 * This exception serves as the parent class for all service communication failures.
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public class ServiceClientException extends RuntimeException {

    /**
     * Constructs a new ServiceClientException with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public ServiceClientException(String message) {
        super(message);
    }

    /**
     * Constructs a new ServiceClientException with the specified detail message and cause.
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause the underlying cause of this exception
     */
    public ServiceClientException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new ServiceClientException with the specified cause.
     *
     * @param cause the underlying cause of this exception
     */
    public ServiceClientException(Throwable cause) {
        super(cause);
    }
}
