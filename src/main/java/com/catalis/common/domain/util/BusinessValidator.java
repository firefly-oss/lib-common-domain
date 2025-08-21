package com.catalis.common.domain.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Utility for validating business rules and collecting validation errors.
 */
public class BusinessValidator {
    
    private final List<String> errors = new ArrayList<>();
    
    public BusinessValidator rule(boolean condition, String errorMessage) {
        if (!condition) {
            errors.add(errorMessage);
        }
        return this;
    }
    
    public <T> BusinessValidator rule(T value, Predicate<T> predicate, String errorMessage) {
        if (value != null && predicate.test(value)) {
            errors.add(errorMessage);
        }
        return this;
    }
    
    public BusinessValidator rule(Supplier<Boolean> condition, String errorMessage) {
        try {
            return rule(condition.get(), errorMessage);
        } catch (Exception e) {
            errors.add(errorMessage + " (validation failed: " + e.getMessage() + ")");
            return this;
        }
    }
    
    public BusinessValidator notNull(Object value, String errorMessage) {
        return rule(value != null, errorMessage);
    }
    
    public BusinessValidator notBlank(String value, String errorMessage) {
        return rule(value != null && !value.trim().isEmpty(), errorMessage);
    }
    
    public BusinessValidator notEmpty(Collection<?> collection, String errorMessage) {
        return rule(collection != null && !collection.isEmpty(), errorMessage);
    }
    
    public BusinessValidator range(Number value, Number min, Number max, String errorMessage) {
        if (value == null) return this;
        double val = value.doubleValue();
        return rule(val >= min.doubleValue() && val <= max.doubleValue(), errorMessage);
    }
    
    public BusinessValidator min(Number value, Number minimum, String errorMessage) {
        if (value == null) return this;
        return rule(value.doubleValue() >= minimum.doubleValue(), errorMessage);
    }
    
    public BusinessValidator max(Number value, Number maximum, String errorMessage) {
        if (value == null) return this;
        return rule(value.doubleValue() <= maximum.doubleValue(), errorMessage);
    }
    
    public BusinessValidator length(String value, int expectedLength, String errorMessage) {
        if (value == null) return this;
        return rule(value.length() == expectedLength, errorMessage);
    }
    
    public BusinessValidator minLength(String value, int minLength, String errorMessage) {
        if (value == null) return this;
        return rule(value.length() >= minLength, errorMessage);
    }
    
    public BusinessValidator maxLength(String value, int maxLength, String errorMessage) {
        if (value == null) return this;
        return rule(value.length() <= maxLength, errorMessage);
    }
    
    public BusinessValidator matches(String value, String regex, String errorMessage) {
        if (value == null) return this;
        return rule(value.matches(regex), errorMessage);
    }
    
    public <T> BusinessValidator custom(T value, Function<T, Boolean> validator, String errorMessage) {
        if (value == null) return this;
        try {
            return rule(validator.apply(value), errorMessage);
        } catch (Exception e) {
            errors.add(errorMessage + " (validation failed: " + e.getMessage() + ")");
            return this;
        }
    }
    
    public BusinessValidator when(boolean condition, Supplier<BusinessValidator> validationBlock) {
        if (condition) {
            BusinessValidator conditionalValidator = validationBlock.get();
            errors.addAll(conditionalValidator.getErrors());
        }
        return this;
    }
    
    public void validate() {
        if (!errors.isEmpty()) {
            throw new BusinessValidationException(errors);
        }
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public List<String> getErrors() {
        return List.copyOf(errors);
    }
    
    public int getErrorCount() {
        return errors.size();
    }
    
    public BusinessValidator clear() {
        errors.clear();
        return this;
    }
}