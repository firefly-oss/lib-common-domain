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

package com.firefly.common.domain.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * Utility class for resolving generic type parameters at runtime.
 * 
 * <p>This class provides methods to extract generic type information from classes
 * that implement parameterized interfaces, which is useful for automatic handler
 * registration in CQRS frameworks.
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public final class GenericTypeResolver {

    private GenericTypeResolver() {
        // Utility class - prevent instantiation
    }

    /**
     * Resolves the generic type parameter for a given interface.
     * 
     * <p>This method walks up the class hierarchy to find the parameterized type
     * information for the specified interface and returns the type argument at
     * the given index.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // For: class CreateAccountHandler implements CommandHandler<CreateAccountCommand, AccountResult>
     * Class<?> commandType = resolveGenericType(handler.getClass(), CommandHandler.class, 0);
     * // Returns: CreateAccountCommand.class
     * }</pre>
     * 
     * @param implementationClass the concrete class that implements the interface
     * @param targetInterface the parameterized interface to resolve types for
     * @param typeParameterIndex the index of the type parameter to resolve (0-based)
     * @return the resolved type, or null if it cannot be determined
     * @throws IllegalArgumentException if parameters are invalid
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> resolveGenericType(Class<?> implementationClass, 
                                                  Class<?> targetInterface, 
                                                  int typeParameterIndex) {
        if (implementationClass == null) {
            throw new IllegalArgumentException("Implementation class cannot be null");
        }
        if (targetInterface == null) {
            throw new IllegalArgumentException("Target interface cannot be null");
        }
        if (typeParameterIndex < 0) {
            throw new IllegalArgumentException("Type parameter index must be non-negative");
        }

        log.debug("Resolving generic type for class: {} implementing interface: {} at index: {}", 
            implementationClass.getName(), targetInterface.getName(), typeParameterIndex);

        // Walk up the class hierarchy
        Class<?> currentClass = implementationClass;
        while (currentClass != null && currentClass != Object.class) {
            
            // Check direct interfaces
            Type[] interfaces = currentClass.getGenericInterfaces();
            for (Type interfaceType : interfaces) {
                Class<T> resolvedType = extractTypeFromInterface(interfaceType, targetInterface, typeParameterIndex);
                if (resolvedType != null) {
                    log.debug("Resolved generic type: {} for class: {}", resolvedType.getName(), implementationClass.getName());
                    return resolvedType;
                }
            }
            
            // Check superclass
            Type superclass = currentClass.getGenericSuperclass();
            if (superclass instanceof ParameterizedType) {
                Class<T> resolvedType = extractTypeFromSuperclass(superclass, targetInterface, typeParameterIndex, currentClass);
                if (resolvedType != null) {
                    log.debug("Resolved generic type from superclass: {} for class: {}", resolvedType.getName(), implementationClass.getName());
                    return resolvedType;
                }
            }
            
            currentClass = currentClass.getSuperclass();
        }

        log.warn("Could not resolve generic type for class: {} implementing interface: {} at index: {}", 
            implementationClass.getName(), targetInterface.getName(), typeParameterIndex);
        return null;
    }

    /**
     * Extracts type information from a parameterized interface.
     */
    @SuppressWarnings("unchecked")
    private static <T> Class<T> extractTypeFromInterface(Type interfaceType, 
                                                         Class<?> targetInterface, 
                                                         int typeParameterIndex) {
        if (!(interfaceType instanceof ParameterizedType)) {
            return null;
        }

        ParameterizedType parameterizedType = (ParameterizedType) interfaceType;
        Type rawType = parameterizedType.getRawType();

        // Check if this is the target interface
        if (rawType.equals(targetInterface)) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            
            if (typeParameterIndex >= typeArguments.length) {
                log.warn("Type parameter index {} is out of bounds for interface {} with {} type arguments", 
                    typeParameterIndex, targetInterface.getName(), typeArguments.length);
                return null;
            }

            Type typeArgument = typeArguments[typeParameterIndex];
            if (typeArgument instanceof Class) {
                return (Class<T>) typeArgument;
            } else if (typeArgument instanceof ParameterizedType) {
                Type rawTypeArg = ((ParameterizedType) typeArgument).getRawType();
                if (rawTypeArg instanceof Class) {
                    return (Class<T>) rawTypeArg;
                }
            }
        }

        return null;
    }

    /**
     * Extracts type information from a parameterized superclass.
     */
    @SuppressWarnings("unchecked")
    private static <T> Class<T> extractTypeFromSuperclass(Type superclass, 
                                                          Class<?> targetInterface, 
                                                          int typeParameterIndex,
                                                          Class<?> currentClass) {
        if (!(superclass instanceof ParameterizedType)) {
            return null;
        }

        ParameterizedType parameterizedSuperclass = (ParameterizedType) superclass;
        Class<?> superclassRaw = (Class<?>) parameterizedSuperclass.getRawType();

        // Recursively check if the superclass implements the target interface
        return resolveGenericType(superclassRaw, targetInterface, typeParameterIndex);
    }

    /**
     * Convenience method to resolve the command type from a CommandHandler.
     * 
     * @param handlerClass the command handler class
     * @return the command type, or null if it cannot be determined
     */
    public static Class<?> resolveCommandType(Class<?> handlerClass) {
        try {
            Class<?> commandHandlerInterface = Class.forName("com.firefly.common.domain.cqrs.command.CommandHandler");
            return resolveGenericType(handlerClass, commandHandlerInterface, 0);
        } catch (ClassNotFoundException e) {
            log.error("CommandHandler interface not found", e);
            return null;
        }
    }

    /**
     * Convenience method to resolve the query type from a QueryHandler.
     * 
     * @param handlerClass the query handler class
     * @return the query type, or null if it cannot be determined
     */
    public static Class<?> resolveQueryType(Class<?> handlerClass) {
        try {
            Class<?> queryHandlerInterface = Class.forName("com.firefly.common.domain.cqrs.query.QueryHandler");
            return resolveGenericType(handlerClass, queryHandlerInterface, 0);
        } catch (ClassNotFoundException e) {
            log.error("QueryHandler interface not found", e);
            return null;
        }
    }
}
