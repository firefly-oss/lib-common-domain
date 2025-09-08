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

import com.firefly.common.domain.tracing.CorrelationContext;
import com.firefly.common.domain.validation.AutoValidationProcessor;
import com.firefly.common.domain.validation.ValidationException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of CommandBus with automatic handler discovery,
 * tracing support, error handling, validation, and metrics integration.
 */
@Slf4j
@Component
public class DefaultCommandBus implements CommandBus {

    private final Map<Class<? extends Command<?>>, CommandHandler<?, ?>> handlers = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;
    private final CorrelationContext correlationContext;
    private final AutoValidationProcessor autoValidationProcessor;
    private final MeterRegistry meterRegistry;
    private final Counter commandProcessedCounter;
    private final Counter commandFailedCounter;
    private final Counter validationFailedCounter;
    private final Timer commandProcessingTimer;

    @Autowired
    public DefaultCommandBus(ApplicationContext applicationContext,
                            CorrelationContext correlationContext,
                            AutoValidationProcessor autoValidationProcessor,
                            @Autowired(required = false) MeterRegistry meterRegistry) {
        this.applicationContext = applicationContext;
        this.correlationContext = correlationContext;
        this.autoValidationProcessor = autoValidationProcessor;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics if MeterRegistry is available
        if (meterRegistry != null) {
            this.commandProcessedCounter = Counter.builder("firefly.cqrs.command.processed")
                .description("Number of commands processed successfully")
                .register(meterRegistry);
            this.commandFailedCounter = Counter.builder("firefly.cqrs.command.failed")
                .description("Number of commands that failed processing")
                .register(meterRegistry);
            this.validationFailedCounter = Counter.builder("firefly.cqrs.command.validation.failed")
                .description("Number of commands that failed validation")
                .register(meterRegistry);
            this.commandProcessingTimer = Timer.builder("firefly.cqrs.command.processing.time")
                .description("Time taken to process commands")
                .register(meterRegistry);
        } else {
            this.commandProcessedCounter = null;
            this.commandFailedCounter = null;
            this.validationFailedCounter = null;
            this.commandProcessingTimer = null;
        }
        
        discoverHandlers();
    }
    
    // Constructor for backward compatibility without metrics and validation processor
    public DefaultCommandBus(ApplicationContext applicationContext,
                            CorrelationContext correlationContext,
                            AutoValidationProcessor autoValidationProcessor) {
        this(applicationContext, correlationContext, autoValidationProcessor, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Mono<R> send(Command<R> command) {
        Instant startTime = Instant.now();
        String commandType = command.getClass().getSimpleName();
        
        return Mono.fromCallable(() -> {
                    log.info("CQRS Command Processing Started - Type: {}, ID: {}, CorrelationId: {}",
                            commandType, command.getCommandId(), command.getCorrelationId());

                    CommandHandler<Command<R>, R> handler = (CommandHandler<Command<R>, R>) handlers.get(command.getClass());
                    if (handler == null) {
                        log.error("CQRS Command Handler Not Found - Type: {}, ID: {}, Available handlers: {}",
                                commandType, command.getCommandId(), handlers.keySet().stream()
                                        .map(Class::getSimpleName).toList());
                        throw new CommandHandlerNotFoundException("No handler found for command: " + command.getClass().getName());
                    }

                    log.debug("CQRS Command Handler Found - Type: {}, Handler: {}",
                            commandType, handler.getClass().getSimpleName());
                    return handler;
                })
                .flatMap(handler -> {
                    // Set correlation context if available
                    if (command.getCorrelationId() != null) {
                        correlationContext.setCorrelationId(command.getCorrelationId());
                    }
                    
                    // Perform automatic Jakarta validation first, then custom validation
                    return autoValidationProcessor.validate(command)
                            .flatMap(autoValidationResult -> {
                                if (!autoValidationResult.isValid()) {
                                    // Record validation failure metric
                                    if (validationFailedCounter != null) {
                                        validationFailedCounter.increment();
                                    }

                                    log.warn("CQRS Command Auto-Validation Failed - Type: {}, ID: {}, Violations: {}",
                                            commandType, command.getCommandId(), autoValidationResult.getSummary());
                                    return Mono.error(new ValidationException(autoValidationResult));
                                }

                                // If automatic validation passes, perform custom validation
                                return command.validate()
                                        .flatMap(customValidationResult -> {
                                            if (!customValidationResult.isValid()) {
                                                // Record validation failure metric
                                                if (validationFailedCounter != null) {
                                                    validationFailedCounter.increment();
                                                }

                                                log.warn("CQRS Command Custom-Validation Failed - Type: {}, ID: {}, Violations: {}",
                                                        commandType, command.getCommandId(), customValidationResult.getSummary());
                                                return Mono.error(new ValidationException(customValidationResult));
                                            }

                                            // Execute the command handler
                                            return handler.handle(command);
                                        });
                            })
                            .doOnSuccess(result -> {
                                Duration processingTime = Duration.between(startTime, Instant.now());
                                log.info("CQRS Command Processing Completed - Type: {}, ID: {}, Duration: {}ms, Result: {}",
                                        commandType, command.getCommandId(), processingTime.toMillis(),
                                        result != null ? "Success" : "Null");

                                // Record success metrics
                                if (commandProcessedCounter != null) {
                                    commandProcessedCounter.increment();
                                }
                                if (commandProcessingTimer != null) {
                                    commandProcessingTimer.record(processingTime);
                                }
                            })
                            .doOnError(error -> {
                                Duration processingTime = Duration.between(startTime, Instant.now());
                                log.error("CQRS Command Processing Failed - Type: {}, ID: {}, Duration: {}ms, Error: {}, Cause: {}",
                                        commandType, command.getCommandId(), processingTime.toMillis(),
                                        error.getClass().getSimpleName(), error.getMessage(), error);

                                // Record failure metrics
                                if (commandFailedCounter != null) {
                                    commandFailedCounter.increment();
                                }
                            })
                            .doFinally(signalType -> correlationContext.clear());
                })
                .onErrorMap(throwable -> {
                    if (throwable instanceof CommandHandlerNotFoundException) {
                        return throwable;
                    }
                    if (throwable instanceof ValidationException) {
                        return throwable;
                    }
                    return new CommandProcessingException("Failed to process command: " + command.getCommandId(), throwable);
                });
    }

    @Override
    public <C extends Command<R>, R> void registerHandler(CommandHandler<C, R> handler) {
        Class<C> commandType = handler.getCommandType();
        
        if (handlers.containsKey(commandType)) {
            log.warn("Replacing existing handler for command: {}", commandType.getName());
        }
        
        handlers.put(commandType, handler);
        log.info("Registered command handler for: {}", commandType.getName());
    }

    @Override
    public <C extends Command<?>> void unregisterHandler(Class<C> commandType) {
        CommandHandler<?, ?> removed = handlers.remove(commandType);
        if (removed != null) {
            log.info("Unregistered command handler for: {}", commandType.getName());
        } else {
            log.warn("No handler found to unregister for command: {}", commandType.getName());
        }
    }

    @Override
    public boolean hasHandler(Class<? extends Command<?>> commandType) {
        return handlers.containsKey(commandType);
    }

    /**
     * Discovers all CommandHandler beans in the ApplicationContext and registers them.
     */
    @SuppressWarnings("unchecked")
    private void discoverHandlers() {
        Map<String, CommandHandler> handlerBeans = applicationContext.getBeansOfType(CommandHandler.class);
        
        for (CommandHandler<?, ?> handler : handlerBeans.values()) {
            try {
                registerHandler(handler);
            } catch (Exception e) {
                log.error("Failed to register command handler: {}", handler.getClass().getName(), e);
            }
        }
        
        log.info("Discovered and registered {} command handlers", handlers.size());
    }

    /**
     * Exception thrown when no handler is found for a command.
     */
    public static class CommandHandlerNotFoundException extends RuntimeException {
        public CommandHandlerNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when command processing fails.
     */
    public static class CommandProcessingException extends RuntimeException {
        public CommandProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}