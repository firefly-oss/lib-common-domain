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
package com.firefly.common.domain.client.config;

import com.fasterxml.jackson.core.type.TypeReference;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Configuration class for response handling including type mapping, deserialization,
 * validation, and transformation.
 * 
 * @param <T> the response type
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public class ResponseConfiguration<T> {
    
    private final Class<T> responseType;
    private final TypeReference<T> typeReference;
    private final Function<String, T> customDeserializer;
    private final Function<T, Mono<T>> validator;
    private final Function<T, T> transformer;
    private final boolean validateResponse;
    private final boolean transformResponse;
    
    private ResponseConfiguration(Builder<T> builder) {
        this.responseType = builder.responseType;
        this.typeReference = builder.typeReference;
        this.customDeserializer = builder.customDeserializer;
        this.validator = builder.validator;
        this.transformer = builder.transformer;
        this.validateResponse = builder.validateResponse;
        this.transformResponse = builder.transformResponse;
    }
    
    /**
     * Creates a new ResponseConfiguration builder for the specified type.
     *
     * @param responseType the response type class
     * @param <T> the response type
     * @return a new builder instance
     */
    public static <T> Builder<T> builder(Class<T> responseType) {
        return new Builder<>(responseType);
    }
    
    /**
     * Creates a new ResponseConfiguration builder for the specified type reference.
     *
     * @param typeReference the response type reference for generic types
     * @param <T> the response type
     * @return a new builder instance
     */
    public static <T> Builder<T> builder(TypeReference<T> typeReference) {
        return new Builder<>(typeReference);
    }
    
    /**
     * Creates a simple ResponseConfiguration for the specified type.
     *
     * @param responseType the response type class
     * @param <T> the response type
     * @return a simple configuration
     */
    public static <T> ResponseConfiguration<T> of(Class<T> responseType) {
        return new Builder<>(responseType).build();
    }
    
    /**
     * Creates a simple ResponseConfiguration for the specified type reference.
     *
     * @param typeReference the response type reference
     * @param <T> the response type
     * @return a simple configuration
     */
    public static <T> ResponseConfiguration<T> of(TypeReference<T> typeReference) {
        return new Builder<>(typeReference).build();
    }
    
    /**
     * Gets the response type class.
     *
     * @return the response type class, or null if using type reference
     */
    public Class<T> getResponseType() {
        return responseType;
    }
    
    /**
     * Gets the response type reference for generic types.
     *
     * @return the type reference, or null if using simple class
     */
    public TypeReference<T> getTypeReference() {
        return typeReference;
    }
    
    /**
     * Gets the custom deserializer function.
     *
     * @return the deserializer function, or null if not specified
     */
    public Function<String, T> getCustomDeserializer() {
        return customDeserializer;
    }
    
    /**
     * Gets the response validator function.
     *
     * @return the validator function, or null if not specified
     */
    public Function<T, Mono<T>> getValidator() {
        return validator;
    }
    
    /**
     * Gets the response transformer function.
     *
     * @return the transformer function, or null if not specified
     */
    public Function<T, T> getTransformer() {
        return transformer;
    }
    
    /**
     * Checks if response validation is enabled.
     *
     * @return true if validation should be performed
     */
    public boolean shouldValidateResponse() {
        return validateResponse;
    }
    
    /**
     * Checks if response transformation is enabled.
     *
     * @return true if transformation should be performed
     */
    public boolean shouldTransformResponse() {
        return transformResponse;
    }
    
    /**
     * Builder for ResponseConfiguration.
     *
     * @param <T> the response type
     */
    public static class Builder<T> {
        private final Class<T> responseType;
        private final TypeReference<T> typeReference;
        private Function<String, T> customDeserializer;
        private Function<T, Mono<T>> validator;
        private Function<T, T> transformer;
        private boolean validateResponse = false;
        private boolean transformResponse = false;
        
        private Builder(Class<T> responseType) {
            this.responseType = responseType;
            this.typeReference = null;
        }
        
        private Builder(TypeReference<T> typeReference) {
            this.responseType = null;
            this.typeReference = typeReference;
        }
        
        /**
         * Sets a custom deserializer for the response.
         *
         * @param deserializer function to deserialize response string to object
         * @return this builder instance
         */
        public Builder<T> customDeserializer(Function<String, T> deserializer) {
            this.customDeserializer = deserializer;
            return this;
        }
        
        /**
         * Sets a validator for the response.
         *
         * @param validator function to validate the response
         * @return this builder instance
         */
        public Builder<T> validator(Function<T, Mono<T>> validator) {
            this.validator = validator;
            this.validateResponse = validator != null;
            return this;
        }
        
        /**
         * Sets a transformer for the response.
         *
         * @param transformer function to transform the response
         * @return this builder instance
         */
        public Builder<T> transformer(Function<T, T> transformer) {
            this.transformer = transformer;
            this.transformResponse = transformer != null;
            return this;
        }
        
        /**
         * Enables response validation with a simple validation function.
         *
         * @param validationFunction function that returns true if response is valid
         * @return this builder instance
         */
        public Builder<T> validate(Function<T, Boolean> validationFunction) {
            if (validationFunction != null) {
                this.validator = response -> {
                    if (validationFunction.apply(response)) {
                        return Mono.just(response);
                    } else {
                        return Mono.error(new IllegalArgumentException("Response validation failed"));
                    }
                };
                this.validateResponse = true;
            }
            return this;
        }
        
        /**
         * Enables response validation that throws an exception if validation fails.
         *
         * @param validationFunction function that throws exception if invalid
         * @return this builder instance
         */
        public Builder<T> validateOrThrow(Function<T, T> validationFunction) {
            if (validationFunction != null) {
                this.validator = response -> {
                    try {
                        T validatedResponse = validationFunction.apply(response);
                        return Mono.just(validatedResponse);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                };
                this.validateResponse = true;
            }
            return this;
        }
        
        /**
         * Builds the ResponseConfiguration.
         *
         * @return the configured ResponseConfiguration
         */
        public ResponseConfiguration<T> build() {
            return new ResponseConfiguration<>(this);
        }
    }
}
