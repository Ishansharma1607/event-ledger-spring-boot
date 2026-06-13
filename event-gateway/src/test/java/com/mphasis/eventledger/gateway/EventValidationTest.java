package com.mphasis.eventledger.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mphasis.eventledger.gateway.api.EventRequest;
import com.mphasis.eventledger.gateway.domain.EventEntity;
import com.mphasis.eventledger.gateway.domain.EventStatus;
import com.mphasis.eventledger.gateway.domain.TransactionType;
import com.mphasis.eventledger.gateway.repository.EventRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class EventValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private EventRepository repository;

    @Test
    void validCreditRequestMapsToAcceptedEntity() {
        var request = validRequest(TransactionType.CREDIT, "150.00");

        var entity = EventEntity.accepted(request, "{\"source\":\"mainframe-batch\"}", "trace-123");

        assertThat(validator.validate(request)).isEmpty();
        assertThat(entity.getEventId()).isEqualTo("evt-001");
        assertThat(entity.getStatus()).isEqualTo(EventStatus.ACCEPTED);
        assertThat(entity.getAmount()).isEqualByComparingTo("150.00");
        assertThat(entity.getMetadataJson()).contains("mainframe-batch");
    }

    @Test
    void zeroAmountIsRejected() {
        var request = validRequest(TransactionType.CREDIT, "0.00");

        assertThat(validator.validate(request))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("amount"));
    }

    @Test
    void missingAccountIdIsRejected() {
        var request = new EventRequest(
                "evt-001",
                "",
                TransactionType.CREDIT,
                new BigDecimal("150.00"),
                "USD",
                Instant.parse("2026-05-15T14:02:11Z"),
                Map.of("source", "mainframe-batch")
        );

        assertThat(validator.validate(request))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("accountId"));
    }

    @Test
    void unknownTransactionTypeIsRejectedByJsonBinding() {
        var json = """
                {
                  "eventId": "evt-001",
                  "accountId": "acct-123",
                  "type": "TRANSFER",
                  "amount": 150.00,
                  "currency": "USD",
                  "eventTimestamp": "2026-05-15T14:02:11Z"
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, EventRequest.class))
                .hasMessageContaining("TRANSFER");
    }

    @Test
    void accountEventsAreListedByEventTimestamp() {
        repository.save(EventEntity.accepted(
                new EventRequest("evt-late", "acct-123", TransactionType.CREDIT, new BigDecimal("20.00"), "USD",
                        Instant.parse("2026-05-15T15:02:11Z"), Map.of()),
                "{}",
                "trace-123"
        ));
        repository.save(EventEntity.accepted(
                new EventRequest("evt-early", "acct-123", TransactionType.CREDIT, new BigDecimal("10.00"), "USD",
                        Instant.parse("2026-05-15T14:02:11Z"), Map.of()),
                "{}",
                "trace-123"
        ));

        assertThat(repository.findByAccountIdOrderByEventTimestampAscEventIdAsc("acct-123"))
                .extracting(EventEntity::getEventId)
                .containsExactly("evt-early", "evt-late");
    }

    private EventRequest validRequest(TransactionType type, String amount) {
        return new EventRequest(
                "evt-001",
                "acct-123",
                type,
                new BigDecimal(amount),
                "USD",
                Instant.parse("2026-05-15T14:02:11Z"),
                Map.of("source", "mainframe-batch")
        );
    }
}
