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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of CommandBus with automatic handler discovery,
 * tracing support, error handling, and metrics integration.
 */
@Slf4j
@Component
public class DefaultCommandBus implements CommandBus {

    private final Map<Class<? extends Command<?>>, CommandHandler<?, ?>> handlers = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;
    private final CorrelationContext correlationContext;

    public DefaultCommandBus(ApplicationContext applicationContext, CorrelationContext correlationContext) {
        this.applicationContext = applicationContext;
        this.correlationContext = correlationContext;
        discoverHandlers();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Mono<R> send(Command<R> command) {
        return Mono.fromCallable(() -> {
                    log.debug("Processing command: {} with ID: {}", command.getClass().getSimpleName(), command.getCommandId());
                    
                    CommandHandler<Command<R>, R> handler = (CommandHandler<Command<R>, R>) handlers.get(command.getClass());
                    if (handler == null) {
                        throw new CommandHandlerNotFoundException("No handler found for command: " + command.getClass().getName());
                    }
                    
                    return handler;
                })
                .flatMap(handler -> {
                    // Set correlation context if available
                    if (command.getCorrelationId() != null) {
                        correlationContext.setCorrelationId(command.getCorrelationId());
                    }
                    
                    return handler.handle(command)
                            .doOnSuccess(result -> log.debug("Command {} processed successfully", command.getCommandId()))
                            .doOnError(error -> log.error("Command {} processing failed: {}", command.getCommandId(), error.getMessage()))
                            .doFinally(signalType -> correlationContext.clear());
                })
                .onErrorMap(throwable -> {
                    if (throwable instanceof CommandHandlerNotFoundException) {
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