package com.mphasis.eventledger.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "account-service")
public record AccountServiceProperties(
        String baseUrl,
        int connectTimeoutMillis,
        int readTimeoutMillis
) {

    public AccountServiceProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8081";
        }
        if (connectTimeoutMillis <= 0) {
            connectTimeoutMillis = 1000;
        }
        if (readTimeoutMillis <= 0) {
            readTimeoutMillis = 2000;
        }
    }
}
