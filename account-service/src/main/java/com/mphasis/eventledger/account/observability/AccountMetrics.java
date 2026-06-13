package com.mphasis.eventledger.account.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AccountMetrics {

    private final Counter transactionsApplied;
    private final Counter duplicateTransactions;

    public AccountMetrics(MeterRegistry registry) {
        this.transactionsApplied = Counter.builder("account_service_transactions_applied_total")
                .description("Number of new account transactions applied")
                .register(registry);
        this.duplicateTransactions = Counter.builder("account_service_transactions_duplicate_total")
                .description("Number of duplicate account transactions skipped idempotently")
                .register(registry);
    }

    public void transactionApplied() {
        transactionsApplied.increment();
    }

    public void duplicateTransaction() {
        duplicateTransactions.increment();
    }
}
