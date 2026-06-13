package com.mphasis.eventledger.gateway;

import com.mphasis.eventledger.gateway.api.EventRequest;
import com.mphasis.eventledger.gateway.domain.TransactionType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "resilience4j.retry.instances.account-service.max-attempts=1",
        "resilience4j.circuitbreaker.instances.account-service.minimum-number-of-calls=10"
})
class TracePropagationIntegrationTest {

    private static MockWebServer accountService;

    @Autowired
    private TestRestTemplate restTemplate;

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

    @Test
    void generatedTraceIdIsReturnedAndPropagatedToAccountService() throws Exception {
        accountService.enqueue(new MockResponse()
                .setResponseCode(201)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "created": true,
                          "accountId": "acct-trace",
                          "balance": 42.00,
                          "currency": "USD",
                          "traceId": "ignored"
                        }
                        """));
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var response = restTemplate.postForEntity("/events", new HttpEntity<>(request(), headers), String.class);

        var responseTraceId = response.getHeaders().getFirst("X-Trace-ID");
        var recorded = accountService.takeRequest(1, TimeUnit.SECONDS);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(responseTraceId).isNotBlank();
        assertThat(recorded).isNotNull();
        assertThat(recorded.getHeader("X-Trace-ID")).isEqualTo(responseTraceId);
    }

    @Test
    void healthEndpointGetsTraceHeaderFromFilter() {
        var response = restTemplate.getForEntity("/health", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst("X-Trace-ID")).isNotBlank();
    }

    private EventRequest request() {
        return new EventRequest(
                "evt-trace-integration",
                "acct-trace",
                TransactionType.CREDIT,
                new BigDecimal("42.00"),
                "USD",
                Instant.parse("2026-05-15T14:02:11Z"),
                Map.of("source", "trace-test")
        );
    }
}
