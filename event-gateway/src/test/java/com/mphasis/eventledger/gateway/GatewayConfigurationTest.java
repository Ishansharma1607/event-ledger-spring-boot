package com.mphasis.eventledger.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GatewayConfigurationTest {

    @Autowired
    private Environment environment;

    @Test
    void gatewayRuntimeConfigurationIsLoaded() {
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("event-gateway");
        assertThat(environment.getProperty("server.port")).isEqualTo("8080");
        assertThat(environment.getProperty("account-service.base-url")).isEqualTo("http://localhost:8081");
        assertThat(environment.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
    }
}
