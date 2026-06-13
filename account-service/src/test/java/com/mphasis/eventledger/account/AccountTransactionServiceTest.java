package com.mphasis.eventledger.account;

import com.mphasis.eventledger.account.domain.TransactionType;
import com.mphasis.eventledger.account.observability.AccountMetrics;
import com.mphasis.eventledger.account.service.AccountIdMismatchException;
import com.mphasis.eventledger.account.service.AccountTransactionService;
import com.mphasis.eventledger.account.service.ApplyTransactionCommand;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({AccountTransactionService.class, AccountMetrics.class, AccountTransactionServiceTest.MetricsTestConfig.class})
class AccountTransactionServiceTest {

    @Autowired
    private AccountTransactionService service;

    @Test
    void creditCreatesAccountBalance() {
        var result = service.apply("acct-123", command("evt-001", TransactionType.CREDIT, "150.00",
                "2026-05-15T14:02:11Z"));

        assertThat(result.created()).isTrue();
        assertThat(result.balance()).isEqualByComparingTo("150.00");
        assertThat(result.transaction().eventId()).isEqualTo("evt-001");
    }

    @Test
    void debitReducesExistingBalance() {
        service.apply("acct-123", command("evt-001", TransactionType.CREDIT, "150.00",
                "2026-05-15T14:02:11Z"));

        var result = service.apply("acct-123", command("evt-002", TransactionType.DEBIT, "40.00",
                "2026-05-15T15:02:11Z"));

        assertThat(result.created()).isTrue();
        assertThat(result.balance()).isEqualByComparingTo("110.00");
    }

    @Test
    void duplicateEventIdDoesNotDoubleApplyTransaction() {
        var first = service.apply("acct-123", command("evt-001", TransactionType.CREDIT, "150.00",
                "2026-05-15T14:02:11Z"));
        var duplicate = service.apply("acct-123", command("evt-001", TransactionType.CREDIT, "150.00",
                "2026-05-15T14:02:11Z"));

        assertThat(first.created()).isTrue();
        assertThat(duplicate.created()).isFalse();
        assertThat(duplicate.balance()).isEqualByComparingTo("150.00");
    }

    @Test
    void accountIdMismatchIsRejected() {
        var command = new ApplyTransactionCommand(
                "evt-001",
                "acct-other",
                TransactionType.CREDIT,
                new BigDecimal("150.00"),
                "USD",
                Instant.parse("2026-05-15T14:02:11Z"),
                "trace-abc"
        );

        assertThatThrownBy(() -> service.apply("acct-123", command))
                .isInstanceOf(AccountIdMismatchException.class)
                .hasMessageContaining("acct-123")
                .hasMessageContaining("acct-other");
    }

    @Test
    void accountDetailsReturnTransactionsInChronologicalOrder() {
        service.apply("acct-123", command("evt-002", TransactionType.DEBIT, "40.00",
                "2026-05-15T15:02:11Z"));
        service.apply("acct-123", command("evt-001", TransactionType.CREDIT, "150.00",
                "2026-05-15T14:02:11Z"));

        var account = service.getAccount("acct-123");

        assertThat(account.balance()).isEqualByComparingTo("110.00");
        assertThat(account.transactions())
                .extracting(transaction -> transaction.eventId())
                .containsExactly("evt-001", "evt-002");
    }

    private ApplyTransactionCommand command(String eventId, TransactionType type, String amount, String timestamp) {
        return new ApplyTransactionCommand(
                eventId,
                "acct-123",
                type,
                new BigDecimal(amount),
                "USD",
                Instant.parse(timestamp),
                "trace-abc"
        );
    }

    @TestConfiguration
    static class MetricsTestConfig {

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
