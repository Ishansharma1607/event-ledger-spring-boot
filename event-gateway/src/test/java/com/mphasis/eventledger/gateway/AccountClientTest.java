package com.mphasis.eventledger.gateway;

import com.mphasis.eventledger.gateway.client.AccountClient;
import com.mphasis.eventledger.gateway.client.AccountClientException;
import com.mphasis.eventledger.gateway.client.AccountTransactionRequest;
import com.mphasis.eventledger.gateway.domain.TransactionType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "resilience4j.retry.instances.account-service.max-attempts=3",
        "resilience4j.retry.instances.account-service.wait-duration=10ms",
        "resilience4j.circuitbreaker.instances.account-service.sliding-window-size=10",
        "resilience4j.circuitbreaker.instances.account-service.minimum-number-of-calls=10"
})
class AccountClientTest {

    private static MockWebServer accountService;

    @Autowired
    private AccountClient client;

    @BeforeAll
    static void startServer() throws Exception {
        accountService = new MockWebServer();
        accountService.start();
    }

    @AfterAll
    static void stopServer() throws Exception {
        accountService.shutdown();
    }

    @DynamicPropertySource
    static void accountClientProperties(DynamicPropertyRegistry registry) {
        registry.add("account-service.base-url", () -> accountService.url("/").toString());
    }

    @BeforeEach
    void clearRecordedRequests() throws Exception {
        while (accountService.takeRequest(10, TimeUnit.MILLISECONDS) != null) {
            // Drain requests from earlier tests because MockWebServer is shared for dynamic properties.
        }
    }

    @Test
    void sendsTraceIdAndTransactionJson() throws Exception {
        accountService.enqueue(successResponse());

        var response = client.applyTransaction(request("evt-001"), "trace-client-1");

        var recorded = accountService.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getHeader("X-Trace-ID")).isEqualTo("trace-client-1");
        assertThat(recorded.getPath()).isEqualTo("/accounts/acct-123/transactions");
        assertThat(recorded.getBody().readUtf8())
                .contains("\"eventId\":\"evt-001\"")
                .contains("\"accountId\":\"acct-123\"")
                .contains("\"type\":\"CREDIT\"");
        assertThat(response.balance()).isEqualByComparingTo("150.00");
    }

    @Test
    void retriesTransientServerErrors() {
        var requestCountBefore = accountService.getRequestCount();
        accountService.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":{\"code\":\"DOWN\"}}"));
        accountService.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":{\"code\":\"DOWN\"}}"));
        accountService.enqueue(successResponse());

        var response = client.applyTransaction(request("evt-retry"), "trace-client-2");

        assertThat(response.balance()).isEqualByComparingTo("150.00");
        assertThat(accountService.getRequestCount() - requestCountBefore).isEqualTo(3);
    }

    @Test
    void downstreamFailureBecomesAccountClientException() {
        accountService.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":{\"code\":\"DOWN\"}}"));
        accountService.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":{\"code\":\"DOWN\"}}"));
        accountService.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":{\"code\":\"DOWN\"}}"));

        assertThatThrownBy(() -> client.applyTransaction(request("evt-down"), "trace-client-3"))
                .isInstanceOf(AccountClientException.class)
                .hasMessageContaining("Account Service");
    }

    private AccountTransactionRequest request(String eventId) {
        return new AccountTransactionRequest(
                eventId,
                "acct-123",
                TransactionType.CREDIT,
                new BigDecimal("150.00"),
                "USD",
                Instant.parse("2026-05-15T14:02:11Z")
        );
    }

    private MockResponse successResponse() {
        return new MockResponse()
                .setResponseCode(201)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "created": true,
                          "accountId": "acct-123",
                          "balance": 150.00,
                          "currency": "USD",
                          "traceId": "trace-client"
                        }
                        """);
    }
}
