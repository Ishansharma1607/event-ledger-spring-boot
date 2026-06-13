package com.mphasis.eventledger.account.service;

import com.mphasis.eventledger.account.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record ApplyTransactionCommand(
        String eventId,
        String accountId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        Instant eventTimestamp,
        String traceId
) {
}
