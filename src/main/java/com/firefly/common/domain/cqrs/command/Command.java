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

package com.firefly.common.domain.cqrs.command;

import com.firefly.common.domain.validation.ValidationResult;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base interface for all commands in the CQRS (Command Query Responsibility Segregation) architecture.
 *
 * <p>Commands represent intentions to change state and are processed asynchronously through the
 * {@link CommandBus}. Each command encapsulates all the data needed to perform a specific business
 * operation and includes built-in validation capabilities.
 *
 * <p>Key characteristics of commands:
 * <ul>
 *   <li>Immutable data structures representing user intentions</li>
 *   <li>Include validation logic for business rules</li>
 *   <li>Processed asynchronously with reactive return types</li>
 *   <li>Support correlation context for distributed tracing</li>
 *   <li>Can include metadata for auditing and monitoring</li>
 * </ul>
 *
 * <p>Example implementation:
 * <pre>{@code
 * @Data
 * public class CreateOrderCommand implements Command<OrderCreatedResult> {
 *     private final String customerId;
 *     private final List<OrderItem> items;
 *     private final String correlationId;
 *
 *     @Override
 *     public Mono<ValidationResult> validate() {
 *         ValidationResult.Builder builder = ValidationResult.builder();
 *
 *         if (customerId == null || customerId.trim().isEmpty()) {
 *             builder.addError("customerId", "Customer ID is required");
 *         }
 *
 *         if (items == null || items.isEmpty()) {
 *             builder.addError("items", "Order must contain at least one item");
 *         }
 *
 *         return Mono.just(builder.build());
 *     }
 * }
 * }</pre>
 *
 * @param <R> The type of result returned by this command when processed
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 * @see CommandBus
 * @see CommandHandler
 * @see ValidationResult
 */
public interface Command<R> {

    /**
     * Returns a unique identifier for this command instance.
     *
     * <p>This identifier is used for tracking, logging, and correlation purposes.
     * The default implementation generates a random UUID, but implementations
     * can override this to provide custom ID generation strategies.
     *
     * @return the unique command identifier, never null
     * @since 1.0.0
     */
    default String getCommandId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the timestamp when this command was created.
     *
     * <p>This timestamp is used for auditing, ordering, and timeout calculations.
     * The default implementation returns the current system time, but implementations
     * can override this to provide custom timestamp strategies.
     *
     * @return the command creation timestamp, never null
     * @since 1.0.0
     */
    default Instant getTimestamp() {
        return Instant.now();
    }

    /**
     * Returns the correlation ID for distributed tracing across system boundaries.
     *
     * <p>The correlation ID is used to trace requests across multiple services and
     * components in a distributed system. It should be propagated through all
     * related operations and logged for debugging purposes.
     *
     * <p>If not provided, the {@link com.firefly.common.domain.tracing.CorrelationContext}
     * may generate one automatically during command processing.
     *
     * @return the correlation ID for tracing, or null if not set
     * @since 1.0.0
     * @see com.firefly.common.domain.tracing.CorrelationContext
     */
    default String getCorrelationId() {
        return null;
    }

    /**
     * Returns the identifier of the user or system that initiated this command.
     *
     * <p>This information is used for auditing, authorization, and security purposes.
     * It can represent a user ID, service name, or any other identifier that
     * indicates the source of the command.
     *
     * @return the initiator identifier, or null if not set
     * @since 1.0.0
     */
    default String getInitiatedBy() {
        return null;
    }

    /**
     * Returns additional metadata associated with this command.
     *
     * <p>Metadata can include any additional information that doesn't fit into
     * the standard command fields, such as:
     * <ul>
     *   <li>Request source information (IP address, user agent)</li>
     *   <li>Business context (tenant ID, organization ID)</li>
     *   <li>Processing hints (priority, routing information)</li>
     *   <li>Custom application-specific data</li>
     * </ul>
     *
     * @return a map of metadata key-value pairs, or null if no metadata
     * @since 1.0.0
     */
    default Map<String, Object> getMetadata() {
        return null;
    }

    /**
     * Returns the expected result type for this command.
     *
     * <p>This method is used by the command processing infrastructure to determine
     * the type of result that will be returned when this command is processed.
     * The default implementation returns {@code Object.class}, but implementations
     * should override this to return the specific result type.
     *
     * <p>This information is used for:
     * <ul>
     *   <li>Type safety in command handler registration</li>
     *   <li>Serialization/deserialization of results</li>
     *   <li>Metrics and monitoring categorization</li>
     * </ul>
     *
     * @return the Class representing the expected result type
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    default Class<R> getResultType() {
        return (Class<R>) Object.class;
    }

    /**
     * Validates this command and returns a validation result.
     *
     * <p>This method performs both field-level and business rule validation and is
     * called automatically by the {@link CommandBus} before command handlers execute.
     * If validation fails, the command will not be processed and a
     * {@link com.firefly.common.domain.validation.ValidationException} will be thrown.
     *
     * <p>The validation process supports both synchronous and asynchronous validation:
     * <ul>
     *   <li>Synchronous validation for simple field checks</li>
     *   <li>Asynchronous validation for external service calls</li>
     *   <li>Complex business rule validation</li>
     *   <li>Cross-field validation</li>
     * </ul>
     *
     * <p>The default implementation returns a successful validation result.
     * Override this method to implement custom validation logic specific to your command.
     *
     * <p>Examples:
     * <pre>{@code
     * // Simple field validation
     * @Override
     * public Mono<ValidationResult> validate() {
     *     ValidationResult.Builder builder = ValidationResult.builder();
     *
     *     if (customerId == null || customerId.trim().isEmpty()) {
     *         builder.addError("customerId", "Customer ID is required");
     *     }
     *
     *     if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
     *         builder.addError("amount", "Amount must be positive");
     *     }
     *
     *     return Mono.just(builder.build());
     * }
     *
     * // Async validation with external service
     * @Override
     * public Mono<ValidationResult> validate() {
     *     return validateCustomerExists(customerId)
     *         .flatMap(customerValid -> {
     *             if (!customerValid) {
     *                 return Mono.just(ValidationResult.failure("customerId", "Customer not found"));
     *             }
     *             return validateBusinessRules();
     *         });
     * }
     * }</pre>
     *
     * @return a Mono containing the validation result, never null
     * @since 1.0.0
     * @see ValidationResult
     * @see CommandBus#send(Command)
     * @see com.firefly.common.domain.validation.ValidationException
     */
    default Mono<ValidationResult> validate() {
        return Mono.just(ValidationResult.success());
    }
}