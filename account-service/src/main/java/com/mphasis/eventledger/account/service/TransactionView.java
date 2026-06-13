package com.mphasis.eventledger.account.service;

import com.mphasis.eventledger.account.domain.AccountTransactionEntity;
import com.mphasis.eventledger.account.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionView(
        String eventId,
        String accountId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        Instant eventTimestamp,
        Instant createdAt,
        String traceId
) {

    public static TransactionView from(AccountTransactionEntity transaction) {
        return new TransactionView(
                transaction.getEventId(),
                transaction.getAccountId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getEventTimestamp(),
                transaction.getCreatedAt(),
                transaction.getTraceId()
        );
    }
}
