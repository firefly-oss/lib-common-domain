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

package com.firefly.common.domain.client.sdk;

import com.firefly.common.domain.client.ServiceClient;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * ServiceClient implementation for SDK-based integrations.
 * 
 * <p>This interface extends the base ServiceClient to provide first-class support for
 * SDK-based service integrations, which are common in modern microservice architectures.
 * Unlike REST or gRPC clients that work with raw HTTP/protocol calls, SDK clients
 * provide strongly-typed, domain-specific APIs.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Strongly-typed SDK integration with type safety</li>
 *   <li>Automatic SDK lifecycle management (initialization, cleanup)</li>
 *   <li>Built-in circuit breaker and retry mechanisms for SDK calls</li>
 *   <li>Correlation context propagation through SDK calls</li>
 *   <li>SDK-specific configuration and authentication handling</li>
 *   <li>Reactive programming model with Mono return types</li>
 * </ul>
 * 
 * <p>Common SDK integration patterns:
 * <ul>
 *   <li>AWS SDK clients (S3, DynamoDB, SQS, etc.)</li>
 *   <li>Payment provider SDKs (Stripe, PayPal, etc.)</li>
 *   <li>Banking/Financial service SDKs</li>
 *   <li>Third-party API SDKs (Twilio, SendGrid, etc.)</li>
 * </ul>
 * 
 * <p>Example usage in CQRS Command Handler:
 * <pre>{@code
 * @Component
 * public class ProcessPaymentHandler implements CommandHandler<ProcessPaymentCommand, PaymentResult> {
 *     
 *     private final SdkServiceClient<StripeSDK> stripeClient;
 *     
 *     @Override
 *     public Mono<PaymentResult> handle(ProcessPaymentCommand command) {
 *         return stripeClient.execute(sdk -> 
 *             sdk.charges().create(ChargeCreateParams.builder()
 *                 .setAmount(command.getAmount())
 *                 .setCurrency(command.getCurrency())
 *                 .setSource(command.getPaymentToken())
 *                 .build())
 *         ).map(charge -> new PaymentResult(charge.getId(), charge.getStatus()));
 *     }
 * }
 * }</pre>
 * 
 * <p>Example usage in CQRS Query Handler:
 * <pre>{@code
 * @Component
 * public class GetAccountBalanceHandler implements QueryHandler<GetAccountBalanceQuery, AccountBalance> {
 *     
 *     private final SdkServiceClient<BankingSDK> bankingClient;
 *     
 *     @Override
 *     public Mono<AccountBalance> handle(GetAccountBalanceQuery query) {
 *         return bankingClient.execute(sdk -> 
 *             sdk.accounts().getBalance(query.getAccountId())
 *         );
 *     }
 * }
 * }</pre>
 * 
 * @param <S> The SDK type this client manages
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 * @see ServiceClient
 * @see com.firefly.common.domain.client.sdk.builder.SdkServiceClientBuilder
 */
public interface SdkServiceClient<S> extends ServiceClient<S> {

    /**
     * Executes an operation using the managed SDK instance.
     * 
     * <p>This is the primary method for SDK-based operations. The SDK instance
     * is managed by the client and provided to the operation function. The operation
     * is executed with circuit breaker and retry protection.
     * 
     * @param operation the operation to execute with the SDK
     * @param <R> the result type
     * @return a Mono containing the operation result
     * @throws IllegalStateException if the SDK is not properly initialized
     */
    <R> Mono<R> execute(Function<S, R> operation);

    /**
     * Executes an asynchronous operation using the managed SDK instance.
     * 
     * <p>For SDKs that return CompletableFuture or other async types, this method
     * provides proper integration with the reactive programming model.
     * 
     * @param operation the async operation to execute with the SDK
     * @param <R> the result type
     * @return a Mono containing the operation result
     */
    <R> Mono<R> executeAsync(Function<S, Mono<R>> operation);

    /**
     * Gets the managed SDK instance directly.
     * 
     * <p>Use this method when you need direct access to the SDK for complex
     * operations that don't fit the execute pattern. However, direct access
     * bypasses circuit breaker and retry mechanisms.
     * 
     * @return the SDK instance
     * @throws IllegalStateException if the SDK is not properly initialized
     */
    S getSdk();

    /**
     * Checks if the SDK is properly initialized and ready for use.
     * 
     * @return true if the SDK is ready, false otherwise
     */
    boolean isReady();

    /**
     * Gets the SDK version information.
     * 
     * @return the SDK version string, or null if not available
     */
    String getSdkVersion();

    /**
     * Gets the SDK configuration information.
     * 
     * @return a map of configuration properties
     */
    java.util.Map<String, Object> getSdkConfiguration();

    /**
     * Performs a health check specific to the SDK.
     * 
     * <p>This method should verify that the SDK is properly configured
     * and can communicate with its target service.
     * 
     * @return a Mono that completes successfully if the SDK is healthy
     */
    @Override
    Mono<Void> healthCheck();

    /**
     * Gracefully shuts down the SDK and releases resources.
     * 
     * <p>This method should be called when the client is no longer needed
     * to ensure proper cleanup of SDK resources.
     * 
     * @return a Mono that completes when shutdown is finished
     */
    Mono<Void> shutdown();
}
