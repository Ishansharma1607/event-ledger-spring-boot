package com.mphasis.eventledger.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mphasis.eventledger.account.api.ApplyTransactionRequest;
import com.mphasis.eventledger.account.domain.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void postTransactionReturnsCreatedForNewTransaction() throws Exception {
        mockMvc.perform(post("/accounts/acct-123/transactions")
                        .header("X-Trace-ID", "trace-api-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("evt-001", "acct-123", TransactionType.CREDIT, "150.00"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Trace-ID", "trace-api-1"))
                .andExpect(jsonPath("$.accountId").value("acct-123"))
                .andExpect(jsonPath("$.balance").value(150.00))
                .andExpect(jsonPath("$.created").value(true))
                .andExpect(jsonPath("$.transaction.eventId").value("evt-001"))
                .andExpect(jsonPath("$.traceId").value("trace-api-1"));
    }

    @Test
    void duplicateTransactionReturnsOkAndDoesNotDoubleApply() throws Exception {
        var payload = objectMapper.writeValueAsString(request("evt-dup", "acct-456", TransactionType.CREDIT, "75.00"));

        mockMvc.perform(post("/accounts/acct-456/transactions")
                        .header("X-Trace-ID", "trace-api-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value(75.00))
                .andExpect(jsonPath("$.created").value(true));

        mockMvc.perform(post("/accounts/acct-456/transactions")
                        .header("X-Trace-ID", "trace-api-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(75.00))
                .andExpect(jsonPath("$.created").value(false));
    }

    @Test
    void balanceEndpointReturnsCurrentBalance() throws Exception {
        mockMvc.perform(post("/accounts/acct-balance/transactions")
                        .header("X-Trace-ID", "trace-api-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("evt-bal-1", "acct-balance", TransactionType.CREDIT, "200.00"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/accounts/acct-balance/transactions")
                        .header("X-Trace-ID", "trace-api-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("evt-bal-2", "acct-balance", TransactionType.DEBIT, "30.00"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/acct-balance/balance")
                        .header("X-Trace-ID", "trace-api-3"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-ID", "trace-api-3"))
                .andExpect(jsonPath("$.accountId").value("acct-balance"))
                .andExpect(jsonPath("$.balance").value(170.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.traceId").value("trace-api-3"));
    }

    @Test
    void invalidTransactionAmountReturnsValidationError() throws Exception {
        mockMvc.perform(post("/accounts/acct-invalid/transactions")
                        .header("X-Trace-ID", "trace-api-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("evt-invalid", "acct-invalid", TransactionType.CREDIT, "0.00"))))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Trace-ID", "trace-api-4"))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").value("trace-api-4"));
    }

    @Test
    void accountDetailsReturnsRecentTransactionsChronologically() throws Exception {
        mockMvc.perform(post("/accounts/acct-order/transactions")
                        .header("X-Trace-ID", "trace-api-5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(
                                "evt-late",
                                "acct-order",
                                TransactionType.CREDIT,
                                "20.00",
                                "2026-05-15T15:02:11Z"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/accounts/acct-order/transactions")
                        .header("X-Trace-ID", "trace-api-5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(
                                "evt-early",
                                "acct-order",
                                TransactionType.CREDIT,
                                "10.00",
                                "2026-05-15T14:02:11Z"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/acct-order")
                        .header("X-Trace-ID", "trace-api-5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions[0].eventId").value("evt-early"))
                .andExpect(jsonPath("$.transactions[1].eventId").value("evt-late"));
    }

    private ApplyTransactionRequest request(String eventId, String accountId, TransactionType type, String amount) {
        return request(eventId, accountId, type, amount, "2026-05-15T14:02:11Z");
    }

    private ApplyTransactionRequest request(
            String eventId,
            String accountId,
            TransactionType type,
            String amount,
            String timestamp
    ) {
        return new ApplyTransactionRequest(
                eventId,
                accountId,
                type,
                new BigDecimal(amount),
                "USD",
                Instant.parse(timestamp)
        );
    }
}
