package com.catalis.common.domain.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a command query in the domain layer.
 * Commands must return void and throw exceptions on failure.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandQuery {
    /**
     * Optional description of the command's purpose.
     */
    String value() default "";
}