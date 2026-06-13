package com.mphasis.eventledger.account.error;

import java.time.Instant;

public record ApiErrorResponse(
        ErrorBody error,
        String traceId,
        Instant timestamp
) {

    public static ApiErrorResponse of(String code, String message, String traceId) {
        return new ApiErrorResponse(new ErrorBody(code, message), traceId, Instant.now());
    }

    public record ErrorBody(String code, String message) {
    }
}
