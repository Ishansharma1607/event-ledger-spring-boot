package com.mphasis.eventledger.gateway.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mphasis.eventledger.gateway.domain.EventEntity;
import com.mphasis.eventledger.gateway.domain.EventStatus;
import com.mphasis.eventledger.gateway.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record EventResponse(
        String eventId,
        String accountId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        Instant eventTimestamp,
        JsonNode metadata,
        EventStatus status,
        Instant createdAt,
        String traceId
) {

    public static EventResponse from(EventEntity entity, ObjectMapper objectMapper) {
        return new EventResponse(
                entity.getEventId(),
                entity.getAccountId(),
                entity.getType(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getEventTimestamp(),
                readMetadata(entity, objectMapper),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getTraceId()
        );
    }

    private static JsonNode readMetadata(EventEntity entity, ObjectMapper objectMapper) {
        try {
            return objectMapper.readTree(entity.getMetadataJson());
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }
}
