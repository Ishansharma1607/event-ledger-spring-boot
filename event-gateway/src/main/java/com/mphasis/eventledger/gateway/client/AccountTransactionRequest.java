package com.mphasis.eventledger.gateway.client;

import com.mphasis.eventledger.gateway.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountTransactionRequest(
        String eventId,
        String accountId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        Instant eventTimestamp
) {
}
