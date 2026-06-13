package com.mphasis.eventledger.account.error;

import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;

import static com.mphasis.eventledger.account.observability.TraceFilter.MDC_KEY;

public final class TraceSupport {

    private TraceSupport() {
    }

    public static String responseTraceId(String traceIdHeader) {
        return Optional.ofNullable(MDC.get(MDC_KEY))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .or(() -> Optional.ofNullable(traceIdHeader)
                        .map(String::trim)
                        .filter(value -> !value.isBlank()))
                .orElseGet(() -> UUID.randomUUID().toString());
    }
}
