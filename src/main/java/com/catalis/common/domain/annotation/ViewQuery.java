package com.catalis.common.domain.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a view query in the domain layer (CQRS - read-only operation).
 * This annotation can be applied to a class or directly to the query entrypoint method.
 *
 * Expectations for code annotated with {@code @ViewQuery}:
 * - Provide an entrypoint method (commonly named {@code execute}) that performs no state changes.
 * - Return a DTO/record or domain projection representing the requested data.
 * - Do not use ServiceResult for read-only retrieval; throw exceptions only for programming/config errors,
 *   not for expected absence (prefer Optional handling at repository level and map to exceptions if required by contract).
 *
 * See README.md section "@ViewQuery - Read-Only Operations" for examples and guidelines.
 */
@Documented
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ViewQuery {
    /**
     * Optional description of the query's purpose.
     */
    String value() default "";
}