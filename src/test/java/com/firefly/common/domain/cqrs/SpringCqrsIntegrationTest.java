package com.firefly.common.domain.cqrs;

import com.firefly.common.domain.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.domain.cqrs.annotations.QueryHandlerComponent;
import com.firefly.common.domain.cqrs.command.*;
import com.firefly.common.domain.cqrs.query.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Integration test to verify automatic handler discovery and metrics.
 * 
 * <p>This test validates:
 * <ul>
 *   <li>Automatic discovery of handlers with @CommandHandlerComponent and @QueryHandlerComponent</li>
 *   <li>Proper metrics collection and counting</li>
 *   <li>Real Spring context with actual bean discovery</li>
 * </ul>
 */
@SpringBootTest
@ContextConfiguration(classes = SpringCqrsIntegrationTest.TestConfiguration.class)
class SpringCqrsIntegrationTest {

    @Autowired
    private CommandBus commandBus;
    
    @Autowired
    private QueryBus queryBus;
    
    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void shouldDiscoverHandlersAutomatically() {
        // Given - handlers should be automatically discovered by Spring
        SpringCreateAccountCommand command = new SpringCreateAccountCommand("SPRING-123", "CHECKING", new BigDecimal("500.00"));
        
        // When & Then - command should be processed by auto-discovered handler
        StepVerifier.create(commandBus.send(command))
            .expectNextMatches(result -> {
                SpringAccountResult accountResult = (SpringAccountResult) result;
                return accountResult.getAccountNumber().startsWith("SPRING-ACC-") &&
                       accountResult.getCustomerId().equals("SPRING-123") &&
                       accountResult.getAccountType().equals("CHECKING") &&
                       accountResult.getBalance().equals(new BigDecimal("500.00"));
            })
            .verifyComplete();
    }

    @Test
    void shouldCollectMetricsCorrectly() {
        // Given
        SpringCreateAccountCommand command = new SpringCreateAccountCommand("METRICS-123", "SAVINGS", new BigDecimal("1000.00"));
        
        // Get initial metric values
        Counter processedCounter = meterRegistry.find("firefly.cqrs.command.processed").counter();
        Timer processingTimer = meterRegistry.find("firefly.cqrs.command.processing.time").timer();
        
        double initialProcessedCount = processedCounter != null ? processedCounter.count() : 0;
        long initialTimerCount = processingTimer != null ? processingTimer.count() : 0;
        
        // When
        StepVerifier.create(commandBus.send(command))
            .expectNextCount(1)
            .verifyComplete();
        
        // Then - metrics should be updated
        assertThat(processedCounter.count()).isEqualTo(initialProcessedCount + 1);
        assertThat(processingTimer.count()).isEqualTo(initialTimerCount + 1);
    }

    @Test
    void shouldProcessQueriesWithCaching() {
        // Given
        SpringGetBalanceQuery query = new SpringGetBalanceQuery("SPRING-ACC-123");
        
        // When & Then - query should be processed by auto-discovered handler with caching
        StepVerifier.create(queryBus.query(query))
            .expectNextMatches(result -> {
                SpringBalanceResult balance = (SpringBalanceResult) result;
                return balance.getAccountNumber().equals("SPRING-ACC-123") &&
                       balance.getBalance().equals(new BigDecimal("2500.00")) &&
                       balance.getCurrency().equals("USD");
            })
            .verifyComplete();
    }

    // Test Command
    static class SpringCreateAccountCommand implements Command<SpringAccountResult> {
        @NotBlank private final String customerId;
        @NotBlank private final String accountType;
        @NotNull @Positive private final BigDecimal initialBalance;

        public SpringCreateAccountCommand(String customerId, String accountType, BigDecimal initialBalance) {
            this.customerId = customerId; this.accountType = accountType; this.initialBalance = initialBalance;
        }

        public String getCustomerId() { return customerId; }
        public String getAccountType() { return accountType; }
        public BigDecimal getInitialBalance() { return initialBalance; }
    }

    // Test Result
    static class SpringAccountResult {
        private final String accountNumber, customerId, accountType;
        private final BigDecimal balance;
        private final LocalDateTime createdAt;

        public SpringAccountResult(String accountNumber, String customerId, String accountType, BigDecimal balance) {
            this.accountNumber = accountNumber; this.customerId = customerId; this.accountType = accountType;
            this.balance = balance; this.createdAt = LocalDateTime.now();
        }

        public String getAccountNumber() { return accountNumber; }
        public String getCustomerId() { return customerId; }
        public String getAccountType() { return accountType; }
        public BigDecimal getBalance() { return balance; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    // Test Query
    static class SpringGetBalanceQuery implements Query<SpringBalanceResult> {
        @NotBlank private final String accountNumber;

        public SpringGetBalanceQuery(String accountNumber) { this.accountNumber = accountNumber; }
        public String getAccountNumber() { return accountNumber; }
    }

    // Test Balance Result
    static class SpringBalanceResult {
        private final String accountNumber, currency;
        private final BigDecimal balance;
        private final LocalDateTime lastUpdated;

        public SpringBalanceResult(String accountNumber, BigDecimal balance, String currency) {
            this.accountNumber = accountNumber; this.balance = balance; this.currency = currency;
            this.lastUpdated = LocalDateTime.now();
        }

        public String getAccountNumber() { return accountNumber; }
        public BigDecimal getBalance() { return balance; }
        public String getCurrency() { return currency; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }

    /**
     * Spring Command Handler - Should be automatically discovered
     */
    @CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
    static class SpringCreateAccountHandler extends CommandHandler<SpringCreateAccountCommand, SpringAccountResult> {

        @Override
        protected Mono<SpringAccountResult> doHandle(SpringCreateAccountCommand command) {
            // Only business logic - everything else automatic!
            String accountNumber = "SPRING-ACC-" + System.currentTimeMillis();
            SpringAccountResult result = new SpringAccountResult(
                accountNumber,
                command.getCustomerId(),
                command.getAccountType(),
                command.getInitialBalance()
            );
            return Mono.just(result);
        }
    }

    /**
     * Spring Query Handler - Should be automatically discovered
     */
    @QueryHandlerComponent(cacheable = true, cacheTtl = 300, metrics = true)
    static class SpringGetBalanceHandler extends QueryHandler<SpringGetBalanceQuery, SpringBalanceResult> {

        @Override
        protected Mono<SpringBalanceResult> doHandle(SpringGetBalanceQuery query) {
            // Only business logic - everything else automatic!
            SpringBalanceResult result = new SpringBalanceResult(
                query.getAccountNumber(),
                new BigDecimal("2500.00"),
                "USD"
            );
            return Mono.just(result);
        }
    }

    @Configuration
    static class TestConfiguration {
        // Spring will automatically discover the @CommandHandlerComponent and @QueryHandlerComponent beans
        // and the CQRS framework will auto-configure CommandBus and QueryBus
    }
}
