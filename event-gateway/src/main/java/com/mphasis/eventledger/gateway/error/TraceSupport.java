package com.mphasis.eventledger.gateway.error;

import java.util.Optional;
import java.util.UUID;

public final class TraceSupport {

    private TraceSupport() {
    }

    public static String responseTraceId(String traceIdHeader) {
        return Optional.ofNullable(traceIdHeader)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
    }
}
