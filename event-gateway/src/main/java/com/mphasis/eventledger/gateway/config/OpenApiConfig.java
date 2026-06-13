package com.mphasis.eventledger.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI eventGatewayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Event Gateway API")
                        .version("0.1.0")
                        .description("Public API for validating, tracing, and submitting idempotent financial events."))
                .servers(List.of(new Server()
                        .url("http://localhost:8080")
                        .description("Local Event Gateway")));
    }
}
