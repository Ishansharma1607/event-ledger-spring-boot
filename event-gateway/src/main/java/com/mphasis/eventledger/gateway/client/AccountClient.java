package com.mphasis.eventledger.gateway.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AccountClient {

    private final RestClient restClient;

    public AccountClient(RestClient accountServiceRestClient) {
        this.restClient = accountServiceRestClient;
    }

    @Retry(name = "account-service")
    @CircuitBreaker(name = "account-service")
    public AccountTransactionResponse applyTransaction(AccountTransactionRequest request, String traceId) {
        try {
            return restClient.post()
                    .uri("/accounts/{accountId}/transactions", request.accountId())
                    .header("X-Trace-ID", traceId)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                        throw new AccountClientException("Account Service returned HTTP " + response.getStatusCode().value());
                    })
                    .body(AccountTransactionResponse.class);
        } catch (AccountClientException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new AccountClientException("Account Service is unreachable", ex);
        }
    }
}
