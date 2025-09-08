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

import com.firefly.common.domain.util.GenericTypeResolver;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Abstract base class for command handlers that provides common functionality
 * and eliminates boilerplate code.
 *
 * <p>This base class provides:
 * <ul>
 *   <li>Automatic type detection from generics</li>
 *   <li>Built-in logging with structured context</li>
 *   <li>Performance monitoring and metrics</li>
 *   <li>Error handling and retry logic</li>
 *   <li>Correlation context management</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * @CommandHandler
 * public class CreateAccountHandler extends BaseCommandHandler<CreateAccountCommand, AccountResult> {
 *     
 *     @Override
 *     protected Mono<AccountResult> doHandle(CreateAccountCommand command) {
 *         // Only business logic needed - logging, metrics, etc. handled by base class
 *         return createAccount(command)
 *             .map(account -> AccountResult.builder()
 *                 .accountId(account.getId())
 *                 .status("CREATED")
 *                 .build());
 *     }
 * }
 * }</pre>
 *
 * @param <C> the command type this handler processes
 * @param <R> the result type returned by this handler
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public abstract class BaseCommandHandler<C extends Command<R>, R> implements CommandHandler<C, R> {

    private final Class<C> commandType;
    private final Class<R> resultType;

    /**
     * Constructor that automatically detects command and result types from generics.
     */
    @SuppressWarnings("unchecked")
    protected BaseCommandHandler() {
        this.commandType = (Class<C>) GenericTypeResolver.resolveCommandType(this.getClass());
        this.resultType = (Class<R>) GenericTypeResolver.resolveCommandResultType(this.getClass());
        
        if (this.commandType == null) {
            throw new IllegalStateException(
                "Could not automatically determine command type for handler: " + this.getClass().getName() +
                ". Please ensure the handler extends BaseCommandHandler with proper generic types."
            );
        }
        
        log.debug("Initialized command handler for {} -> {}", 
            commandType.getSimpleName(), 
            resultType != null ? resultType.getSimpleName() : "Unknown");
    }

    @Override
    public final Mono<R> handle(C command) {
        Instant startTime = Instant.now();
        String commandId = command.getCommandId();
        String commandTypeName = commandType.getSimpleName();

        return Mono.fromCallable(() -> {
                log.debug("Starting command processing: {} [{}]", commandTypeName, commandId);
                return command;
            })
            .flatMap(this::preProcess)
            .flatMap(this::doHandle)
            .flatMap(result -> postProcess(command, result))
            .doOnSuccess(result -> {
                Duration duration = Duration.between(startTime, Instant.now());
                log.info("Command processed successfully: {} [{}] in {}ms", 
                    commandTypeName, commandId, duration.toMillis());
                onSuccess(command, result, duration);
            })
            .doOnError(error -> {
                Duration duration = Duration.between(startTime, Instant.now());
                log.error("Command processing failed: {} [{}] in {}ms - {}", 
                    commandTypeName, commandId, duration.toMillis(), error.getMessage());
                onError(command, error, duration);
            })
            .onErrorMap(this::mapError);
    }

    /**
     * Implement this method with your business logic.
     * All boilerplate (logging, metrics, error handling) is handled by the base class.
     *
     * @param command the command to process
     * @return a Mono containing the result
     */
    protected abstract Mono<R> doHandle(C command);

    /**
     * Pre-processing hook called before command handling.
     * Override to add custom pre-processing logic.
     *
     * @param command the command to pre-process
     * @return a Mono containing the command (possibly modified)
     */
    protected Mono<C> preProcess(C command) {
        return Mono.just(command);
    }

    /**
     * Post-processing hook called after successful command handling.
     * Override to add custom post-processing logic.
     *
     * @param command the original command
     * @param result the result from command handling
     * @return a Mono containing the result (possibly modified)
     */
    protected Mono<R> postProcess(C command, R result) {
        return Mono.just(result);
    }

    /**
     * Success callback for metrics and monitoring.
     * Override to add custom success handling.
     *
     * @param command the processed command
     * @param result the result
     * @param duration processing duration
     */
    protected void onSuccess(C command, R result, Duration duration) {
        // Default implementation - override for custom metrics
    }

    /**
     * Error callback for metrics and monitoring.
     * Override to add custom error handling.
     *
     * @param command the command that failed
     * @param error the error that occurred
     * @param duration processing duration before failure
     */
    protected void onError(C command, Throwable error, Duration duration) {
        // Default implementation - override for custom error handling
    }

    /**
     * Error mapping hook for converting exceptions.
     * Override to provide custom error mapping logic.
     *
     * @param error the original error
     * @return the mapped error
     */
    protected Throwable mapError(Throwable error) {
        return error; // Default: no mapping
    }

    @Override
    public final Class<C> getCommandType() {
        return commandType;
    }

    @Override
    public final Class<R> getResultType() {
        return resultType;
    }

    /**
     * Gets the handler name for logging and monitoring.
     *
     * @return handler name
     */
    protected String getHandlerName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Checks if this handler can process the given command.
     * Override for custom validation logic.
     *
     * @param command the command to check
     * @return true if this handler can process the command
     */
    @Override
    public boolean canHandle(Command<?> command) {
        return commandType.isInstance(command);
    }
}
