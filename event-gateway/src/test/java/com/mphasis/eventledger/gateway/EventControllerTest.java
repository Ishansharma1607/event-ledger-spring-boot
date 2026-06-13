package com.mphasis.eventledger.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mphasis.eventledger.gateway.api.EventRequest;
import com.mphasis.eventledger.gateway.client.AccountClient;
import com.mphasis.eventledger.gateway.client.AccountClientException;
import com.mphasis.eventledger.gateway.client.AccountTransactionRequest;
import com.mphasis.eventledger.gateway.client.AccountTransactionResponse;
import com.mphasis.eventledger.gateway.domain.TransactionType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountClient accountClient;

    @Test
    void postEventsReturnsCreatedAndStoresAfterAccountServiceSuccess() throws Exception {
        when(accountClient.applyTransaction(any(AccountTransactionRequest.class), eq("trace-gw-1")))
                .thenReturn(accountResponse("acct-123", "150.00", "trace-gw-1"));

        mockMvc.perform(post("/events")
                        .header("X-Trace-ID", "trace-gw-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("evt-gw-001", "acct-123", "150.00",
                                "2026-05-15T14:02:11Z"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Trace-ID", "trace-gw-1"))
                .andExpect(jsonPath("$.eventId").value("evt-gw-001"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.metadata.source").value("mainframe-batch"));

        var captor = ArgumentCaptor.forClass(AccountTransactionRequest.class);
        verify(accountClient).applyTransaction(captor.capture(), eq("trace-gw-1"));
        assertThat(captor.getValue().eventId()).isEqualTo("evt-gw-001");
        assertThat(captor.getValue().accountId()).isEqualTo("acct-123");
    }

    @Test
    void duplicateEventIdReturnsOkAndDoesNotCallAccountServiceTwice() throws Exception {
        when(accountClient.applyTransaction(any(AccountTransactionRequest.class), eq("trace-gw-2")))
                .thenReturn(accountResponse("acct-dup", "75.00", "trace-gw-2"));
        var payload = objectMapper.writeValueAsString(request("evt-gw-dup", "acct-dup", "75.00",
                "2026-05-15T14:02:11Z"));

        mockMvc.perform(post("/events")
                        .header("X-Trace-ID", "trace-gw-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/events")
                        .header("X-Trace-ID", "trace-gw-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-gw-dup"));

        verify(accountClient, times(1)).applyTransaction(any(AccountTransactionRequest.class), eq("trace-gw-2"));
    }

    @Test
    void getEventByIdReturnsStoredEvent() throws Exception {
        when(accountClient.applyTransaction(any(AccountTransactionRequest.class), eq("trace-gw-3")))
                .thenReturn(accountResponse("acct-read", "10.00", "trace-gw-3"));
        mockMvc.perform(post("/events")
                        .header("X-Trace-ID", "trace-gw-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("evt-gw-read", "acct-read", "10.00",
                                "2026-05-15T14:02:11Z"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/events/evt-gw-read")
                        .header("X-Trace-ID", "trace-gw-3"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-ID", "trace-gw-3"))
                .andExpect(jsonPath("$.eventId").value("evt-gw-read"));
    }

    @Test
    void listEventsForAccountReturnsChronologicalEvents() throws Exception {
        when(accountClient.applyTransaction(any(AccountTransactionRequest.class), eq("trace-gw-4")))
                .thenReturn(accountResponse("acct-order", "30.00", "trace-gw-4"));
        mockMvc.perform(post("/events")
                        .header("X-Trace-ID", "trace-gw-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("evt-gw-late", "acct-order", "20.00",
                                "2026-05-15T15:02:11Z"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/events")
                        .header("X-Trace-ID", "trace-gw-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("evt-gw-early", "acct-order", "10.00",
                                "2026-05-15T14:02:11Z"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/events").param("account", "acct-order")
                        .header("X-Trace-ID", "trace-gw-4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("evt-gw-early"))
                .andExpect(jsonPath("$[1].eventId").value("evt-gw-late"));
    }

    @Test
    void accountServiceOutageReturnsServiceUnavailableAndLocalReadsStillWork() throws Exception {
        when(accountClient.applyTransaction(any(AccountTransactionRequest.class), eq("trace-gw-5")))
                .thenReturn(accountResponse("acct-degraded", "5.00", "trace-gw-5"));
        mockMvc.perform(post("/events")
                        .header("X-Trace-ID", "trace-gw-5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("evt-gw-ok", "acct-degraded", "5.00",
                                "2026-05-15T14:02:11Z"))))
                .andExpect(status().isCreated());

        when(accountClient.applyTransaction(any(AccountTransactionRequest.class), eq("trace-gw-down")))
                .thenThrow(new AccountClientException("Account Service is unreachable"));

        mockMvc.perform(post("/events")
                        .header("X-Trace-ID", "trace-gw-down")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("evt-gw-down", "acct-degraded", "5.00",
                                "2026-05-15T16:02:11Z"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("X-Trace-ID", "trace-gw-down"))
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_SERVICE_UNAVAILABLE"));

        mockMvc.perform(get("/events/evt-gw-ok")
                        .header("X-Trace-ID", "trace-gw-down"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-gw-ok"));
    }

    @Test
    void openApiDocsDescribeGatewayEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Event Gateway API"))
                .andExpect(jsonPath("$.paths['/events']").exists())
                .andExpect(jsonPath("$.paths['/events/{id}']").exists());
    }

    private EventRequest request(String eventId, String accountId, String amount, String timestamp) {
        return new EventRequest(
                eventId,
                accountId,
                TransactionType.CREDIT,
                new BigDecimal(amount),
                "USD",
                Instant.parse(timestamp),
                Map.of("source", "mainframe-batch", "batchId", "B-9042")
        );
    }

    private AccountTransactionResponse accountResponse(String accountId, String balance, String traceId) {
        return new AccountTransactionResponse(true, accountId, new BigDecimal(balance), "USD", traceId);
    }
}
