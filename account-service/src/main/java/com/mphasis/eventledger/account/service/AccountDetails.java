package com.mphasis.eventledger.account.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AccountDetails(
        String accountId,
        BigDecimal balance,
        String currency,
        Instant updatedAt,
        List<TransactionView> transactions
) {
}
