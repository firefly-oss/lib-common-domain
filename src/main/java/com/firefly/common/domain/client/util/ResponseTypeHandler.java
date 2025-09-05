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
package com.firefly.common.domain.client.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firefly.common.domain.client.config.ResponseConfiguration;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Utility class for handling complex response types, deserialization, validation,
 * and transformation in ServiceClient implementations.
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public class ResponseTypeHandler {
    
    private final ObjectMapper objectMapper;
    private final Map<Class<?>, Function<String, ?>> customDeserializers;
    private final Map<Class<?>, Function<?, Boolean>> responseValidators;
    private final Map<Class<?>, Function<?, ?>> responseTransformers;
    
    /**
     * Creates a new ResponseTypeHandler with the specified configuration.
     *
     * @param objectMapper the Jackson ObjectMapper for JSON deserialization
     * @param customDeserializers map of custom deserializers by type
     * @param responseValidators map of response validators by type
     * @param responseTransformers map of response transformers by type
     */
    public ResponseTypeHandler(ObjectMapper objectMapper,
                             Map<Class<?>, Function<String, ?>> customDeserializers,
                             Map<Class<?>, Function<?, Boolean>> responseValidators,
                             Map<Class<?>, Function<?, ?>> responseTransformers) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.customDeserializers = new ConcurrentHashMap<>(customDeserializers != null ? customDeserializers : Map.of());
        this.responseValidators = new ConcurrentHashMap<>(responseValidators != null ? responseValidators : Map.of());
        this.responseTransformers = new ConcurrentHashMap<>(responseTransformers != null ? responseTransformers : Map.of());
    }
    
    /**
     * Creates a new ResponseTypeHandler with default ObjectMapper.
     */
    public ResponseTypeHandler() {
        this(new ObjectMapper(), Map.of(), Map.of(), Map.of());
    }
    
    /**
     * Deserializes a response string to the specified type.
     *
     * @param responseBody the response body as string
     * @param responseType the target response type
     * @param <T> the response type
     * @return a Mono containing the deserialized response
     */
    @SuppressWarnings("unchecked")
    public <T> Mono<T> deserialize(String responseBody, Class<T> responseType) {
        try {
            // Check for custom deserializer
            Function<String, ?> customDeserializer = customDeserializers.get(responseType);
            if (customDeserializer != null) {
                log.debug("Using custom deserializer for type: {}", responseType.getSimpleName());
                T result = (T) customDeserializer.apply(responseBody);
                return Mono.just(result);
            }
            
            // Use Jackson ObjectMapper for standard deserialization
            T result = objectMapper.readValue(responseBody, responseType);
            return Mono.just(result);
            
        } catch (Exception e) {
            log.error("Failed to deserialize response to type {}: {}", responseType.getSimpleName(), e.getMessage());
            return Mono.error(new RuntimeException("Deserialization failed for type " + responseType.getSimpleName(), e));
        }
    }
    
    /**
     * Deserializes a response string using TypeReference for generic types.
     *
     * @param responseBody the response body as string
     * @param typeReference the type reference for generic types
     * @param <T> the response type
     * @return a Mono containing the deserialized response
     */
    public <T> Mono<T> deserialize(String responseBody, TypeReference<T> typeReference) {
        try {
            T result = objectMapper.readValue(responseBody, typeReference);
            return Mono.just(result);
        } catch (Exception e) {
            log.error("Failed to deserialize response using TypeReference: {}", e.getMessage());
            return Mono.error(new RuntimeException("Deserialization failed using TypeReference", e));
        }
    }
    
    /**
     * Processes a response with the specified configuration.
     *
     * @param responseBody the response body as string
     * @param responseConfig the response configuration
     * @param <T> the response type
     * @return a Mono containing the processed response
     */
    public <T> Mono<T> processResponse(String responseBody, ResponseConfiguration<T> responseConfig) {
        Mono<T> responseMono;
        
        // Deserialize using custom deserializer if provided
        if (responseConfig.getCustomDeserializer() != null) {
            try {
                T result = responseConfig.getCustomDeserializer().apply(responseBody);
                responseMono = Mono.just(result);
            } catch (Exception e) {
                return Mono.error(new RuntimeException("Custom deserialization failed", e));
            }
        } else if (responseConfig.getTypeReference() != null) {
            // Use TypeReference for generic types
            responseMono = deserialize(responseBody, responseConfig.getTypeReference());
        } else if (responseConfig.getResponseType() != null) {
            // Use Class for simple types
            responseMono = deserialize(responseBody, responseConfig.getResponseType());
        } else {
            return Mono.error(new IllegalArgumentException("No response type or type reference specified"));
        }
        
        // Apply validation if configured
        if (responseConfig.shouldValidateResponse() && responseConfig.getValidator() != null) {
            responseMono = responseMono.flatMap(responseConfig.getValidator());
        }
        
        // Apply transformation if configured
        if (responseConfig.shouldTransformResponse() && responseConfig.getTransformer() != null) {
            responseMono = responseMono.map(responseConfig.getTransformer());
        }
        
        return responseMono;
    }
    
    /**
     * Validates a response using the configured validator for the type.
     *
     * @param response the response to validate
     * @param responseType the response type
     * @param <T> the response type
     * @return a Mono containing the validated response
     */
    @SuppressWarnings("unchecked")
    public <T> Mono<T> validateResponse(T response, Class<T> responseType) {
        Function<?, Boolean> validator = responseValidators.get(responseType);
        if (validator != null) {
            try {
                Function<T, Boolean> typedValidator = (Function<T, Boolean>) validator;
                if (typedValidator.apply(response)) {
                    return Mono.just(response);
                } else {
                    return Mono.error(new IllegalArgumentException("Response validation failed for type " + responseType.getSimpleName()));
                }
            } catch (Exception e) {
                return Mono.error(new RuntimeException("Response validation error", e));
            }
        }
        return Mono.just(response);
    }
    
    /**
     * Transforms a response using the configured transformer for the type.
     *
     * @param response the response to transform
     * @param responseType the response type
     * @param <T> the response type
     * @return the transformed response
     */
    @SuppressWarnings("unchecked")
    public <T> T transformResponse(T response, Class<T> responseType) {
        Function<?, ?> transformer = responseTransformers.get(responseType);
        if (transformer != null) {
            try {
                Function<T, T> typedTransformer = (Function<T, T>) transformer;
                return typedTransformer.apply(response);
            } catch (Exception e) {
                log.warn("Response transformation failed for type {}: {}", responseType.getSimpleName(), e.getMessage());
                return response; // Return original response if transformation fails
            }
        }
        return response;
    }
    
    /**
     * Adds a custom deserializer for the specified type.
     *
     * @param responseType the response type
     * @param deserializer the custom deserializer function
     * @param <T> the response type
     */
    @SuppressWarnings("unchecked")
    public <T> void addCustomDeserializer(Class<T> responseType, Function<String, T> deserializer) {
        if (responseType != null && deserializer != null) {
            this.customDeserializers.put(responseType, (Function<String, ?>) deserializer);
        }
    }
    
    /**
     * Adds a response validator for the specified type.
     *
     * @param responseType the response type
     * @param validator the validator function
     * @param <T> the response type
     */
    @SuppressWarnings("unchecked")
    public <T> void addResponseValidator(Class<T> responseType, Function<T, Boolean> validator) {
        if (responseType != null && validator != null) {
            this.responseValidators.put(responseType, (Function<?, Boolean>) validator);
        }
    }
    
    /**
     * Adds a response transformer for the specified type.
     *
     * @param responseType the response type
     * @param transformer the transformer function
     * @param <T> the response type
     */
    @SuppressWarnings("unchecked")
    public <T> void addResponseTransformer(Class<T> responseType, Function<T, T> transformer) {
        if (responseType != null && transformer != null) {
            this.responseTransformers.put(responseType, (Function<?, ?>) transformer);
        }
    }
    
    /**
     * Gets the ObjectMapper used for deserialization.
     *
     * @return the ObjectMapper instance
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
