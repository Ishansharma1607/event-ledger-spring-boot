package com.mphasis.eventledger.account.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI accountServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Account Service API")
                        .version("0.1.0")
                        .description("Internal API for idempotent account transactions, balances, and transaction history."))
                .servers(List.of(new Server()
                        .url("http://localhost:8081")
                        .description("Local Account Service")));
    }
}
