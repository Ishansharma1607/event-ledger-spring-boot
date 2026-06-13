package com.mphasis.eventledger.account.api;

import com.mphasis.eventledger.account.service.AccountDetails;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AccountResponse(
        String accountId,
        BigDecimal balance,
        String currency,
        Instant updatedAt,
        List<TransactionResponse> transactions,
        String traceId
) {

    public static AccountResponse from(AccountDetails account, String traceId) {
        return new AccountResponse(
                account.accountId(),
                account.balance(),
                account.currency(),
                account.updatedAt(),
                account.transactions().stream().map(TransactionResponse::from).toList(),
                traceId
        );
    }
}
