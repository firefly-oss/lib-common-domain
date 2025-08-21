package com.catalis.common.domain.util;

import java.util.List;

/**
 * Exception thrown when business validation rules fail.
 */
public class BusinessValidationException extends RuntimeException {
    
    private final List<String> errors;
    
    public BusinessValidationException(List<String> errors) {
        super("Business validation failed: " + String.join(", ", errors));
        this.errors = List.copyOf(errors);
    }
    
    public List<String> getErrors() {
        return errors;
    }
}