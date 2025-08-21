package com.catalis.common.domain.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a view query in the domain layer.
 * Views must return a record or DTO and never modify state.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ViewQuery {
    /**
     * Optional description of the query's purpose.
     */
    String value() default "";
}