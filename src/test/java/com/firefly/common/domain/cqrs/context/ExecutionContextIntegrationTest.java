package com.firefly.common.domain.cqrs.context;

import com.firefly.common.domain.cqrs.command.CommandBus;
import com.firefly.common.domain.cqrs.command.DefaultCommandBus;
import com.firefly.common.domain.cqrs.query.DefaultQueryBus;
import com.firefly.common.domain.cqrs.query.QueryBus;
import com.firefly.common.domain.tracing.CorrelationContext;
import com.firefly.common.domain.validation.AutoValidationProcessor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Integration test demonstrating ExecutionContext usage with CQRS framework.
 */
class ExecutionContextIntegrationTest {

    private CommandBus commandBus;
    private QueryBus queryBus;

    @BeforeEach
    void setUp() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        CorrelationContext correlationContext = new CorrelationContext();
        AutoValidationProcessor validationProcessor = new AutoValidationProcessor(null);
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        
        // Set up command bus (simplified for testing)
        commandBus = mock(DefaultCommandBus.class);
        
        // Set up query bus
        queryBus = new DefaultQueryBus(applicationContext, correlationContext, validationProcessor, cacheManager, meterRegistry);
        
        // Register handlers manually for testing
        ((DefaultQueryBus) queryBus).registerHandler(new GetTenantAccountBalanceHandler());
    }

    @Test
    void testCommandWithExecutionContext() {
        // Given
        CreateTenantAccountCommand command = new CreateTenantAccountCommand(
            "CUST-123", 
            "SAVINGS", 
            new BigDecimal("1000.00")
        );
        
        ExecutionContext context = ExecutionContext.builder()
            .withUserId("user-456")
            .withTenantId("premium-tenant")
            .withSource("mobile-app")
            .withFeatureFlag("premium-features", true)
            .withFeatureFlag("auto-approve", true)
            .withProperty("priority", "HIGH")
            .build();
        
        // Create handler for testing
        CreateTenantAccountHandler handler = new CreateTenantAccountHandler();
        
        // When & Then
        StepVerifier.create(handler.doHandle(command, context))
            .expectNextMatches(result -> {
                assertThat(result.getAccountNumber()).startsWith("premium-tenant-SAVINGS-");
                assertThat(result.getCustomerId()).isEqualTo("CUST-123");
                assertThat(result.getTenantId()).isEqualTo("premium-tenant");
                assertThat(result.getAccountType()).isEqualTo("SAVINGS");
                assertThat(result.getBalance()).isEqualTo(new BigDecimal("1100.00")); // $100 bonus
                assertThat(result.getStatus()).isEqualTo("ACTIVE"); // auto-approved
                assertThat(result.isPremiumFeatures()).isTrue();
                return true;
            })
            .verifyComplete();
    }

    @Test
    void testQueryWithExecutionContext() {
        // Given
        GetTenantAccountBalanceQuery query = new GetTenantAccountBalanceQuery("ACC-123", "CUST-456");
        
        ExecutionContext context = ExecutionContext.builder()
            .withUserId("user-789")
            .withTenantId("premium-tenant")
            .withSource("mobile-app")
            .withFeatureFlag("enhanced-view", true)
            .withFeatureFlag("premium-features", true)
            .build();
        
        // When & Then
        StepVerifier.create(queryBus.query(query, context))
            .expectNextMatches(result -> {
                TenantAccountBalance balance = (TenantAccountBalance) result;
                assertThat(balance.getAccountNumber()).isEqualTo("ACC-123");
                assertThat(balance.getTenantId()).isEqualTo("premium-tenant");
                assertThat(balance.getCurrentBalance()).isEqualTo(new BigDecimal("5000.00"));
                assertThat(balance.getAvailableBalance()).isEqualTo(new BigDecimal("6000.00")); // 20% overdraft
                assertThat(balance.getCurrency()).isEqualTo("USD");
                assertThat(balance.isEnhancedView()).isTrue();
                assertThat(balance.getAdditionalInfo()).contains(
                    "Enhanced view enabled",
                    "Last accessed by: user-789",
                    "Access source: mobile-app",
                    "Tenant: premium-tenant",
                    "Mobile optimized data"
                );
                return true;
            })
            .verifyComplete();
    }

    @Test
    void testContextAwareHandlerWithoutContext() {
        // Given
        CreateTenantAccountCommand command = new CreateTenantAccountCommand(
            "CUST-123", 
            "SAVINGS", 
            new BigDecimal("1000.00")
        );
        
        CreateTenantAccountHandler handler = new CreateTenantAccountHandler();
        
        // When & Then - should throw exception when called without context
        StepVerifier.create(handler.doHandle(command))
            .expectError(UnsupportedOperationException.class)
            .verify();
    }

    @Test
    void testContextValidation() {
        // Given
        CreateTenantAccountCommand command = new CreateTenantAccountCommand(
            "CUST-123", 
            "SAVINGS", 
            new BigDecimal("1000.00")
        );
        
        // Context without required tenant ID
        ExecutionContext context = ExecutionContext.builder()
            .withUserId("user-456")
            .withSource("mobile-app")
            .build();
        
        CreateTenantAccountHandler handler = new CreateTenantAccountHandler();
        
        // When & Then - should fail validation
        StepVerifier.create(handler.doHandle(command, context))
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    void testFeatureFlagBehavior() {
        // Given
        CreateTenantAccountCommand command = new CreateTenantAccountCommand(
            "CUST-123", 
            "SAVINGS", 
            new BigDecimal("15000.00") // High amount
        );
        
        // Context without auto-approve flag
        ExecutionContext context = ExecutionContext.builder()
            .withUserId("user-456")
            .withTenantId("basic-tenant")
            .withSource("web-app")
            .withFeatureFlag("premium-features", false)
            .build();
        
        CreateTenantAccountHandler handler = new CreateTenantAccountHandler();
        
        // When & Then
        StepVerifier.create(handler.doHandle(command, context))
            .expectNextMatches(result -> {
                assertThat(result.getStatus()).isEqualTo("PENDING_APPROVAL"); // High amount, no auto-approve
                assertThat(result.getBalance()).isEqualTo(new BigDecimal("15000.00")); // No bonus
                assertThat(result.isPremiumFeatures()).isFalse();
                return true;
            })
            .verifyComplete();
    }
}
