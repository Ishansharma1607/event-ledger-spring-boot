package com.mphasis.eventledger.account.api;

import com.mphasis.eventledger.account.service.AccountDetails;

import java.math.BigDecimal;

public record BalanceResponse(
        String accountId,
        BigDecimal balance,
        String currency,
        String traceId
) {

    public static BalanceResponse from(AccountDetails account, String traceId) {
        return new BalanceResponse(account.accountId(), account.balance(), account.currency(), traceId);
    }
}
