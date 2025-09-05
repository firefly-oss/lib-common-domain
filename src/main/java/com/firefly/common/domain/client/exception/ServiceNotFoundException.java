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
 * Exception thrown when a service endpoint or resource is not found.
 * This typically corresponds to HTTP 404 responses or gRPC NOT_FOUND status.
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public class ServiceNotFoundException extends ServiceClientException {

    /**
     * Constructs a new ServiceNotFoundException with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public ServiceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new ServiceNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause the underlying cause of this exception
     */
    public ServiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new ServiceNotFoundException with the specified cause.
     *
     * @param cause the underlying cause of this exception
     */
    public ServiceNotFoundException(Throwable cause) {
        super(cause);
    }
}
