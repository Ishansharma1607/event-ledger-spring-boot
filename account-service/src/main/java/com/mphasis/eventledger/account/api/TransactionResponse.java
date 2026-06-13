package com.mphasis.eventledger.account.api;

import com.mphasis.eventledger.account.domain.TransactionType;
import com.mphasis.eventledger.account.service.TransactionView;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        String eventId,
        String accountId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        Instant eventTimestamp,
        Instant createdAt,
        String traceId
) {

    public static TransactionResponse from(TransactionView transaction) {
        return new TransactionResponse(
                transaction.eventId(),
                transaction.accountId(),
                transaction.type(),
                transaction.amount(),
                transaction.currency(),
                transaction.eventTimestamp(),
                transaction.createdAt(),
                transaction.traceId()
        );
    }
}
