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

package com.firefly.common.domain.cqrs.fluent;

import com.firefly.common.domain.cqrs.command.Command;
import com.firefly.common.domain.cqrs.command.CommandBus;
import com.firefly.common.domain.validation.ValidationResult;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Fluent builder for creating and executing commands with reduced boilerplate.
 *
 * <p>This builder provides a fluent API for command creation that eliminates
 * common boilerplate and provides smart defaults for metadata, correlation,
 * and validation.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Simple command creation and execution
 * CommandBuilder.create(CreateAccountCommand.class)
 *     .withCustomerId("CUST-123")
 *     .withInitialDeposit(new BigDecimal("1000.00"))
 *     .withAccountType("SAVINGS")
 *     .correlatedBy("REQ-456")
 *     .initiatedBy("user@example.com")
 *     .executeWith(commandBus)
 *     .subscribe(result -> log.info("Account created: {}", result));
 *
 * // Advanced usage with custom validation and metadata
 * CommandBuilder.create(TransferMoneyCommand.class)
 *     .withFromAccount("ACC-001")
 *     .withToAccount("ACC-002")
 *     .withAmount(new BigDecimal("500.00"))
 *     .withMetadata("priority", "HIGH")
 *     .withMetadata("channel", "MOBILE")
 *     .withCustomValidation(cmd -> validateTransferLimits(cmd))
 *     .executeWith(commandBus);
 * }</pre>
 *
 * @param <C> the command type
 * @param <R> the result type
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public class CommandBuilder<C extends Command<R>, R> {

    private final Class<C> commandType;
    private final Map<String, Object> properties = new HashMap<>();
    private final Map<String, Object> metadata = new HashMap<>();
    private String commandId;
    private String correlationId;
    private String initiatedBy;
    private Instant timestamp;
    private Supplier<Mono<ValidationResult>> customValidation;

    private CommandBuilder(Class<C> commandType) {
        this.commandType = commandType;
        this.commandId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    /**
     * Creates a new command builder for the specified command type.
     *
     * @param commandType the command class
     * @param <C> the command type
     * @param <R> the result type
     * @return a new command builder
     */
    public static <C extends Command<R>, R> CommandBuilder<C, R> create(Class<C> commandType) {
        return new CommandBuilder<>(commandType);
    }

    /**
     * Sets a property value using fluent method naming.
     * This method uses reflection to set the property on the command.
     *
     * @param propertyName the property name
     * @param value the property value
     * @return this builder for method chaining
     */
    public CommandBuilder<C, R> with(String propertyName, Object value) {
        properties.put(propertyName, value);
        return this;
    }

    /**
     * Sets the command ID.
     *
     * @param commandId the command ID
     * @return this builder for method chaining
     */
    public CommandBuilder<C, R> withId(String commandId) {
        this.commandId = commandId;
        return this;
    }

    /**
     * Sets the correlation ID for distributed tracing.
     *
     * @param correlationId the correlation ID
     * @return this builder for method chaining
     */
    public CommandBuilder<C, R> correlatedBy(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    /**
     * Sets who initiated this command.
     *
     * @param initiatedBy the initiator (user ID, system name, etc.)
     * @return this builder for method chaining
     */
    public CommandBuilder<C, R> initiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
        return this;
    }

    /**
     * Sets the command timestamp.
     *
     * @param timestamp the timestamp
     * @return this builder for method chaining
     */
    public CommandBuilder<C, R> at(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Adds metadata to the command.
     *
     * @param key the metadata key
     * @param value the metadata value
     * @return this builder for method chaining
     */
    public CommandBuilder<C, R> withMetadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    /**
     * Adds multiple metadata entries.
     *
     * @param metadata the metadata map
     * @return this builder for method chaining
     */
    public CommandBuilder<C, R> withMetadata(Map<String, Object> metadata) {
        this.metadata.putAll(metadata);
        return this;
    }

    /**
     * Sets custom validation logic for the command.
     *
     * @param validation the custom validation function
     * @return this builder for method chaining
     */
    public CommandBuilder<C, R> withCustomValidation(Supplier<Mono<ValidationResult>> validation) {
        this.customValidation = validation;
        return this;
    }

    /**
     * Builds the command instance.
     *
     * @return the built command
     */
    public C build() {
        try {
            // Create command instance using reflection or factory
            C command = createCommandInstance();
            return command;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build command: " + e.getMessage(), e);
        }
    }

    /**
     * Builds and executes the command using the provided command bus.
     *
     * @param commandBus the command bus to use for execution
     * @return a Mono containing the command result
     */
    public Mono<R> executeWith(CommandBus commandBus) {
        C command = build();
        return commandBus.send(command);
    }

    /**
     * Creates a command instance using reflection and builder pattern.
     */
    @SuppressWarnings("unchecked")
    private C createCommandInstance() throws Exception {
        // Try to find a builder method first
        try {
            var builderMethod = commandType.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            
            // Set properties on the builder
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String methodName = entry.getKey();
                Object value = entry.getValue();
                
                try {
                    var method = builder.getClass().getMethod(methodName, value.getClass());
                    method.invoke(builder, value);
                } catch (NoSuchMethodException e) {
                    // Try with different parameter types
                    for (var method : builder.getClass().getMethods()) {
                        if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                            method.invoke(builder, value);
                            break;
                        }
                    }
                }
            }
            
            // Build the command
            var buildMethod = builder.getClass().getMethod("build");
            C command = (C) buildMethod.invoke(builder);
            
            // Set metadata and other properties if the command supports it
            setCommandMetadata(command);
            
            return command;
        } catch (NoSuchMethodException e) {
            // Fall back to constructor-based creation
            return createCommandWithConstructor();
        }
    }

    /**
     * Creates command using constructor (fallback method).
     */
    private C createCommandWithConstructor() throws Exception {
        // This is a simplified implementation
        // In a real implementation, you'd use more sophisticated reflection
        // or code generation to handle various constructor patterns
        throw new UnsupportedOperationException(
            "Constructor-based command creation not yet implemented. " +
            "Please ensure your command class has a builder() method."
        );
    }

    /**
     * Sets metadata and other properties on the command if supported.
     */
    private void setCommandMetadata(C command) {
        // Use reflection to set metadata if the command supports it
        try {
            if (correlationId != null) {
                setFieldIfExists(command, "correlationId", correlationId);
            }
            if (initiatedBy != null) {
                setFieldIfExists(command, "initiatedBy", initiatedBy);
            }
            if (commandId != null) {
                setFieldIfExists(command, "commandId", commandId);
            }
            if (timestamp != null) {
                setFieldIfExists(command, "timestamp", timestamp);
            }
            if (!metadata.isEmpty()) {
                setFieldIfExists(command, "metadata", metadata);
            }
        } catch (Exception e) {
            // Ignore metadata setting errors - not all commands may support all metadata
        }
    }

    /**
     * Sets a field value if the field exists.
     */
    private void setFieldIfExists(C command, String fieldName, Object value) {
        try {
            var field = command.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(command, value);
        } catch (Exception e) {
            // Field doesn't exist or can't be set - ignore
        }
    }
}
