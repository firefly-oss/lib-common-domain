package com.firefly.common.domain.cqrs.command;

import com.firefly.common.domain.cqrs.annotations.CommandHandler;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Test Command Handler: Create Account
 * 
 * <p>This demonstrates the consolidated CQRS approach:
 * <ul>
 *   <li>@CommandHandler annotation for configuration and Spring registration</li>
 *   <li>Extends CommandHandler&lt;Command, Result&gt; for automatic type detection</li>
 *   <li>Only implement doHandle() - everything else is automatic</li>
 *   <li>Built-in validation, logging, metrics, error handling</li>
 * </ul>
 */
@CommandHandler(timeout = 30000, retries = 3, metrics = true)
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountCreatedResult> {

    @Override
    protected Mono<AccountCreatedResult> doHandle(CreateAccountCommand command) {
        // Only business logic - validation, logging, metrics handled automatically!
        String accountNumber = "ACC-" + System.currentTimeMillis();
        AccountCreatedResult result = new AccountCreatedResult(
            accountNumber,
            command.getCustomerId(),
            command.getAccountType(),
            command.getInitialBalance(),
            "ACTIVE"
        );
        return Mono.just(result);
    }

    // No getCommandType() needed - automatically detected from generics!
}
