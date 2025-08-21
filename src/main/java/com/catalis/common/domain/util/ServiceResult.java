package com.catalis.common.domain.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Represents the result of a service call with success/failure state.
 * Use this for operations that can fail and you want to handle errors functionally.
 */
public sealed interface ServiceResult<T> permits ServiceResult.Success, ServiceResult.Failure {
    
    record Success<T>(T data) implements ServiceResult<T> {}
    
    record Failure<T>(String error, Throwable cause) implements ServiceResult<T> {
        public Failure(String error) {
            this(error, null);
        }
    }
    
    static <T> ServiceResult<T> success(T data) {
        return new Success<>(data);
    }
    
    static <T> ServiceResult<T> failure(String error, Throwable cause) {
        return new Failure<>(error, cause);
    }
    
    static <T> ServiceResult<T> failure(String error) {
        return new Failure<>(error, null);
    }
    
    static <T> ServiceResult<T> of(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(e.getMessage(), e);
        }
    }
    
    default boolean isSuccess() {
        return this instanceof Success<T>;
    }
    
    default boolean isFailure() {
        return this instanceof Failure<T>;
    }
    
    default Optional<T> getData() {
        return this instanceof Success<T> success ? Optional.of(success.data()) : Optional.empty();
    }
    
    default Optional<String> getError() {
        return this instanceof Failure<T> failure ? Optional.of(failure.error()) : Optional.empty();
    }
    
    default Optional<Throwable> getCause() {
        return this instanceof Failure<T> failure ? Optional.ofNullable(failure.cause()) : Optional.empty();
    }
    
    default <U> ServiceResult<U> map(Function<T, U> mapper) {
        return this instanceof Success<T> success 
            ? ServiceResult.success(mapper.apply(success.data()))
            : (ServiceResult<U>) this;
    }
    
    default <U> ServiceResult<U> flatMap(Function<T, ServiceResult<U>> mapper) {
        return this instanceof Success<T> success 
            ? mapper.apply(success.data())
            : (ServiceResult<U>) this;
    }
    
    default ServiceResult<T> filter(Predicate<T> predicate, String errorMessage) {
        if (this instanceof Success<T> success) {
            return predicate.test(success.data()) 
                ? this 
                : ServiceResult.failure(errorMessage);
        }
        return this;
    }
    
    default ServiceResult<T> onSuccess(Consumer<T> action) {
        if (this instanceof Success<T> success) {
            action.accept(success.data());
        }
        return this;
    }
    
    default ServiceResult<T> onFailure(Consumer<String> action) {
        if (this instanceof Failure<T> failure) {
            action.accept(failure.error());
        }
        return this;
    }
    
    default T orElse(T defaultValue) {
        return this instanceof Success<T> success ? success.data() : defaultValue;
    }
    
    default T orElseGet(Supplier<T> supplier) {
        return this instanceof Success<T> success ? success.data() : supplier.get();
    }
    
    default T orElseThrow() {
        if (this instanceof Success<T> success) {
            return success.data();
        }
        Failure<T> failure = (Failure<T>) this;
        throw failure.cause() != null 
            ? new RuntimeException(failure.error(), failure.cause())
            : new RuntimeException(failure.error());
    }
    
    default <X extends Throwable> T orElseThrow(Supplier<X> exceptionSupplier) throws X {
        if (this instanceof Success<T> success) {
            return success.data();
        }
        throw exceptionSupplier.get();
    }
    
    default ServiceResult<T> peek(Consumer<T> action) {
        if (this instanceof Success<T> success) {
            action.accept(success.data());
        }
        return this;
    }
    
    default ServiceResult<T> peekError(Consumer<String> action) {
        if (this instanceof Failure<T> failure) {
            action.accept(failure.error());
        }
        return this;
    }
    
    default ServiceResult<T> recover(Function<String, T> recovery) {
        if (this instanceof Failure<T> failure) {
            try {
                return ServiceResult.success(recovery.apply(failure.error()));
            } catch (Exception e) {
                return ServiceResult.failure("Recovery failed: " + e.getMessage(), e);
            }
        }
        return this;
    }
    
    default ServiceResult<T> recoverWith(Function<String, ServiceResult<T>> recovery) {
        if (this instanceof Failure<T> failure) {
            try {
                return recovery.apply(failure.error());
            } catch (Exception e) {
                return ServiceResult.failure("Recovery failed: " + e.getMessage(), e);
            }
        }
        return this;
    }
    
    static <T, U> ServiceResult<T> combine(ServiceResult<T> first, ServiceResult<U> second, Function<T, Function<U, T>> combiner) {
        if (first.isSuccess() && second.isSuccess()) {
            T firstData = first.getData().orElseThrow();
            U secondData = second.getData().orElseThrow();
            return ServiceResult.success(combiner.apply(firstData).apply(secondData));
        }
        
        if (first.isFailure()) {
            return (ServiceResult<T>) first;
        }
        
        return ServiceResult.failure(second.getError().orElse("Unknown error"));
    }
    
    default boolean isEmpty() {
        return this instanceof Success<T> success && success.data() == null;
    }
    
    default ServiceResult<T> ifEmpty(Supplier<ServiceResult<T>> alternative) {
        if (isEmpty()) {
            return alternative.get();
        }
        return this;
    }
}