/*
 * Copyright 2024 Firefly Software Solutions Inc.
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

package com.firefly.common.domain.client;

import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Type-safe wrapper for SDK service clients.
 * 
 * <p>This class provides a type-safe wrapper around ServiceClient for SDK operations,
 * eliminating the need for casting when working with SDK instances.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create a typed SDK client
 * TypedSdkClient<PaymentSDK> client = ServiceClient.sdk("payment-service", PaymentSDK.class)
 *     .sdkSupplier(() -> new PaymentSDK(apiKey))
 *     .build()
 *     .typed();
 *
 * // Type-safe operations - no casting required!
 * Mono<PaymentResult> result = client.call(sdk -> sdk.processPayment(request));
 * Mono<PaymentResult> asyncResult = client.callAsync(sdk -> sdk.processPaymentAsync(request));
 * PaymentSDK sdk = client.sdk();
 * }</pre>
 *
 * @param <S> the SDK type
 * @author Firefly Software Solutions Inc
 * @since 2.0.0
 */
public class TypedSdkClient<S> {

    private final ServiceClient serviceClient;

    /**
     * Creates a new typed SDK client wrapper.
     *
     * @param serviceClient the underlying service client
     */
    public TypedSdkClient(ServiceClient serviceClient) {
        this.serviceClient = serviceClient;
    }

    /**
     * Execute a synchronous operation with the SDK instance.
     *
     * <p>This method provides type-safe access to the SDK without casting.
     *
     * @param operation the operation to execute with the SDK
     * @param <R> the return type
     * @return a Mono containing the operation result
     */
    @SuppressWarnings("unchecked")
    public <R> Mono<R> call(Function<S, R> operation) {
        return serviceClient.call(sdk -> operation.apply((S) sdk));
    }

    /**
     * Execute an asynchronous operation with the SDK instance.
     *
     * <p>This method provides type-safe access to the SDK for async operations.
     *
     * @param operation the async operation to execute with the SDK
     * @param <R> the return type
     * @return a Mono containing the operation result
     */
    @SuppressWarnings("unchecked")
    public <R> Mono<R> callAsync(Function<S, Mono<R>> operation) {
        return serviceClient.callAsync(sdk -> operation.apply((S) sdk));
    }

    /**
     * Get direct access to the SDK instance.
     *
     * <p>This method provides direct, type-safe access to the SDK instance.
     *
     * @return the SDK instance
     */
    @SuppressWarnings("unchecked")
    public S sdk() {
        return serviceClient.sdk();
    }

    /**
     * Get the underlying ServiceClient.
     *
     * @return the underlying service client
     */
    public ServiceClient getServiceClient() {
        return serviceClient;
    }
}
