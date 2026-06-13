package com.mphasis.eventledger.account.api;

import com.mphasis.eventledger.account.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record ApplyTransactionRequest(
        @NotBlank String eventId,
        @NotBlank String accountId,
        @NotNull TransactionType type,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull Instant eventTimestamp
) {
}
