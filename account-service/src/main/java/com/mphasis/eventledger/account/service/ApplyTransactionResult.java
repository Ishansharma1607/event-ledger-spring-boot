package com.mphasis.eventledger.account.service;

import java.math.BigDecimal;

public record ApplyTransactionResult(
        boolean created,
        String accountId,
        BigDecimal balance,
        String currency,
        TransactionView transaction
) {
}
