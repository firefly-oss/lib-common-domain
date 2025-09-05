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

import reactor.core.publisher.Mono;

/**
 * Interface for handling commands in the CQRS architecture.
 *
 * <p>Command handlers are responsible for processing commands and producing results.
 * They contain the business logic for command execution and should be stateless
 * to ensure thread safety and scalability.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Execute business logic for a specific command type</li>
 *   <li>Validate command data and business rules</li>
 *   <li>Coordinate with domain services and repositories</li>
 *   <li>Publish domain events when appropriate</li>
 *   <li>Return meaningful results or errors</li>
 * </ul>
 *
 * <p>Example implementation:
 * <pre>{@code
 * @Component
 * public class CreateOrderHandler implements CommandHandler<CreateOrderCommand, OrderResult> {
 *
 *     private final OrderRepository orderRepository;
 *     private final DomainEventPublisher eventPublisher;
 *
 *     @Override
 *     public Mono<OrderResult> handle(CreateOrderCommand command) {
 *         return validateCommand(command)
 *             .flatMap(this::createOrder)
 *             .flatMap(this::publishOrderCreatedEvent)
 *             .map(order -> new OrderResult(order.getId(), "CREATED"));
 *     }
 *
 *     @Override
 *     public Class<CreateOrderCommand> getCommandType() {
 *         return CreateOrderCommand.class;
 *     }
 * }
 * }</pre>
 *
 * @param <C> The command type this handler processes
 * @param <R> The result type returned by this handler
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 * @see Command
 * @see CommandBus
 */
public interface CommandHandler<C extends Command<R>, R> {

    /**
     * Handles the given command asynchronously.
     *
     * <p>This method contains the core business logic for processing the command.
     * It should be idempotent when possible and handle errors gracefully.
     *
     * @param command the command to handle, guaranteed to be non-null
     * @return a Mono containing the result of command processing
     * @throws IllegalArgumentException if the command is invalid
     * @throws RuntimeException for business logic errors
     */
    Mono<R> handle(C command);

    /**
     * Returns the command type this handler can process.
     *
     * <p>Used for automatic handler registration and routing by the CommandBus.
     * This method is called during application startup to register the handler.
     *
     * @return the command class, must not be null
     */
    Class<C> getCommandType();

    /**
     * Returns the result type this handler produces.
     *
     * <p>Used for type safety and result processing. The default implementation
     * returns null, which is sufficient for most use cases. Override this method
     * if you need explicit type introspection.
     *
     * @return the result class, may be null
     */
    default Class<R> getResultType() {
        return null; // Can be overridden for explicit type information
    }

    /**
     * Validates whether this handler can process the given command.
     *
     * <p>Default implementation checks command type compatibility using instanceof.
     * Override this method if you need custom validation logic.
     *
     * @param command the command to validate, may be null
     * @return true if this handler can process the command, false otherwise
     */
    default boolean canHandle(Command<?> command) {
        return getCommandType().isInstance(command);
    }
}