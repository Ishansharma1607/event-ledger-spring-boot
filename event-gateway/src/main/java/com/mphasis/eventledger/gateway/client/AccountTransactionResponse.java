package com.mphasis.eventledger.gateway.client;

import java.math.BigDecimal;

public record AccountTransactionResponse(
        boolean created,
        String accountId,
        BigDecimal balance,
        String currency,
        String traceId
) {
}
