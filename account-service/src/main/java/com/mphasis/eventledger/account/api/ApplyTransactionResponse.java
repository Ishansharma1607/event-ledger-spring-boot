package com.mphasis.eventledger.account.api;

import com.mphasis.eventledger.account.service.ApplyTransactionResult;

import java.math.BigDecimal;

public record ApplyTransactionResponse(
        boolean created,
        String accountId,
        BigDecimal balance,
        String currency,
        TransactionResponse transaction,
        String traceId
) {

    public static ApplyTransactionResponse from(ApplyTransactionResult result, String traceId) {
        return new ApplyTransactionResponse(
                result.created(),
                result.accountId(),
                result.balance(),
                result.currency(),
                TransactionResponse.from(result.transaction()),
                traceId
        );
    }
}
