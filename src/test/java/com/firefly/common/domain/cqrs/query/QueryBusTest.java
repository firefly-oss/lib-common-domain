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

package com.firefly.common.domain.cqrs.query;

import com.firefly.common.domain.cqrs.query.DefaultQueryBus.QueryHandlerNotFoundException;
import com.firefly.common.domain.tracing.CorrelationContext;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Comprehensive test suite for QueryBus functionality using banking domain examples.
 * Tests cover query processing, caching, handler registration, and correlation context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Banking Query Bus - Retrieving Customer and Account Information")
class QueryBusTest {

    @Mock
    private ApplicationContext applicationContext;
    
    @Mock
    private CorrelationContext correlationContext;
    
    @Mock
    private CacheManager cacheManager;

    private DefaultQueryBus queryBus;
    private Cache mockCache;

    @BeforeEach
    void setUp() {
        when(applicationContext.getBeansOfType(QueryHandler.class))
            .thenReturn(Map.of());

        mockCache = new ConcurrentMapCache("query-cache");
        lenient().when(cacheManager.getCache(anyString())).thenReturn(mockCache);

        queryBus = new DefaultQueryBus(applicationContext, correlationContext, cacheManager);
    }

    @Test
    @DisplayName("Should retrieve customer account balance successfully")
    void shouldRetrieveCustomerAccountBalance() {
        // Given: A customer wants to check their account balance
        GetAccountBalanceQuery query = new GetAccountBalanceQuery("ACC-12345", "CORR-123");
        
        GetAccountBalanceHandler handler = new GetAccountBalanceHandler();
        queryBus.registerHandler(handler);

        // When: The balance query is processed
        Mono<AccountBalance> result = queryBus.query(query);

        // Then: The account balance should be returned
        StepVerifier.create(result)
            .assertNext(balance -> {
                assertThat(balance.getAccountNumber()).isEqualTo("ACC-12345");
                assertThat(balance.getAvailableBalance()).isEqualTo(new BigDecimal("2500.00"));
                assertThat(balance.getCurrentBalance()).isEqualTo(new BigDecimal("2750.00"));
                assertThat(balance.getCurrency()).isEqualTo("USD");
                assertThat(balance.getLastUpdated()).isNotNull();
            })
            .verifyComplete();

        // And: Correlation context should be properly managed
        verify(correlationContext).setCorrelationId("CORR-123");
        verify(correlationContext).clear();
    }

    @Test
    @DisplayName("Should retrieve customer transaction history with pagination")
    void shouldRetrieveCustomerTransactionHistory() {
        // Given: A customer wants to view their recent transactions
        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(
            "ACC-12345", 
            10, 
            0, 
            "CORR-456"
        );
        
        GetTransactionHistoryHandler handler = new GetTransactionHistoryHandler();
        queryBus.registerHandler(handler);

        // When: The transaction history query is processed
        Mono<TransactionHistory> result = queryBus.query(query);

        // Then: The transaction history should be returned
        StepVerifier.create(result)
            .assertNext(history -> {
                assertThat(history.getAccountNumber()).isEqualTo("ACC-12345");
                assertThat(history.getTransactions()).hasSize(3);
                assertThat(history.getTotalCount()).isEqualTo(3);
                assertThat(history.getPageSize()).isEqualTo(10);
                assertThat(history.getPageNumber()).isEqualTo(0);
                
                // Verify transaction details
                Transaction firstTransaction = history.getTransactions().get(0);
                assertThat(firstTransaction.getType()).isEqualTo("DEPOSIT");
                assertThat(firstTransaction.getAmount()).isEqualTo(new BigDecimal("1000.00"));
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should cache query results for cacheable queries")
    void shouldCacheQueryResultsForCacheableQueries() {
        // Given: A cacheable query
        GetAccountBalanceQuery query = new GetAccountBalanceQuery("ACC-12345", "CORR-789");
        
        GetAccountBalanceHandler handler = new GetAccountBalanceHandler();
        queryBus.registerHandler(handler);

        // When: The query is executed twice
        Mono<AccountBalance> firstResult = queryBus.query(query);
        Mono<AccountBalance> secondResult = queryBus.query(query);

        // Then: Both should return the same result
        StepVerifier.create(firstResult)
            .assertNext(balance -> assertThat(balance.getAccountNumber()).isEqualTo("ACC-12345"))
            .verifyComplete();

        StepVerifier.create(secondResult)
            .assertNext(balance -> assertThat(balance.getAccountNumber()).isEqualTo("ACC-12345"))
            .verifyComplete();

        // And: Cache should be used (verify cache interaction)
        assertThat(mockCache.get(query.getCacheKey())).isNotNull();
    }

    @Test
    @DisplayName("Should handle query when no handler is registered")
    void shouldHandleQueryWhenNoHandlerRegistered() {
        // Given: A query with no registered handler
        GetAccountBalanceQuery query = new GetAccountBalanceQuery("ACC-99999", null);

        // When: The query is sent to the bus
        Mono<AccountBalance> result = queryBus.query(query);

        // Then: Should throw QueryHandlerNotFoundException
        StepVerifier.create(result)
            .expectError(QueryHandlerNotFoundException.class)
            .verify();
    }

    @Test
    @DisplayName("Should support handler registration and unregistration")
    void shouldSupportHandlerRegistrationAndUnregistration() {
        // Given: A query handler
        GetAccountBalanceHandler handler = new GetAccountBalanceHandler();

        // When: Handler is registered
        queryBus.registerHandler(handler);

        // Then: Handler should be available
        assertThat(queryBus.hasHandler(GetAccountBalanceQuery.class)).isTrue();

        // When: Handler is unregistered
        queryBus.unregisterHandler(GetAccountBalanceQuery.class);

        // Then: Handler should no longer be available
        assertThat(queryBus.hasHandler(GetAccountBalanceQuery.class)).isFalse();
    }

    // Test Query: Get Account Balance
    @Data
    static class GetAccountBalanceQuery implements Query<AccountBalance> {
        private final String accountNumber;
        private final String correlationId;

        @Override
        public String getCacheKey() {
            return "account-balance:" + accountNumber;
        }

        @Override
        public boolean isCacheable() {
            return true;
        }
    }

    // Test Query: Get Transaction History
    @Data
    static class GetTransactionHistoryQuery implements Query<TransactionHistory> {
        private final String accountNumber;
        private final int pageSize;
        private final int pageNumber;
        private final String correlationId;

        @Override
        public String getCacheKey() {
            return "transaction-history:" + accountNumber + ":" + pageSize + ":" + pageNumber;
        }

        @Override
        public boolean isCacheable() {
            return true;
        }
    }

    // Test Result: Account Balance
    @Data
    static class AccountBalance {
        private final String accountNumber;
        private final BigDecimal currentBalance;
        private final BigDecimal availableBalance;
        private final String currency;
        private final LocalDateTime lastUpdated;
    }

    // Test Result: Transaction History
    @Data
    static class TransactionHistory {
        private final String accountNumber;
        private final List<Transaction> transactions;
        private final int totalCount;
        private final int pageSize;
        private final int pageNumber;
    }

    // Test Entity: Transaction
    @Data
    static class Transaction {
        private final String transactionId;
        private final String type;
        private final BigDecimal amount;
        private final String description;
        private final LocalDateTime timestamp;
    }

    // Test Handler: Get Account Balance
    static class GetAccountBalanceHandler implements QueryHandler<GetAccountBalanceQuery, AccountBalance> {
        
        @Override
        public Mono<AccountBalance> handle(GetAccountBalanceQuery query) {
            // Simulate account balance retrieval
            AccountBalance balance = new AccountBalance(
                query.getAccountNumber(),
                new BigDecimal("2750.00"),
                new BigDecimal("2500.00"),
                "USD",
                LocalDateTime.now()
            );
            
            return Mono.just(balance);
        }

        @Override
        public Class<GetAccountBalanceQuery> getQueryType() {
            return GetAccountBalanceQuery.class;
        }

        @Override
        public boolean supportsCaching() {
            return true;
        }

        @Override
        public Long getCacheTtlSeconds() {
            return 300L; // 5 minutes
        }
    }

    // Test Handler: Get Transaction History
    static class GetTransactionHistoryHandler implements QueryHandler<GetTransactionHistoryQuery, TransactionHistory> {
        
        @Override
        public Mono<TransactionHistory> handle(GetTransactionHistoryQuery query) {
            // Simulate transaction history retrieval
            List<Transaction> transactions = List.of(
                new Transaction("TXN-001", "DEPOSIT", new BigDecimal("1000.00"), "Initial deposit", LocalDateTime.now().minusDays(5)),
                new Transaction("TXN-002", "WITHDRAWAL", new BigDecimal("250.00"), "ATM withdrawal", LocalDateTime.now().minusDays(3)),
                new Transaction("TXN-003", "TRANSFER", new BigDecimal("500.00"), "Transfer to savings", LocalDateTime.now().minusDays(1))
            );
            
            TransactionHistory history = new TransactionHistory(
                query.getAccountNumber(),
                transactions,
                transactions.size(),
                query.getPageSize(),
                query.getPageNumber()
            );
            
            return Mono.just(history);
        }

        @Override
        public Class<GetTransactionHistoryQuery> getQueryType() {
            return GetTransactionHistoryQuery.class;
        }

        @Override
        public boolean supportsCaching() {
            return true;
        }

        @Override
        public Long getCacheTtlSeconds() {
            return 60L; // 1 minute
        }
    }
}
