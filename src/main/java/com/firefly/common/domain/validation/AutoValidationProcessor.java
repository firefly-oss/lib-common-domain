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

package com.firefly.common.domain.validation;

import com.firefly.common.domain.validation.annotations.*;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Automatic validation processor that handles annotation-based validation.
 *
 * <p>This processor eliminates the need for manual validation code by
 * automatically processing validation annotations on command and query fields.
 *
 * <p>Supported annotations:
 * <ul>
 *   <li>@Valid - Marks fields for validation</li>
 *   <li>@NotNull - Ensures field is not null</li>
 *   <li>@NotEmpty - Ensures string is not empty</li>
 *   <li>@Email - Validates email format</li>
 *   <li>@Min/@Max - Validates numeric ranges</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public class AutoValidationProcessor {

    private static final Pattern DEFAULT_EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    /**
     * Automatically validates an object using its validation annotations.
     *
     * @param object the object to validate
     * @return a Mono containing the validation result
     */
    public static Mono<ValidationResult> validate(Object object) {
        if (object == null) {
            return Mono.just(ValidationResult.failure("object", "Object cannot be null"));
        }

        ValidationResult.Builder builder = ValidationResult.builder();
        Class<?> clazz = object.getClass();

        try {
            // Check if the class or any field has @Valid annotation
            boolean hasValidation = clazz.isAnnotationPresent(Valid.class);
            
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Valid.class) || hasValidation) {
                    validateField(object, field, builder);
                }
            }
        } catch (Exception e) {
            log.error("Error during automatic validation", e);
            builder.addError("validation", "Validation failed: " + e.getMessage());
        }

        return Mono.just(builder.build());
    }

    /**
     * Validates a single field using its annotations.
     */
    private static void validateField(Object object, Field field, ValidationResult.Builder builder) {
        try {
            field.setAccessible(true);
            Object value = field.get(object);
            String fieldName = field.getName();

            // Process validation annotations
            for (Annotation annotation : field.getAnnotations()) {
                processAnnotation(fieldName, value, annotation, builder);
            }
        } catch (IllegalAccessException e) {
            log.error("Cannot access field: " + field.getName(), e);
            builder.addError(field.getName(), "Cannot validate field: " + e.getMessage());
        }
    }

    /**
     * Processes a single validation annotation.
     */
    private static void processAnnotation(String fieldName, Object value, Annotation annotation, ValidationResult.Builder builder) {
        if (annotation instanceof NotNull) {
            validateNotNull(fieldName, value, (NotNull) annotation, builder);
        } else if (annotation instanceof NotEmpty) {
            validateNotEmpty(fieldName, value, (NotEmpty) annotation, builder);
        } else if (annotation instanceof Email) {
            validateEmail(fieldName, value, (Email) annotation, builder);
        } else if (annotation instanceof Min) {
            validateMin(fieldName, value, (Min) annotation, builder);
        } else if (annotation instanceof Max) {
            validateMax(fieldName, value, (Max) annotation, builder);
        }
    }

    /**
     * Validates @NotNull annotation.
     */
    private static void validateNotNull(String fieldName, Object value, NotNull annotation, ValidationResult.Builder builder) {
        if (value == null) {
            String message = formatMessage(annotation.message(), fieldName, null);
            builder.addError(fieldName, message, "NOT_NULL");
        }
    }

    /**
     * Validates @NotEmpty annotation.
     */
    private static void validateNotEmpty(String fieldName, Object value, NotEmpty annotation, ValidationResult.Builder builder) {
        if (value == null) {
            String message = formatMessage(annotation.message(), fieldName, null);
            builder.addError(fieldName, message, "NOT_EMPTY");
            return;
        }

        if (value instanceof String) {
            String stringValue = (String) value;
            if (annotation.trim()) {
                stringValue = stringValue.trim();
            }
            if (stringValue.isEmpty()) {
                String message = formatMessage(annotation.message(), fieldName, null);
                builder.addError(fieldName, message, "NOT_EMPTY");
            }
        }
    }

    /**
     * Validates @Email annotation.
     */
    private static void validateEmail(String fieldName, Object value, Email annotation, ValidationResult.Builder builder) {
        if (value == null || (value instanceof String && ((String) value).isEmpty())) {
            if (!annotation.allowEmpty()) {
                String message = formatMessage(annotation.message(), fieldName, null);
                builder.addError(fieldName, message, "EMAIL");
            }
            return;
        }

        if (value instanceof String) {
            String email = (String) value;
            Pattern pattern = annotation.pattern().isEmpty() ? 
                DEFAULT_EMAIL_PATTERN : 
                Pattern.compile(annotation.pattern());
            
            if (!pattern.matcher(email).matches()) {
                String message = formatMessage(annotation.message(), fieldName, null);
                builder.addError(fieldName, message, "EMAIL");
            }
        }
    }

    /**
     * Validates @Min annotation.
     */
    private static void validateMin(String fieldName, Object value, Min annotation, ValidationResult.Builder builder) {
        if (value == null) {
            if (!annotation.allowNull()) {
                String message = formatMessage(annotation.message(), fieldName, String.valueOf(annotation.value()));
                builder.addError(fieldName, message, "MIN");
            }
            return;
        }

        long minValue = annotation.value();
        boolean valid = false;

        if (value instanceof Number) {
            Number number = (Number) value;
            if (value instanceof BigDecimal) {
                valid = ((BigDecimal) value).compareTo(BigDecimal.valueOf(minValue)) >= 0;
            } else {
                valid = number.longValue() >= minValue;
            }
        }

        if (!valid) {
            String message = formatMessage(annotation.message(), fieldName, String.valueOf(minValue));
            builder.addError(fieldName, message, "MIN");
        }
    }

    /**
     * Validates @Max annotation.
     */
    private static void validateMax(String fieldName, Object value, Max annotation, ValidationResult.Builder builder) {
        if (value == null) {
            if (!annotation.allowNull()) {
                String message = formatMessage(annotation.message(), fieldName, String.valueOf(annotation.value()));
                builder.addError(fieldName, message, "MAX");
            }
            return;
        }

        long maxValue = annotation.value();
        boolean valid = false;

        if (value instanceof Number) {
            Number number = (Number) value;
            if (value instanceof BigDecimal) {
                valid = ((BigDecimal) value).compareTo(BigDecimal.valueOf(maxValue)) <= 0;
            } else {
                valid = number.longValue() <= maxValue;
            }
        }

        if (!valid) {
            String message = formatMessage(annotation.message(), fieldName, String.valueOf(maxValue));
            builder.addError(fieldName, message, "MAX");
        }
    }

    /**
     * Formats validation messages with placeholders.
     */
    private static String formatMessage(String template, String fieldName, String value) {
        String message = template.replace("{field}", fieldName);
        if (value != null) {
            message = message.replace("{value}", value);
        }
        return message;
    }
}
