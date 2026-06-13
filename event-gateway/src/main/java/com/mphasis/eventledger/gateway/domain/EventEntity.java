package com.mphasis.eventledger.gateway.domain;

import com.mphasis.eventledger.gateway.api.EventRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "events")
public class EventEntity {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private String eventId;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Lob
    @Column(name = "metadata_json", nullable = false)
    private String metadataJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "trace_id", nullable = false)
    private String traceId;

    protected EventEntity() {
    }

    private EventEntity(
            String eventId,
            String accountId,
            TransactionType type,
            BigDecimal amount,
            String currency,
            Instant eventTimestamp,
            String metadataJson,
            EventStatus status,
            String traceId
    ) {
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.accountId = Objects.requireNonNull(accountId, "accountId");
        this.type = Objects.requireNonNull(type, "type");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.eventTimestamp = Objects.requireNonNull(eventTimestamp, "eventTimestamp");
        this.metadataJson = Objects.requireNonNull(metadataJson, "metadataJson");
        this.status = Objects.requireNonNull(status, "status");
        this.traceId = Objects.requireNonNull(traceId, "traceId");
        this.createdAt = Instant.now();
    }

    public static EventEntity accepted(EventRequest request, String metadataJson, String traceId) {
        return new EventEntity(
                request.eventId(),
                request.accountId(),
                request.type(),
                request.amount(),
                request.currency(),
                request.eventTimestamp(),
                metadataJson,
                EventStatus.ACCEPTED,
                traceId
        );
    }

    public String getEventId() {
        return eventId;
    }

    public String getAccountId() {
        return accountId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getTraceId() {
        return traceId;
    }
}
