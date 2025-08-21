package com.catalis.common.domain.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a command in the domain layer (CQRS - state-changing operation).
 * This annotation can be applied to a class or directly to the command entrypoint method.
 *
 * Expectations for code annotated with {@code @CommandQuery}:
 * - Provide an entrypoint method (commonly named {@code execute}) that performs a state change.
 * - The command entrypoint should return {@code void}.
 * - Validation/business rule violations should be signaled by throwing exceptions (e.g., BusinessValidationException).
 * - Do not return ServiceResult for commands; failures should raise exceptions to ensure upstream saga/transaction handling.
 *
 * See README.md section "@CommandQuery - State-Changing Operations" for examples and guidelines.
 */
@Documented
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandQuery {
    /**
     * Optional description of the command's purpose.
     */
    String value() default "";
}